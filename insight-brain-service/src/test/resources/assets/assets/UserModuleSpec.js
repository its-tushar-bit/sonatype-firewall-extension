describe('UserModuleSpec', function() {
  beforeEach(module('UserModule', function($provide) {
    $provide.factory('hudson', ['$http', function($http) {
        return $http;
      }
    ]);
  }));

  afterEach(inject(function($httpBackend) {
    $httpBackend.verifyNoOutstandingExpectation();
    $httpBackend.verifyNoOutstandingRequest();
  }));

  describe('UserListController', function () {
    var scope = null;
    
    function getController() {
      inject(function ($controller, $rootScope) {
        scope = $rootScope.$new();
        $controller('UserListController', {
          $scope : scope 
        });
      });
      return scope;
    }

    afterEach(function () {
      scope.$destroy();
    });

    it('Successful Request', inject(function ($httpBackend, CLMLocations) {
      $httpBackend.expectGET(SpecUtil.toRegExp(CLMLocations.getUserListUrl())).respond([{ id : 'foo' }]);
      getController();
      $httpBackend.flush();

      expect(scope.error).toBeFalsy();
      expect(scope.users.length).toEqual(1);
    }));
    
    it('Unsuccessful Request', inject(function ($httpBackend, CLMLocations) {
      $httpBackend.expectGET(SpecUtil.toRegExp(CLMLocations.getUserListUrl())).respond(500);
      getController();
      $httpBackend.flush();

      expect(scope.error).toBeTruthy();
      expect(scope.users).toBeFalsy();

      // User clicks reload
      $httpBackend.expectGET(SpecUtil.toRegExp(CLMLocations.getUserListUrl())).respond([{ id : 'foo' }]);
      scope.doLoad();
      expect(scope.error).toBeFalsy();
      $httpBackend.flush();

      expect(scope.error).toBeFalsy();
      expect(scope.users.length).toEqual(1);
    }));
  });
});