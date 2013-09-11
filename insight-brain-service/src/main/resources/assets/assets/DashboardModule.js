/**
 * @license Copyright (c) 2012-2013 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
//using global variable to share between apps
var currentUrl;
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
          ['ui.compat', 'ui.bootstrap', 'CLMLocation', 'CommonServices'],
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
              'Messages',
              'CLMLocations',
              'serverStatus',
              function($rootScope, $location, $window, messages, CLMLocations, serverStatus) {
                // The page contains unsaved changes, continuing will discard
                // them.
                $rootScope.tempState = null;

                $rootScope.$on('$stateChangeError', function(event, toState, toParams, fromState, fromParams, error) {
                  $rootScope.error = messages.getHttpErrorMessage(error);
                });

                $rootScope.$on('$locationChangeStart', function(event, newUrl, oldUrl) {
                  currentUrl = newUrl;
                  
                  //don't bother checking unless the status has resolved
                  if ($rootScope.username && $rootScope.licensed) {
                    var e;
                    $rootScope.tempNewUrl = null;
                  
                    if (newUrl != $rootScope.tempState) {
                      // give components a chance to negate the page change
                      e = $rootScope.$broadcast('pageChangeStarted', currentUrl);
                      if (e.defaultPrevented) {
                        event.preventDefault();
                        $rootScope.tempNewUrl = newUrl;
                        showMasterModal();
                        return;
                      }

                      $rootScope.$broadcast('pageChangeAccepted', currentUrl); 
                    }
                  } else if ($rootScope.username && currentUrl.indexOf('management/configuration/productlicense') == -1){
                    event.preventDefault();
                  }
                  $rootScope.tempState = null;
              }, function(status) {
                if (status) {
                  $rootScope.error = 'Unable to initialize the application';
                } else {
                  // nothing to do, some redirect must've occurred
                }
              });
                
                serverStatus.check().then(function(data){
                  $rootScope.username = data.username;
                  $rootScope.licensed = data.licensed;
                  if (currentUrl) {
                    $window.location = currentUrl;
                  }
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
                  $rootScope.$broadcast('pageChangeAccepted', currentUrl);
                  $(window).unbind('beforeunload', fn);
                });

                // this causes the browser to notify the user that the page
                // contains unsaved data
                $(window).bind('beforeunload', fn);
              }]);

  // this is a fix to bootstrap to stop the 'too much recursion' error when
  // multiple modals are fighting for focus
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
            $rootScope.$broadcast('pageChangeAccepted', currentUrl);
            $rootScope.tempState = $rootScope.tempNewUrl;
            $location.url(currentUrl);
          }
        };
      }]);

  dashboardApp.controller('dashboardController', ['$scope', '$state', function($scope, $state) {
    function switchDashboard() {
      for ( var i = 0; i < $scope.availableDashboards.length; i++) {
        if (window.location.href.indexOf($scope.availableDashboards[i].href) !== -1) {
          $scope.selectedDashboard = $scope.availableDashboards[i];
          break;
        }
      }
    }

    $scope.$state = $state;
    $scope.availableDashboards = [{
      name: 'Management',
      href: 'index.html#/management'
    }, {
      name: 'Reports',
      href: 'reports.html#/reports'
    }];

    $scope.$watch('$state.current.name', switchDashboard);
    switchDashboard();
  }]);

  dashboardApp.service('licenseChecker', ['$http', '$q', 'CLMLocations', function($http, $q, CLMLocations) {
    return {
      check: function() {
        var deferred = $q.defer();
        $http.get(CLMLocations.getLicenseSummaryUrl(), {
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

  dashboardApp.service('securityStatusChecker', ['$http', '$q', 'CLMLocations', function($http, $q, CLMLocations) {
    return {
      check: function() {
        var deferred = $q.defer();
        $http.get(CLMLocations.getStatusUrl(), {
          params: {
            timestamp: new Date().getTime()
          }
        }).success(function(data) {
          deferred.resolve(data);
        }).error(function() {
          deferred.reject();
        });
        return deferred.promise;
      }
    };
  }]);

  dashboardApp.service('serverStatus', ['$rootScope', '$http', '$q', '$window', '$location', 'CLMLocations', 'licenseChecker',
      'securityStatusChecker',
      function($rootScope, $http, $q, $window, $location, CLMLocations, licenseChecker, securityStatusChecker) {
        return {
          check: function() {
            var deferred = $q.defer();
            securityStatusChecker.check().then(function(data) {
              if (data.account) {
                licenseChecker.check().then(function() {
                  deferred.resolve({username: data.account, licensed: true});
                  if (currentUrl) {
                    $window.location = currentUrl;
                  } else {
                    $window.location = '../';
                  }
                }, function(status) {
                  if (status == 402) {
                    deferred.resolve({username: data.account, licensed: false});
                    $window.location = '../assets/index.html#/management/configuration/productlicense';
                  } else {
                    deferred.reject(status);
                  }
                });
              } else {
                deferred.reject();
                $window.location = '../login-assets/login.html';
              }
            }, function(status) {
              deferred.reject(status);
            });

            return deferred.promise;
          }
        };
      }]);
}());