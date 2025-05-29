import axios from 'axios';

let backendApiUrlFromConfig = null;
let configPromise = null;

export async function loadAppConfig() {
    if (backendApiUrlFromConfig && configPromise && (await configPromise) === backendApiUrlFromConfig) {
        // Already loaded and promise resolved to the same URL, no need to re-fetch
        return backendApiUrlFromConfig;
    }
    if (configPromise && !(await configPromise.catch(() => null))) {
        // Promise exists but might have failed or is still pending
        return configPromise;
    }


    console.log("Attempting to load app config from /app-config");

    configPromise = axios.get('/app-config')
        .then(response => {
            if (response.data && response.data.backendApiUrl) {
                backendApiUrlFromConfig = response.data.backendApiUrl;
                console.log('App config loaded: Backend API URL set to ->', backendApiUrlFromConfig);
            } else {
                console.error('Backend API URL not found in /app-config response. Using a hardcoded fallback for this session.');
                backendApiUrlFromConfig = 'http://localhost:8080/api/fallback-config-error';
            }
            return backendApiUrlFromConfig;
        })
        .catch(error => {
            console.error('Error fetching /app-config:', error.message, '. Using a hardcoded fallback for this session.');
            backendApiUrlFromConfig = 'http://localhost:8080/api/fallback-fetch-error';
            return backendApiUrlFromConfig;
        });

    return configPromise;
}

export function getBackendApiUrl() {
    if (!backendApiUrlFromConfig) {
        console.warn('Backend API URL from config not yet available or loading failed. App may not function correctly until resolved.');
        return 'http://localhost:8080/api/error-url-not-loaded';
    }
    return backendApiUrlFromConfig;
}