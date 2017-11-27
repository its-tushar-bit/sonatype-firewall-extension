/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved. Includes
 * the third-party code listed at
 * http://links.sonatype.com/products/clm/attributions. "Sonatype" is a
 * trademark of Sonatype, Inc.
 */
(function() {
  'use strict';

  module.exports = function(grunt) {
    function extractFromPom(nodeName) {
      var DOMParser = require('xmldom').DOMParser;
      var doc = new DOMParser().parseFromString(grunt.file.read('pom.xml'));
      var node = doc.documentElement.getElementsByTagName(nodeName)[0];
      return node.firstChild.nodeValue;
    }

    var path = require('path');
    var webpackCmd = path.join('node_modules', '.bin', 'webpack');
    var webpackDevServerCmd = path.join('node_modules', '.bin', 'webpack-dev-server');
    require('load-grunt-tasks')(grunt);
    require('time-grunt')(grunt);

    var lintSrc = ['<%= config.test %>/**/*.js'];

    grunt.initConfig({
      config: {
        pom: {
          clmVersion: extractFromPom('version')
        },
        angularDebug: false,
        buildTimestamp: new Date().getTime(),
        frontend: 'src/main/frontend',
        test: 'src/test/frontend',
        generated: 'target/classes/assets',
        temp: '.tmp',
        templates: '**/*.tpl.html'
      },
      configure_override: {
        build: {
          config: {
            angularDebug: false
          }
        },
        develop: {
          config: {
            angularDebug: true
          }
        }
      },
      clean: {
        build: [
          '<%= config.generated %>'
        ],
        temp: [
          '<%= config.temp %>'
        ]
      },
      copy: {
        build: {
          expand: true,
          cwd: '<%= config.frontend %>',
          src: [
            '**/*.{html,ttf,woff,woff2,png,gif,jpg,ico}',
            '!<%= config.templates %>',
            '!lib/**'
          ],
          dest: '<%= config.generated %>'
        },
        develop: {
          expand: true,
          cwd: '<%= config.frontend %>',
          src: [
            '**/*.{html,css,ttf,woff,woff2,png,gif,jpg,ico}',
            '!lib/*',
            'lib/**/*.{js,css,ttf,woff,woff2}',
            '!lib/**/test/*'
          ],
          dest: '<%= config.generated %>'
        }
      },
      eslint: {
        target: lintSrc
      },
      template: {
        options: {
          data: function() {
            return grunt.config.get();
          }
        },
        build: {
          expand: true,
          cwd: '<%= config.generated %>',
          src: ['**/index.html', 'css/style-scss.*.css'],
          dest: '<%= config.generated %>'
        },
        dev: {
          expand: true,
          cwd: '<%= config.generated %>',
          src: ['**/index.html', 'scss/scss.css'],
          dest: '<%= config.generated %>'
        }
      },
      watch: {
        options: {
          cwd: '<%= config.frontend %>'
        },
        assets: {
          files: [
            '**/*.{html,css,eot,svg,ttf,woff,png,gif,jpg}'
          ],
          tasks: [
            'configure_override:develop',
            'copy:develop',
            'template:dev'
          ]
        }
      },
      focus: {
        dev: {
          include: ['assets']
        }
      },
      exec: {
        'webpack': webpackCmd,
        'webpack-prod': webpackCmd + ' -p --env.production', // -p for production - adds uglify and NODE_ENV
        'webpack-watch': webpackCmd + ' -w',
        'webpack-watch-brain': webpackCmd + ' -w --env.brainOnly',
        'webpack-watch-gallery': webpackDevServerCmd + ' --config webpack.config.gallery.js'
      },
      concurrent: {
        options: {
          logConcurrentOutput: true
        },
        watch: {
          tasks: ['focus:dev', 'exec:webpack-watch']
        },
        watchBrain: {
          tasks: ['focus:dev', 'exec:webpack-watch-brain']
        },
        watchGallery: {
          tasks: ['focus:dev', 'exec:webpack-watch-gallery']
        }
      }
    });

    grunt.task.registerMultiTask('configure_override', 'Set configuration for Grunt task', function() {
      grunt.config.merge(this.data);
    });

    grunt.registerTask('build', [
      'configure_override:build',

      'eslint',
      'clean',
      'exec:webpack',
      'copy:build',
      'template:build',
      'clean:temp'
    ]);

    grunt.registerTask('build-prod', [
      'configure_override:build',

      'eslint',
      'clean',
      'exec:webpack-prod',
      'copy:build',
      'template:build',
      'clean:temp'
    ]);

    grunt.registerTask('deploy', [
      'build',

      'clean:temp'
    ]);

    grunt.registerTask('m2e', [
      'configure_override:develop',

      'clean:temp',
      'copy:develop',
      'template:dev',
      'clean:temp'
    ]);

    grunt.registerTask('develop-all', [
      'configure_override:develop',

      'eslint',
      'clean:temp',
      'copy:develop',
      'template:dev',
      'concurrent:watch',
      'clean:temp'
    ]);

    grunt.registerTask('develop-brain', [
      'configure_override:develop',

      'eslint',
      'clean:temp',
      'copy:develop',
      'template:dev',
      'concurrent:watchBrain',
      'clean:temp'
    ]);

    grunt.registerTask('gallery', [
      'copy',
      'concurrent:watchGallery'
    ]);

    grunt.registerTask('default', ['develop-brain']);
  };
}());
