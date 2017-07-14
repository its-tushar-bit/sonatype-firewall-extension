var uglify = require('rollup-plugin-uglify');
var minify = require('uglify-js').minify;
var scss = require('rollup-plugin-scss');
var legacy = require('rollup-plugin-legacy');
var commonjs = require('rollup-plugin-commonjs');
var alias = require('rollup-plugin-alias');

var isProd = process.env.BUILD === 'production';

var plugins = [
  scss({
    output: 'target/classes/assets/policy/css/cip-loader.css',
    outputStyle: isProd ? 'compressed' : 'nested'
  }),
  legacy({
    // add a default export, corresponding to `Base64`
    'src/main/frontend/lib/Base64.js': 'Base64',
    'src/main/frontend/util/Globals.js': {
      messageTemplate: 'messageTemplate',
      AngularUtils: 'AngularUtils',
      AngularStateUtils: 'AngularStateUtils'
    }
  }),
  commonjs({ include: 'src/main/frontend/lib/angular-ui-router/**' }),

  // angular-ui-router depends on angular by name, so tell rollup where to find it
  alias({ angular: __dirname + '/../src/main/frontend/lib/angular/angular.js' })
];

if (isProd) {
  plugins.push(uglify({}, minify));
}

module.exports = {
  entry: 'src/main/frontend/cip/cip-loader-index.js',
  sourceMap: isProd ? false : 'inline',
  plugins: plugins,
  dest: 'target/classes/assets/policy/js/cip-loader.js'
};
