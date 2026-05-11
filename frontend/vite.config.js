import { defineConfig } from 'vite';
import vue from '@vitejs/plugin-vue';

// Vite configuration that proxies API calls during development to the Spring Boot backend
export default defineConfig({
  plugins: [vue()],
  server: {
    proxy: {
      '/api': {
        target: 'http://localhost:8080',
        changeOrigin: true
      }
    }
  }
});