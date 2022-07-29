/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
const webpack = require('webpack');
const path = require('path');

const outputPath = path.resolve(__dirname, 'target/classes/assets');

const config = {
  mode: 'development',
  context: path.resolve(__dirname, 'src/test/frontend'),
  entry: './specRoot.js',
  output: {
    path: outputPath,
    publicPath: '',
    filename: 'test-bundle.js',
  },
  plugins: [
    new webpack.DefinePlugin({
      CLM_BUILD_TIMESTAMP: 0,
      CLM_SERVER_VERSION: '1',
    }),
    new webpack.ProvidePlugin({
      Buffer: ['buffer', 'Buffer'],
    }),
  ],
  resolve: {
    extensions: ['.js', '.jsx'],
    fallback: { crypto: false },
    alias: {
      MainRoot: path.resolve(__dirname, 'src/main/frontend'),
      TestRoot: path.resolve(__dirname, 'src/test/frontend'),
    },
  },
  module: {
    rules: [
      {
        test: /\.jsx?$/,
        exclude: /node_modules|src[\/\\]main[\/\\]frontend[\/\\]lib[\/\\](protovis|Base64)/,
        use: {
          loader: 'babel-loader',
          options: {
            presets: ['@babel/preset-react', ['@babel/preset-env', { modules: 'commonjs' }]],
          },
        },
      },
      {
        test: /\.jsx?$/,
        include: /node_modules[\/\\](fuse\.js|asn1.js|@uirouter|enzyme-matchers)/,
        use: {
          loader: 'babel-loader',
          options: {
            presets: ['@babel/preset-react', ['@babel/preset-env', { modules: 'commonjs' }]],
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
        use: 'null-loader',
      },
      {
        test: /\.(png|svg)$/,
        type: 'asset/resource',
        generator: {
          filename: 'images/[name][ext]',
        },
      },
    ],
  },
  devtool: 'eval',
};

module.exports = function (env) {
  env = env || {};

  if (!env.skipTestCoverage) {
    config.module.rules.unshift({
      test: /\.jsx?$/,
      include: /src[\/\\]main[\/\\]frontend/,
      exclude: /[\/\\]lib[\/\\]/,
      use: {
        loader: 'istanbul-instrumenter-loader',
        options: {
          esModules: true,
        },
      },
    });
  }

  return config;
};
