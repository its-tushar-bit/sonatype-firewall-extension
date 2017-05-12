var uglify = require('rollup-plugin-uglify');
var minify = require('uglify-js').minify;
var scss = require('rollup-plugin-scss');
var legacy = require('rollup-plugin-legacy');

var isProd = process.env.BUILD === 'production';

var plugins = [
  scss({
    outputStyle: isProd ? 'compressed' : 'nested'
  }),
  legacy({
    'src/main/frontend/util/Globals.js': {
      messageTemplate: 'messageTemplate',
      AngularUtils: 'AngularUtils',
      AngularStateUtils: 'AngularStateUtils'
    }
  })
];

if (isProd) {
  plugins.push(uglify({}, minify));
}

module.exports = {
  entry: 'src/main/frontend/cip/cip-index.js',
  sourceMap: isProd ? false : 'inline',
  plugins: plugins,
  dest: 'target/classes/assets/cip/cip.js'
};
