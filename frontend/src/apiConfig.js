// movie-review-frontend/src/apiConfig.js
import axios from 'axios';

let apiBasePathFromConfig = null;
let configPromise = null;

export async function loadAppConfig() {
    if (apiBasePathFromConfig && configPromise && (await configPromise.catch(() => null)) === apiBasePathFromConfig) {
        return apiBasePathFromConfig;
    }
    if (configPromise && !(await configPromise.catch(() => null))) {
        return configPromise;
    }

    console.log("Attempting to load app config from /app-config");
    configPromise = axios.get('/app-config') // Fetches from its own frontend server
        .then(response => {
            if (response.data && response.data.apiBasePath) {
                apiBasePathFromConfig = response.data.apiBasePath;
                console.log('App config loaded: API Base Path set to ->', apiBasePathFromConfig);
            } else {
                console.error('apiBasePath not found in /app-config response. Defaulting to /api.');
                apiBasePathFromConfig = '/api'; // Default if something goes wrong with response structure
            }
            return apiBasePathFromConfig;
        })
        .catch(error => {
            console.error('Error fetching /app-config:', error.message, '. Defaulting API Base Path to /api.');
            apiBasePathFromConfig = '/api'; // Default on network error or other fetch issues
            return apiBasePathFromConfig;
        });
    return configPromise;
}

export function getApiBasePath() {
    if (!apiBasePathFromConfig) {
        console.warn('API Base Path from config not yet available or loading failed. App calls may fail or use a default.');
        // This default is a fallback if loadAppConfig hasn't completed or failed badly
        return '/api';
    }
    return apiBasePathFromConfig;
}