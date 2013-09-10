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

  var dashboardApp = angular.module('DashboardModule', ['ui.compat', 'ui.bootstrap', 'CLMLocation', 'CommonServices'], [
        '$stateProvider', '$routeProvider', '$urlRouterProvider',
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
        }
      ]).service('statusChecker', function(){
        
      }).run([
        '$rootScope', '$http', '$location', '$window', 'Messages', 'CLMLocations', 'licenseChecker', 'securityStatusChecker',
        function($rootScope, $http, $location, $window, messages, CLMLocations, licenseChecker, securityStatusChecker) {
          securityStatusChecker.check().then(function(status){
            if (!status.account) {
              $window.location = '../login-assets/login.html';
            } else {
              $rootScope.username = data.account;
              $rootScope.forcedRedirect = null;
              licenseChecker.check().then(null, function() {
                $rootScope.forcedRedirect = '/management/configuration/productlicense';
                $location.path($rootScope.forcedRedirect);
              });
            }
          }, function() {
            //TODO: error getting status, uh-oh!
          });
          
          // The page contains unsaved changes, continuing will discard them.
          $rootScope.tempState = null;

          $rootScope.$on('$stateChangeError', function(event, toState, toParams, fromState, fromParams, error) {
            $rootScope.error = messages.getHttpErrorMessage(error);
          });

          $rootScope.$on('$locationChangeStart', function(event, newUrl, oldUrl) {
            var e;
            $rootScope.tempNewUrl = null;
            $rootScope.tempDestination = $location.url();

            if (newUrl !== oldUrl && newUrl != $rootScope.tempState) {
              //special case where back button is hit, locationUrl will be the same as the oldUrl!!
              if (oldUrl.indexOf($rootScope.tempDestination) > -1) {
                $rootScope.tempDestination = newUrl.substring(newUrl.indexOf('#') + 1);
              }
              //give components a chance to negate the page change
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

          $rootScope.$on('$locationChangeSuccess', function() {
            if ($rootScope.forcedRedirect) {
              $location.path($rootScope.forcedRedirect);
            }
          });

          var fn = function(event) {
            if (!masterModalShown) {
              var e = $rootScope.$broadcast('pageChangeStarted');
              return e.defaultPrevented ? e.message ||
                  'The page may contain unsaved changes, continuing will discard them.' : undefined;
            }
          };

          //make sure to cleanup event listeners
          $rootScope.$on('$destroy', function() {
            $rootScope.$broadcast('pageChangeAccepted', $rootScope.tempDestination);
            $(window).unbind('beforeunload', fn);
          });

          //this causes the browser to notify the user that the page contains unsaved data
          $(window).bind('beforeunload', fn);
        }
      ]);

  //this is a fix to bootstrap to stop the 'too much recursion' error when multiple modals are fighting for focus
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

  dashboardApp.controller('UnsavedController', [
    '$rootScope', '$scope', '$location', function($rootScope, $scope, $location) {
      $scope.close = function(shouldContinue) {
        hideMasterModal();
        if (shouldContinue) {
          $rootScope.$broadcast('pageChangeAccepted', $rootScope.tempDestination);
          $rootScope.tempState = $rootScope.tempNewUrl;
          $location.url($rootScope.tempDestination);
        }
      };
    }
  ]);

  dashboardApp.controller('dashboardController', function($scope, $state) {
    function switchDashboard() {
      for (var i = 0; i < $scope.availableDashboards.length; i++) {
        if (window.location.href.indexOf($scope.availableDashboards[i].href) !== -1) {
          $scope.selectedDashboard = $scope.availableDashboards[i];
          break;
        }
      }
    }

    $scope.$state = $state;
    $scope.availableDashboards = [
      {
        name: 'Management',
        href: 'index.html#/management'
      },
      {
        name: 'Reports',
        href: 'reports.html#/reports'
      }
    ];

    $scope.$watch('$state.current.name', switchDashboard);
    switchDashboard();
  });

  dashboardApp.service('licenseChecker', [
    '$http', '$q', 'CLMLocations', function($http, $q, CLMLocations) {
      return {
        check: function() {
          var deferred = $q.defer();
          $http.get(CLMLocations.getLicenseSummaryUrl()).success(function() {
            deferred.resolve();
          }).error(function(data, status) {
                if (status === 402) {
                  deferred.reject();
                }
                else {
                  deferred.resolve();
                }
              });
          return deferred.promise;
        }
      };
    }
  ]);
  
  dashboardApp.service('securityStatusChecker', [
    '$http', '$q', 'CLMLocations', function($http, $q, CLMLocations) {
      return {
        check: function() {
          var deferred = $q.defer();
          $http.get(CLMLocations.getStatusUrl(), {
            params: {
              timestamp: new Date().getTime()
            }
          }).success(function(data) {
            deferred.resolve();
          }).error(function(data, status) {
            if (status === 402) {
              deferred.reject();
            }
            else {
              deferred.resolve();
            }
          });
          return deferred.promise;
        }
      };
    }
  ]);
}());