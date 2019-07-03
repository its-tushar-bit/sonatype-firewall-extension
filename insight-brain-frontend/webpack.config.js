const webpack = require('webpack');
const path = require('path');
const ExtractTextPlugin = require('extract-text-webpack-plugin');
const CSSSplitPlugin = require('css-split-webpack-plugin').default;
const StyleLintPlugin = require('stylelint-webpack-plugin');
const transformObjectRestSpread = require('babel-plugin-transform-object-rest-spread');
const transformJsx = require('babel-plugin-transform-react-jsx');
const transformRuntime = require('babel-plugin-transform-runtime');

const CopyModulesPlugin = require('copy-modules-webpack-plugin');

const webpackOutputPath = 'assets';
const webpackOutputDir = path.resolve(__dirname, 'target/classes', webpackOutputPath);

/**
 * Create a webpack config for the given paths and options
 * @param entryPath path to the javascript entry file for this config, relative to src/main/frontend
 * @param outputPath path to the javascript output file, relative to the assets dir
 * @param cssOutputPath path to the css output file, relative to the assets dir
 * @param env webpack environment object, expected to contain 'production' and 'clmServerVersion' properties
 * @param externals configuration object to use on the `externals` property
 */
function config({ entryPath, outputPath, cssOutputPath, env, externals }) {
  const production = env.production,
      extractSass = new ExtractTextPlugin({ filename: cssOutputPath }),
      getCssPlugins = () => [
        extractSass,
        new CSSSplitPlugin({
          size: 4095,
          filename: '[name]-[part].[ext]'
        })
      ],
      productionPlugins = [
        new CopyModulesPlugin({
          destination: path.join('target', 'webpack-modules')
        })
      ],
      plugins = [
        new webpack.DefinePlugin({
          CLM_BUILD_TIMESTAMP: new Date().getTime(),
          CLM_SERVER_VERSION: JSON.stringify(env.clmServerVersion)
        }),
        new StyleLintPlugin({ syntax: 'scss' })
      ].concat(
          cssOutputPath ? getCssPlugins() : [],
          productionPlugins
      );

  return {
    context: path.resolve(__dirname, 'src/main/frontend'),
    entry: entryPath,
    output: {
      path: webpackOutputDir,
      filename: outputPath
    },
    resolve: {
      extensions: ['.js', '.jsx']
    },
    module: {
      rules: [{
        test: /\.jsx?$/,
        // NOTE: babel's transformRuntime and webpack's exports-loader cannot be used on the
        // same files due to https://github.com/webpack/webpack/issues/4039#issuecomment-274094298
        exclude: /node_modules|src[\/\\]main[\/\\]frontend[\/\\]lib[\/\\](protovis|Base64)/,
        use: {
          loader: 'babel-loader',
          options: {
            presets: [['env', { modules: false }]],
            plugins: [
              transformObjectRestSpread,
              transformJsx,
              [transformRuntime, { polyfill: false }]
            ]
          }
        }
      }, {
        test: /\.jsx?$/,
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
            {
              loader: 'resolve-url-loader',
              options: { attempts: 1 }
            },
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
    plugins: plugins,
    externals,
    devtool: production ? undefined : 'eval-sourcemap',
    devServer: {
      port: 8070,

      // makes misconfiguration of the backend easier to notice - without this, OSX will allow the backend to run on all
      // interfaces while this runs on just localhost, even if they're on the same port
      host: '0.0.0.0',
      contentBase: path.join(__dirname, 'target', 'classes'),
      publicPath: '/assets/',
      proxy: [{
        context: ['/rest', '/api', '/ui', '/policy-assets'],
        target: 'http://localhost:8072/'
      }]
    }
  };
}

module.exports = function(env) {
  env = env || {};

  const brainConfig = config({
        entryPath: './index.js',
        outputPath: 'bundle.js',
        cssOutputPath: 'style.css',
        env
      }),

      versionGraphConfig = config({
        entryPath: './version-graph/view-details-index.js',
        outputPath: 'viewdetails.js',
        cssOutputPath: 'viewdetails.css',
        env
      }),
      versionGraphAppConfig = config({
        entryPath: './version-graph/version-graph-app-index.js',
        outputPath: 'version.graph.app.js',
        cssOutputPath: 'version.graph.app.css',
        env
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
      externals: Object.assign({}, jqueryExternals, angularExternals)
    }),
    config({
      entryPath: './cip/cip-loader-index.js',
      outputPath: 'cip-loader.js',
      cssOutputPath: 'cip-loader.css',
      env,
      externals: jqueryExternals
    }),
    config({
      entryPath: './cip/cip-index.js',
      outputPath: 'cip.js',
      cssOutputPath: 'cip.css',
      env,
      externals: Object.assign({}, jqueryExternals, angularExternals)
    }),
    config({
      entryPath: './audit-report/external-index.js',
      outputPath: 'external.js',
      env
    })
  ];
};
