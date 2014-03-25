describe('reportApp', function() {
  var scope, state, currentUserSuccess, currentUserFail, licenseCheckerFail, licenseCheckerSuccess;
  
  beforeEach(module('ReportModule', 'ReportViolations', function($provide) {
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
    $provide.value('CurrentUser', {
      then : function (success, fail) {
        currentUserSuccess = success;
        currentUserFail = fail;
        return this;
      }
    });
    $provide.value('licenseChecker', {
      check : function () {
        return {
          then : function (success, fail) {
            licenseCheckerFail = fail;
            licenseCheckerSuccess = success;
          }
        };
      }
    });
  }));
  
  beforeEach(inject(function($rootScope, $state, $controller, $httpBackend, CLMLocations) {
    $rootScope.licensed = true;
    scope = $rootScope.$new();
    state = $state;

    $state.go('reports.violations');

    currentUserSuccess({
      authenticated : true,
      username : 'user'
    });
    $httpBackend.expectGET(SpecUtil.toRegExp(CLMLocations.getProductFeaturesUrl())).respond(['policy-monitoring']);
    $httpBackend.expectGET('../assets/management.html?').respond('<div></div>');
    $httpBackend.expectGET('../report-assets/violations/report-list.html?').respond('<div></div>');
    $httpBackend.expectGET(SpecUtil.toRegExp(CLMLocations.getActionStageUrl())).respond(MockData.getActionStageData());
    $httpBackend.expectGET(SpecUtil.toRegExp('/rest/application/services/summary')).respond(ApplicationMockData.getApplicationSummaryData());

    $controller('ReportViolationsController', { $scope: scope, $state: state });

    $httpBackend.flush();
  }));

  afterEach(inject(function($httpBackend) {
    scope.$destroy();
    $httpBackend.verifyNoOutstandingExpectation();
    $httpBackend.verifyNoOutstandingRequest();
  }));

  it('loads data', function () {
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

  describe('Filters', function () {
    it('Application Name', function () {
      scope.appFilter = 'appl';
      expect(scope.isVisible(scope.applications[0])).toBeTruthy();
      scope.appFilter = 'foobar';
      expect(scope.isVisible(scope.applications[0])).toBeFalsy();
    });

    it('Organization Name', function () {
      scope.appFilter = 'OLE'; // triggers case sensitivity vs Ole
      expect(scope.isVisible(scope.applications[0])).toBeTruthy();
      scope.appFilter = 'foobar';
      expect(scope.isVisible(scope.applications[0])).toBeFalsy();
    });

    it('Null', function () {
      // App filter starts empty, we shouldn't explode a
      expect(scope.isVisible(scope.applications[0])).toBeTruthy();
    });

    it('Empty', function () {
      scope.appFilter = '';
      expect(scope.isVisible(scope.applications[0])).toBeTruthy();
    });
  });
});