/**
 * @license Copyright (c) 2012-2014 Sonatype, Inc. All rights reserved. Includes
 * the third-party code listed at
 * http://links.sonatype.com/products/clm/attributions. "Sonatype" is a
 * trademark of Sonatype, Inc.
 */
'use strict';

var LIVERELOAD_PORT = 35729;
var liveReloadSnippet = require('connect-livereload')({ port: LIVERELOAD_PORT });
var proxySnippet = require('grunt-connect-proxy/lib/utils').proxyRequest;
var mountFolder = function(connect, dir) {
  return connect.static(require('path').resolve(dir));
};
var config = {
  components: 'src/main/resources/assets',
  filtered: 'src/main/filtered-resources/assets',
  gruntFiltered: 'grunt/filtered',
  assets: 'src/main/wro4j',
  dist: 'grunt/working/dist',
  tmp: 'grunt/working/.tmp',
  debug: 'grunt/working/debug'
};

module.exports = function(grunt) {
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
        '<%= config.assets %>/{,*/}*.js'
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
        hostname: 'localhost'
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
      livereload: {
        options: {
          middleware: function(connect) {
            return [
              liveReloadSnippet,
              proxySnippet,
              mountFolder(connect, config.components),
              mountFolder(connect, config.gruntFiltered),
              mountFolder(connect, config.debug),
            ];
          }
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
              mountFolder(connect, config.gruntFiltered),
              mountFolder(connect, config.debug),
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
            '{,*/}{,*/}*.html',
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
          cwd: '<%= config.assets %>/',
          dest: '<%= config.debug %>/assets',
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
    useminPrepare: {
      src: ['<%= config.gruntFiltered %>/assets/index.html', '<%= config.gruntFiltered %>/assets/reports.html'],
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
    watch: {
      assets: {
        files: ['<%= config.assets %>/{,*/}{,*/}*'],
        tasks: ['copy:debug']
      },
      components: {
        files: ['<%= config.components %>/{,*/}{,*/}{,*/}*'],
        tasks: ['copy:debug']
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
    'copy:debug',
    'connect:livereload',
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

  grunt.registerTask('default', [
    'test',
    'build'
  ]);
};