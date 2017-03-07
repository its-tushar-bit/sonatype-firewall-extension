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
        gallery: 'src/main/component-gallery/app',
        generated: 'target/classes/assets',
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
      jscs: {
        check: {
          src: [
              '<%= config.frontend %>/**/*.js',
              '!<%= config.frontend %>/lib/**/*'
          ],
          options: {
            config: ".jscsrc"
          }
        },
        fix: {
          src: [
              '<%= config.frontend %>/**/*.js',
              '!<%= config.frontend %>/lib/**/*'
          ],
          options: {
            config: ".jscsrc",
            fix: true // Autofix code style violations when possible.
          }
        }
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
        },
        gallery: {
          files: {
            '<%= config.temp %>/scss/gallery.css': '<%= config.gallery %>/scss/gallery.scss'
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
        },
        compile_styles: {
          files: [
            '<%= config.frontend %>/**/*.{css,scss}'
          ],
          tasks: [
            'sass:build',
          ]
        },
        gallery_styles: {
          files: [
            '<%= config.gallery %>/scss/*.{css,scss}'
          ],
          tasks: [
            'sass:gallery',
          ]
        }
      },
      focus: {
        dev: {
          include: ['develop', 'develop_styles']
        },
        gallery: {
          include: ['compile_styles', 'gallery_styles']
        }
      },
      express: {
        options: {
          // Override defaults here
        },
        gallery: {
          options: {
            script: 'gallery.js'
          }
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

    grunt.registerTask('bower-gallery', 'install bower dependencies in component gallery', function() {
      var execSync = require('child_process').execSync;
      var bowerCmd = path.join(__dirname, 'node_modules/.bin/bower') + ' install';
      execSync(bowerCmd, {cwd: './src/main/component-gallery/app'});
    });

    grunt.registerTask('build', [
      'configure_override:build',

      'jshint',
      'jscs:check',
      'clean',
      'copy:build',
      'sass:build',
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

      'clean:temp'
    ]);

    grunt.registerTask('m2e', [
      'configure_override:develop',

      'clean:temp',
      'copy:develop',
      'sass:build',
      'copy:develop_sass',
      'template:dev',
      'clean:temp'
    ]);

    grunt.registerTask('develop', [
      'configure_override:develop',

      'jshint',
      'jscs:check',
      'bower:install',
      'clean:temp',
      'copy:develop',
      'sass:build',
      'copy:develop_sass',
      'template:dev',
      'focus:dev',
      'clean:temp'
    ]);

    grunt.registerTask('fix', ['jscs:fix']);

    grunt.registerTask('gallery', [
      'bower-gallery',
      'sass:build',
      'sass:gallery',
      'express',
      'focus:gallery'
    ]);
  };
}());
