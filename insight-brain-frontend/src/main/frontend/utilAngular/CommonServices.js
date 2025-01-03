/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import StableBodyService from './StableBodyService';
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

services.service('Messages', function () {
  return Messages;
});

services.service('Modal', [
  '$modal',
  function ($modal) {
    return {
      open(config) {
        return $modal.open({
          windowClass: 'iq-modal',
          backdropClass: 'iq-modal-backdrop',
          animation: false,
          ...config,
        });
      },
    };
  },
]);

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

services.filter('terseTimeSpan', function () {
  var rules = angular.extend(
    {
      diffFunction: function (date) {
        return date;
      },
    },
    timeAbbreviations
  );
  return new ElapsedTimeFilterFactory(rules);
});

export const terseAgo = new ElapsedTimeFilterFactory({
  ...timeAbbreviations,
  diffFunction: (date) => new Date().getTime() - date,
});

/**
 * English language abbreviations for elapsed time.
 */
services.filter('terseAgo', () => terseAgo);

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

services.service('timeAgoService', function () {
  return {
    renderDate: timeAgo,
  };
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

/**
 * Filter strings to fit within a set length, padding the end with ellipsis.
 * Default length is 25, but can be overridden.
 * i.e.
 * {{ value | truncate:20 }}
 *
 * Generally if you are filtering text, it's to ensure that it fits within some boundary element. CSS rules for that
 * element should take into account the possibility of increased font sizes on client machines and prefer to specify
 * boundary sizes in em.
 */
services.filter('truncate', function () {
  return function (text, length) {
    var end = '...';
    if (isNaN(length)) {
      length = 25;
    }
    if (text.length <= length) {
      return text;
    } else {
      return String(text).substring(0, length - end.length) + end;
    }
  };
});

services.service('BaseUrl', [
  function () {
    return {
      get: () => getBaseUrl(window.location.href),
    };
  },
]);

services.service('LastSelectedOrganization', [
  function () {
    var lastOrg = {};
    return {
      get: function () {
        return lastOrg;
      },
      set: function (org) {
        lastOrg = angular.copy(org);
      },
      clear: function () {
        lastOrg = {};
      },
    };
  },
]);

services.filter('EncodeURIComponent', [
  '$window',
  function ($window) {
    return $window.encodeURIComponent;
  },
]);

services.filter('safeDivide', function () {
  return function (value, max) {
    return max === 0 ? 0 : value / max;
  };
});

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

/**
 * Returns the last element of a path with the assumption that it is a file name. Path elements are assumed to be
 * delimited by a '/'.
 */
services.filter('fileName', function () {
  return function (path) {
    var pathDelimiter = '/';
    var stringPath = String(path);
    // Avoid checking the last character as paths might end in a delimiter.
    var lastIndexOfDelimiter = stringPath.lastIndexOf(pathDelimiter, stringPath.length - 2);

    if (lastIndexOfDelimiter > -1) {
      // If the last character is a delimiter, do not return it.
      if (stringPath.charAt(stringPath.length - 1) === pathDelimiter) {
        return stringPath.substring(lastIndexOfDelimiter + 1, stringPath.length - 1);
      }

      return stringPath.substring(lastIndexOfDelimiter + 1);
    }

    return stringPath;
  };
});

services.service('stable.body.service', StableBodyService);

export default services;
