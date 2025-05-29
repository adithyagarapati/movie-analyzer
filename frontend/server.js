import express from 'express';
import path from 'path';
import { fileURLToPath } from 'url';

const app = express();
const PORT = parseInt(process.env.PORT || '3000', 10);

const ACTUAL_BACKEND_API_URL = process.env.BACKEND_API_URL || 'http://localhost:8080';

let frontendConfigServiceEnabled = true;
let frontendSimulatedHealth = 'OK';

const __filename = fileURLToPath(import.meta.url);
const __dirname = path.dirname(__filename);

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

app.get('/app-config', (req, res) => {
    if (!frontendConfigServiceEnabled) {
        console.log('Serving /app-config - FAILED (service disabled for demo)');
        return res.status(503).json({ error: 'Frontend configuration service is temporarily disabled for demo.' });
    }
    console.log('Serving /app-config with backend URL:', ACTUAL_BACKEND_API_URL);
    return res.json({ backendApiUrl: ACTUAL_BACKEND_API_URL });
});

app.get('/livez', (req, res) => {
    res.status(200).send('Frontend server is live');
});

app.get('/readyz', (req, res) => {
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

const buildPath = path.join(__dirname, 'dist');
app.use(express.static(buildPath));
console.log(`Serving static files from ${buildPath}`);

app.get('*', (req, res) => {
    const nonSpaPaths = ['/app-config', '/livez', '/readyz'];
    if (req.path.startsWith('/debug/') || nonSpaPaths.includes(req.path)) {
        // Let Express handle it or 404 if no specific route matched
        // This prevents SPA fallback for API/debug routes.
        // If no other route handles it, Express sends 404 by default.
        return;
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
    console.log(`Actual Backend API URL (from env): ${ACTUAL_BACKEND_API_URL}`);
    console.log(`Frontend /app-config service initially: ${frontendConfigServiceEnabled ? 'ENABLED' : 'DISABLED'}`);
    console.log(`Frontend simulated health initially: ${frontendSimulatedHealth}`);
});