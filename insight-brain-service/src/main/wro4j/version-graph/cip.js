/** @license
 * Copyright (c) 2012-2014 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/rhc/oss/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
/*global $, angular, Insight, Brain */
(function () {
  'use strict';

  function createStateFn(stateName) {
    return function (arg) {
      injector.invoke(['$rootScope', 'GAV', 'State', function ($rootScope, GAV, State) {
        $rootScope.$apply(function () {
          GAV.set(null);
          State.set(stateName, arg);
        });
      }]);
    };
  }

  function waitOnInjector(fn, args) {
    if (injector) {
      fn(args);
    } else {
      setTimeout(waitOnInjector(fn, args), 10);
    }
  }

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

  var injector = null,
      authHandler = null,
      module = angular.module('CIP', ['ngRoute']).config(['$routeProvider', function ($routeProvider) {
        $routeProvider.when('/', {
          templateUrl : '../../version-graph.html',
          controller : 'CIPController'
        });
        $routeProvider.otherwise({
          redirect : '/'
        });
      }]).run(['$rootScope', '$injector', function ($rootScope, $injector) {
        injector = $injector;

        $rootScope.setError = function (error) {
          $rootScope.errorMessage = getErrorMessage(error);
        };

        $rootScope.retryFn = function () {
          $rootScope.errorMessage = null;
          $rootScope.$broadcast('reload');
        };

        $rootScope.selectApplication = clmEndpoint.selectApplication;
        $rootScope.canMigrate = clmEndpoint.migrate;
        $rootScope.type = clmEndpoint.type;
      }]);

  $.ajaxSetup = function (ajaxConfig) {
    Insight.setHeaders(ajaxConfig.headers);
  };
  $.extend(true, window, {
    "Insight": {
      "clearGav": function () {
        waitOnInjector(function(){
          injector.invoke(['GAV', '$rootScope', function (GAV, $rootScope) {
            $rootScope.$apply(function () {
              GAV.set(null);
            });
          }]);
        });
      },
      "registerMarkUpgradeListener": function (listener) {
        waitOnInjector(function(){
          injector.invoke(['$rootScope', function ($rootScope) {
            $rootScope.$on('markUpgrade', function (event, gav) {
              listener(gav.groupId, gav.artifactId, gav.version);
            });
          }]);
        });
      },
      "registerViewDetailsListener": function (listener) {
        waitOnInjector(function(){
          injector.invoke(['GAV', 'SelectedApp', '$rootScope', function (GAV, SelectedApp, $rootScope) {
            $rootScope.$on('viewDetails', function (event, version) {
              var gav = GAV.get();

              listener(SelectedApp.get(),
                      gav.groupId,
                      gav.artifactId,
                      version,
                      gav.classifier,
                      gav.extension,
                      version === gav.version ? gav.hash : null,
                      version === gav.version ? gav.matchState : null,
                      gav.proprietary);
            });
          }]);
        });
      },
      "setGav": function (arg) {
        waitOnInjector(function(){
          injector.invoke(['GAV', '$rootScope', function (GAV, $rootScope) {
            $rootScope.$apply(function () {
              GAV.set(arg);
            });
          }]);
        });
      },
      "setHeaders" : function (headers) {
        waitOnInjector(function(){
          injector.invoke(['$http', '$rootScope', function ($http, $rootScope) {
            $rootScope.$apply(function () {
              angular.extend($http.defaults.headers.common, headers);
            });
          }]);
        });
      },
      "setError": function (arg) {
        waitOnInjector(function(){
          injector.invoke(['$rootScope', 'GAV', 'State', function ($rootScope, GAV, State) {
            $rootScope.$apply(function () {
              GAV.set(null);

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
          }]);
        });
      },
      "setAuthFailureHandler" : function (handler) {
        authHandler = hander;
      },
      "setPending": createStateFn('pending'),
      "setUnassigned": createStateFn('unassigned'),
      "setFiltered": createStateFn('filtered')
    }
  });

  module.service('GAV', function () {
    var selected = null,
        gav = null;
    return {
      get : function () {
        return gav;
      },
      set : function (g) {
        gav = g;
        selected = null;
      },
      getSelected : function () {
        return selected || gav;
      },
      setSelected : function (g) {
        selected = g;
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

  /**
   * Service to provide the selected application.  Persisted via cookies.
   */
  module.service('SelectedApp', ['GAV', function (GAV) {
    // Not cached as another browser tab could be touching the cookie
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
        } else {
          if (GAV.get()) {
            return GAV.get().appId;
          }
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
      }
    };
  }]);

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

  module.controller('ComponentController', ['$scope', 'GAV', 'SelectedApp', '$http', function ($scope, GAV, SelectedApp, $http) {
    function gavChanged() {
      var gav = GAV.get() ? angular.extend({ appId : SelectedApp.get() }, GAV.get()) : null;

      $scope.errorMessage = null;

      if (!angular.equals($scope.gav, gav)) {
        $scope.componentDetailsList = null;
        $scope.loaded = false;
        $scope.gav = gav;

        if (gav && gav.appId && !$scope.isUnknown() ) {
          $http.get(Brain[clmEndpoint.type].getComponentDetailsListUrl(gav)).success(function (data) {
            $scope.componentDetailsList = data.list ? data.list : data;
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

    $scope.isUnknown = function () {
      var gav = GAV.get(),
          matchState = gav && gav.matchState ? gav.matchState.toLowerCase() : null;
      return matchState === 'unknown';
    };

    $scope.$on('reload', function () {
      $scope.gav = null;
      gavChanged();
    });

    $scope.$watch(function () {
      return GAV.get();
    }, gavChanged);

    $scope.$watch(function () {
      return SelectedApp.get();
    }, gavChanged);
  }]);

  module.controller('StatusController', ['$scope', 'State', 'GAV', 'SelectedApp', function ($scope, State, GAV, SelectedApp) {
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

  module.controller('DetailsController', ['$scope', '$http', 'SelectedApp', 'GAV', function ($scope, $http, SelectedApp, GAV) {
    function gavChanged() {
      var gav = GAV.getSelected() ? angular.extend({ appId : SelectedApp.get() }, GAV.getSelected()) : null;

      if (!angular.equals(last, gav)) {
        $scope.componentDetails = null;
        last = gav;

        if (gav && gav.appId) {
          $http.get(Brain[clmEndpoint.type].getArtifactInfoUrl(gav)).success(function (data) {
            $scope.componentDetails = data;

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
          }).error(function () {
            $scope.setError(arguments);
          });
        }
      }
    }
    var last = {};

    $scope.isManual = function () {
      return $scope.componentDetails && $scope.componentDetails.identificationSource == 'Manual';
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
          return ' artifactInfoSecurityUnspecified';
        }
        else if ($scope.componentDetails.securityVulnerabilities[0].severity >= 8) {
          return ' artifactInfoSecurityCritical';
        }
        else if ($scope.componentDetails.securityVulnerabilities[0].severity >= 4) {
          return ' artifactInfoSecuritySevere';
        }
        else if ($scope.componentDetails.securityVulnerabilities[0].severity >= 0) {
          return ' artifactInfoSecurityModerate';
        }
        else {
          return ' artifactInfoSecurityModerate';
        }
      }
    };

    $scope.viewDetails = function () {
      $scope.$emit('viewDetails', GAV.getSelected().version);
    };

    $scope.markUpgrade = function () {
      $scope.$emit('markUpgrade', GAV.getSelected());
    };

    $scope.canMigrate = clmEndpoint.migrate;

    $scope.$on('reload', function () {
      last = {};
      gavChanged();
    });

    $scope.$watch(function () {
      return GAV.getSelected();
    }, gavChanged);

    $scope.$watch(function () {
      return SelectedApp.get();
    }, gavChanged);
  }]);

  module.directive('graph', ['GAV', function (GAV) {
    return {
      scope : {
        versions : '=graph'
      },
      template : '<div ng-show="versions">' +
                   '<div id="aiVersionChartContainer">' +
                     '<div id="aiVersionChartLabels"></div>' +
                     '<div id="aiVersionChartViz" style="overflow:hidden"></div>' +
                   '</div>' +
                   '<div id="custom">No Custom Metadata</div>' +
                 '</div>',
      link : function (scope, element) {
        scope.$watch('versions', function (versions) {
          if (versions) {
            $.each(versions, function(index, component) {
              if (component.version === GAV.get().version) {
                component.hash = GAV.get().hash;
                return false;
              }
            });

            Insight.ComponentInformation({
              data: {
                versions: versions,
                version: GAV.get().version
              },
              selectable: true,
              versionClick: function(version) {
                scope.$apply(function () {
                  $.each(versions, function(index, component) {
                    if (component.version === version) {
                      GAV.setSelected(component);
                      return false;
                    }
                  });
                });
              },
              versionDblClick: function(version) {
                scope.$emit('viewDetails', version);
              }
            });
            $('#custom').height($('#custom').height() + $('#detailsparent').outerHeight() -
                    $('#aiVersionChart').height());
          }
        });
      }
    };
  }]);

  module.directive('licenses', function () {
    return {
      scope : {
        licenses : '=',
        emptyText : '@'
      },
      template : '<span ng-repeat="license in licenses" class="license">{{license.licenseName}}</span>' +
                 '<span ng-if="licenses.length == 0">{{emptyText}}</span>'
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
        unit = 'Year';
      }
      else if (diff > 30 * 24 * 60 * 60 * 1000) {
        val = diff / (30 * 24 * 60 * 60 * 1000);
        unit = 'Month';
      }
      else if (diff > 24 * 60 * 60 * 1000) {
        val = diff / (24 * 60 * 60 * 1000);
        unit = 'Day';
      }
      else if (diff > 60 * 60 * 1000) {
        val = diff / (60 * 60 * 1000);
        unit = 'Hour';
      }
      else if (diff > 60 * 1000) {
        val = diff / (60 * 1000);
        unit = 'Minute';
      }
      else {
        return 'Seconds Ago';
      }
      val = Math.floor(val);
      if (val > 1) {
        unit += 's';
      }
      return val + ' ' + unit + ' Ago';
    };
  });

}());