/**
 * @license Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
/*global $, angular, Insight, Brain, clmEndpoint, window */
(function () {
  'use strict';

  function isInvalidAppId(status) {
    // Eclipse plugin 2.0 goes against the HDS which returns 402, Eclipse plugin 2.1+ goes against the Brain which returns 404
    return Brain.getVersion ? (status === 404 || status === 403) : (status === 402);
  }

  var injector = null,
      authHandler = null,
      injectorTimeout = null;

  angular.module('version.graph.app', ['version.graph']).run(['$rootScope', '$injector', 'ErrorMessage', function ($rootScope, $injector, errorMessage) {
    injector = $injector;

    $rootScope.setError = function (error) {
      $rootScope.errorMessage = errorMessage(error);
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
      waitOnInjector(['$rootScope', 'Coordinates', 'State', 'Properties', function ($rootScope, Coordinates, State, Properties) {
        safeApply($rootScope, function () {
          Coordinates.set(null);
          Properties.reset();
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
    'Insight': {
      'clearGav': createStateFn(null),
      'registerMarkUpgradeListener': function (listener) {
        waitOnInjector(['Coordinates', '$rootScope', function (Coordinates, $rootScope) {
          $rootScope.$on('markUpgrade', function (event, gav) {
            if (Coordinates.getFormat() === 'maven') {
              listener(gav.groupId, gav.artifactId, gav.version);
            }
          });
        }]);
      },
      'registerViewDetailsListener': function (listener) {
        waitOnInjector(['Coordinates', 'Properties', 'OwnerContext', '$rootScope', function (Coordinates, Properties, OwnerContext, $rootScope) {
          $rootScope.$on('viewDetails', function (event, version) {

            if (Coordinates.getFormat() === 'maven') {
              var gav = Coordinates.get();

              listener(OwnerContext.ownerId,
                      gav.groupId,
                      gav.artifactId,
                      version,
                      gav.classifier,
                      gav.extension,
                      Properties.getHash(),
                      Properties.getMatchState(),
                      Properties.getProprietary());
            }
          });
        }]);
      },
      'registerCoordsViewDetailsListener': function (listener) {
        waitOnInjector(['Coordinates', 'OwnerContext', 'Properties', '$rootScope', function (Coordinates, OwnerContext, Properties, $rootScope) {
          $rootScope.$on('viewDetails', function (event, version) {
            var coordinates = [];

            angular.forEach(Coordinates.get(), function (value, field) {
              coordinates.push(field);

              if ('version' === field) {
                coordinates.push(version);
              }
              else {
                coordinates.push(value);
              }
            });

            listener(OwnerContext.ownerId, Coordinates.getFormat(), coordinates,
                    Properties.getHash(),
                    Properties.getMatchState(),
                    Properties.getProprietary());
          });
        }]);
      },
      'registerCoordsMarkUpgradeListener': function (listener) {
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
        waitOnInjector(['Coordinates', 'OwnerContext', 'State', 'Properties', '$rootScope', function (Coordinates, OwnerContext, State, Properties, $rootScope) {
          safeApply($rootScope, function () {
            Coordinates.set(componentType, coordinates ? coordinates : {}); //coordinates may be null for unknown
            State.set(null);

            Properties.reset();
            Properties.setMatchState(properties.matchState);
            Properties.setProprietary(properties.proprietary);
            Properties.setFilename(properties.filename);
            Properties.setHash(properties.hash);

            if (properties.appId) {
              OwnerContext.setApplicationId(properties.appId);
            }
          });
        }], true);
      },
      /**
       * @deprecated since 1.13.0 Included for backwards compatibility with existing clients
       */
      'setGav': function (arg) {
        waitOnInjector(['Coordinates', 'OwnerContext', 'State', 'Properties', '$rootScope', function (Coordinates, OwnerContext, State, Properties, $rootScope) {
          safeApply($rootScope, function () {
            if (arg.appId) {
              OwnerContext.setApplicationId(arg.appId);
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
      'setHeaders' : function (headers) {
        waitOnInjector(['$http', '$rootScope', function ($http, $rootScope) {
          safeApply($rootScope, function () {
            angular.extend($http.defaults.headers.common, headers);
          });
        }]);
      },
      'setError': function (arg) {
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
      'setAuthFailureHandler' : function (handler) {
        authHandler = handler;
      },
      'setPending': createStateFn('pending'),
      'setUnassigned': createStateFn('unassigned'),
      'setFiltered': createStateFn('filtered')
    }
  });
}());
