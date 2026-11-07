import { defineConfig } from 'vite';
import react from '@vitejs/plugin-react';

// In sviluppo la SPA gira su :5173 e inoltra /api a Spring Boot (:8080):
// browser e API restano sullo stesso origin, quindi niente CORS e cookie di sessione normali.
export default defineConfig({
  plugins: [react()],
  server: {
    port: 5173,
    proxy: {
      '/api': 'http://localhost:8080',
    },
  },
  build: {
    outDir: 'dist',
    emptyOutDir: true,
  },
});
