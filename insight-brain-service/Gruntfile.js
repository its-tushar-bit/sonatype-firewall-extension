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

    var angularVersion = extractFromPom('angularjs.version');
    var path = require('path');
    var styleguideAssets = require('./Gruntfile.styleguide.js');
    require('load-grunt-tasks')(grunt);
    require('time-grunt')(grunt);

    grunt.initConfig({
      config: {
        pom: {
          angularJsVersion: angularVersion,
          clmVersion: extractFromPom('version')
        },
        angularDebug: false,
        buildTimestamp: new Date().getTime(),
        frontend: 'src/main/frontend',
        styleguideSrc: 'src/main/styleguide',
        generated: 'target/classes/assets-new',
        styleguide: 'target/styleguide',
        temp: '.tmp',
        templates: '**/*.tpl.html'
      },
      bower: {
        install: {
          options: {
            copy: false
          }
        }
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
            '<%= config.generated %>/js/brain.client.js': '<%= config.frontend %>/brain-client/brain.client.js'
          }
        },
        build: {
          expand: true,
          cwd: '<%= config.frontend %>',
          src: [
            '**/*.{html,ttf,woff,woff2,png,gif,jpg}',
            '!<%= config.templates %>',
            '!lib/*',
            'lib/**/*.{js,css,ttf,woff,woff2}',
            '!lib/**/test/*'
          ],
          dest: '<%= config.generated %>'
        },
        develop: {
          expand: true,
          cwd: '<%= config.frontend %>',
          src: [
            '**/*.{html,js,css,ttf,woff,woff2,png,gif,jpg}',
            '!lib/*',
            'lib/**/*.{js,css,ttf,woff,woff2}',
            '!lib/**/test/*'
          ],
          dest: '<%= config.generated %>'
        },
        develop_sass: {
          expand: true,
          cwd: '<%= config.temp %>',
          src: [
            '**/*.css'
          ],
          dest: '<%= config.generated %>'
        }
      },
      csslint: {
        build: [
          '<%= config.frontend %>/**/*.css',
          '!<%= config.frontend %>/lib/**/*'
        ]
      },
      cssmin: {
        build_cip: {
          files: {
            '<%= config.generated %>/cip/cip.css': [
              '<%= config.frontend %>/cip/*.css',
              '<%= config.frontend %>/version-graph/content.css',
              '<%= config.temp %>/scss/cip.css',
              '<%= config.frontend %>/multi-select.css'
            ]
          }
        }
      },
      "file-creator": {
        styleguide: {
          files: [
            {
              file: '<%= config.temp %>/styleguide.css',
              method: function(fs, fd, done) {
                fs.writeSync(fd, styleguideAssets.sonatypeIconsFont + styleguideAssets.fontAwesomeFont);
                done();
              }
            }
          ]
        }
      },
      filerev: {
        build: {
          src: [
            '<%= config.generated %>/**/*.{js,css}',
            '!<%= config.generated %>/lib/**/*',
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
      html2js: {
        options: {
          base: '<%= config.frontend %>',
          quoteChar: '\'',
          useStrict: true,
          module: 'templates'
        },
        build: {
          src: ['<%= config.frontend %>/<%= config.templates %>'],
          dest: '<%= config.temp %>/js/templates.module.js'
        }
      },
      jshint: {
        options: {
          jshintrc: true
        },
        build: [
          '<%= config.frontend %>/**/*.js',
          '!<%= config.frontend %>/lib/**/*',
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
            '<%= config.generated %>/js/brain.client.js': '<%= config.generated %>/js/brain.client.js',

            // Included for CLM Application Reports
            '<%= config.generated %>/policy-assets/js/cip-loader.js': '<%= config.generated %>/policy/js/cip-loader.js',
            '<%= config.generated %>/policy-assets/js/brain.client.js': '<%= config.generated %>/js/brain.client.js'
          }
        },
        build: {
          expand: true,
          cwd: '<%= config.generated %>',
          src: ['**/index.html', 'css/style-scss.*.css'],
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
              '<%= config.frontend %>/index.html'
            ]
          },
          options: {
            dest: '<%= config.generated %>/',
            staging: '<%= config.temp %>',
            type: 'html'
          }
        },
        // build_version_graph relies on the symmetry between ide/eclipse and rm/nexus. If this symmetry is broken
        // a task target for each will be required
        build_version_graph: {
          files: {
            src: [
              '<%= config.frontend %>/version-graph/*/*/index.html',
              '<%= config.frontend %>/version-graph/*/*/viewdetails.html'
            ]
          },
          options: {
            // useminPrepare has a bug with relative paths https://github.com/yeoman/grunt-usemin/issues/297
            dest: '<%= config.generated %>/version-graph/ide/eclipse',
            staging: '<%= config.temp %>/concat/version-graph/ide',
            type: 'html'
          }
        }
      },
      usemin: {
        html: [
          '<%= config.generated %>/index.html',
          '<%= config.generated %>/version-graph/ide/eclipse/index.html',
          '<%= config.generated %>/version-graph/ide/eclipse/viewdetails.html',
          '<%= config.generated %>/version-graph/rm/nexus/index.html',
          '<%= config.generated %>/version-graph/rm/nexus/viewdetails.html'
        ],
        options: {
          assetsDirs: [
            '<%= config.generated %>/',
            '<%= config.generated %>/version-graph/ide/eclipse',
            '<%= config.generated %>/version-graph/rm/nexus'
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
            '<%= config.temp %>/scss/bootstrap.css': '<%= config.frontend %>/lib/bootstrap/bootstrap.scss',
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
                preprocessor: process.execPath + ' ' + path.join(path.dirname(require.resolve('node-sass')), '/../bin/node-sass')
              }
            },
            template: {
              include: [
                '<%= config.temp %>/scss/bootstrap.css',
                '<%= config.frontend %>/lib/bootstrap-toggle/bootstrap2-toggle-2.2.0.css',
                '<%= config.frontend %>/lib/components-font-awesome/css/font-awesome.css',
                '<%= config.temp %>/styleguide.css',
                '<%= config.frontend %>/lib/jquery/jquery-1.8.3.min.js',
                '<%= config.frontend %>/lib/angular-' + angularVersion + '/angular.js',
                '<%= config.frontend %>/lib/angular-' + angularVersion + '/angular-sanitize.js',
                '<%= config.frontend %>/lib/ui-bootstrap-tpls-0.8.0.min.js',
                '<%= config.frontend %>/util/AngularCommon.js',
                '<%= config.frontend %>/FormsModule.js',
                '<%= config.styleguideSrc %>/styleguide.js'
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
          files: [
            '<%= config.frontend %>/**/*.{html,eot,svg,ttf,woff,png,gif,js,jpg}'
          ],
          tasks: [
            'configure_override:develop',
            'copy:develop',
            'template:build'
          ]
        },
        develop_styles: {
          files: [
            '<%= config.frontend %>/**/*.{css,scss}'
          ],
          tasks: [
            'configure_override:develop',
            'copy:develop',
            'template:build',
            'sass:build',
            'copy:develop_sass'
          ]
        }
      }
    });

    grunt.task.registerMultiTask('configure_override', 'Set configuration for Grunt task', function() {
      grunt.config.merge(this.data);
    });

    grunt.registerTask('build', [
      'configure_override:build',

      // Current CSS will fail build if linted
      //'csslint:build',
      'jshint',
      'clean',
      'copy:build',
      'copy:build_cip',
      'copy:build_brain_client',
      'sass',
      'html2js:build',
      'useminPrepare',
      'concat:generated',
      'uglify:generated',
      'cssmin:generated',
      'cssmin:build_cip',
      'filerev',
      'usemin',
      'filerev_replace',
      'template',

      'clean:temp'
    ]);

    grunt.registerTask('deploy', [
      'build',
      'livingstyle',

      'clean:temp'
    ]);

    grunt.registerTask('m2e', [
      'configure_override:develop',

      'clean',
      'copy:develop',
      'copy:build_cip',
      'copy:build_brain_client',
      'sass',
      'copy:develop_sass',
      'cssmin:build_cip',
      'template',
      'clean:temp'
    ]);

    grunt.registerTask('develop', [
      'configure_override:develop',

      'jshint',
      'bower:install',
      'clean',
      'copy:develop',
      'copy:build_cip',
      'copy:build_brain_client',
      'sass',
      'copy:develop_sass',
      'cssmin:build_cip',
      'template',
      'watch',
      'clean:temp'
    ]);

    grunt.registerTask('livingstyle', [
      'clean:styleguide',
      'bower:install',
      'sass:build',
      'file-creator:styleguide',
      'styleguide:build',
      'clean:temp'
    ]);
  };
}());
