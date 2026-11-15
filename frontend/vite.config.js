import { defineConfig } from 'vite';

export default defineConfig({
  server: { proxy: { '/payments': process.env.API_PROXY_TARGET || 'http://localhost:8085', '/orders': process.env.API_PROXY_TARGET || 'http://localhost:8085' } },
});
