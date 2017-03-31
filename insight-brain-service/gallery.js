/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved. Includes
 * the third-party code listed at
 * http://links.sonatype.com/products/clm/attributions. "Sonatype" is a
 * trademark of Sonatype, Inc.
 */

// NOTE: use 'grunt gallery' command to run gallery app

var rollup = require('rollup-endpoint');
var html = require('rollup-plugin-html');
var express = require('express');
var app = express();
var port = 4040;

app.get('/assets/app-bundle.js', rollup.serve({
  entry: 'src/main/component-gallery/app/main.js',
  plugins: [
      html({include: '**/*.html'})
  ]
}));

app.use(express.static('src/main/component-gallery'));
app.use(express.static('src/main/frontend'));
app.use(express.static('.tmp/scss'));

app.listen(port);
console.log('\x1b[35m%s\x1b[0m', 'Gallery is running at http://localhost:' + port);

module.exports = app;
