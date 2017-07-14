var uglify = require('rollup-plugin-uglify');
var minify = require('uglify-js').minify;
var scss = require('rollup-plugin-scss');
var commonjs = require('rollup-plugin-commonjs');

var isProd = process.env.BUILD === 'production';

var plugins = [
  scss({
    outputStyle: isProd ? 'compressed' : 'nested'
  }),
  commonjs({ include: 'src/main/frontend/lib/angular-ui-router/**' })
];

if (isProd) {
  plugins.push(uglify({}, minify));
}

module.exports = {
  entry: 'src/main/frontend/audit-report/audit-report-index.js',
  sourceMap: isProd ? false : 'inline',
  plugins: plugins,
  dest: 'target/classes/assets/audit-report/audit-report.js',
  format: 'iife',

  // angular is included in another script loaded before this one, so just reference it via the global
  external: ['angular'],
  globals: {
    angular: 'angular'
  }
};
