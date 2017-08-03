var uglify = require('rollup-plugin-uglify');
var minify = require('uglify-js').minify;
var html = require('rollup-plugin-html');
var commonjs = require('rollup-plugin-commonjs');
var alias = require('rollup-plugin-alias');
var buble = require('rollup-plugin-buble');

var isProd = process.env.BUILD === 'production';

var plugins = [
  html({
    include: '**/*.html'
  }),
  commonjs({ include: 'src/main/frontend/lib/angular-ui-router/**' }),

  // angular-ui-router depends on angular by name, so tell rollup where to find it
  alias({ angular: __dirname + '/../src/main/frontend/lib/angular/angular.js' }),
  buble()
];

if (isProd) {
  plugins.push(uglify({}, minify));
}

export default {
  entry: 'src/main/frontend/index.js',
  sourceMap: isProd ? false : 'inline',
  plugins: plugins,
  format: 'iife',
  dest: 'target/classes/assets/bundle.js'
};
