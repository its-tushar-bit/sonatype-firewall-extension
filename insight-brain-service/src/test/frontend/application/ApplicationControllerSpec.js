describe('ContactController', function () {
  var scope;

  beforeEach(module('ApplicationModule', function($provide) {
    $provide.value('ApplicationId', {
      encoded: function() {
        return 'bom1-12345678';
      }
    });
    $provide.value('OrganizationId', {
      encoded: function() {
        return null;
      }
    });
  }));

  beforeEach(inject(function ($rootScope, $controller) {
    scope = $rootScope.$new();
    scope.$close = jasmine.createSpy('closeSpy');
    $controller('ContactController', { $scope: scope, contextId : 'foo', contextType : 'application' });
  }));
  afterEach(function () {
    scope.$destroy();
  });

  it('Error', function () {
    scope.setQueryResults(null, "Failure");
    expect(scope.alerts[0].msg).toEqual("Failure");
  });

  it('Query Error', function () {
    scope.setQueryResults(null, "Failure");
    expect(scope.alerts[0].msg).toEqual("Failure");
    expect(scope.queryResults).toEqual(null);
  });

  it('Query Results+Error', function () {
    scope.setQueryResults([{ id : 'bar' }], "Failure");
    expect(scope.alerts[0].msg).toEqual("Failure");
    expect(scope.queryResults).toEqual([{ id : 'bar' }]);
  });

  it('Query Results', function () {
    scope.setQueryResults([{ id : 'bar' }]);
    expect(scope.alerts.length).toEqual(0);
    expect(scope.queryResults).toEqual([{ id : 'bar' }]);
  });

  it('Select a user', function () {
    scope.selectUser({ id : 'bar' });
    expect(scope.$close).toHaveBeenCalledWith({ id : 'bar' });
  });
});
