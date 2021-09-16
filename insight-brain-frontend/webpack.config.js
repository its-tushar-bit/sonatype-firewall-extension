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
const webpackOutputDir = path.resolve(__dirname, 'target/classes', webpackOutputPath);

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
function config({ entryPath, outputPath, cssOutputPath, env, externals }) {
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
    },
    getCssPlugins = () => [new MiniCssExtractPlugin({ filename: cssOutputPath })],
    productionPlugins = [
      new CopyModulesPlugin({
        destination: path.join('target', 'webpack-modules'),
        includePackageJsons: true,
      }),
    ],
    copyPluginFromGlobs = [
      { from: '**/index.html', transform: true },
      { from: 'version-graph/**/viewdetails.html', transform: true },
      { from: 'version-graph/version-graph.html', transform: true },
      { from: 'version-graph/details.html', transform: true },
      { from: 'version-graph/**/version-graph-*.*', transform: true },
      { from: 'version-graph/**/viewdetails-*.*', transform: true },
      { from: 'cip/cip-claim-component.html', transform: true },
      { from: 'brain.client.js', transform: true },
      { from: 'reports.*', transform: true },
      { from: '**/*.{ttf,woff,png,svg,gif,jpg,ico}', transform: false },
    ],
    plugins = [
      new CopyPlugin({
        patterns: copyPluginFromGlobs.map(({ from, transform }) => ({
          from,
          to: path.join(__dirname, 'target/classes/assets'),
          transform: transform ? transformCopiedFile : undefined,
        })),
      }),
      new webpack.DefinePlugin(buildConstants),
      new StyleLintPlugin({ syntax: 'scss' }),
      new EslintPlugin({
        emitWarning: !production,
        context: __dirname,
        exclude: [
          'node_modules',
          'src/main/frontend/lib',
          'src/main/frontend/audit-report',
          'src/main/frontend/version-graph',
          'src/main/frontend/cip',
        ],
      }),
    ].concat(cssOutputPath ? getCssPlugins() : [], productionPlugins),
    // Babel is used to transpile JSX only. All ES6 syntax is passed on to browsers at this point
    reactLoaderBaseRule = {
      test: /\.jsx$/,
      use: { loader: 'babel-loader' },
    };

  return {
    mode: 'development', // overridden by --mode flag
    context: path.resolve(__dirname, 'src/main/frontend'),
    entry: entryPath,
    output: {
      path: webpackOutputDir,
      publicPath: './',
      filename: outputPath,
    },
    resolve: {
      extensions: ['.js', '.jsx'],

      // sjcl tries to load the node crypto module, don't allow it
      fallback: { crypto: false },
    },
    module: {
      rules: [
        reactLoaderBaseRule,
        {
          test: require.resolve(path.join(__dirname, 'src/main/frontend/lib/protovis/protovis.min')),
          use: {
            loader: 'exports-loader',
            options: {
              exports: 'default pv',
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
    externals,
    devtool: production ? undefined : 'eval-source-map',
    devServer: {
      port: 8070,

      // makes misconfiguration of the backend easier to notice - without this, OSX will allow the backend to run on all
      // interfaces while this runs on just localhost, even if they're on the same port
      host: '0.0.0.0',
      static: {
        directory: path.join(__dirname, 'target', 'classes'),
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
  };
}

module.exports = function (env) {
  env = env || {};

  const brainConfig = config({
      entryPath: './index.js',
      outputPath: 'bundle.js',
      cssOutputPath: 'style.css',
      env,
    }),
    versionGraphConfig = config({
      entryPath: './version-graph/view-details-index.js',
      outputPath: 'viewdetails.js',
      cssOutputPath: 'viewdetails.css',
      env,
    }),
    versionGraphAppConfig = config({
      entryPath: './version-graph/version-graph-app-index.js',
      outputPath: 'version.graph.app.js',
      cssOutputPath: 'version.graph.app.css',
      env,
    }),
    // to be used as the `externals` config on bundles that expect jquery to already be defined.  Prevents
    // loading of multiple copies of jquery
    jqueryExternals = {
      jquery: 'jQuery',
    },
    angularExternals = {
      angular: 'angular',
    };

  if (env.brainOnly) {
    return brainConfig;
  }

  if (env.versionGraphOnly) {
    return [versionGraphConfig, versionGraphAppConfig];
  }

  return [
    brainConfig,
    versionGraphConfig,
    versionGraphAppConfig,
    config({
      entryPath: './audit-report/audit-report-index.js',
      outputPath: 'audit-report.js',
      cssOutputPath: 'audit-report.css',
      env,
      externals: Object.assign({}, jqueryExternals, angularExternals),
    }),
    config({
      entryPath: './cip/cip-loader-index.js',
      outputPath: 'cip-loader.js',
      cssOutputPath: 'cip-loader.css',
      env,
      externals: jqueryExternals,
    }),
    config({
      entryPath: './cip/cip-index.js',
      outputPath: 'cip.js',
      cssOutputPath: 'cip.css',
      env,
      externals: Object.assign({}, jqueryExternals, angularExternals),
    }),
    config({
      entryPath: './audit-report/external-index.js',
      outputPath: 'external.js',
      env,
    }),
  ];
};
