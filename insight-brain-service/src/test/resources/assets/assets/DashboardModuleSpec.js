describe('dashboardApp', function() {
  'use strict';
  var scope, state, currentUserSuccess, currentUserFail, licenseCheckerFail, licenseCheckerSuccess;

  beforeEach(module('DashboardModule', function($stateProvider, $provide) {
    $provide.value('$window', {
      location: {
        href: 'http://blah/index.html',
        replace: jasmine.createSpy()
      },
      navigator: {
        userAgent: {}
      },
      document: {
        createElement: function() {
          return null;
        }
      }
    });
    $provide.value('CurrentUser', {
      then : function (success, fail) {
        currentUserSuccess = success;
        currentUserFail = fail;
        return this;
      }
    });
    $provide.value('licenseChecker', {
      check : function () {
        return {
          then : function (success, fail) {
            licenseCheckerFail = fail;
            licenseCheckerSuccess = success;
          }
        }
      }
    });

    $stateProvider.state('test', {url: '/test/:testId'}).state('management', {}).state('management.configuration', {}).state(
            'management.configuration.productlicense', {});
  }));

  afterEach(inject(function($httpBackend) {
    if (scope) {
      scope.$destroy();
    }
    $httpBackend.verifyNoOutstandingExpectation();
    $httpBackend.verifyNoOutstandingRequest();
  }));

  beforeEach(inject(function($rootScope, $state, $controller, $httpBackend) {
    scope = $rootScope.$new();
    state = $state;

    $controller('dashboardController', {
      $scope: scope,
      $state: state
    });
  }));

  describe('Validate proper requests made on initialization', function () {
    var event = {
      preventDefault: jasmine.createSpy('preventDefault')
    };

    it('normal', inject(function($rootScope, $httpBackend, $state, $stateParams, $window, CLMLocations) {
      var toParams = {testId:'blah'},
          event = null;

      scope.$apply(function () {
        event = $rootScope.$broadcast('$stateChangeStart', 'test', toParams, '', {});
      });

      expect(event.defaultPrevented).toBeTruthy();

      scope.$apply(function () {
        currentUserSuccess({
          username: 'user',
          authenticated: true
        });
      });

      expect($rootScope.initialized).toBeFalsy();
      expect($rootScope.licensed).toBeFalsy();

      scope.$apply(function () {
        licenseCheckerSuccess({
          expiryTimestamp : 0 // technically in the past but JS doesn't check this
        });
      });

      expect($rootScope.licensed).toBeTruthy();
      expect($rootScope.initialized).toBeTruthy();

      expect($state.current.name).toEqual('test');
      expect($stateParams).toEqual(toParams);
    }));

    it('bad license', inject(function($rootScope, $httpBackend, $state, $stateParams, $window, CLMLocations) {
      var event = null;
      scope.$apply(function () {
        currentUserSuccess({
          username: 'user',
          authenticated: true
        });
        licenseCheckerFail(['', 402]);
        event = $rootScope.$broadcast('$stateChangeStart', 'test', {}, '', {});
      });

      expect(event.defaultPrevented).toBeTruthy();
      expect($rootScope.initialized).toBeTruthy();
      expect($rootScope.licensed).toBeFalsy();
      expect($state.current.name).toEqual('management.configuration.productlicense');
    }));

    it('bad license in separate application', inject(function($rootScope, $httpBackend, $state, $stateParams, $window, CLMLocations) {
      // now test with bad license from something other than index.html (i.e. reports.html)
      $window.location.href = 'http://blah/reports.html';

      currentUserSuccess({
        username: 'user',
        authenticated: true
      });
      licenseCheckerFail(['', 402]);
      var event = $rootScope.$broadcast('$stateChangeStart', 'test', {}, '', {});

      expect(event.defaultPrevented).toBeTruthy();
      expect($window.location.replace).toHaveBeenCalledWith('index.html#/management/configuration/productlicense');
    }));

    it('bad auth', inject(function($rootScope, $httpBackend, $state, $stateParams, $window, CLMLocations) {
      $window.location.replace.reset();

      currentUserSuccess({
        username: 'user',
        authenticated: false
      });
      licenseCheckerFail(['', 401]);
      var event = $rootScope.$broadcast('$stateChangeStart', 'test', {}, '', {});

      expect(event.defaultPrevented).toBeTruthy();
      expect($rootScope.username).toBeFalsy();
      expect($rootScope.licensed).toBeFalsy();
      expect($window.location.replace).toHaveBeenCalledWith(
              '../login-assets/login.html?redirectTo=' + encodeURIComponent('http://blah/index.html'));
    }));
  });

  describe('Normal Operation', function () {
    beforeEach(inject(function($rootScope, $state, $controller, $httpBackend) {
      $rootScope.username = 'user';
      $rootScope.authenticated = true;
      $rootScope.licensed = true;
    }));

    it('Validate location change event is broadcast properly', inject(function($rootScope) {
      var successStart = false, successAccept = false;
      $rootScope.$on('pageChangeStarted', function(event, destination) {
        successStart = true;
      });
      $rootScope.$on('pageChangeAccepted', function(event, destination) {
        successAccept = true;
      });

      $rootScope.$broadcast('$locationChangeStart', 'http://www.cnn.com', 'http://www.google.com');

      waitsFor(function() {
        return successStart;
      }, "pageChangeStarted event not properly retrieved", 1000);
      waitsFor(function() {
        return successAccept;
      }, "pageChangeAccepted event not properly retrieved", 1000);
    }));

    it('Adjusts dashboard to the current state', inject(function($window, $state) {
      $window.location.href = 'http://www.blah.com/index.html#/management/application';
      $state.current.name = 'management.application';
      scope.$digest();
      expect(scope.selectedDashboard.name).toBe('Management');

      $window.location.href = 'http://www.blah.com/index.html#/reports/violations';
      $state.current.name = 'reports.violations';
      scope.$digest();
      expect(scope.selectedDashboard.name).toBe('Reports');
    }));
  });
});
