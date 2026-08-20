import { defineConfig, Plugin } from "vite";
import react from "@vitejs/plugin-react";
import { crx } from "@crxjs/vite-plugin";
import { build as esbuild } from "esbuild";
import { resolve } from "path";
import manifest from "./src/manifest.json" with { type: "json" };

// Content scripts must run as classic scripts (chrome.scripting.registerContentScripts
// injects them without "type=module"). Rollup's default ESM output emits `import`
// statements at the top of the file, which fails at runtime. Bundle each content
// script into a self-contained IIFE with esbuild after Vite finishes.
function contentScriptsAsIife(): Plugin {
  return {
    name: "hexawatch-content-scripts-iife",
    apply: "build",
    async closeBundle() {
      const outDir = resolve(__dirname, "dist/src/content");
      const entries: Record<string, string> = {
        maven: resolve(__dirname, "src/content/maven.ts"),
        npm: resolve(__dirname, "src/content/npm.ts"),
        pypi: resolve(__dirname, "src/content/pypi.ts"),
      };
      for (const [name, entry] of Object.entries(entries)) {
        await esbuild({
          entryPoints: [entry],
          bundle: true,
          format: "iife",
          platform: "browser",
          target: "chrome110",
          outfile: `${outDir}/${name}.js`,
          logLevel: "info",
        });
      }
    },
  };
}

export default defineConfig({
  plugins: [react(), crx({ manifest }), contentScriptsAsIife()],
  build: {
    outDir: "dist",
    emptyOutDir: true,
    rollupOptions: {
      input: {
        popup: "src/popup/index.html",
        options: "src/options/index.html",
        waiver: "src/waiver/index.html",
      },
    },
  },
});
