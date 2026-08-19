/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

export const Messages = {
  getHttpErrorMessage: function (args) {
    if (!args) {
      return;
    }
    if (typeof args === 'string') {
      return args;
    }
    if (Array.isArray(args) || args.toString() === '[object Arguments]') {
      args = {
        status: args[1],
        data: args[0],
        headers: args.length >= 3 ? args[2] : null,
      };
    }

    // handle axios error objects
    if (args.response) {
      return Messages.getHttpErrorMessage(args.response);
    } else {
      let message = '',
        headers = typeof args.headers === 'function' ? args.headers() : args.headers;
      if (args.status <= 0 || args.status >= 1000) {
        message = 'Unable to reach Sonatype IQ Server';
      } else if (
        args.data &&
        (!headers || !headers['content-type'] || headers['content-type'].indexOf('text/html') === -1)
      ) {
        message = args.data;

        if (typeof message === 'object') {
          message = message.message || 'Error';
        }
      }
      // Ensure message for typical proxy errors
      else if (args.status === 502) {
        message = 'Bad Gateway';
      } else if (args.status === 503) {
        message = 'Service Unavailable';
      } else if (args.status === 504) {
        message = 'Gateway Timeout';
      } else if (args.status) {
        message = 'Error ' + args.status;
      } else {
        message = 'Error';
      }
      return message;
    }
  },
};

/**
 * English language phrases for elapsed time.
 */

/**
 * English language abbreviations for time span.
 */
var timeAbbreviations = {
  year: 'y',
  month: 'mo',
  day: 'd',
  hour: 'h',
  minute: 'min',
  seconds: '1min',
  highlightMultiples: false,
  separator: '',
  suffix: '',
};

export const terseAgo = new ElapsedTimeFilterFactory({
  ...timeAbbreviations,
  diffFunction: (date) => new Date().getTime() - date,
});

export const timeAgo = new ElapsedTimeFunctionFactory({
  seconds: 'Just now',
  diffFunction: (date) => new Date().getTime() - date,
  year: 'year',
  month: 'month',
  day: 'day',
  hour: 'hour',
  minute: 'minute',
  highlightMultiples: true,
  separator: ' ',
  suffix: ' ago',
});

function ElapsedTimeFilterFactory(rules) {
  return function (date) {
    var timeAgo = new ElapsedTimeFunctionFactory(rules)(date);
    return timeAgo.age + rules.separator + timeAgo.qualifier;
  };
}

/**
 * Factory function to share elapsed time calculations while allowing for separate output formats.
 * @param rules
 * @returns {Function}
 * @constructor
 */
function ElapsedTimeFunctionFactory(rules) {
  return function (date) {
    var diff,
      unit,
      val,
      localRules = rules;

    if (!date) {
      return {
        age: '',
        qualifier: '',
      };
    }

    diff = localRules.diffFunction(date);

    if (diff > 12 * 30 * 24 * 60 * 60 * 1000) {
      val = diff / (12 * 30 * 24 * 60 * 60 * 1000);
      unit = localRules.year;
    } else if (diff > 30 * 24 * 60 * 60 * 1000) {
      val = diff / (30 * 24 * 60 * 60 * 1000);
      unit = localRules.month;
    } else if (diff > 24 * 60 * 60 * 1000) {
      val = diff / (24 * 60 * 60 * 1000);
      unit = localRules.day;
    } else if (diff > 60 * 60 * 1000) {
      val = diff / (60 * 60 * 1000);
      unit = localRules.hour;
    } else if (diff > 60 * 1000) {
      val = diff / (60 * 1000);
      unit = localRules.minute;
    } else {
      return {
        age: '',
        qualifier: localRules.seconds,
      };
    }
    val = Math.floor(val);
    if (rules.highlightMultiples) {
      if (val > 1) {
        unit += 's';
      }
    }

    return {
      age: val,
      qualifier: unit + localRules.suffix,
    };
  };
}
