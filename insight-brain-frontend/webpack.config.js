/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
const webpack = require('webpack');
const path = require('path');
const fs = require('fs');
const StyleLintPlugin = require('stylelint-webpack-plugin');
const MiniCssExtractPlugin = require('mini-css-extract-plugin');
const DOMParser = require('xmldom').DOMParser;
const EslintPlugin = require('eslint-webpack-plugin');

const CopyPlugin = require('copy-webpack-plugin');
const CopyModulesPlugin = require('copy-modules-webpack-plugin');

const webpackOutputPath = 'assets';
const webpackOutputDir = path.resolve(__dirname, 'target/generated-resources/webpack', webpackOutputPath);

function extractFromPom(nodeName) {
  const doc = new DOMParser().parseFromString(fs.readFileSync('pom.xml', 'utf-8'));
  const node = doc.documentElement.getElementsByTagName(nodeName)[0];
  return node.firstChild.nodeValue;
}

/**
 * Create a webpack config for the given paths and options
 * @param entryPath path to the javascript entry file for this config, relative to src/main/frontend
 * @param outputPath path to the javascript output file, relative to the assets dir
 * @param cssOutputPath path to the css output file, relative to the assets dir
 * @param env webpack environment object, expected to contain 'production' property
 * @param externals configuration object to use on the `externals` property
 */
function config({ entryPath, outputPath, cssOutputPath, env, externals, es5 = false }) {}

module.exports = function (env) {
  env = env || {};

  // Specifications for each bundle that this build produces
  const bundleConfigs = [
    {
      name: 'bundle',
      entryPath: './index.jsx',
      outputPath: 'bundle.js',
      cssOutputPath: 'style.css',
    },
    {
      name: 'viewdetails-react',
      entryPath: './version-graph/viewdetails-react/index.jsx',
      outputPath: 'viewdetails-react.js',
      cssOutputPath: 'viewdetails-react.css',
    },
    {
      name: 'version-graph-react',
      entryPath: './version-graph/version-graph-react/index.jsx',
      outputPath: 'version-graph-react.js',
      cssOutputPath: 'version-graph-react.css',
    },
  ];

  // Determine which bundles to include based on environment flags
  let activeBundles = [];
  if (env.brainOnly) {
    activeBundles = [bundleConfigs[0]]; // Only brain bundle
  } else if (env.versionGraphOnly) {
    activeBundles = bundleConfigs.slice(1); // All except brain bundle
  } else {
    activeBundles = bundleConfigs; // All bundles
  }

  // Create a combined multi-entry configuration
  const entryMap = Object.fromEntries(activeBundles.map((bundle) => [bundle.name, bundle.entryPath]));
  const cssMap = Object.fromEntries(activeBundles.map((bundle) => [bundle.name, bundle.cssOutputPath]));

  function transformCopiedFile(content) {
    let contentStr = content.toString();

    for (let key in buildConstants) {
      contentStr = contentStr.replace(new RegExp(key, 'g'), buildConstants[key]);
    }

    return Buffer.from(contentStr);
  }

  const production = env.production,
    buildConstants = {
      CLM_BUILD_TIMESTAMP: new Date().getTime(),
      CLM_SERVER_VERSION: JSON.stringify(extractFromPom('version')),
      'process.env.NODE_ENV': JSON.stringify(production ? 'production' : 'development'),
    },
    copyPluginFromGlobs = [
      { from: '**/index.html', transform: true },
      { from: 'version-graph/**/viewdetails.html', transform: true },
      { from: 'reports.*', transform: true },
      { from: '**/*.{ttf,woff,png,svg,gif,jpg,ico}', transform: false },
    ],
    productionPlugins = [
      new CopyModulesPlugin({
        destination: path.join('target', 'webpack-modules'),
        includePackageJsons: true,
      }),
    ],
    plugins = [
      new CopyPlugin({
        patterns: copyPluginFromGlobs.map(({ from, transform }) => ({
          from,
          to: path.join(__dirname, 'target/generated-resources/webpack/assets'),
          transform: transform ? transformCopiedFile : undefined,
        })),
      }),
      new webpack.DefinePlugin(buildConstants),
      new webpack.ProvidePlugin({
        Buffer: ['buffer', 'Buffer'],
      }),
      new StyleLintPlugin({ syntax: 'scss' }),
      new EslintPlugin({
        emitWarning: !production,
        context: __dirname,
        exclude: ['node_modules', 'src/main/frontend/lib', 'src/main/frontend/version-graph'],
      }),
      new MiniCssExtractPlugin({
        filename: ({ chunk }) => {
          return cssMap[chunk.name] || '[name].css';
        },
      }),
    ].concat(production ? productionPlugins : []);

  // Create the final configuration
  const config = {
    mode: 'development', // overridden by --mode flag
    target: ['web'],
    context: path.resolve(__dirname, 'src/main/frontend'),
    entry: entryMap,
    output: {
      path: webpackOutputDir,
      publicPath: production ? './' : '/assets/',
      filename: '[name].js',
    },
    resolve: {
      extensions: ['.js', '.jsx'],

      // sjcl tries to load the node crypto module, don't allow it
      fallback: { crypto: false },
      alias: {
        MainRoot: path.resolve(__dirname, 'src/main/frontend'),
        TestRoot: path.resolve(__dirname, 'src/test/frontend'),
      },
    },
    module: {
      rules: [
        {
          test: /\.jsx$/,
          use: {
            loader: 'babel-loader',
            options: {
              presets: ['@babel/preset-react'],
            },
          },
        },
        {
          test: require.resolve(path.join(__dirname, 'src/main/frontend/lib/Base64')),
          use: {
            loader: 'exports-loader',
            options: {
              exports: 'default Base64',
            },
          },
        },
        {
          test: /\.html$/,
          use: {
            loader: 'html-loader',
            options: {
              sources: false,
            },
          },
        },
        {
          test: /\.s?css$/,
          use: [
            { loader: MiniCssExtractPlugin.loader },
            { loader: 'css-loader' },
            {
              loader: 'resolve-url-loader',
            },
            {
              loader: 'sass-loader',
              options: {
                implementation: require('sass'),
                sourceMap: true,
              },
            },
          ],
        },
        {
          test: /\.(png|jpg|jpeg|gif)/,
          type: 'asset/resource',
          generator: {
            filename: 'images/[name][ext]',
          },
        },
        {
          test: /\.(ttf|eot|woff2?|svg)$/,
          type: 'asset/resource',
          generator: {
            filename: 'fonts/[name][ext]',
          },
        },
      ],
    },
    plugins: plugins,
    devtool: production ? undefined : 'eval-source-map',
    devServer: {
      port: 8070,

      // makes misconfiguration of the backend easier to notice - without this, OSX will allow the backend to run on all
      // interfaces while this runs on just localhost, even if they're on the same port
      host: '0.0.0.0',
      allowedHosts: ['localhost', '.localdomain', '.nexus.local', 'host.testcontainers.internal'],
      static: {
        directory: path.join(__dirname, 'target', 'classes'),
        publicPath: '/',
        serveIndex: true,
        watch: true,
      },
      proxy: [
        {
          context: ['/rest', '/api', '/ui', '/policy-assets', '/saml'],
          target: 'http://localhost:8072/',
        },
      ],
    },
    name: 'insight-brain-frontend',
  };

  return config;
};
