describe('reportApp', function() {
  var scope, state;
  
  beforeEach(module('reportApp', 'ReportViolations', 'DashboardModule', function($provide) {
    $provide.value('$window', {
      location: {
        reload: function(){}
      },
      navigator: {
        userAgent:{}
      },
      document: {
        createElement: function(){ return null ;}
      }
    });
    $provide.value('securityStatusChecker', {
      check : function(){
        return {
          then: function(data) {
          }
        };
      }
    });
  }));
  
  beforeEach(inject(function($rootScope, $state, $controller, $httpBackend, CLMLocations) {
    $rootScope.initialized = true;
    $rootScope.username = 'user';
    $rootScope.authenticated = true;
    $rootScope.licensed = true;
    scope = $rootScope.$new();
    state = $state;

    $httpBackend.expectGET(CLMLocations.getActionStageUrl()).respond(MockData.getActionStageData());
    $httpBackend.expectGET(SpecUtil.toRegExp('/rest/application/services/summary')).respond(ApplicationMockData.getApplicationSummaryData());

    $controller('ReportViolationsController', { $scope: scope, $state: state });

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