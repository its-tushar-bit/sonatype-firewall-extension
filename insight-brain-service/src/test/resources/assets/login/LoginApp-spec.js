describe('Tests for the LoginApp', function() {

  beforeEach(module('LoginApp'));

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

    it('validate initial state.', function() {
      expect(scope.data).not.toBeUndefined();
    });
  });
});
