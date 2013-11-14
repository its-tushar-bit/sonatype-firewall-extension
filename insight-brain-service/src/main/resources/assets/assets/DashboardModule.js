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

  var dashboardApp = angular.module('DashboardModule', ['ui.router', 'ui.bootstrap', 'CLMLocation', 'CommonServices',
      'UserControls', 'ngRoute'], ['$stateProvider', '$routeProvider', '$urlRouterProvider',
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
      }]);

  dashboardApp.run([
      '$rootScope',
      '$location',
      '$window',
      '$state',
      'Messages',
      'CLMLocations',
      'licenseChecker',
      'CurrentUser',
      function($rootScope, $location, $window, $state, messages, CLMLocations, licenseChecker, currentUser) {
        function checkBootstrap() {
          if (username) {
            // User is logged in
            if ($rootScope.licensed) {
              // Typical usage, server is licensed
              stateChangePrevention(); // Remove block
              $rootScope.initialized = true;
              if (savedState) {
                $state.transitionTo(savedState.toState, savedState.toParams);
              }
            } else if ($rootScope.licensed === false) {
              // Server is unlicensed, redirect to product licensing page
              if ($window.location.href.indexOf('/index.html') === -1) {
                $window.location.replace('index.html#/management/configuration/productlicense');
              } else {
                stateChangePrevention(); // Remove existing block
                $rootScope.$on('$stateChangeStart', function (event, toState, toParams) {
                  if (toState.name !== 'management.configuration.productlicense') {
                    event.preventDefault();
                  }
                });
                $rootScope.initialized = true;
                $state.transitionTo('management.configuration.productlicense');
              }
            }
          }
        }
        function redirectToLogin() {
          var to = '../login-assets/login.html',
              current = $window.location.href;
          if (current && current.indexOf('/login-assets/login.html') == -1) {
            to = to + '?redirectTo=' + encodeURIComponent(current);
          }
          $window.location.replace(to);
        }

        var savedState = null,
            stateChangePrevention = $rootScope.$on('$stateChangeStart', function (event, toState, toParams) {
              event.preventDefault();
              savedState = {
                toState : toState,
                toParams : toParams
              };
            }),
            username = null;

        licenseChecker.check().then(function () {
          $rootScope.licensed = true;
          checkBootstrap();
        }, function (result) {
          if (result[1] === 402) {
           $rootScope.licensed = false;
           checkBootstrap();
          } else if (username) {
            $rootScope.error = 'Unable to initialize the application';
          } else {
            $rootScope.$watch('username', function (newVal) {
              if (newVal) {
                $rootScope.error = 'Unable to initialize the application';
              }
            });
          }
        });

        currentUser.then(function (data) {
          if (!data.authenticated) {
            redirectToLogin();
          } else {
            username = data.username;
            checkBootstrap();
          }
        }, function (error) {
          if (error[1] === 401) {
            redirectToLogin(); 
          } else {
            $rootScope.error = 'Unable to initialize the application';
          }
        });

        $rootScope.$on('logout', function () {
          redirectToLogin();
        });
        $rootScope.$on('$stateChangeError', function(event, toState, toParams, fromState, fromParams, error) {
          $rootScope.error = messages.getHttpErrorMessage(error);
        });

        // The page contains unsaved changes, continuing will discard them.
        $rootScope.tempState = null;

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

  dashboardApp.controller('dashboardController', ['$scope', '$state', '$window', 'CLMLocations', '$http', '$rootScope',
      function($scope, $state, $window, CLMLocations, $http, $rootScope) {
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
}());