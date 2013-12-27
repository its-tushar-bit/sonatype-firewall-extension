describe('dashboardHeader', function() {
  var scope, state;

  beforeEach(module('DashboardHeader', function($stateProvider) {
    $stateProvider.state('test', {url: '/test/:testId'}).state('management', {}).state('management.configuration', {}).state(
                'management.configuration.productlicense', {});
  });

  beforeEach(inject(function($rootScope, $state) {
    scope = $rootScope.$new();
    state = $state;

    $controller('dashboardHeaderController', {
      $scope: scope,
      $state: state
    });
  }));

  afterEach(inject(function($httpBackend) {
    if (scope) {
      scope.$destroy();
    }
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