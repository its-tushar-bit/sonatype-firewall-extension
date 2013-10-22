describe('dashboardApp', function() {
  var scope, state;

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

  beforeEach(inject(function($rootScope, $state, $controller) {
    $rootScope.username = 'user';
    $rootScope.authenticated = true;
    $rootScope.licensed = true;
    scope = $rootScope.$new();
    state = $state;

    $controller('dashboardController', {
      $scope: scope,
      $state: state
    });
  }));

  it('Validate proper requests made on initialization', inject(function($rootScope, $httpBackend, $state, $stateParams, $window,
          CLMLocations) {
    var event = {
      preventDefault: jasmine.createSpy('preventDefault')
    };

    function cleanScope() {
      // dump the vars so that we can test from scratch
      delete $rootScope.username;
      delete $rootScope.authenticated;
      delete $rootScope.licensed;
    }

    cleanScope();
    $httpBackend.expectGET(SpecUtil.toRegExp(CLMLocations.getSessionUrl())).respond({
      username: 'user',
      authenticated: true
    });
    $httpBackend.expectGET(SpecUtil.toRegExp(CLMLocations.getLicenseSummaryUrl().split('?')[0])).respond({
      username: 'user'
    });
    var toParams = {testId:'blah'};
    var event = $rootScope.$broadcast('$stateChangeStart', 'test', toParams, '', {});
    $httpBackend.flush();

    expect(event.defaultPrevented).toBeTruthy();
    expect($rootScope.username).toEqual('user');
    expect($rootScope.authenticated).toBeTruthy();
    expect($rootScope.licensed).toBeTruthy();
    expect($state.current.name).toEqual('test');
    expect($stateParams).toEqual(toParams);

    // now test with bad license
    cleanScope();

    $httpBackend.expectGET(SpecUtil.toRegExp(CLMLocations.getSessionUrl())).respond({
      username: 'user',
      authenticated: true
    });
    $httpBackend.expectGET(SpecUtil.toRegExp(CLMLocations.getLicenseSummaryUrl().split('?')[0])).respond(402);
    var event = $rootScope.$broadcast('$stateChangeStart', 'test', {}, '', {});
    $httpBackend.flush();

    expect(event.defaultPrevented).toBeTruthy();
    expect($rootScope.username).toEqual('user');
    expect($rootScope.authenticated).toBeTruthy();
    expect($rootScope.licensed).toBeFalsy();
    expect($state.current.name).toEqual('management.configuration.productlicense');

    // now test with bad license from something other than index.html (i.e.
    // reports.html)
    $window.location.href = 'http://blah/reports.html';
    cleanScope();

    $httpBackend.expectGET(SpecUtil.toRegExp(CLMLocations.getSessionUrl())).respond({
      username: 'user',
      authenticated: true
    });
    $httpBackend.expectGET(SpecUtil.toRegExp(CLMLocations.getLicenseSummaryUrl().split('?')[0])).respond(402);
    var event = $rootScope.$broadcast('$stateChangeStart', 'test', {}, '', {});
    $httpBackend.flush();

    expect(event.defaultPrevented).toBeTruthy();
    expect($rootScope.username).toEqual('user');
    expect($rootScope.authenticated).toBeTruthy();
    expect($rootScope.licensed).toBeFalsy();
    expect($window.location.replace).toHaveBeenCalledWith('index.html#/management/configuration/productlicense');

    // now test with bad auth
    $window.location.replace.reset();
    cleanScope();

    $httpBackend.expectGET(SpecUtil.toRegExp(CLMLocations.getSessionUrl())).respond(401);
    var event = $rootScope.$broadcast('$stateChangeStart', 'test', {}, '', {});
    $httpBackend.flush();

    expect(event.defaultPrevented).toBeTruthy();
    expect($rootScope.username).toBeFalsy();
    expect($rootScope.authenticated).toBeFalsy();
    expect($rootScope.licensed).toBeFalsy();
    expect($window.location.replace).toHaveBeenCalledWith(
            '../login-assets/login.html?redirectTo=' + encodeURIComponent('http://blah/reports.html'));
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
