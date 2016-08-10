describe('dashboardViewSummary.directive.spec', function() {
  var scope, commonFilters = {
    applicationIds: ['1', '2'],
    policyThreatTypes: ['3', '4'],
    stageTypeIds: ['5', '6'],
    applicationTagIds: ['7', '8'],
    policyThreatLevel: [3, 9]
  };

  beforeEach(module('dashboard.utils'));

  afterEach(inject(function($httpBackend) {
    scope.$destroy();
    $httpBackend.verifyNoOutstandingExpectation(false);
    $httpBackend.verifyNoOutstandingRequest();
  }));

  beforeEach(inject(function($rootScope, $httpBackend) {
    scope = $rootScope.$new();
    scope.filters={};
    $httpBackend.expectGET('dashboard-view-summary').respond('<div></div>');
  }));

  it('Predefined filters sent in GET request', inject(function($compile, $httpBackend, CLMLocations) {
    scope.filters = commonFilters;

    $httpBackend.expectPOST(CLMLocations.getDashboardViewingSummaryUrl()).respond();
    $compile(angular.element('<div dashboard-view-summary filters="filters"></div>'))(scope);
    $httpBackend.flush();
    expect(scope.$$childHead.error).toBeNull();
  }));

  it('Data loaded from server into model properly', inject(function($compile, $httpBackend, CLMLocations) {
    var data = {
      matchedApplications: 2,
      matchedPolicies: 4,
      matchedComponents: 300
    };

    $httpBackend.expectPOST(CLMLocations.getDashboardViewingSummaryUrl()).respond(data);
    $compile(angular.element('<div dashboard-view-summary filters="filters"></div>'))(scope);
    $httpBackend.flush();
    expect(scope.$$childHead.data).toEqual(data);
    expect(scope.$$childHead.error).toBeNull();
  }));

  it('Error propogated to scope', inject(function($compile, $httpBackend, CLMLocations) {
    $httpBackend.expectPOST(CLMLocations.getDashboardViewingSummaryUrl()).respond(404, 'You screwed up');
    $compile(angular.element('<div dashboard-view-summary filters="filters"></div>'))(scope);
    $httpBackend.flush();
    expect(scope.$$childHead.error).toBeDefined();
  }));
});
