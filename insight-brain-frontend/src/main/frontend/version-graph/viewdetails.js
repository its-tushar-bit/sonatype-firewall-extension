/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
/*global angular, window, Brain, clmEndpoint */
var query,
  module,
  injector,
  logQueue = [],
  logFn = defaultLogFn;

function toLicenseNames(licenses) {
  var names = [];
  angular.forEach(licenses, function (license) {
    names.push(license.licenseName);
  });
  return names;
}

function decode(encodedString) {
  return decodeURIComponent((encodedString || '').replace(/\+/g, '%20'));
}

function compareStringProperty(a, b, field) {
  var aUpper = a[field].toUpperCase(),
    bUpper = b[field].toUpperCase();
  return aUpper < bUpper ? -1 : aUpper > bUpper ? 1 : 0;
}

function defaultLogFn(message) {
  logQueue.push(arguments);
  if (window.console) {
    window.console.error(message);
  }
}

query = (function () {
  var search = window.location.search,
    result = {};
  if (search.length === 0) {
    return result;
  }
  search = search.substring(1).split('&');
  angular.forEach(search, function (item) {
    var field = item.split('=');
    result[decode(field[0])] = decode(field[1]);
  });
  return result;
})();

function transformPolicyAlerts(alerts) {
  var retval = [];
  angular.forEach(alerts, function (alert) {
    var threat;
    if (alert.trigger.threatLevel > 7) {
      threat = 4;
    } else if (alert.trigger.threatLevel > 3) {
      threat = 3;
    } else if (alert.trigger.threatLevel > 1) {
      threat = 2;
    } else if (alert.trigger.threatLevel === 1) {
      threat = 1;
    } else {
      threat = 0;
    }
    angular.forEach(alert.trigger.componentFacts, function (componentFact) {
      angular.forEach(componentFact.constraintFacts, function (constraintFact) {
        angular.forEach(constraintFact.conditionFacts, function (conditionFact) {
          retval.push({
            policyName: alert.trigger.policyName,
            threat: threat,
            constraintName: constraintFact.constraintName,
            reason: conditionFact.reason,
          });
        });
      });
    });
  });
  retval.sort(function (a, b) {
    var retVal;
    if (a.threat !== b.threat) {
      return b.threat - a.threat;
    }
    retVal = compareStringProperty(a, b, 'policyName');
    if (retVal !== 0) {
      return retVal;
    }
    return compareStringProperty(a, b, 'constraintName');
  });
  return retval;
}

function waitOnInjector(method) {
  if (injector) {
    injector.invoke(method);
  } else {
    setTimeout(function () {
      waitOnInjector(method);
    }, 10);
  }
}

function safeApply(scope, fn) {
  if (scope.$$phase || scope.$root.$$phase) {
    // already apply in progress, just call the function
    fn();
  } else {
    // otherwise wrap the function in apply
    scope.$apply(fn);
  }
}

angular.extend(window, {
  setClmHeaders: function setClmHeaders(headers) {
    waitOnInjector([
      '$rootScope',
      '$http',
      function ($rootScope, $http) {
        angular.extend($http.defaults.headers.common, headers);
        safeApply($rootScope, function () {
          $rootScope.$broadcast('reload');
        });
      },
    ]);
  },
  /**
   * @since 1.12
   */
  Insight: {
    setLogger: function (newLogFn) {
      // iterate over each exception
      angular.forEach(logQueue, function (args) {
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

module = angular.module('viewdetails', []).run([
  '$injector',
  function ($injector) {
    injector = $injector;
  },
]);

module.constant('query', query);
module.controller('view', [
  '$http',
  '$scope',
  'query',
  '$q',
  function ($http, $scope, query, $q) {
    $scope.$on('reload', function () {
      $scope.reload();
    });

    var appId = query.appId,
      deferLoad = query.deferLoad,
      identifier = query.componentIdentifier ? JSON.parse(query.componentIdentifier) : null,
      hash = query.hash,
      proprietary = query.proprietary,
      matchState = query.matchState;

    if (identifier === null) {
      identifier = query.groupId
        ? {
            format: 'maven',
            coordinates: {
              groupId: query.groupId,
              artifactId: query.artifactId,
              version: query.version,
              classifier: query.classifier,
              extension: query.extension,
            },
          }
        : {};
    }

    // TODO Determine where the GAV is coming from, should it be a query string or should Eclipse call a JS function?
    $scope.data = null;

    $scope.linkTarget = clmEndpoint.linkTarget;

    function getErrorMessage(data, status, headersFn) {
      var message = '',
        headers = headersFn ? headersFn() : {};
      if (status === 0 || status >= 1000) {
        message = 'Network error while contacting server';
      } else if (data && headers['content-type'] && headers['content-type'].indexOf('text/plain') >= 0) {
        message = data;
      } else if (status === 502) {
        message = 'Bad Gateway';
      } else if (status === 503) {
        message = 'Service Unavailable';
      } else if (status === 504) {
        message = 'Gateway Timeout';
      } else {
        message = 'Error ' + status;
      }
      return message;
    }
    $scope.reload = function () {
      $scope.error = null;
      $scope.errorMessage = null;

      var promises = [];

      promises.push(
        $http.get(
          Brain[clmEndpoint.type].getComponentUrl(
            'application',
            appId,
            identifier.format,
            hash,
            matchState,
            proprietary,
            identifier.coordinates
          ),
          {
            headers: {
              Accept: 'application/json',
            },
          }
        )
      );
      if (clmEndpoint.showContext) {
        promises.push($http.get(Brain.getApplicationListUrl()));
      }

      $q.all(promises).then(
        function (results) {
          $scope.data = results[0].data;
          $scope.data.observedLicenses = toLicenseNames($scope.data.observedLicenses);
          $scope.data.declaredLicenses = toLicenseNames($scope.data.declaredLicenses);
          $scope.data.overriddenLicenses = toLicenseNames($scope.data.overriddenLicenses);
          $scope.data.policyAlerts = transformPolicyAlerts($scope.data.policyAlerts);
          angular.forEach($scope.data.securityVulnerabilities, function (item) {
            if (item.severity !== null) {
              item.severity = Math.floor(item.severity);
            }
          });
          $scope.data.securityVulnerabilities.sort(function (a, b) {
            if (b.severity === null) {
              return a.severity === null ? 0 : -1;
            } else if (a.severity === null) {
              return 1;
            }
            return b.severity - a.severity;
          });
          if (clmEndpoint.showContext) {
            $scope.data.appName = results[1].data[appId];
          }
        },
        function (errorData) {
          $scope.error = errorData.status;
          $scope.errorMessage = getErrorMessage(errorData.data, errorData.status, errorData.headers);
        }
      );
    };
    if (deferLoad !== 'true') {
      $scope.reload();
    }

    $scope.isSvGrouped = function (index) {
      if (index === 0) {
        return false;
      }
      return (
        $scope.data.securityVulnerabilities[index - 1].severity === $scope.data.securityVulnerabilities[index].severity
      );
    };
    $scope.getSvUrl = function (item) {
      if (item.url) {
        return item.url;
      }
      if (clmEndpoint.type === 'rm') {
        if (item.source === 'osvdb') {
          return 'http://osvdb.org/' + item.refId;
        }
        if (item.source === 'cve') {
          return 'http://cve.mitre.org/cgi-bin/cvename.cgi?name=' + item.refId;
        }
      } else {
        // clmEndpoint.type === 'ide'
        return '../../../index.html#/vulnerabilities/' + item.refId;
      }
    };
    $scope.getSvName = function (issue) {
      var retVal = issue.refId.toUpperCase();
      if (retVal.indexOf(issue.source.toUpperCase()) !== 0) {
        retVal = issue.source.toUpperCase() + '-' + retVal;
      }
      return retVal;
    };
    $scope.isPolicyGrouped = function (index) {
      if (index === 0) {
        return false;
      }
      return $scope.data.policyAlerts[index - 1].policyName === $scope.data.policyAlerts[index].policyName;
    };
    $scope.isConstraintGrouped = function (index) {
      if (index === 0) {
        return false;
      }
      return $scope.data.policyAlerts[index - 1].constraintName === $scope.data.policyAlerts[index].constraintName;
    };
    $scope.showContext = function () {
      return clmEndpoint.showContext;
    };
  },
]);

module.factory('$exceptionHandler', function () {
  return function (exception) {
    var message = exception.toString(); // Should look something like - Error: Borked
    if (exception.stack) {
      message += '\n' + exception.stack; // non-standard but supported by recent major browsers (ie10+, webkit, etc.)
    }
    logFn.call(null, message);
  };
});

export default module;
