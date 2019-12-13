/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
const path = require('path');
const JasmineWebpackPlugin = require('jasmine-webpack-plugin');
const transformObjectRestSpread = require('babel-plugin-transform-object-rest-spread');
const transformJsx = require('babel-plugin-transform-react-jsx');

const outputPath = path.resolve(__dirname, 'target/classes/assets');

module.exports = {
  context: path.resolve(__dirname, 'src/test/frontend'),
  entry: './specRoot.js',
  output: {
    path: outputPath,
    filename: 'test-bundle.js'
  },
  plugins: [new JasmineWebpackPlugin()],
  resolve: {
    extensions: ['.js', '.jsx']
  },
  module: {
    rules: [
      {
        test: /\.jsx?$/,
        exclude: /node_modules|src[\/\\]main[\/\\]frontend[\/\\]lib[\/\\](protovis|Base64)/,
        use: {
          loader: 'babel-loader',
          options: {
            presets: ['env'],
            plugins: [transformObjectRestSpread, transformJsx]
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
      },
      {
        test: /\.s?css$/,
        use: 'null-loader'
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
