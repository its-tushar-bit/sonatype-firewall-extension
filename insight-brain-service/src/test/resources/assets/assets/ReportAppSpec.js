describe('reportApp', function() {
  var scope, state, licenseCheckedSpy;

  beforeEach(module('reportApp', 'ReportList', 'DashboardModule'));
  beforeEach(module(function($provide) {
    licenseCheckedSpy = jasmine.createSpy('then');
    $provide.value('licenseChecker', {
      check: function() {
        return {
          then: licenseCheckedSpy
        }
      }
    });
  }));

  beforeEach(inject(function($rootScope, $state, $controller, $httpBackend, CLMLocations) {
    expect(licenseCheckedSpy).toHaveBeenCalled();

    scope = $rootScope.$new();
    state = $state;

    $httpBackend.expectGET(CLMLocations.getActionStageUrl()).respond(MockData.getActionStageData());
    $httpBackend.expectGET(SpecUtil.toRegExp('/rest/application/services/summary')).respond(ApplicationMockData.getApplicationSummaryData());
    $httpBackend.expectGET('../assets/components/report-list.html?').respond('<div></div>');

    $controller('ReportListController', { $scope: scope, $state: state });

    $httpBackend.flush();
  }));

  afterEach(inject(function($httpBackend) {
    scope.$destroy();
    $httpBackend.verifyNoOutstandingExpectation();
    $httpBackend.verifyNoOutstandingRequest();
  }));

  it('loads data', function() {
    var mockStageData = MockData.getActionStageData();
    var mockApplicationSummaryData = ApplicationMockData.getApplicationSummaryData();

    expect(scope.stages).not.toBeUndefined();
    expect(scope.stages.length).toEqual(mockStageData.length);
    expect(scope.stages[0].id).toEqual(mockStageData[0].id);
    expect(scope.stages[scope.stages.length - 1].name).toEqual(mockStageData[mockStageData.length - 1].name);

    expect(scope.applications).not.toBeUndefined();
    expect(scope.applications.length).toEqual(mockApplicationSummaryData.length);
    expect(scope.applications[0].id).toEqual(mockApplicationSummaryData[0].id);
  });
});