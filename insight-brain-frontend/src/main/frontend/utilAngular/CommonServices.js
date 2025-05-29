/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { getBaseUrl } from '../util/urlUtil';

var services = angular.module('CommonServices', []);

export const Messages = {
  getHttpErrorMessage: function (args) {
    if (!args) {
      return;
    }
    if (typeof args === 'string') {
      return args;
    }
    if (angular.isArray(args) || args.toString() === '[object Arguments]') {
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
        headers = angular.isFunction(args.headers) ? args.headers() : args.headers;
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
      // Angular misses statusText (cf. https://github.com/angular/angular.js/pull/2665)
      // , so at least ensure message for typical proxy errors
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
services.filter('ago', function () {
  var rules = {
    year: 'year',
    month: 'month',
    day: 'day',
    hour: 'hour',
    minute: 'minute',
    seconds: 'seconds ago',
    highlightMultiples: true,
    separator: ' ',
    suffix: ' ago',
    diffFunction: function (date) {
      return new Date().getTime() - date;
    },
  };
  return new ElapsedTimeFilterFactory(rules);
});

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

/**
 * Intended to reduce the granularity of results from the 'ago' filter for cases where precision is not needed for the
 * last 24 hours.
 */
services.filter('agoLastDay', function () {
  return function (agoString) {
    if (agoString.indexOf('seconds ago') > -1 || agoString.indexOf('minute') > -1 || agoString.indexOf('hour') > -1) {
      return 'Less than a day ago';
    }
    return agoString;
  };
});

services.service('BaseUrl', [
  function () {
    return {
      get: () => getBaseUrl(window.location.href),
    };
  },
]);

services.service('ApplicationId', [
  '$state',
  function ($state) {
    return {
      encoded: function () {
        var applicationPublicId = $state.params.applicationPublicId;
        return applicationPublicId ? encodeURI(applicationPublicId) : null;
      },
      raw: function () {
        return $state.params.applicationPublicId;
      },
    };
  },
]);

services.service('OrganizationId', [
  '$state',
  function ($state) {
    return {
      encoded: function () {
        var organizationId = $state.params.organizationId;
        return organizationId ? encodeURI(organizationId) : null;
      },
      raw: function () {
        return $state.params.organizationId;
      },
    };
  },
]);

export default services;
