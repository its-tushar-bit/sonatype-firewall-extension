var uglify = require('rollup-plugin-uglify');
var minify = require('uglify-js').minify;
var legacy = require('rollup-plugin-legacy');
var commonjs = require('rollup-plugin-commonjs');
var alias = require('rollup-plugin-alias');

var isProd = process.env.BUILD === 'production';

var plugins = [
  legacy({
    // add a default export, corresponding to `Base64`
    'src/main/frontend/lib/Base64.js': 'Base64'
  }),
  commonjs({ include: 'src/main/frontend/lib/angular-ui-router/**' }),

  // angular-ui-router depends on angular by name, so tell rollup where to find it
  alias({ angular: __dirname + '/../src/main/frontend/lib/angular/angular.js' })
];

if (isProd) {
  plugins.push(uglify({}, minify));
}

module.exports = {
  entry: 'src/main/frontend/audit-report/external-index.js',
  sourceMap: false,
  plugins: plugins,
  context: 'this',
  dest: 'target/classes/assets/assets/js/external.js'
};
