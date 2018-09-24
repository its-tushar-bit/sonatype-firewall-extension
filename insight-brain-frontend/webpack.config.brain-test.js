const path = require('path');
const JasmineWebpackPlugin = require('jasmine-webpack-plugin');
const transformObjectRestSpread = require('babel-plugin-transform-object-rest-spread');

const outputPath = path.resolve(__dirname, 'target/classes/assets');

module.exports = {
  context: path.resolve(__dirname, 'src/test/frontend'),
  entry: './specRoot.js',
  output: {
    path: outputPath,
    filename: 'test-bundle.js'
  },
  plugins: [new JasmineWebpackPlugin()],
  module: {
    rules: [
      {
        test: /\.js$/,
        exclude: /node_modules|src[\/\\]main[\/\\]frontend[\/\\]lib[\/\\](protovis|Base64)/,
        use: {
          loader: 'babel-loader',
          options: {
            presets: ['env'],
            plugins: [transformObjectRestSpread]
          }
        }
      },
      {
        test: /\.html$/,
        use: {
          loader: 'html-loader',
          options: {
            attrs: false
          }
        }
      }, {
        // ZeroClipboard includes a swf file in an import statement, so webpack needs to know what to do about that,
        // even in tests
        test: /\.swf$/,
        loader: 'file-loader',
        options: {
          name: '[name].[ext]'
        }
      }
    ]
  },
  devtool: 'eval',
  devServer: {
    index: '_specRunner.html',
    port: 8235,
    host: '0.0.0.0'
  }
};
