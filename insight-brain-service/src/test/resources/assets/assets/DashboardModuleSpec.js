describe('dashboardApp', function() {
  var scope;
  var policyViolations = [
    {
      applicationId: "fooID",
      applicationName: "Foo App",
      artifactId: "commons-httpclient",
      groupId: "commons-httpclient",
      hash: "f0776db1593e215146d2",
      id: "6852087f771e4ee292e368a0287e5fbb",
      policyEvaluationId: "2cca57dc20b947e999cc348f83da1e5b",
      policyId: "f219cdc1b9bd4bd089343dcdc542e757",
      policyName: "Bar Policy",
      threatCategory: "SECURITY",
      threatLevel: 10,
      version: "3.1"
    },
    {
      applicationId: "fooID",
      applicationName: "Foo App",
      artifactId: "geronimo-security",
      groupId: "org.apache.geronimo.framework",
      hash: "848d7549ef7ec13ce546",
      id: "5e833e5982534083a035019c07d66507",
      policyEvaluationId: "2cca57dc20b947e999cc348f83da1e5b",
      policyId: "f219cdc1b9bd4bd089343dcdc542e757",
      policyName: "Bar Policy",
      threatCategory: "SECURITY",
      threatLevel: 10,
      version: "2.1"
    },
    {
      applicationId: "barID",
      applicationName: "Bar App",
      artifactId: "jetty",
      groupId: "org.mortbay.jetty",
      hash: "494308fc2d433720c778",
      id: "ea3eab1e0cb04c898714d235b236fa26",
      policyEvaluationId: "2cca57dc20b947e999cc348f83da1e5b",
      policyId: "f219cdc1b9bd4bd089343dcdc542e757",
      policyName: "Bar Policy",
      threatCategory: "SECURITY",
      threatLevel: 10,
      version: "6.1.15"
    }
  ];

  beforeEach(module('DashboardModule'));

  afterEach(inject(function($httpBackend) {
    if (scope) {
      scope.$destroy();
    }
    $httpBackend.verifyNoOutstandingExpectation();
    $httpBackend.verifyNoOutstandingRequest();
  }));

  describe('dashboardController', function() {
    var applicationsData;

    beforeEach(inject(function($rootScope, $controller, $httpBackend, CLMLocations) {
      scope = $rootScope.$new();
      applicationsData = ApplicationMockData.getApplicationsData();

      $httpBackend.expectGET(CLMLocations.getApplicationsUrl()).respond(applicationsData);
      $httpBackend.expectGET(CLMLocations.getPolicyViolationsUrl() + '?maxResults=20').respond(policyViolations);
      $controller('DashboardController', { $scope: scope });
      $httpBackend.flush();
    }));

    it('loads applications', function() {
      expect(scope.applications.length).toBe(applicationsData.length);
      expect(scope.applications[0].id).toBe(applicationsData[0].id);
    });

    it('loads policy violations', function() {
      expect(scope.highestRisks.length).toBe(policyViolations.length);
      expect(scope.highestRisks[0].id).toBe(policyViolations[0].id);
    });

    it('filters policy violations by application', inject(function($httpBackend, CLMLocations) {
      expect(scope.appliedApplicationPublicIds.length).toBe(0);
      scope.queuedApplicationPublicIds = ['fooID'];
      scope.applyFilters();
      $httpBackend.expectGET(CLMLocations.getPolicyViolationsUrl() + '?applicationPublicIds=fooID&maxResults=20').respond([
        policyViolations[0],
        policyViolations[1]
      ]);
      scope.$digest();
      $httpBackend.flush();
      expect(scope.appliedApplicationPublicIds.length).toBe(1);
      expect(scope.highestRisks.length).toBe(2);
    }));

    it('cancels filters', function() {
      scope.appliedApplicationPublicIds = [];
      scope.queuedApplicationPublicIds = ['fooID'];
      scope.cancelFilters();

      expect(scope.appliedApplicationPublicIds.length).toBe(0);
    });
  });
});