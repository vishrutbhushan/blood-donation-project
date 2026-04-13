import { defineConfig } from 'vite';

export default defineConfig({
  server: {
    host: true,
    port: 3001,
    proxy: {
      '/api/backend': {
        target: 'http://localhost:8080',
        changeOrigin: true,
        rewrite: (path) => path.replace(/^\/api\/backend/, ''),
      },
      '/api/who': {
        target: 'http://localhost:8082',
        changeOrigin: true,
      },
      '/api/redcross': {
        target: 'http://localhost:8081',
        changeOrigin: true,
      },
    },
  },
});
