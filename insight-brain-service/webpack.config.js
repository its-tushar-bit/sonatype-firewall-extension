const path = require('path');
const ExtractTextPlugin = require('extract-text-webpack-plugin');
const transformObjectRestSpread = require('babel-plugin-transform-object-rest-spread');
const transformRuntime = require('babel-plugin-transform-runtime');

const webpackOutputPath = 'assets';
const webpackOutputDir = path.resolve(__dirname, 'target/classes', webpackOutputPath);

/**
 * Create a webpack config for the given paths and options
 * @param entryPath path to the javascript entry file for this config, relative to src/main/frontend
 * @param outputPath path to the javascript output file, relative to the assets dir
 * @param cssOutputPath path to the css output file, relative to the assets dir
 * @param production {boolean} whether this is a production build
 * @param externals configuration object to use on the `externals` property
 */
function config({ entryPath, outputPath, cssOutputPath, production, externals }) {
  const extractSass = new ExtractTextPlugin({ filename: cssOutputPath });

  return {
    context: path.resolve(__dirname, 'src/main/frontend'),
    entry: entryPath,
    output: {
      path: webpackOutputDir,
      publicPath: '/assets/',
      filename: outputPath
    },
    module: {
      rules: [{
        test: /\.js$/,
        exclude: /node_modules|src[\/\\]main[\/\\]frontend[\/\\]lib/,
        use: {
          loader: 'babel-loader',
          options: {
            presets: [['env', { modules: false }]],
            plugins: [
              transformObjectRestSpread,
              [transformRuntime, { polyfill: false }]
            ]
          }
        }
      }, {
        test: /\.js$/,
        enforce: 'pre',
        exclude: /node_modules|src[\/\\]main[\/\\]frontend[\/\\](lib|cip|audit-report|version-graph)/,
        use: 'eslint-loader'

      }, {
        test: require.resolve(path.join(__dirname, 'src/main/frontend/lib/protovis/protovis.min')),
        use: 'exports-loader?pv'
      }, {
        test: require.resolve(path.join(__dirname, 'src/main/frontend/lib/Base64')),
        use: 'exports-loader?Base64'
      }, {
        test: /\.html$/,
        use: {
          loader: 'html-loader',
          options: {
            attrs: false
          }
        }
      }, {
        test: /\.s?css$/,
        use: extractSass.extract({
          use: [
            { loader: 'css-loader' },
            { loader: 'resolve-url-loader' },
            {
              loader: 'sass-loader',
              options: {
                sourceMap: true
              }
            }
          ]
        })
      }, {
        test: /\.(png|jpg|jpeg|gif)/,
        loader: 'file-loader',
        options: {
          name: 'images/[name].[ext]'
        }
      }, {
        test: /\.(ttf|eot|woff2?|svg)$/,
        loader: 'file-loader',
        options: {
          name: 'fonts/[name].[ext]'
        }
      }]
    },
    plugins: [
      extractSass
    ],
    externals,
    devtool: production ? undefined : 'eval'
  };
}

module.exports = function(env) {
  env = env || {};

  const brainConfig = config({
        entryPath: './index.js',
        outputPath: 'bundle.js',
        cssOutputPath: 'css/style.css',
        production: env.production
      }),

      // to be used as the `externals` config on bundles that expect jquery to already be defined.  Prevents
      // loading of multiple copies of jquery
      jqueryExternals = {
        'jquery': 'jQuery'
      },
      angularExternals = {
        'angular': 'angular'
      };

  if (env.brainOnly) {
    return brainConfig;
  }
  else {
    return [
      brainConfig,
      config({
        entryPath: './audit-report/audit-report-index.js',
        outputPath: 'audit-report/audit-report.js',
        cssOutputPath: 'audit-report/audit-report.css',
        production: env.production,
        externals: Object.assign({}, jqueryExternals, angularExternals)
      }),
      config({
        entryPath: './cip/cip-loader-index.js',
        outputPath: 'policy/js/cip-loader.js',
        cssOutputPath: 'policy/css/cip-loader.css',
        production: env.production,
        externals: jqueryExternals
      }),
      config({
        entryPath: './cip/cip-index.js',
        outputPath: 'cip/cip.js',
        cssOutputPath: 'cip/cip.css',
        production: env.production,
        externals: Object.assign({}, jqueryExternals, angularExternals)
      }),
      config({
        entryPath: './audit-report/external-index.js',
        outputPath: 'assets/js/external.js',
        production: env.production
      }),
      config({
        entryPath: './version-graph/version-graph-app-index.js',
        outputPath: 'version-graph/version.graph.app.js',
        cssOutputPath: 'version-graph/version.graph.app.css',
        production: env.production
      }),
      config({
        entryPath: './version-graph/view-details-index.js',
        outputPath: 'version-graph/viewdetails.js',
        cssOutputPath: 'version-graph/viewdetails.css',
        production: env.production
      })
    ];
  }
};
