import { defineConfig } from "vite";
import react from "@vitejs/plugin-react-swc";
import path from "path";
import { componentTagger } from "lovable-tagger";

// https://vitejs.dev/config/
export default defineConfig(({ mode }) => ({
  server: {
    host: "::",
    port: 8090,
    hmr: {
      overlay: false,
    },
  },
  plugins: [react(), mode === "development" && componentTagger()].filter(Boolean),
  resolve: {
    alias: {
      "@": path.resolve(__dirname, "./src"),
    },
  },
  esbuild: {
    // Strip debug logging (tokens, payloads) from production bundles; console.error/warn are kept
    pure: mode === "production" ? ["console.log", "console.debug", "console.info"] : [],
  },
  define: {
    // Polyfill global for sockjs-client (used by @stomp/stompjs)
    global: 'globalThis',
  },
}));
