/// <reference types='vitest' />

import { dirname } from "node:path";
import { fileURLToPath } from "node:url";
import tailwindcss from "@tailwindcss/vite";
import react from "@vitejs/plugin-react";
import { defineConfig } from "vite";
import tsconfigPaths from "vite-tsconfig-paths";

const __dirname = dirname(fileURLToPath(import.meta.url));

export default defineConfig({
  root: __dirname,
  cacheDir: "../../node_modules/.vite/apps/hq",

  server: {
    port: 4201,
    host: "localhost",
  },

  preview: {
    port: 4301,
    host: "localhost",
  },

  plugins: [react(), tailwindcss(), tsconfigPaths()],

  build: {
    outDir: "../../dist/apps/hq",
    reportCompressedSize: true,
    commonjsOptions: {
      transformMixedEsModules: true,
    },
  },

  test: {
    globals: true,
    cache: {
      dir: "../../node_modules/.vitest",
    },
    environment: "jsdom",
    include: ["src/**/*.{test,spec}.{js,mjs,cjs,ts,mts,cts,jsx,tsx}"],
    reporters: ["default"],
    coverage: {
      reportsDirectory: "../../coverage/apps/hq",
      provider: "v8",
    },
  },
});
