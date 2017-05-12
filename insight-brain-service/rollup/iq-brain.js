var uglify = require('rollup-plugin-uglify');
var minify = require('uglify-js').minify;
var legacy = require('rollup-plugin-legacy');

var isProd = process.env.BUILD === 'production';

var plugins = [
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

export default {
  entry: 'src/main/frontend/index.js',
  sourceMap: isProd ? false : 'inline',
  plugins: plugins,
  format: 'iife',
  dest: 'target/classes/assets/bundle.js'
};
