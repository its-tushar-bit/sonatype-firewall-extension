var uglify = require('rollup-plugin-uglify');
var minify = require('uglify-js').minify;
var scss = require('rollup-plugin-scss');

var isProd = process.env.BUILD === 'production';

var plugins = [
  scss({
    outputStyle: isProd ? 'compressed' : 'nested'
  })
];

if(isProd) {
  plugins.push(uglify({}, minify));
}

module.exports = {
  entry: 'src/main/frontend/version-graph/version-graph-app-index.js',
  sourceMap: isProd ? false : 'inline',
  plugins: plugins,
  context: 'this',
  dest: 'target/classes/assets/version-graph/version.graph.app.js'
};
