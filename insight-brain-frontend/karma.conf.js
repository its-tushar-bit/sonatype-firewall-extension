/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

// Karma configuration
// Generated on Tue Apr 14 2020 09:10:29 GMT-0400 (Eastern Daylight Time)

module.exports = function (config) {
  config.set({
    // base path that will be used to resolve all patterns (eg. files, exclude)
    basePath: '',

    // frameworks to use
    // available frameworks: https://npmjs.org/browse/keyword/karma-adapter
    frameworks: ['jasmine'],

    // list of files / patterns to load in the browser
    files: ['target/generated-resources/webpack/assets/test-bundle.js'],

    // list of files / patterns to exclude
    exclude: [],

    // test results reporter to use
    // possible values: 'dots', 'progress', plus those at https://npmjs.org/browse/keyword/karma-reporter
    reporters: ['summary', 'junit', 'coverage-istanbul'],

    client: {
      // remove or switch to true to see console logging from the tests in the output
      captureConsole: false,

      jasmine: {
        // the tests for the bundles outside of the main bundle depend on a bunch of global mutable state and
        // are unfortunately quite fragile. They are known to run correctly when run in alphabetical order, but
        // not necessarily in random order.
        random: false,

        // whether to stop suite execution when a spec fails
        stopOnSpecFailure: false,

        // Increase async test timeout for CI environments with resource contention
        timeoutInterval: 60000, // 60 seconds (default is 5 seconds)
      },
    },

    // web server port
    port: 9876,

    // enable / disable colors in the output (reporters and logs)
    colors: true,

    // level of logging
    // possible values: config.LOG_DISABLE || config.LOG_ERROR || config.LOG_WARN || config.LOG_INFO || config.LOG_DEBUG
    logLevel: config.LOG_INFO,

    // enable / disable watching file and executing tests whenever any file changes
    autoWatch: true,

    // start these browsers
    // available browser launchers: https://npmjs.org/browse/keyword/karma-launcher
    browsers: ['ChromeHeadless'],

    // Continuous Integration mode
    // if true, Karma captures browsers, runs the tests and exits
    singleRun: true,

    // Concurrency level
    // how many browser should be started simultaneous
    concurrency: Infinity,

    // Increase timeouts for CI environments with resource contention
    browserNoActivityTimeout: 120000, // 2 minutes
    captureTimeout: 120000,
    browserDisconnectTimeout: 30000,
    browserDisconnectTolerance: 3,

    coverageIstanbulReporter: {
      reports: ['text-summary', 'lcovonly'],
      fixWebpackSourcePaths: true,
      dir: 'target/coverage',
    },

    junitReporter: {
      outputDir: 'target/karma-reports',
      outputFile: 'jasmine.xml',
      useBrowserName: false,
    },
  });
};
