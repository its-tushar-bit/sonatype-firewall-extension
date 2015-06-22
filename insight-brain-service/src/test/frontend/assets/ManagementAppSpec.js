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

  //TODO: https://issues.sonatype.org/browse/CLM-4600
  //integration of the app/org tree view should populate with some tests
});
