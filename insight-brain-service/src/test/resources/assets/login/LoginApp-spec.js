describe('Tests for the LoginApp', function() {

  beforeEach(module('LoginApp'));

  beforeEach(module('LoginApp', function($provide) {
    $provide.value('$window', {
      location: '/default',
      navigator: function() {
      }
    });
    $provide.factory('hudson', [
      '$http', function($http) {
        return $http;
      }
    ]);
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
      expect($window.location).toEqual('/default');
    }));

    it('validate login.', inject(function($httpBackend, $window, CLMLocations) {
      // validate invalid login
      $httpBackend.expectPOST(SpecUtil.toRegExp(CLMLocations.getLoginUrl())).respond(401);
      scope.data.username = 'admin';
      scope.data.password = 'admin';
      scope.signIn();
      $httpBackend.flush();
      expect(scope.loginError).toEqual('Invalid credentials entered, please try again.');
      expect(scope.data).toEqual({
        username: 'admin',
        password: 'admin'
      });

      // validate valid login
      $httpBackend.expectPOST(SpecUtil.toRegExp(CLMLocations.getLoginUrl())).respond(200);
      scope.data.username = 'admin';
      scope.data.password = 'admin';
      scope.signIn();
      $httpBackend.flush();
      expect(scope.loginError).toBeUndefined();
      expect(scope.data).toEqual({});
      expect($window.location).toEqual('../');
    }))
  });
});
