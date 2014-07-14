/**
 * @license Copyright (c) 2012-2014 Sonatype, Inc. All rights reserved. Includes
 * the third-party code listed at
 * http://links.sonatype.com/products/clm/attributions. "Sonatype" is a
 * trademark of Sonatype, Inc.
 */
(function () {
  'use strict';
  var LIVERELOAD_PORT = 35729;
  var liveReloadSnippet = require('connect-livereload')({ port: LIVERELOAD_PORT });
  var proxySnippet = require('grunt-connect-proxy/lib/utils').proxyRequest;
  var mountFolder = function(connect, dir) {
    return connect.static(require('path').resolve(dir));
  };

  module.exports = function(grunt) {
    // parse angularjs.version out of pom file
    function getAngularVersion() {
      var DOMParser = require('xmldom').DOMParser;
      var doc = new DOMParser().parseFromString(grunt.file.read('pom.xml'));
      var node = doc.documentElement.getElementsByTagName('angularjs.version')[0];
      return node.firstChild.nodeValue;
    }

    var config = {
      components: 'src/main/resources/assets',
      filtered: 'src/main/filtered-resources/assets',
      gruntFiltered: 'grunt/filtered',
      assets: 'src/main/wro4j',
      brainClientAssets: 'src/main/brain-client',
      dist: 'grunt/working/dist',
      tmp: 'grunt/working/.tmp',
      debug: 'grunt/working/debug',
      scss: 'grunt/scss',
      docs: 'grunt/docs',
      angularJsVersion: getAngularVersion()
    };

    require('load-grunt-tasks')(grunt);
    require('time-grunt')(grunt);

    grunt.initConfig({
      config: config,
      jshint: {
        options: {
          jshintrc: '.jshintrc'
        },
        all: [
          'Gruntfile.js',
          '<%= config.assets %>/{,*/}*.js',
          '<%= config.brainClientAssets %>/{,*/}*.js'
        ]
      },
      clean: {
        dist: {
          files: [
            {
              dot: true,
              src: [
                '<%= config.tmp %>',
                '<%= config.dist %>'
              ]
            }
          ]
        },
        server: '<%= config.tmp %>',
        debug: '<%= config.debug %>'
      },
      connect: {
        options: {
          port: 9090,
          hostname: '0.0.0.0'
        },
        proxies: [
          {
            context: '/rest',
            host: 'localhost',
            port: 8070,
            https: false,
            changeOrigin: false
          }
        ],
        clmServer: {
          options: {
            middleware: function(connect) {
              return [
                liveReloadSnippet,
                proxySnippet,
                mountFolder(connect, config.components),
                mountFolder(connect, config.debug),
                mountFolder(connect, config.gruntFiltered)
              ];
            }
          }
        },
        styleguide: {
          options: {
            port: 9070,
            middleware: function(connect) {
              return [
                mountFolder(connect, config.docs)
              ];
            },
            keepalive: true
          }
        },
        metrics: {
          options: {
            middleware: function(connect) {
              return [
                liveReloadSnippet,
                function(req, res, options) {
                  req.headers.Authorization = 'Basic YWRtaW46YWRtaW4xMjM=';
                  proxySnippet(req, res, options);
                },
                mountFolder(connect, config.components),
                mountFolder(connect, config.debug),
                mountFolder(connect, config.gruntFiltered)
              ];
            }
          }
        },
        test: {
          options: {
            middleware: function(connect) {
              return [
                mountFolder(connect, config.tmp),
                mountFolder(connect, config.dist)
              ];
            }
          }
        }
      },
      concurrent: {
        copy: [
          'copy:dist',
          'copy:scripts',
          'copy:styles'
        ]
      },
      copy: {
        dist: {
          files: [{
            expand: true,
            cwd: '<%= config.filtered %>/',
            dest: '<%= config.dist %>/',
            src: ['{,*/}{,*/}*.js', 'assets/management.html', 'policy/index.html']
          }, {
            expand: true,
            dot: true,
            cwd: '<%= config.components %>',
            dest: '<%= config.dist %>',
            src: [
              '{,*/}{,*/}{,*/}*.html',
              'assets/lib/**/*',
              'assets/img/{,*/}*.{gif,webp}',
              'assets/fonts/*'
            ]
          }, {
            expand: true,
            dot: true,
            cwd: '<%= config.gruntFiltered %>',
            dest: '<%= config.dist %>',
            src: [
              '{,*/}{,*/}*.html'
            ]
          }]
        },
        scripts: {
          expand: true,
          cwd: '<%= config.assets %>/',
          dest: '<%= config.tmp %>/',
          src: '{,*/}*.js'
        },
        styles: {
          expand: true,
          cwd: '<%= config.assets %>/',
          dest: '<%= config.tmp %>/',
          src: '{,*/}*.css'
        },
        filtered: {
          src: '<%= config.gruntFiltered %>/assets/index.html',
          dest: '<%= config.debug %>/assets/index.html',
          options: {
            process: function(content, path) {
              return grunt.template.process(content);
            }
          }
        },
        debug: {
          files: [{
            expand: true,
            cwd: '<%= config.filtered %>/',
            dest: '<%= config.debug %>/',
            src: ['{,*/}{,*/}*.js', 'assets/management.html', 'policy/index.html']
          }, {
            expand: true,
            cwd: '<%= config.components %>/application',
            dest: '<%= config.debug %>/application-assets',
            src: '{,*/}{,*/}*'
          }, {
            expand: true,
            cwd: '<%= config.components %>/assets',
            dest: '<%= config.debug %>/assets',
            src: '{,*/}{,*/}*'
          }, {
            expand: true,
            cwd: '<%= config.components %>/policy',
            dest: '<%= config.debug %>/policy-assets',
            src: '{,*/}{,*/}*'
          }, {
            expand: true,
            cwd: '<%= config.components %>/organization',
            dest: '<%= config.debug %>/organization-assets',
            src: '{,*/}{,*/}*'
          }, {
            expand: true,
            cwd: '<%= config.components %>/configuration',
            dest: '<%= config.debug %>/configuration-assets',
            src: '{,*/}{,*/}*'
          }, {
            expand: true,
            cwd: '<%= config.components %>/security',
            dest: '<%= config.debug %>/security-assets',
            src: '{,*/}{,*/}*'
          }, {
            expand: true,
            cwd: '<%= config.components %>/report',
            dest: '<%= config.debug %>/report-assets',
            src: '{,*/}{,*/}*'
          }, {
            expand: true,
            cwd: '<%= config.components %>/dashboard',
            dest: '<%= config.debug %>/dashboard-assets',
            src: '{,*/}{,*/}*'
          }, {
            expand: true,
            cwd: '<%= config.assets %>/',
            dest: '<%= config.debug %>/assets',
            src: '{,*/}{,*/}*'
          }, {
            expand: true,
            cwd: '<%= config.brainClientAssets %>/',
            dest: '<%= config.debug %>/policy-assets/js',
            src: '{,*/}{,*/}*'
          },
          /* This is a hack to get around how we currently minify lib css into a css directory */
          {
            expand: true,
            cwd: '<%= config.assets %>/lib/bootstrap',
            dest: '<%= config.debug %>/assets/lib',
            src: '*.css'
          }, {
            expand: true,
            cwd: '<%= config.assets %>/lib/glyphicons',
            dest: '<%= config.debug %>/assets/lib',
            src: '*.css'
          }, {
            expand: true,
            cwd: '<%= config.assets %>/lib/X-editable',
            dest: '<%= config.debug %>/assets/lib',
            src: '*.css'
          }
          /* End hack */
          ]
        }
      },
      karma: {
        unit: {
          configFile: 'karma.conf.js',
          singleRun: true
        }
      },
      sass: {
        clmServer: {
          files: [{
            expand: true,
            flatten: true,
            src: '<%= config.scss %>/*.scss',
            dest: '<%= config.debug %>/assets/scss/',
            ext: '.css'
          }]
        }
      },
      useminPrepare: {
        src: ['<%= config.gruntFiltered %>/assets/index.html'],
        options: {
          dest: '<%= config.dist %>',
          staging: '<%= config.tmp %>',
          root: '<%= config.tmp %>'
        }
      },
      usemin: {
        html: ['<%= config.dist %>/{,*/}*.html'],
        css: ['<%= config.dist %>/css/{,*/}*.css'],
        options: {
          dirs: ['<%= config.dist %>']
        }
      },
      open: {
        server: {
          url: 'http://localhost:<%= connect.options.port %>/assets/index.html'
        }
      },
      phantomas: {
        index : {
          options : {
            indexPath : './grunt/metrics/phantomas/',
            url       : 'http://localhost:<%= connect.options.port %>/assets/index.html',
            numberOfRuns: 10
          }
        }
      },
      styleguide: {
        styledocco: {
          options: {
            framework: {
              name: 'styledocco'
            },
            name: 'CLM Living Style Guide'
          },
          files: {
            '<%= config.docs %>': '<%= config.assets %>/**/*.css'
          }
        }
      },
      watch: {
        assets: {
          files: [
            '<%= config.assets %>/{,*/}{,*/}*.css',
            '<%= config.assets %>/{,*/}{,*/}*.js'
          ],
          tasks: ['copy:debug']
        },
        scssAssets: {
          files: '<%= config.assets %>/{,*/}{,*/}*.scss',
          tasks: ['sass:clmServer']
        },
        brainClientAssets: {
          files: ['<%= config.brainClientAssets %>/{,*/}{,*/}*'],
          tasks: ['copy:debug']
        },
        components: {
          files: ['<%= config.components %>/{,*/}{,*/}{,*/}*'],
          tasks: ['copy:debug']
        },
        filtered: {
          files: ['<%= config.gruntFiltered %>/{,*/}{,*/}{,*/}*'],
          tasks: ['copy:filtered']
        },
        livereload: {
          options: {
            livereload: LIVERELOAD_PORT
          },
          files: [
            '<%= config.gruntFiltered %>/{,*/}{,*/}*.html',
            '<%= config.components %>/{,*/}{,*/}{,*/}*.html',
            '<%= config.debug %>/assets/{,*/}{,*/}*.js',
            '<%= config.debug %>/assets/{,*/}{,*/}*.css'
          ]
        }
      }
    });

    grunt.registerTask('server', [
      'clean:debug',
      'configureProxies',
      'sass:clmServer',
      'copy:filtered',
      'copy:debug',
      'connect:clmServer',
      'open',
      'watch'
    ]);

    grunt.registerTask('test', [
      'jshint',
      'clean:server',
      'concurrent:copy',
      'karma'
    ]);

    grunt.registerTask('build', [
      'clean:dist',
      'useminPrepare',
      'sass:clmServer',
      'concurrent:copy',
      'concat',
      'uglify',
      'usemin'
    ]);

    grunt.registerTask('metrics', [
      'clean:debug',
      'configureProxies',
      'copy:debug',
      'connect:metrics',
      'phantomas:index'
    ]);

    grunt.registerTask('livingstyle', [
      'styleguide',
      'connect:styleguide'
    ]);

    grunt.registerTask('default', [
      'test',
      'build'
    ]);
  };
}());