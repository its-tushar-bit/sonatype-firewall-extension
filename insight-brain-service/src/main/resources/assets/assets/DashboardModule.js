/**
 * @license Copyright (c) 2012-2013 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
(function() {
  "use strict";
  var masterModalShown = false;

  var showMasterModal = function() {
    if (!masterModalShown) {
      masterModalShown = true;
      $('#unsavedModal').modal('show');
      $('.modal-backdrop').addClass('master-modal-backdrop');
    }
  };

  var hideMasterModal = function() {
    $('#unsavedModal').modal('hide');
    $('.modal-backdrop').removeClass('master-modal-backdrop');
    masterModalShown = false;
  };

  var dashboardApp = angular.module(
          'DashboardModule',
          ['ui.router', 'ui.bootstrap', 'CLMLocation', 'CommonServices'],
          ['$stateProvider', '$routeProvider', '$urlRouterProvider',
              function($stateProvider, $routeProvider, $urlRouterProvider) {
                $stateProvider.state('home', {
                  url: '/',
                  controller: angular.noop
                });

                var fn = function($rootScope, messages) {
                  $rootScope.error = 'Unknown Address';
                };
                fn.$inject = ['$rootScope', 'Messages'];
                $urlRouterProvider.otherwise(function($injector, $location) {
                  $injector.invoke(fn);
                });
              }]).run(
          [
              '$rootScope',
              '$location',
              '$window',
              '$state',
              'Messages',
              'CLMLocations',
              'serverStatus',
              function($rootScope, $location, $window, $state, messages, CLMLocations, serverStatus) {

                // The page contains unsaved changes, continuing will discard
                // them.
                $rootScope.tempState = null;

                $rootScope.$on('$stateChangeError', function(event, toState, toParams, fromState, fromParams, error) {
                  $rootScope.error = messages.getHttpErrorMessage(error);
                });

                $rootScope.$on('$stateChangeStart', function(event, toState, toParams, fromState, fromParams) {
                  // first state change occurs on first page load, so we always want to prevent the first state change
                  // so that we can first check security and license status
                  if (!$rootScope.authenticated) {
                    event.preventDefault();
                    // save the state they were going to for use later, we will reuse this if the security/license check
                    // all passes
                    var storedState = toState;
                    var storedParams = toParams;
                    serverStatus.check().then(function(data) {
                      // all good, off you go!
                      $state.transitionTo(storedState, storedParams);
                    }, function(data) {
                      // naughty naughty, no auth
                      if (data.status === 401 || !$rootScope.authenticated) {
                        var to = '../login-assets/login.html';
                        var current = $window.location.href;
                        if (current && current.indexOf('/login-assets/login.html') == -1) {
                          to = to + '?redirectTo=' + encodeURIComponent(current);
                        }
                        $window.location.replace(to);
                      } else if (data.status == 402 || !$rootScope.licensed) { // even naughtier, no license!
                        if ($window.location.href.indexOf('/index.html') === -1) {
                          $window.location.replace('index.html#/management/configuration/productlicense');
                        } else {
                          $state.transitionTo('management.configuration.productlicense');
                        }
                      } else if (data.status) {
                        $rootScope.error = 'Unable to initialize the application';
                      }
                    });
                  } else if (!$rootScope.licensed && toState.name != 'management.configuration.productlicense') {
                    // not licensed and trying to browse to a page other than license
                    event.preventDefault();
                    $state.transitionTo('management.configuration.productlicense');
                  }
                });

                $rootScope.$on('$locationChangeStart', function(event, newUrl, oldUrl) {
                  var e;
                  $rootScope.tempNewUrl = null;
                  $rootScope.tempDestination = $location.url();

                  if (newUrl !== oldUrl && newUrl != $rootScope.tempState) {
                    // special case where back button is hit, locationUrl will be the same as the oldUrl!!
                    if (oldUrl.indexOf($rootScope.tempDestination) > -1) {
                      $rootScope.tempDestination = newUrl.substring(newUrl.indexOf('#') + 1);
                    }
                    // give components a chance to negate the page change
                    e = $rootScope.$broadcast('pageChangeStarted', $rootScope.tempDestination);
                    if (e.defaultPrevented) {
                      event.preventDefault();
                      $rootScope.tempNewUrl = newUrl;
                      showMasterModal();
                      return;
                    }

                    $rootScope.$broadcast('pageChangeAccepted', $rootScope.tempDestination);
                  }
                  $rootScope.tempState = null;
                });

                var fn = function(event) {
                  if (!masterModalShown) {
                    var e = $rootScope.$broadcast('pageChangeStarted');
                    return e.defaultPrevented ? e.message
                            || 'The page may contain unsaved changes, continuing will discard them.' : undefined;
                  }
                };

                // make sure to cleanup event listeners
                $rootScope.$on('$destroy', function() {
                  $rootScope.$broadcast('pageChangeAccepted', $rootScope.tempDestination);
                  $(window).unbind('beforeunload', fn);
                });

                // this causes the browser to notify the user that the page contains unsaved data
                $(window).bind('beforeunload', fn);
              }]);

  // this is a fix to bootstrap to stop the 'too much recursion' error when multiple modals are fighting for focus
  $.fn.modal.Constructor.prototype.enforceFocus = function() {
    var that = this;
    var done = false;
    $(document).on('focusin.modal', function(e) {
      if (!done && that.$element[0] !== e.target && !that.$element.has(e.target).length) {
        done = true;
        that.$element.focus();
      }
    });
  };

  dashboardApp.controller('UnsavedController', ['$rootScope', '$scope', '$location',
      function($rootScope, $scope, $location) {
        $scope.close = function(shouldContinue) {
          hideMasterModal();
          if (shouldContinue) {
            $rootScope.$broadcast('pageChangeAccepted', $scope.tempDestination);
            $rootScope.tempState = $rootScope.tempNewUrl;
            $location.url($scope.tempDestination);
          }
        };
      }]);

  dashboardApp.controller('dashboardController', ['$scope', '$state', '$window', function($scope, $state, $window) {
    function switchDashboard() {
      for ( var i = 0; i < $scope.availableDashboards.length; i++) {
        if ($window.location.href && $window.location.href.indexOf($scope.availableDashboards[i].selector) !== -1) {
          $scope.selectedDashboard = $scope.availableDashboards[i];
          break;
        }
      }
    }

    $scope.$state = $state;
    $scope.availableDashboards = [{
      name: 'Management',
      href: 'index.html#/management/application',
      selector: '#/management'
    }, {
      name: 'Reports',
      href: 'reports.html#/reports/violations',
      selector: '#/reports'
    }];

    $scope.$watch('$state.current.name', switchDashboard);
    switchDashboard();
  }]);

  dashboardApp.service('licenseChecker', ['$http', '$q', 'CLMLocations', function($http, $q, CLMLocations) {
    return {
      check: function() {
        var deferred = $q.defer();
        $http.get(CLMLocations.getLicenseSummaryUrl()).success(function(data) {
          deferred.resolve(data);
        }).error(function(data, status) {
          deferred.reject(status);
        });
        return deferred.promise;
      }
    };
  }]);

  dashboardApp.service('securityStatusChecker', ['$http', '$q', 'CLMLocations', function($http, $q, CLMLocations) {
    return {
      check: function() {
        var deferred = $q.defer();
        $http.get(CLMLocations.getSessionUrl(), {
          params: {
            timestamp: new Date().getTime()
          }
        }).success(function(data) {
          deferred.resolve(data);
        }).error(function(data, status) {
          deferred.reject(status);
        });
        return deferred.promise;
      }
    };
  }]);

  dashboardApp.service('serverStatus', ['$rootScope', '$http', '$q', '$window', '$location', 'CLMLocations',
      'licenseChecker', 'securityStatusChecker',
      function($rootScope, $http, $q, $window, $location, CLMLocations, licenseChecker, securityStatusChecker) {
        return {
          check: function() {
            var deferred = $q.defer();
            securityStatusChecker.check().then(function(data) {
              $rootScope.authenticated = data.authenticated;
              $rootScope.username = data.username;
              licenseChecker.check().then(function() {
                $rootScope.licensed = true;
                deferred.resolve();
              }, function(status) {
                deferred.reject({
                  status: status
                });
              });
            }, function(status) {
              deferred.reject({
                status: status
              });
            });

            return deferred.promise;
          }
        };
      }]);
}());