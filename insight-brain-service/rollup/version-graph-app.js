var uglify = require('rollup-plugin-uglify');
var minify = require('uglify-js').minify;
var scss = require('rollup-plugin-scss');
var commonjs = require('rollup-plugin-commonjs');
var alias = require('rollup-plugin-alias');
var buble = require('rollup-plugin-buble');

var isProd = process.env.BUILD === 'production';

var plugins = [
  scss({
    outputStyle: isProd ? 'compressed' : 'nested'
  }),
  commonjs({ include: 'src/main/frontend/lib/angular-ui-router/**' }),

  // angular-ui-router depends on angular by name, so tell rollup where to find it
  alias({ angular: __dirname + '/../src/main/frontend/lib/angular/angular.js' }),
  buble({ objectAssign: 'angular.extend' })
];

if (isProd) {
  plugins.push(uglify({}, minify));
}

module.exports = {
  entry: 'src/main/frontend/version-graph/version-graph-app-index.js',
  sourceMap: isProd ? false : 'inline',
  plugins: plugins,
  context: 'this',
  dest: 'target/classes/assets/version-graph/version.graph.app.js'
};
