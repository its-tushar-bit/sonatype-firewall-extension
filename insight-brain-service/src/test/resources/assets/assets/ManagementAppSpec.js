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
    expect(scope.panes).not.toBeUndefined();
    expect(scope.panes.length).toEqual(2);
    expect(scope.panes[0].name).toEqual('Organizations');
    expect(scope.panes[1].name).toEqual('Applications');
  });
});
