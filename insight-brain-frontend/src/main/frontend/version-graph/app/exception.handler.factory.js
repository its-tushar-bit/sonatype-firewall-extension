/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
/*global angular */

function defaultLogFn(message) {
  logQueue.push(arguments);
  if (window.console) {
    window.console.error(message);
  }
}

var logQueue = [],
  logFn = defaultLogFn;

$.extend(true, window, {
  Insight: {
    /**
     * @since 1.12
     */
    setLogger: function (newLogFn) {
      // iterate over each exception
      $.each(logQueue, function (index, args) {
        setTimeout(function () {
          newLogFn.apply(null, args);
        }, 0);
      });
      // Assign logger
      logFn = newLogFn;
      logQueue = null;
    },
    /**
     * Resets the logger to the default, used for testing.
     * @since 1.12
     */
    resetLogger: function () {
      logQueue = [];
      logFn = defaultLogFn;
    },
  },
});

export default function exceptionHandler() {
  return function (exception) {
    var message = exception.toString(); // Should look something like - Error: Borked
    if (exception.stack) {
      message += '\n' + exception.stack; // non-standard but supported by recent major browsers (ie10+, webkit, etc.)
    }
    logFn.call(null, message);
  };
}
