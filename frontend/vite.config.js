import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'

// https://vitejs.dev/config/
export default defineConfig({
  plugins: [react()],
  server: { // Settings for Vite's dev server (npm run dev)
    port: 5173, // Default Vite dev port, or choose another like 3000
    strictPort: true,
  }
})