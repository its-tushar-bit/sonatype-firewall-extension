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
        generated: 'target/classes/assets',
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
        build: {
          expand: true,
          cwd: '<%= config.frontend %>',
          src: [
            '**/*.{html,ttf,woff,woff2,png,gif,jpg,ico}',
            '!<%= config.templates %>',
            '!lib/*',
            'lib/**/*.{js,css,ttf,woff,woff2,swf}',
            '!lib/**/test/*'
          ],
          dest: '<%= config.generated %>'
        },
        develop: {
          expand: true,
          cwd: '<%= config.frontend %>',
          src: [
            '**/*.{html,js,css,ttf,woff,woff2,png,gif,jpg,ico}',
            '!lib/*',
            'lib/**/*.{js,css,ttf,woff,woff2,swf}',
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
            '!<%= config.generated %>/lib/**/*'
          ]
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
          '!<%= config.frontend %>/lib/**/*'
        ]
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
        }
      },
      usemin: {
        html: [
          '<%= config.generated %>/index.html'
        ],
        options: {
          assetsDirs: [
            '<%= config.generated %>/'
          ]
        }
      },
      useminAuditReport: {
        html: [
          '<%= config.generated %>/audit-report/index.html'
        ]
      },
      sass: {
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
                '<%= config.frontend %>/lib/bootstrap/bootstrap-slider-2.0.0.css',
                '<%= config.frontend %>/lib/components-font-awesome/css/font-awesome.css',
                '<%= config.frontend %>/management.css',
                '<%= config.temp %>/styleguide.css',
                '<%= config.frontend %>/lib/jquery/jquery.min.js',
                '<%= config.frontend %>/lib/angular/angular.min.js',
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
            'template:dev'
          ]
        },
        develop_styles: {
          files: [
            '<%= config.frontend %>/**/*.{css,scss}'
          ],
          tasks: [
            'configure_override:develop',
            'copy:develop',
            'template:dev',
            'sass:build',
            'copy:develop_sass'
          ]
        }
      }
    });

    grunt.task.registerMultiTask('configure_override', 'Set configuration for Grunt task', function() {
      grunt.config.merge(this.data);
    });

    grunt.registerTask('useminAuditReport', function () {
      var useminSecondTargetConfig = grunt.config('useminAuditReport');
      grunt.config.set('usemin', useminSecondTargetConfig);
      grunt.task.run('usemin');
    });

    grunt.registerTask('build', [
      'configure_override:build',

      'jshint',
      'clean',
      'copy:build',
      'sass',
      'html2js:build',
      'useminPrepare',
      'concat:generated',
      'uglify:generated',
      'cssmin:generated',
      'filerev',
      'usemin',
      'useminAuditReport',
      'template:build',

      'clean:temp'
    ]);

    grunt.registerTask('deploy', [
      'build',
      'livingstyle',

      'clean:temp'
    ]);

    grunt.registerTask('m2e', [
      'configure_override:develop',

      'clean:temp',
      'copy:develop',
      'sass',
      'copy:develop_sass',
      'template:dev',
      'clean:temp'
    ]);

    grunt.registerTask('develop', [
      'configure_override:develop',

      'jshint',
      'bower:install',
      'clean:temp',
      'copy:develop',
      'sass',
      'copy:develop_sass',
      'template:dev',
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
