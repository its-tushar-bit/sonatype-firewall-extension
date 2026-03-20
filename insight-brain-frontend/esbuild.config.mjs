/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import * as esbuild from 'esbuild';
import { sassPlugin } from 'esbuild-sass-plugin';
import * as fs from 'fs';
import * as path from 'path';
import { fileURLToPath } from 'url';
import { DOMParser } from 'xmldom';
import { createServer } from 'http';
import { request as httpRequest } from 'http';

const __dirname = path.dirname(fileURLToPath(import.meta.url));
const srcDir = path.resolve(__dirname, 'src/main/frontend');
const outDir = path.resolve(__dirname, 'target/generated-resources/webpack/assets');

// Parse CLI args
const args = process.argv.slice(2);
const production = args.includes('--production');
const serve = args.includes('--serve');

// Extract version from pom.xml
function extractFromPom(nodeName) {
  const doc = new DOMParser().parseFromString(fs.readFileSync('pom.xml', 'utf-8'));
  const node = doc.documentElement.getElementsByTagName(nodeName)[0];
  return node.firstChild.nodeValue;
}

const buildTimestamp = new Date().getTime();
const serverVersion = JSON.stringify(extractFromPom('version'));

// Bundle configurations
const allBundles = [
  { name: 'bundle', entry: './index.jsx' },
  { name: 'viewdetails-react', entry: './version-graph/viewdetails-react/index.jsx' },
  { name: 'version-graph-react', entry: './version-graph/version-graph-react/index.jsx' },
];

const activeBundles = allBundles;

// Build constants — used by esbuild's define (JS substitution) and by
// copyStaticFiles for HTML template replacement.
const defineConfig = {
  CLM_BUILD_TIMESTAMP: String(buildTimestamp),
  CLM_SERVER_VERSION: serverVersion,
  'process.env.NODE_ENV': JSON.stringify(production ? 'production' : 'development'),
};

// Copy static files (HTML with template replacement, images, fonts, etc.)
// Async so it can overlap with esbuild bundling; internally sequential to
// avoid counter-productive disk contention on small files.
async function copyStaticFiles() {
  const copyGlobs = [
    { pattern: /index\.html$/, transform: true },
    { pattern: /viewdetails\.html$/, transform: true },
    { pattern: /reports\.(html|js)$/, transform: false },
    { pattern: /\.(ttf|woff|woff2|png|svg|gif|jpg|ico)$/, transform: false },
  ];

  async function walkDir(dir, baseDir) {
    const entries = await fs.promises.readdir(dir, { withFileTypes: true });
    for (const entry of entries) {
      const fullPath = path.join(dir, entry.name);
      if (entry.isDirectory()) {
        // Skip node_modules, target, etc.
        if (entry.name === 'node_modules' || entry.name === 'target') continue;
        await walkDir(fullPath, baseDir);
      } else {
        const relativePath = path.relative(baseDir, fullPath);
        for (const glob of copyGlobs) {
          if (glob.pattern.test(entry.name) || glob.pattern.test(relativePath)) {
            const destPath = path.join(outDir, relativePath);
            await fs.promises.mkdir(path.dirname(destPath), { recursive: true });
            if (glob.transform) {
              let content = await fs.promises.readFile(fullPath, 'utf-8');
              for (const [key, value] of Object.entries(defineConfig)) {
                content = content.replaceAll(key, value);
              }
              await fs.promises.writeFile(destPath, content);
            } else {
              await fs.promises.copyFile(fullPath, destPath);
            }
            break;
          }
        }
      }
    }
  }

  await walkDir(srcDir, srcDir);

  // Copy fontawesome-webfont.ttf to fonts/ — PdfGenerator loads it from this
  // classpath location (assets/fonts/fontawesome-webfont.ttf).  esbuild's file
  // loader places node_modules assets under a deep _.._/ path instead.
  const faSource = path.resolve(__dirname, 'node_modules/components-font-awesome/fonts/fontawesome-webfont.ttf');
  const faDest = path.join(outDir, 'fonts/fontawesome-webfont.ttf');
  await fs.promises.copyFile(faSource, faDest);
}

// Rewrite relative url() paths to absolute before sass merges files together.
// After sass compilation, all url() references resolve against the entry file's
// directory, losing the original source file context.  The absolute filesystem
// paths injected here are only intermediary - esbuild's file loader then picks
// them up, copies the referenced assets to the output directory, and replaces
// these paths with the correct relative output URLs.  This is the esbuild-sass-plugin
// documented solution (equivalent to webpack's resolve-url-loader).
const sassPrecompile = (source, pathname) => {
  // url() takes a URL, which always uses forward slashes as path delimiters.
  // On Windows, backslashes would additionally be consumed as CSS escape
  // characters by the SASS compiler (e.g. D:\foo → D:/foo).
  const basedir = path.dirname(pathname).replaceAll('\\', '/');
  return source.replace(/(url\(\s*['"]?)(\.\.?\/)([^'")]+['"]?\s*\))/g, `$1${basedir}/$2$3`);
};

// CopyModulesPlugin equivalent - copies source files of all bundled modules to target/webpack-modules
async function copyModules(metafile) {
  if (!production || !metafile) return;

  const destDir = path.resolve(__dirname, 'target', 'webpack-modules');
  const cwd = process.cwd();

  const copiedPkgJsons = new Set();

  for (const rawInputPath of Object.keys(metafile.inputs)) {
    // Strip query strings and fragment identifiers (e.g. ?v=4.7.0, ?#iefix, #fragment)
    const inputPath = rawInputPath.replace(/[?#].*$/, '');
    const absPath = path.resolve(inputPath);
    const relativePath = path.relative(cwd, absPath).replace(/\.\.\//g, '__..__/');
    const destPath = path.join(destDir, relativePath);

    try {
      await fs.promises.mkdir(path.dirname(destPath), { recursive: true });
      await fs.promises.copyFile(absPath, destPath);

      // Copy package.json if in node_modules
      if (inputPath.includes('node_modules')) {
        const parts = inputPath.split('node_modules/');
        const modulePart = parts[parts.length - 1];
        const moduleName = modulePart.startsWith('@')
          ? modulePart.split('/').slice(0, 2).join('/')
          : modulePart.split('/')[0];

        if (!copiedPkgJsons.has(moduleName)) {
          const pkgJsonPath = path.resolve('node_modules', moduleName, 'package.json');
          const destPkgJson = path.join(destDir, 'node_modules', moduleName, 'package.json');
          try {
            await fs.promises.access(pkgJsonPath);
            await fs.promises.mkdir(path.dirname(destPkgJson), { recursive: true });
            await fs.promises.copyFile(pkgJsonPath, destPkgJson);
          } catch {
            // package.json doesn't exist for this module
          }
          copiedPkgJsons.add(moduleName);
        }
      }
    } catch (err) {
      console.warn(`Warning: could not copy ${inputPath}: ${err.message}`);
    }
  }
}

// Shared sass configuration — used by both build and dev server.
// importMapper is called by the sass plugin's canonicalize hook for all
// directives (@import, @use, @forward), making it the right place to
// remap custom path prefixes.
const sassImportMapper = (importPath) => {
  // Return native OS paths (backslashes on Windows) — the plugin's resolveImport
  // uses path.sep to locate the last separator when prepending '_' for SASS partials.
  // Forward-slash paths cause that logic to fail on Windows.
  //
  // On Windows, Dart Sass resolves bare @use paths relative to the containing
  // file's canonical file:// URL, then passes the result through canonicalize.
  // By the time importMapper sees it, path separators are backslashes
  // (e.g. "…\waivers\MainRoot\scss\mixins"), so we must match both / and \.
  if (importPath.startsWith('~')) {
    return path.resolve(__dirname, 'node_modules', importPath.slice(1));
  }
  const mainRootMatch = importPath.match(/MainRoot[/\\]/);
  if (mainRootMatch) {
    return path.resolve(srcDir, importPath.substring(mainRootMatch.index + 'MainRoot/'.length));
  }
  const nodeModulesMatch = importPath.match(/node_modules[/\\]/);
  if (nodeModulesMatch) {
    return path.resolve(__dirname, importPath.substring(nodeModulesMatch.index));
  }
  return importPath;
};

const sharedBuildOptions = {
  bundle: true,
  outdir: outDir,
  assetNames: '[dir]/[name]',
  target: ['es2020'],
  platform: 'browser',
  // Explicit mainFields ensures the 'module' entry is preferred even for
  // require() calls.  Without this, esbuild deprioritizes 'module' for CJS
  // require(), causing d3 v4 (main: Node build, module: browser build) to
  // pull in its Node-only code.
  mainFields: ['browser', 'module', 'main'],
  format: 'iife',
  jsx: 'automatic',
  jsxImportSource: 'react',
  loader: {
    '.js': 'jsx',
    '.jsx': 'jsx',
    '.png': 'file',
    '.jpg': 'file',
    '.jpeg': 'file',
    '.gif': 'file',
    '.svg': 'file',
    '.ttf': 'file',
    '.eot': 'file',
    '.woff': 'file',
    '.woff2': 'file',
    '.css': 'css',
  },
  define: defineConfig,
  alias: {
    MainRoot: srcDir,
    TestRoot: path.resolve(__dirname, 'src/test/frontend'),
  },
  plugins: [
    sassPlugin({
      type: 'css',
      implementation: 'sass-embedded',
      importMapper: sassImportMapper,
      precompile: sassPrecompile,
      silenceDeprecations: ['import', 'global-builtin', 'color-functions', 'slash-div', 'function-units'],
    }),
  ],
  logLevel: 'info',
  logOverride: {
    'css-syntax-error': 'silent',
  },
};

// Build each bundle
async function buildAll() {
  console.log(`Building ${activeBundles.map(b => b.name).join(', ')} in ${production ? 'production' : 'development'} mode…`);

  fs.mkdirSync(outDir, { recursive: true });

  // Start static file copying (IO-bound) in parallel with esbuild bundling (CPU-bound)
  console.log('Copying static files…');
  const copyPromise = copyStaticFiles();

  const results = [];

  for (const bundle of activeBundles) {
    console.log(`  Building ${bundle.name}…`);

    const result = await esbuild.build({
      ...sharedBuildOptions,
      entryPoints: [path.resolve(srcDir, bundle.entry)],
      entryNames: bundle.name,
      metafile: true,
      minify: production,
      sourcemap: production ? false : 'inline',
    });

    results.push(result);
  }

  // CopyModulesPlugin equivalent (production only)
  if (production && results.length > 0) {
    console.log('Copying module sources…');
    for (const result of results) {
      await copyModules(result.metafile);
    }
  }

  // Ensure static file copying has finished
  await copyPromise;

  console.log('Build complete!');
}

// Dev server with proxy
async function startDevServer() {
  console.log('Starting dev server on port 8070…');

  fs.mkdirSync(outDir, { recursive: true });
  await copyStaticFiles();

  const contexts = [];

  for (const bundle of activeBundles) {
    const ctx = await esbuild.context({
      ...sharedBuildOptions,
      entryPoints: [path.resolve(srcDir, bundle.entry)],
      entryNames: bundle.name,
      sourcemap: 'inline',
    });

    await ctx.watch();
    contexts.push(ctx);
  }

  // Use the first context's serve for static file serving (servedir covers
  // all bundles' outputs since they all write to outDir).
  const { host, port: esbuildPort } = await contexts[0].serve({
    servedir: outDir,
    fallback: path.join(outDir, 'index.html'),
  });

  // Thin proxy: API paths go to the backend, everything else to esbuild's serve.
  const proxyPaths = ['/rest', '/api', '/ui', '/policy-assets', '/saml'];
  const backendTarget = 'http://localhost:8072';

  const server = createServer((req, res) => {
    const target = proxyPaths.some(p => req.url.startsWith(p))
      ? backendTarget
      : `http://${host}:${esbuildPort}`;

    const proxyReq = httpRequest(
      `${target}${req.url}`,
      { method: req.method, headers: req.headers },
      (proxyRes) => {
        res.writeHead(proxyRes.statusCode, proxyRes.headers);
        proxyRes.pipe(res);
      }
    );
    proxyReq.on('error', () => {
      res.writeHead(502);
      res.end('Backend unavailable');
    });
    req.pipe(proxyReq);
  });

  server.listen(8070, '0.0.0.0', () => {
    console.log('Dev server listening on http://localhost:8070');
  });

  process.on('SIGINT', async () => {
    console.log('\nShutting down…');
    for (const ctx of contexts) {
      await ctx.dispose();
    }
    server.close();
    process.exit(0);
  });
}

// Main
if (serve) {
  startDevServer().catch((err) => {
    console.error('Dev server failed:', err);
    process.exit(1);
  });
} else {
  buildAll().catch((err) => {
    console.error('Build failed:', err);
    process.exit(1);
  });
}
