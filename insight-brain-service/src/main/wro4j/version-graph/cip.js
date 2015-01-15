/** @license
 * Copyright (c) 2012-2014 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
/*global $, angular, Insight, Brain, clmEndpoint, window */
(function () {
  'use strict';

  function isInvalidAppId(status) {
    // Eclipse plugin 2.0 goes against the SaaS which returns 402, Eclipse plugin 2.1+ goes against the Brain which returns 404
    return Brain.getVersion ? (status === 404 || status === 403) : (status === 402);
  }

  function getErrorMessage(error) {
    var responseText = error[0],
        status = error[1],
        headers = error[2];

    if (status === 0 || status >= 1000) {
      return 'Network error while contacting server';
    }
    else if (responseText && (headers('Content-Type') || '').indexOf('text/plain') >= 0) {
      return responseText;
    }
    else {
      return 'Error ' + status;
    }
  }

  function defaultLogFn(message) {
    logQueue.push(arguments);
    if (window.console) {
      window.console.error(message);
    }
  }

  var injector = null,
      authHandler = null,
      injectorTimeout = null,
      logQueue = [],
      logFn = defaultLogFn,
      module = angular.module('CIP', []).run(['$rootScope', '$injector', function ($rootScope, $injector) {
        injector = $injector;

        $rootScope.setError = function (error) {
          $rootScope.errorMessage = getErrorMessage(error);
        };

        $rootScope.retryFn = function () {
          $rootScope.errorMessage = null;
          $rootScope.$broadcast('reload');
        };

        $rootScope.selectApplication = clmEndpoint.selectApplication;
        $rootScope.migrateSupported = clmEndpoint.migrate;
        $rootScope.viewDetailsSupported = clmEndpoint.viewDetails;
        $rootScope.type = clmEndpoint.type;
      }]);

  /**
   * Waits on the AngularJS application to boot then calls the specified function
   *
   * fn - AngularJS function to call
   * single - only the last function with single set to true will be called
   */
  function waitOnInjector(fn, single) {
    if (single && injectorTimeout) {
      clearTimeout(injectorTimeout);
      injectorTimeout = null;
    }

    if (injector) {
      injector.invoke(fn);
    }
    else {
      var timeout = setTimeout(function() {
        waitOnInjector(fn);
      }, 10);
      if (single) {
        injectorTimeout = timeout;
      }
    }
  }

  function createStateFn(stateName) {
    return function (arg) {
      waitOnInjector(['$rootScope', 'Coordinates', 'State', function ($rootScope, Coordinates, State) {
        safeApply($rootScope, function () {
          Coordinates.set(null);
          State.set(stateName, arg);
        });
      }], true);
    };
  }

  function safeApply(scope, fn) {
    if (scope.$$phase || scope.$root.$$phase) {
      //already apply in progress, just call the function
      fn();
    }
    else {
      //otherwise wrap the function in apply
      scope.$apply(fn);
    }
  }

  var ajaxSetup = $.ajaxSetup;
  $.ajaxSetup = function (ajaxConfig) {
    if (ajaxConfig && ajaxConfig.headers) {
      Insight.setHeaders(ajaxConfig.headers);
    }
    return ajaxSetup.apply($, arguments);
  };
  $.extend(true, window, {
    "Insight": {
      "clearGav": createStateFn(null),
      "registerMarkUpgradeListener": function (listener) {
        waitOnInjector(['Coordinates', '$rootScope', function (Coordinates, $rootScope) {
          $rootScope.$on('markUpgrade', function (event, gav) {
            if (Coordinates.getFormat() === 'maven') {
              listener(gav.groupId, gav.artifactId, gav.version);
            }
          });
        }]);
      },
      "registerViewDetailsListener": function (listener) {
        waitOnInjector(['Coordinates', 'Properties', 'SelectedApp', '$rootScope', function (Coordinates, Properties, SelectedApp, $rootScope) {
          $rootScope.$on('viewDetails', function (event, version) {

            if (Coordinates.getFormat() === 'maven') {
              var gav = Coordinates.get();

              listener(SelectedApp.get(),
                      gav.groupId,
                      gav.artifactId,
                      version,
                      gav.classifier,
                      gav.extension,
                      version === gav.version ? Properties.getHash() : null,
                      version === gav.version ? Properties.getMatchState() : null,
                      Properties.getProprietary());
            }
          });
        }]);
      },
      "registerCoordsViewDetailsListener": function (listener) {
        waitOnInjector(['Coordinates', 'SelectedApp', 'Properties', '$rootScope', function (Coordinates, SelectedApp, Properties, $rootScope) {
          $rootScope.$on('viewDetails', function (event, version) {
            var coordinates = [],
                origVersion;

            angular.forEach(Coordinates.get(), function (value, field) {
              coordinates.push(field);

              if ('version' === field) {
                coordinates.push(version);
                origVersion = value;
              }
              else {
                coordinates.push(value);
              }
            });

            listener(SelectedApp.get(), Coordinates.getFormat(), coordinates,
                    version === origVersion ? Properties.getHash() : null,
                    version === origVersion ? Properties.getMatchState() : null,
                    Properties.getProprietary());
          });
        }]);
      },
      "registerCoordsMarkUpgradeListener": function (listener) {
        waitOnInjector(['$rootScope', function ($rootScope) {
          $rootScope.$on('markUpgrade', function (event, gav) {
            listener(gav.groupId, gav.artifactId, gav.version);
          });
        }]);
      },
      /**
       * @since 1.13.0
       */
      'setCoordinates' : function (componentType, coordinates, properties) {
        properties = properties || {};
        waitOnInjector(['Coordinates', 'SelectedApp', 'State', 'Properties', '$rootScope', function (Coordinates, SelectedApp, State, Properties, $rootScope) {
          safeApply($rootScope, function () {
            Coordinates.set(componentType, coordinates ? coordinates : {}); //coordinates may be null for unknown
            State.set(null);

            Properties.reset();
            Properties.setMatchState(properties.matchState);
            Properties.setProprietary(properties.proprietary);
            Properties.setFilename(properties.filename);
            Properties.setHash(properties.hash);

            if (properties.appId) {
              SelectedApp.set(properties.appId);
            }
          });
        }], true);
      },
      /**
       * @deprecated since 1.13.0 Included for backwards compatibility with existing clients
       */
      "setGav": function (arg) {
        waitOnInjector(['Coordinates', 'SelectedApp', 'State', 'Properties', '$rootScope', function (Coordinates, SelectedApp, State, Properties, $rootScope) {
          safeApply($rootScope, function () {
            if (arg.appId) {
              SelectedApp.set(arg.appId);
            }
            State.set(null);

            Properties.reset();
            Properties.setMatchState(arg.matchState);
            Properties.setProprietary(arg.proprietary);
            Properties.setFilename(arg.filename);
            Properties.setHash(arg.hash);

            var gav = {
              groupId : arg.groupId,
              artifactId : arg.artifactId,
              version : arg.version
            };
            if (arg.extension) {
              gav.extension = arg.extension;
            }
            if (arg.classifier) {
              gav.classifier = arg.classifier;
            }
            Coordinates.set('maven', gav);
          });
        }], true);
      },
      "setHeaders" : function (headers) {
        waitOnInjector(['$http', '$rootScope', function ($http, $rootScope) {
          safeApply($rootScope, function () {
            angular.extend($http.defaults.headers.common, headers);
          });
        }]);
      },
      "setError": function (arg) {
        waitOnInjector(['$rootScope', 'Coordinates', 'State', function ($rootScope, Coordinates, State) {
          safeApply($rootScope, function () {
            Coordinates.set(null);

            if (isInvalidAppId(arg.errorCode)) {
              State.set('invalid-appid', arg);
            }
            else if (arg.errorCode === 401) {
              State.set('invalid-credentials', arg);
            }
            else {
              State.set('failure', arg);
            }
          });
        }], true);
      },
      "setAuthFailureHandler" : function (handler) {
        authHandler = handler;
      },
      "setPending": createStateFn('pending'),
      "setUnassigned": createStateFn('unassigned'),
      "setFiltered": createStateFn('filtered'),
      /**
       * @since 1.12
       */
      "setLogger" : function (newLogFn) {
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
      'resetLogger' : function () {
        logQueue = [];
        logFn = defaultLogFn;
      }
    }
  });

  module.service('Coordinates', function () {
    var selected = null,
        coordinates = null,
        format = null;
    return {
      get : function () {
        return coordinates;
      },
      getFormat : function () {
        return format;
      },
      set : function (t, c) {
        coordinates = c;
        format = t;
        selected = null;
      },
      getSelected : function () {
        return selected || coordinates;
      },
      setSelected : function (c) {
        if (c && coordinates && c.version === coordinates.version) {
          selected = null;
        } else {
          selected = c;
        }
      }
    };
  });

  module.service('State', function () {
    var state = null,
        arg = null;
    return {
      get : function () {
        return state;
      },
      getArgs : function () {
        return arg;
      },
      set : function (newState, newArg) {
        state = newState;
        arg = newArg;
      }
    };
  });

  module.service('Properties', function () {
    var properties = {};
    return {
      getFilename : function () {
        return properties.filename;
      },
      getHash : function () {
        return properties.hash;
      },
      getMatchState : function () {
        return properties.matchState;
      },
      getProprietary : function () {
        return properties.proprietary;
      },
      reset : function () {
        properties = {};
      },
      setFilename : function (filename) {
        properties.filename = filename;
      },
      setHash : function (hash) {
        properties.hash = hash;
      },
      setMatchState : function (matchState) {
        properties.matchState = matchState;
      },
      setProprietary : function (proprietary) {
        properties.proprietary = proprietary;
      },
      isUnknown : function () {
        return (properties.matchState || '').toLowerCase() === 'unknown';
      }
    };
  });

  /**
   * Service to provide the selected application.  Persisted via cookies.
   */
  module.service('SelectedApp', function () {
    // Not cached as another browser tab could be touching the cookie
    var storedAppId = null;
    return {
      get : function () {
        if (clmEndpoint.selectApplication) {
          var clmAppId = null;

          $.each(document.cookie.split(';'), function (index, cookie) {
            cookie = cookie.split('=');
            if (cookie[0] === 'clmAppId') {
              clmAppId = cookie[1];
              return false;
            }
          });

          return clmAppId;
        }
        else {
          return storedAppId;
        }
      },
      set : function (applicationId) {
        if (clmEndpoint.selectApplication) {
          if (applicationId) {
            var date = new Date();
            date.setTime(date.getTime() + (60 * 60 * 24 * 365));
            document.cookie = 'clmAppId=' + applicationId + '; expires=' + date.toGMTString();
          } else {
            document.cookie = 'clmAppId=; expires=Thu, 01-Jan-70 00:00:01 GMT;';
          }
        }
        else {
          storedAppId = applicationId;
        }
      }
    };
  });

  module.service('Applications', ['$http', '$q', function ($http, $q) {
    var deferred = null;
    return {
      get : function () {
        if (deferred === null) {
          deferred = $q.defer();
          $http.get(Brain.getApplicationListUrl()).success(function (data) {
            deferred.resolve(data);
          }).error(function () {
            deferred.reject(arguments);
            deferred = null; // all future requests to retrigger
          });
        }
        return deferred.promise;
      }
    };
  }]);

  module.controller('CIPController', ['$scope', 'SelectedApp', function ($scope, SelectedApp) {
    $scope.canLoad = function () {
      return !$scope.selectApplication || SelectedApp.get();
    };
    $scope.linkTarget = clmEndpoint.linkTarget;
  }]);

  module.controller('ApplicationController', ['$scope', 'Applications', 'SelectedApp', function ($scope, Applications, SelectedApp) {
    $scope.doLoad = function () {
      Applications.get().then(function (data) {
        $scope.applications = data;
      }, function (error) {
        $scope.setError(error);
      });
    };

    $scope.applications = null;
    $scope.selectedApplication = SelectedApp.get();

    $scope.$on('reload', function () {
      $scope.doLoad();
    });

    // Monitor user's change to dropdown
    $scope.$watch('selectedApplication', function (app) {
      SelectedApp.set(app);
    });

    $scope.doLoad();
  }]);

  module.controller('ComponentController', ['$scope', 'Coordinates', 'SelectedApp', 'Properties', '$http', function ($scope, Coordinates, SelectedApp, Properties, $http) {
    function coordinatesChanged() {
      var coordinates = Coordinates.get() ? { coordinates : Coordinates.get(), appId : SelectedApp.get() } : null;

      $scope.errorMessage = null;

      if (!angular.equals($scope.coordinates, coordinates)) {
        $scope.componentDetailsList = null;
        $scope.loaded = false;
        $scope.coordinates = coordinates;

        if (coordinates && coordinates.appId && !Properties.isUnknown()) {
          $http.get(Brain[clmEndpoint.type].getComponentListUrl(SelectedApp.get(), Coordinates.getFormat(), Properties.getHash(), Properties.getMatchState(), Properties.getProprietary(), Coordinates.get())).success(function (data) {
            $scope.componentDetailsList = data.list ? data.list : data;
            for (var i = 0; i < $scope.componentDetailsList.length; i++) {
              $scope.componentDetailsList[i].proprietary = Coordinates.get().proprietary;
            }
            $scope.loaded = true;
          }).error(function () {
            $scope.setError(arguments);
          });
        }
      }
    }

    $scope.setError = function (error) {
      $scope.errorMessage = getErrorMessage(error);
    };

    $scope.retryFn = function () {
      $scope.$broadcast('reload');
    };

    $scope.$on('reload', function () {
      $scope.coordinates = null;
      coordinatesChanged();
    });

    $scope.$watch(function () {
      return Properties.isUnknown();
    }, function () {
      $scope.isUnknown = Properties.isUnknown();
    });

    $scope.$watch(function () {
      return Coordinates.get();
    }, coordinatesChanged);

    $scope.$watch(function () {
      return SelectedApp.get();
    }, coordinatesChanged);
  }]);

  module.controller('StatusController', ['$scope', 'State', 'Coordinates', 'SelectedApp', function ($scope, State, Coordinates, SelectedApp) {
    $scope.openView = function ($event, action) {
      $event.preventDefault();
      clmEndpoint.openView(action);
    };

    $scope.$watch(function () {
      return State.get();
    }, function (state) {
      $scope.state = state;
    });

    $scope.$watch(function () {
      return State.getArgs();
    }, function (stateArgs) {
      $scope.stateArgs = stateArgs;
    });

    $scope.$watch(function () {
      return SelectedApp.get();
    }, function (selectedApp) {
      $scope.selectedApp = selectedApp;
    });
  }]);

  module.controller('DetailsController', ['$scope', '$http', 'SelectedApp', 'Coordinates', 'Properties', function ($scope, $http, SelectedApp, Coordinates, Properties) {
    function coordinatesChanged() {
      var coordinates = Coordinates.getSelected() ? { coordinates : Coordinates.getSelected(), appId : SelectedApp.get() } : null;

      if (!angular.equals(last, coordinates)) {
        $scope.componentDetails = null;
        $scope.highestPolicyThreat = null;
        last = coordinates;

        if (coordinates && coordinates.appId && !Properties.isUnknown()) {
          $http.get(Brain[clmEndpoint.type].getComponentUrl(SelectedApp.get(), Coordinates.getFormat(), Properties.getHash(), Properties.getMatchState(), Properties.getProprietary(), coordinates.coordinates)).success(function (data) {
            $scope.componentDetails = data;
            $scope.componentDetails.proprietary = Coordinates.getSelected().proprietary;

            var i = 0;
            while (i < $scope.componentDetails.securityVulnerabilities.length) {
              if ($scope.componentDetails.securityVulnerabilities[i].status === 'Not Applicable') {
                $scope.componentDetails.securityVulnerabilities.splice(i, 1);
              } else {
                i++;
              }
            }

            $scope.componentDetails.securityVulnerabilities.sort(function (a, b) {
              if (a.severity === b.severity) {
                return 0;
              } else if (a.severity === null) {
                return 1;
              } else if (b.severity === null) {
                return -1;
              }
              return b.severity - a.severity;
            });

            $scope.componentDetails.policyAlerts.sort(function(alertA, alertB) {
              return alertB.trigger.threatLevel - alertA.trigger.threatLevel;
            });
            $scope.highestPolicyThreat = {
              level: $scope.componentDetails.policyAlerts.length > 0 ? $scope.componentDetails.policyAlerts[0].trigger.threatLevel : null,
              violatedPolicies: $scope.componentDetails.policyAlerts.length
            };
          }).error(function () {
            $scope.setError(arguments);
          });
        }
      }
    }
    var last = {};

    $scope.isManual = function () {
      return $scope.componentDetails && $scope.componentDetails.identificationSource === 'Manual';
    };

    $scope.canMigrate = function () {
      var coordinates = Coordinates.get(),
          selected = Coordinates.getSelected();

      return coordinates && selected && coordinates.version !== selected.version;
    };

    $scope.getMaximumSeverity = function () {
      if ($scope.componentDetails) {
        if ($scope.componentDetails.securityVulnerabilities.length === 0) {
          return 'NA';
        }
        else if ($scope.componentDetails.securityVulnerabilities[0].severity === null) {
          return 'Unscored';
        }
        else {
          return $scope.componentDetails.securityVulnerabilities[0].severity;
        }
      }
    };

    $scope.getColorClass = function () {
      if ($scope.componentDetails) {
        if ($scope.componentDetails.securityVulnerabilities.length === 0) {
          return ' unspecified';
        }
        else if ($scope.componentDetails.securityVulnerabilities[0].severity >= 7) {
          return ' critical';
        }
        else if ($scope.componentDetails.securityVulnerabilities[0].severity >= 4) {
          return ' severe';
        }
        else {
          return ' moderate';
        }
      }
    };

    $scope.viewDetails = function () {
      $scope.$emit('viewDetails', Coordinates.getSelected().version);
    };

    $scope.markUpgrade = function () {
      $scope.$emit('markUpgrade', Coordinates.getSelected());
    };

    $scope.$on('reload', function () {
      last = {};
      coordinatesChanged();
    });

    $scope.$watch(function () {
      return Coordinates.getSelected();
    }, coordinatesChanged);

    $scope.$watch(function () {
      return SelectedApp.get();
    }, coordinatesChanged);
  }]);

  module.directive('graph', ['Coordinates', function (Coordinates) {
    return {
      scope : {
        versions : '=graph'
      },
      template : '<div ng-show="versions">' +
                   '<div id="aiVersionChartContainer">' +
                     '<div id="aiVersionChartLabels"></div>' +
                     '<div id="aiVersionChartViz" style="overflow:hidden"></div>' +
                   '</div>' +
                 '</div>',
      link : function (scope) {
        scope.$watch('versions', function (versions) {
          if (versions) {
            $.each(versions, function(index, component) {
              if (component.version === Coordinates.get().version) {
                component.hash = Coordinates.get().hash;
                return false;
              }
            });

            Insight.ComponentInformation({
              data: {
                nextMajorRevisionIndex : versions.nextMajorRevisionIndex,
                versions: versions,
                version: Coordinates.get().version
              },
              selectable: true,
              versionClick: function(version) {
                scope.$apply(function () {
                  $.each(versions, function(index, component) {
                    if (component.componentIdentifier.coordinates.version === version) {
                      Coordinates.setSelected(component.componentIdentifier.coordinates);
                      return false;
                    }
                  });
                });
              },
              versionDblClick: function(version) {
                scope.$emit('viewDetails', version);
              }
            });
          }
        });
      }
    };
  }]);

  module.directive('licenses', function () {
    return {
      scope : {
        licenses : '=',
        status : '@',
        emptyText : '@'
      },
      template : '<span ng-repeat="license in licenses" class="license">{{license.licenseName}}{{!$last ? "," : ""}}</span>' +
                 '<span ng-if="licenses.length == 0">{{emptyText}}</span>' +
                 '<span ng-if="status" class="clm-license-status {{status | lowercase}}">{{status}}</span>'
    };
  });

  // Copied from our AngularCommon library
  module.filter('ago', function() {
    return function(date) {
      var ago = '',
          diff,
          unit,
          val;

      if (!date) {
        return ago;
      }
      diff = new Date().getTime() - date;

      if (diff > 12 * 30 * 24 * 60 * 60 * 1000) {
        val = diff / (12 * 30 * 24 * 60 * 60 * 1000);
        unit = 'year';
      }
      else if (diff > 30 * 24 * 60 * 60 * 1000) {
        val = diff / (30 * 24 * 60 * 60 * 1000);
        unit = 'month';
      }
      else if (diff > 24 * 60 * 60 * 1000) {
        val = diff / (24 * 60 * 60 * 1000);
        unit = 'day';
      }
      else if (diff > 60 * 60 * 1000) {
        val = diff / (60 * 60 * 1000);
        unit = 'hour';
      }
      else if (diff > 60 * 1000) {
        val = diff / (60 * 1000);
        unit = 'minute';
      }
      else {
        return 'seconds ago';
      }
      val = Math.floor(val);
      if (val > 1) {
        unit += 's';
      }
      return val + ' ' + unit + ' ago';
    };
  });

  // Copied from our AngularCommon library
  module.filter('agoLastDay', function() {
    return function(agoString) {
      if(agoString.indexOf('seconds ago') > -1 || agoString.indexOf('minute') > -1 || agoString.indexOf('hour') > -1){
        return 'Less than a day ago';
      }
      return agoString;
    };
  });

  module.factory('$exceptionHandler', function () {
    return function (exception) {
      var message = exception.toString(); // Should look something like - Error: Borked
      if (exception.stack) {
        message += '\n' + exception.stack; // non-standard but supported by recent major browsers (ie10+, webkit, etc.)
      }
      logFn.call(null, message);
    };
  });

  module.filter('namePart', function () {
    return function (input) {
      if (angular.isArray(input)) {
        var result = [];
        angular.forEach(input, function (part) {
          if (part.field) {
            result.push(part);
          }
        });
        return result;
      }
      return input;
    };
  });
}());
