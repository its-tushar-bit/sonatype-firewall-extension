describe('ManagementModule', function() {
  var scope;

  beforeEach(module('OrganizationModule', 'ApplicationModule', 'AngularCommon'));
  beforeEach(inject(function($rootScope, $state, $controller, commonCodeFactory) {
    scope = $rootScope.$new();

    $controller('ManagementController', {
      $scope: scope,
      $state: $state,
      commonCodeFactory: commonCodeFactory
    });
  }));
  afterEach(function() {
    scope.$destroy();
  });

  it('Lists Org before App', function() {
    expect(scope.managementPanes).not.toBeUndefined();
    expect(scope.managementPanes.length).toEqual(4);
    expect(scope.managementPanes[0].name).toEqual('Organizations');
    expect(scope.managementPanes[1].name).toEqual('Applications');
    expect(scope.managementPanes[2].name).toEqual('Security');
    expect(scope.managementPanes[3].name).toEqual('Configuration');
  });
});
