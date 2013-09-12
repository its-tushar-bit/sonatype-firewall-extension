describe('dashboardApp', function() {
  var scope, state;

  beforeEach(module('DashboardModule', function($stateProvider, $provide) {
    $provide.value('$window', {
      location: {
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

    $stateProvider.state('test', {}).state('management', {}).state('management.configuration', {}).state(
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
    $rootScope.licensed = true;
    scope = $rootScope.$new();
    state = $state;

    $controller('dashboardController', {
      $scope: scope,
      $state: state
    });
  }));

  it('Validate proper requests made on initialization',
          inject(function($rootScope, $httpBackend, $state, $window, CLMLocations) {
            var event = {
              preventDefault: jasmine.createSpy('preventDefault')
            };

            // dump the vars so that we can test from scratch
            delete $rootScope.username;
            delete $rootScope.licensed;
            delete $rootScope.initialized;

            $httpBackend.expectGET(SpecUtil.toRegExp(CLMLocations.getStatusUrl())).respond({
              username: 'user'
            });
            $httpBackend.expectGET(SpecUtil.toRegExp(CLMLocations.getLicenseSummaryUrl().split('?')[0]])).respond({
              username: 'user'
            });
            var event = $rootScope.$broadcast('$stateChangeStart', 'test', {}, '', {});
            $httpBackend.flush();

            expect(event.defaultPrevented).toEqual(true);
            expect($rootScope.username).toEqual('user');
            expect($rootScope.licensed).toEqual(true);
            expect($rootScope.initialized).toEqual(true);
            expect($state.current.name).toEqual('test');

            // now test with bad license
            delete $rootScope.username;
            delete $rootScope.licensed;
            delete $rootScope.initialized;

            $httpBackend.expectGET(SpecUtil.toRegExp(CLMLocations.getStatusUrl())).respond({
              username: 'user'
            });
            $httpBackend.expectGET(SpecUtil.toRegExp(CLMLocations.getLicenseSummaryUrl())).respond(402);
            var event = $rootScope.$broadcast('$stateChangeStart', 'test', {}, '', {});
            $httpBackend.flush();

            expect(event.defaultPrevented).toEqual(true);
            expect($rootScope.username).toEqual('user');
            expect($rootScope.licensed).toEqual(false);
            expect($rootScope.initialized).toEqual(true);
            expect($state.current.name).toEqual('management.configuration.productlicense');

            // now test with bad auth
            delete $rootScope.username;
            delete $rootScope.licensed;
            delete $rootScope.initialized;

            $httpBackend.expectGET(SpecUtil.toRegExp(CLMLocations.getStatusUrl())).respond({
              username: null
            });
            var event = $rootScope.$broadcast('$stateChangeStart', 'test', {}, '', {});
            $httpBackend.flush();

            expect(event.defaultPrevented).toEqual(true);
            expect($rootScope.username).toBeUndefined();
            expect($rootScope.licensed).toBeUndefined();
            expect($rootScope.initialized).toBeUndefined();
            expect($window.location.replace.mostRecentCall.args[0]).toMatch(/\.\.\/login-assets\/login\.html\?timestamp=[0-9]+/);
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
});