describe('Tests for the LoginApp', function() {

  beforeEach(module('LoginApp'));

  beforeEach(module('LoginApp', function($provide) {
    $provide.value('$window', {
      location: {
        href: 'http://blah/rest?redirectTo=' + encodeURIComponent('http://blah/something_& '),
        replace: jasmine.createSpy()
      },
      navigator: function() {
      }
    });
    $provide.factory('hudson', ['$http', function($http) {
      return $http;
    }]);
  }));

  describe('LoginController', function() {

    var scope;

    beforeEach(inject(function($rootScope, $controller) {
      scope = $rootScope.$new();
      $controller('LoginController', {
        $scope: scope
      });
    }));

    afterEach(inject(function($httpBackend) {
      $httpBackend.verifyNoOutstandingExpectation();
      $httpBackend.verifyNoOutstandingRequest();
      scope.$destroy();
    }));

    it('validate initial state.', inject(function($window) {
      expect(scope.data).toEqual({});
    }));

    it('Invalid Login', inject(function($httpBackend, $window, CLMLocations) {
      // validate invalid login
      $httpBackend.expectPOST(SpecUtil.toRegExp(CLMLocations.getLoginUrl())).respond(401);
      scope.$apply(function() {
        scope.data.username = 'adminuser';
        scope.data.password = 'adminpass';
      });
      scope.signIn();
      expect(scope.processing).toBeTruthy();
      $httpBackend.flush();
      expect(scope.redirecting).toBeFalsy();
      expect(scope.loginError).toEqual('Invalid credentials. Please try again.');
    }));

    it('Server Down', inject(function($httpBackend, $window, CLMLocations, Messages) {
      // validate non-login response
      $httpBackend.expectPOST(SpecUtil.toRegExp(CLMLocations.getLoginUrl())).respond(0);
      scope.$apply(function() {
        scope.data.username = 'adminuser';
        scope.data.password = 'adminpass';
      });
      scope.signIn();
      expect(scope.processing).toBeTruthy();
      $httpBackend.flush();
      expect(scope.redirecting).toBeFalsy();
      expect(scope.loginError).toEqual(Messages.getHttpErrorMessage([null, 0, null, null]));
    }));

    it('Valid Login', inject(function($httpBackend, $window, CLMLocations) {
      // validate valid login
      $httpBackend.expectPOST(SpecUtil.toRegExp(CLMLocations.getLoginUrl())).respond(200);
      scope.$apply(function() {
        scope.data.username = 'adminuser';
        scope.data.password = 'adminpass';
      });
      scope.signIn();
      expect(scope.processing).toBeTruthy();
      $httpBackend.flush();

      expect($window.location.replace).toHaveBeenCalledWith('http://blah/something_& ');
      expect(scope.loginError).toBeFalsy();
      expect(scope.redirecting).toBeTruthy();
    }));

    it('Valid Login bad redirect', inject(function($httpBackend, $window, CLMLocations) {
      // validate valid login
      $window.location.href = 'http://blah/rest?redirectTo=' + encodeURIComponent('http://blah2/something');
      $httpBackend.expectPOST(SpecUtil.toRegExp(CLMLocations.getLoginUrl())).respond(200);
      scope.$apply(function() {
        scope.data.username = 'adminuser';
        scope.data.password = 'adminpass';
      });
      scope.signIn();
      expect(scope.processing).toBeTruthy();
      $httpBackend.flush();

      expect($window.location.replace).toHaveBeenCalledWith('../');
      expect(scope.loginError).toBeFalsy();
      expect(scope.redirecting).toBeTruthy();
    }));
  });
});
