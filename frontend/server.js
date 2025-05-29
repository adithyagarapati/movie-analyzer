import express from 'express';
import path from 'path';
import { fileURLToPath } from 'url';
import { createProxyMiddleware, fixRequestBody } from 'http-proxy-middleware';

const app = express();
const PORT = parseInt(process.env.PORT || '3000', 10);

// This URL is for this Node.js server to talk to the backend container.
// It uses the Docker internal DNS name.
const INTERNAL_BACKEND_API_TARGET = process.env.INTERNAL_BACKEND_API_URL || 'http://localhost:8081/error-target-not-set'; // Fallback to a non-existent local port for safety

// The React app will make calls to /api/* which will be proxied
const PROXY_PATH_PREFIX = '/api'; // All backend calls from React will start with this

// Frontend Server In-Memory State for Debug/Demo
let frontendConfigServiceEnabled = true;
let frontendSimulatedHealth = 'OK';

const __filename = fileURLToPath(import.meta.url);
const __dirname = path.dirname(__filename);

// --- Debug API Endpoints for Frontend Server ---
app.post('/debug/frontend/config-service/enable', (req, res) => {
    frontendConfigServiceEnabled = true;
    console.log('DEBUG: Frontend /app-config service ENABLED');
    res.status(200).json({ status: 'Frontend /app-config service enabled' });
});

app.post('/debug/frontend/config-service/disable', (req, res) => {
    frontendConfigServiceEnabled = false;
    console.log('DEBUG: Frontend /app-config service DISABLED');
    res.status(200).json({ status: 'Frontend /app-config service disabled' });
});

app.post('/debug/frontend/health/set-ok', (req, res) => {
    frontendSimulatedHealth = 'OK';
    console.log('DEBUG: Frontend simulated health set to OK');
    res.status(200).json({ status: 'Frontend simulated health set to OK' });
});

app.post('/debug/frontend/health/set-unhealthy', (req, res) => {
    frontendSimulatedHealth = 'UNHEALTHY';
    console.log('DEBUG: Frontend simulated health set to UNHEALTHY');
    res.status(200).json({ status: 'Frontend simulated health set to UNHEALTHY' });
});


// --- API endpoint to provide runtime configuration to the client ---
// The client now needs to know the base path for proxied API calls
app.get('/app-config', (req, res) => {
    if (!frontendConfigServiceEnabled) {
        console.log('Serving /app-config - FAILED (service disabled for demo)');
        return res.status(503).json({ error: 'Frontend configuration service is temporarily disabled.' });
    }
    console.log('Serving /app-config with apiBasePath:', PROXY_PATH_PREFIX);
    // The React app will prepend this to its calls, e.g., /api/reviews_history
    return res.json({ apiBasePath: PROXY_PATH_PREFIX });
});

// --- Kubernetes Health Probe Endpoints ---
app.get('/livez', (req, res) => {
    console.trace("Liveness probe received for frontend server");
    res.status(200).send('Frontend server is live');
});

app.get('/readyz', (req, res) => {
    console.trace("Readiness probe received for frontend server");
    if (frontendSimulatedHealth === 'UNHEALTHY') {
        console.log('READYNESS PROBE: Frontend is UNHEALTHY (simulated)');
        return res.status(503).send('Frontend server is not ready (simulated unhealthy)');
    }
    if (!frontendConfigServiceEnabled) {
        console.log('READYNESS PROBE: Frontend is NOT READY (/app-config disabled)');
        return res.status(503).send('Frontend server is not ready (/app-config service disabled)');
    }
    console.log('READYNESS PROBE: Frontend is READY');
    return res.status(200).send('Frontend server is ready');
});


// --- Proxy API requests to the backend ---
// All requests to /api/* will be proxied to the INTERNAL_BACKEND_API_TARGET
app.use(PROXY_PATH_PREFIX, createProxyMiddleware({
    target: INTERNAL_BACKEND_API_TARGET,
    changeOrigin: true, // Important for virtual hosted sites, good practice
    pathRewrite: (path, req) => {
        // Rewrites /api/reviews_history/Animal to /reviews_history/Animal before forwarding
        const newPath = path.startsWith(PROXY_PATH_PREFIX) ? path.substring(PROXY_PATH_PREFIX.length) : path;
        return newPath;
    },
    onProxyReq: (proxyReq, req, res) => {
        // Fix for proxied POST/PUT requests with body-parser or express.json()
        // See: https://github.com/chimurai/http-proxy-middleware/issues/40
        // And: https://github.com/chimurai/http-proxy-middleware/blob/master/src/plugins/fix-request-body/README.md
        if (req.body) {
            let bodyData = JSON.stringify(req.body);
            // In case if content-type is application/x-www-form-urlencoded -> we need to change to application/json
            proxyReq.setHeader('Content-Type','application/json');
            proxyReq.setHeader('Content-Length', Buffer.byteLength(bodyData));
            // Write body data to proxy request
            proxyReq.write(bodyData);
        }
        console.log(`[Proxy Req] ${req.method} ${req.originalUrl} -> ${INTERNAL_BACKEND_API_TARGET}${proxyReq.path}`);
    },
    onProxyRes: (proxyRes, req, res) => {
        console.log(`[Proxy Res] ${req.method} ${req.originalUrl} <- ${proxyRes.statusCode} ${INTERNAL_BACKEND_API_TARGET}${req.url}`);
    },
    onError: (err, req, res) => {
        console.error('[Proxy Err] Error proxying request:', err.message, 'for', req.originalUrl, 'to', INTERNAL_BACKEND_API_TARGET);
        if (!res.headersSent) {
            res.writeHead(502, { 'Content-Type': 'application/json' }); // Bad Gateway
        }
        res.end(JSON.stringify({
            error: 'Proxy Error',
            message: 'Could not connect to the backend service.',
            details: err.message
        }));
    }
}));


// --- Serve static files from the React build ---
const buildPath = path.join(__dirname, 'dist');
app.use(express.static(buildPath));
console.log(`Serving static files from ${buildPath}`);

// --- SPA Fallback: Serve index.html for any other requests that don't match static files ---
app.get('*', (req, res) => {
    // List of paths that should NOT be handled by SPA fallback
    const apiAndDebugPaths = ['/app-config', '/livez', '/readyz'];
    if (req.path.startsWith('/debug/') || req.path.startsWith(PROXY_PATH_PREFIX) || apiAndDebugPaths.includes(req.path)) {
        // If it's an API or debug path not handled by a specific route above,
        // let Express default to 404 eventually. Do not send index.html.
        if (!res.headersSent) { // Check if proxy or other middleware already sent response
             // If this path wasn't handled by proxy or specific GET routes, it's likely a 404 for an API path
            console.log(`Path ${req.path} not handled by SPA fallback, allowing 404 or other handlers.`);
        }
        return; // Important: Do not proceed to sendFile for these paths
    }

    const indexPath = path.join(buildPath, 'index.html');
    console.log(`SPA fallback: serving ${indexPath} for ${req.path}`);
    res.sendFile(indexPath, (err) => {
        if (err) {
            console.error(`Error sending index.html for ${req.path}: ${err.message}`);
            if (!res.headersSent) {
                res.status(500).send('Error serving application.');
            }
        }
    });
});

app.listen(PORT, '0.0.0.0', () => {
    console.log(`Frontend server listening on port ${PORT}`);
    console.log(`Proxying API requests starting with ${PROXY_PATH_PREFIX} to internal backend: ${INTERNAL_BACKEND_API_TARGET}`);
    console.log(`Frontend /app-config service initially: ${frontendConfigServiceEnabled ? 'ENABLED' : 'DISABLED'}`);
    console.log(`Frontend simulated health initially: ${frontendSimulatedHealth}`);
});