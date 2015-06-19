/**
 * @license Copyright (c) 2011-present Sonatype, Inc. All rights reserved. Includes
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

    require('load-grunt-tasks')(grunt);
    require('time-grunt')(grunt);

    grunt.initConfig({
      config: {
        pom: {
          angularJsVersion: extractFromPom('angularjs.version'),
          angularDebug: extractFromPom('angular.debug'),
          clmVersion: extractFromPom('version')
        },
        buildTimestamp: new Date().getTime(),
        frontend: 'src/main/frontend',
        generated: 'target/classes/assets-new',
        styleguide: 'target/styleguide',
        temp: '.tmp'
      },
      clean: {
        build: [
          '<%= config.generated %>'
        ],
        temp: [
          '<%= config.temp %>'
        ],
        styleguide: [
          '<%= config.styleguide %>'
        ]
      },
      copy: {
        build_cip: {
          expand: true,
          cwd: '<%= config.frontend %>',
          src: [
            'cip/**/*.js',
            'policy/js/cip-loader.js'
          ],
          dest: '<%= config.generated %>'
        },
        build_brain_client: {
          files: {
            '<%= config.generated %>/policy/js/brain.client.js': '<%= config.frontend %>/brain-client/brain.client.js',
            '<%= config.generated %>/assets/js/brain.client.js': '<%= config.frontend %>/brain-client/brain.client.js'
          }
        },
        build: {
          expand: true,
          cwd: '<%= config.frontend %>',
          src: ['**/*.{html,eot,svg,ttf,woff,png,gif}', 'assets/lib/**/*.{js,css}'],
          dest: '<%= config.generated %>'
        },
        develop: {
          expand: true,
          cwd: '<%= config.temp %>/concat',
          src: ['**/*'],
          dest: '<%= config.generated %>/assets'
        }
      },
      csslint: {
        build: [
          '<%= config.frontend %>/**/*.css',
          '!<%= config.frontend %>/assets/lib/**/*'
        ]
      },
      cssmin: {
        build_cip: {
          files: {
            '<%= config.generated %>/cip/cip.css': [
              '<%= config.frontend %>/cip/*.css',
              '<%= config.frontend %>/assets/version-graph/content.css',
              '<%= config.temp %>/scss/cip.css',
              '<%= config.frontend %>/assets/multi-select.css'
            ]
          }
        }
      },
      filerev: {
        build: {
          src: [
            '<%= config.generated %>/**/*.{js,css}',
            '!<%= config.generated %>/assets/lib/**/*',
            '!<%= config.generated %>/**/brain.client.js',
            '!<%= config.generated %>/policy/js/cip-loader.js'
          ]
        }
      },
      filerev_replace: {
        options: {
          assets_root: '<%= config.generated %>/policy/js'
        },
        compiled_assets: {
          src: '<%= config.generated %>/policy/js/cip-loader.js'
        }
      },
      jshint: {
        build: [
          '<%= config.frontend %>/**/*.js',
          '!<%= config.frontend %>/assets/lib/**/*',
          '!<%= config.frontend %>/policy/js/cip-loader.js'
        ]
      },
      template: {
        options: {
          data: function() {
            return grunt.config.get();
          }
        },
        build_cip: {
          files: {
            '<%= config.generated %>/policy/js/cip-loader.js': '<%= config.generated %>/policy/js/cip-loader.js',
            '<%= config.generated %>/policy/js/brain.client.js': '<%= config.generated %>/policy/js/brain.client.js',
            '<%= config.generated %>/assets/js/brain.client.js': '<%= config.generated %>/assets/js/brain.client.js'
          }
        },
        build: {
          expand: true,
          cwd: '<%= config.generated %>',
          src: '**/index.html',
          dest: '<%= config.generated %>'
        }
      },
      uglify: {
        options: {
          preserveComments: 'some'
        }
      },
      useminPrepare: {
        build: {
          files: {
            src: [
              '<%= config.frontend %>/assets/index.html'
            ]
          },
          options: {
            dest: '<%= config.generated %>/assets/',
            staging: '<%= config.temp %>',
            type: 'html'
          }
        },
        // build_version_graph relies on the symmetry between ide/eclipse and rm/nexus. If this symmetry is broken
        // a task target for each will be required
        build_version_graph: {
          files: {
            src: [
              '<%= config.frontend %>/assets/version-graph/*/*/index.html',
              '<%= config.frontend %>/assets/version-graph/*/*/viewdetails.html'
            ]
          },
          options: {
            // useminPrepare has a bug with relative paths https://github.com/yeoman/grunt-usemin/issues/297
            dest: '<%= config.generated %>/assets/version-graph/ide/eclipse',
            staging: '<%= config.temp %>/concat/assets/version-graph/ide',
            type: 'html'
          }
        }
      },
      usemin: {
        html: [
          '<%= config.generated %>/assets/index.html',
          '<%= config.generated %>/assets/version-graph/ide/eclipse/index.html',
          '<%= config.generated %>/assets/version-graph/ide/eclipse/viewdetails.html',
          '<%= config.generated %>/assets/version-graph/rm/nexus/index.html',
          '<%= config.generated %>/assets/version-graph/rm/nexus/viewdetails.html'
        ],
        options: {
          assetsDirs: [
            '<%= config.generated %>/assets',
            '<%= config.generated %>/assets/version-graph/ide/eclipse',
            '<%= config.generated %>/assets/version-graph/rm/nexus'
          ]
        }
      },
      sass: {
        build_cip: {
          files: {
            '<%= config.temp %>/scss/cip.css': '<%= config.frontend %>/scss/cip.scss',
            '<%= config.temp %>/scss/viewdetails.css': '<%= config.frontend %>/scss/viewdetails.scss'
          }
        },
        build: {
          files: {
            '<%= config.temp %>/scss/bootstrap.css': '<%= config.frontend %>/assets/lib/bootstrap/bootstrap.scss',
            '<%= config.temp %>/scss/scss.css': '<%= config.frontend %>/scss/scss.scss'
          }
        }
      },
      styleguide: {
        build: {
          options: {
            name: 'CLM Living Style Guide',
            framework: {
              name: 'styledocco',
              options: {
                // Node ignores PATHEXT in Windows for spawn. See https://github.com/joyent/node/issues/2318
                preprocessor: process.platform === 'win32' ? 'sass.bat' : 'sass'
              }
            },
            template: {
              include: [
                '<%= config.temp %>/scss/bootstrap.css'
              ]
            }
          },
          files: {
            '<%= config.styleguide %>': '<%= config.frontend %>/scss/*.scss'
          }
        }
      },
      watch: {
        develop: {
          cwd: '',
          files: [
            '<%= config.frontend %>/**/*.{html,eot,svg,ttf,woff,png,gif,js}'
          ],
          tasks: [
            'copy:build',
            'template:build',
            'useminPrepare:build',
            'concat:generated',
            'copy:develop',
            'usemin'
          ]
        },
        develop_styles: {
          cwd: '<%= config.frontend %>',
          file: ['**/*.{css,scss}'],
          tasks: [
            'copy:build',
            'template:build',
            'sass:build',
            'useminPrepare:build',
            'concat:generated',
            'copy:develop',
            'usemin'
          ]
        }
      }
    });

    grunt.registerTask('build', [
      // Current CSS will fail build if linted
      //'csslint:build',
      'jshint',
      'clean',
      'copy:build',
      'copy:build_cip',
      'copy:build_brain_client',
      'template',
      'sass',
      'useminPrepare',
      'concat:generated',
      'uglify:generated',
      'cssmin:generated',
      'cssmin:build_cip',
      'filerev',
      'usemin',
      'filerev_replace',

      'livingstyle',

      'clean:temp'
    ]);

    grunt.registerTask('develop', [
      'jshint',
      'clean',
      'copy:build',
      'copy:build_cip',
      'copy:build_brain_client',
      'template',
      'sass',
      'useminPrepare',
      'concat:generated',
      'cssmin:build_cip',
      'copy:develop',
      'usemin',

      'watch:develop',

      'clean:temp'
    ]);

    grunt.registerTask('livingstyle', [
      'clean:styleguide',
      'sass:build',
      'styleguide:build'
    ])
  };
}());
