describe('DashboardModule', function() {
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
      threatCategory: "OTHER",
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
  var newestViolations = [
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
      threatCategory: "OTHER",
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
  ], tags = [
   {
     id: "tagid1",
     organizationId: 'orgId1',
     name: "TagOne",
     nameLowercaseNoWhitespace: "tagone",
     description: "Tag One Description"
   }, {
      id: "tagid2",
      organizationId: 'orgId2',
      name: "TagTwo",
      nameLowercaseNoWhitespace: "tagtwo",
      description: "Tag Two Description"
    }
  ], applications = [
    {
      id: 'applicationId1',
      publicId: 'applicationPublicId1',
      name: 'ApplicationOne',
      organizationId: 'orgId1'
    }
  ], organizations = [
    {
      id: 'orgId1',
      name: 'OrganizationOne'
    },
    {
      id: 'orgId2',
      name: 'OrganizationTwo'
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
    var stageTypeData;

    beforeEach(inject(function($rootScope, $controller, $httpBackend, CLMLocations) {
      scope = $rootScope.$new();

      stageTypeData = [{
        id: 'type1',
        name: 'Type 1'
      },{
        id: 'type2',
        name: 'Type 2'
      }];

      $httpBackend.expectGET(CLMLocations.getActionStageUrl()).respond(stageTypeData);
      $httpBackend.expectGET(CLMLocations.getApplicationsUrl()).respond(applications);
      $httpBackend.expectGET(CLMLocations.getOrganizationsUrl()).respond(organizations);
      $httpBackend.expectGET(CLMLocations.getApplicationTags()).respond(tags);
      $httpBackend.expectGET(CLMLocations.getPolicyViolationsUrl() + '?maxResults=20').respond(policyViolations);
      $httpBackend.expectGET(CLMLocations.getPolicyViolationsUrl() + '?maxResults=20&newest=true').respond(newestViolations);
      $controller('DashboardController', { $scope: scope });
      $httpBackend.flush();
    }));

    it('loads applications', function() {
      expect(scope.applications.length).toBe(applications.length);
      expect(scope.applications[0].id).toBe(applications[0].id);
    });
    
    it('loads stage types', function() {
      expect(scope.stageTypes.length).toBe(stageTypeData.length);
      expect(scope.stageTypes[0].id).toBe(stageTypeData[0].id);
      expect(scope.stageTypes[1].id).toBe(stageTypeData[1].id);
    })

    it('loads application tags', function() {
      expect(scope.applicationTags.length).toBe(tags.length);
      expect(scope.applicationTags[0].id).toBe(tags[0].id);
      expect(scope.applicationTags[0].owner).toBe(organizations[0].name);
    });

    it('loads policy violations', function() {
      expect(scope.highestRisks.length).toBe(policyViolations.length);
      expect(scope.highestRisks[0].id).toBe(policyViolations[0].id);

      expect(scope.newestRisks.length).toBe(newestViolations.length);
      expect(scope.newestRisks[0].id).toBe(newestViolations[0].id);
    });

    it('filters policy violations by application', inject(function($httpBackend, CLMLocations) {
      expect(scope.filters.applicationPublicIds.applied.length).toBe(0);
      scope.filters.applicationPublicIds.queued = ['fooID'];
      scope.applyFilters();
      $httpBackend.expectGET(CLMLocations.getPolicyViolationsUrl() + '?applicationPublicIds=fooID&maxResults=20').respond([
        policyViolations[0],
        policyViolations[1]
      ]);
      $httpBackend.expectGET(CLMLocations.getPolicyViolationsUrl() + '?applicationPublicIds=fooID&maxResults=20&newest=true').respond([
        newestViolations[0]
      ]);
      $httpBackend.flush();
      expect(scope.filters.applicationPublicIds.applied.length).toBe(1);
      expect(scope.highestRisks.length).toBe(2);
      expect(scope.newestRisks.length).toBe(1);
    }));

    it('filters policy violations by policy threat type', inject(function($httpBackend, CLMLocations) {
      expect(scope.filters.policyThreatTypes.applied.length).toBe(0);
      scope.filters.policyThreatTypes.queued = ['security', 'other'];
      scope.applyFilters();
      $httpBackend.expectGET(CLMLocations.getPolicyViolationsUrl() +
        '?maxResults=20&policyThreatCategories=security,other').respond([policyViolations[0]]);
      $httpBackend.expectGET(CLMLocations.getPolicyViolationsUrl() +
        '?maxResults=20&newest=true&policyThreatCategories=security,other').respond([newestViolations[0]]);
      $httpBackend.flush();
      expect(scope.filters.policyThreatTypes.applied).toEqual(['security', 'other']);
      expect(scope.highestRisks.length).toBe(1);
      expect(scope.newestRisks.length).toBe(1);
    }));
    
    it('filters policy violations by stage type', inject(function($httpBackend, CLMLocations) {
      expect(scope.filters.stageTypeIds.applied.length).toBe(0);
      scope.filters.stageTypeIds.queued = ['type1', 'type2'];
      scope.applyFilters();
      $httpBackend.expectGET(CLMLocations.getPolicyViolationsUrl() +
        '?maxResults=20&stageIds=type1&stageIds=type2').respond([policyViolations[0]]);
      $httpBackend.expectGET(CLMLocations.getPolicyViolationsUrl() +
        '?maxResults=20&newest=true&stageIds=type1&stageIds=type2').respond([newestViolations[0]]);
      $httpBackend.flush();
      expect(scope.filters.stageTypeIds.applied).toEqual(['type1', 'type2']);
      expect(scope.highestRisks.length).toBe(1);
      expect(scope.newestRisks.length).toBe(1);
    }));

    it('filters policy violations by application tag', inject(function($httpBackend, CLMLocations) {
      expect(scope.filters.applicationTagIds.queued.length).toBe(0);
      scope.filters.applicationTagIds.queued = ['fooID'];
      scope.applyFilters();
      $httpBackend.expectGET(CLMLocations.getPolicyViolationsUrl() + '?maxResults=20&tagIds=fooID').respond([
        policyViolations[0],
        policyViolations[1]
      ]);
      $httpBackend.expectGET(CLMLocations.getPolicyViolationsUrl() + '?maxResults=20&newest=true&tagIds=fooID').respond([
        newestViolations[0]
      ]);
      scope.$digest();
      $httpBackend.flush();
      expect(scope.filters.applicationTagIds.applied).toEqual(['fooID']);
      expect(scope.highestRisks.length).toBe(2);
      expect(scope.newestRisks.length).toBe(1);
    }));

    it('cancels filters', function() {
      scope.filters.applicationPublicIds.applied = [];
      scope.filters.applicationPublicIds.queued = ['fooID'];
      scope.filters.policyThreatTypes.applied = [];
      scope.filters.policyThreatTypes.queued = ['security'];
      scope.filters.stageTypeIds.applied = [];
      scope.filters.stageTypeIds.queued = ['type1'];
      scope.filters.applicationTagIds.applied = [];
      scope.filters.applicationTagIds.queued = ['tagID'];
      scope.cancelFilters();

      expect(scope.filters.applicationPublicIds.applied.length).toBe(0);
      expect(scope.filters.applicationPublicIds.queued.length).toBe(0);
      expect(scope.filters.policyThreatTypes.applied.length).toBe(0);
      expect(scope.filters.policyThreatTypes.queued.length).toBe(0);
      expect(scope.filters.stageTypeIds.applied.length).toBe(0);
      expect(scope.filters.stageTypeIds.queued.length).toBe(0);
      expect(scope.filters.applicationTagIds.applied.length).toBe(0);
      expect(scope.filters.applicationTagIds.queued.length).toBe(0);
    });

    it('converts from application.publicId to application.name', function(){
      expect(scope.applicationNameFor('applicationPublicId1')).toBe('ApplicationOne');
    });

    it('converts from policyThreatCategory.id to policyThreatCategory.name', function(){
      expect(scope.policyThreatTypeNameFor('security')).toBe('Security');
    });
    
    it('converts from stageType.id to stageType.name', function(){
      expect(scope.stageTypeNameFor('type1')).toBe('Type 1');
    });

    it('handles http errors', inject(function($httpBackend, CLMLocations){
      expect(scope.error).toBeNull();
      scope.applyFilters();
      $httpBackend.expectGET(CLMLocations.getPolicyViolationsUrl() +'?maxResults=20').respond(500, 'An error');
      $httpBackend.expectGET(CLMLocations.getPolicyViolationsUrl() +'?maxResults=20&newest=true').respond([newestViolations[0]]);
      $httpBackend.flush();
      expect(scope.error).toBeDefined();
      expect(scope.error.status).toBe(500);
      expect(scope.error.data).toBe('An error');

      scope.applyFilters();
      $httpBackend.expectGET(CLMLocations.getPolicyViolationsUrl() +'?maxResults=20').respond([policyViolations[0]]);
      $httpBackend.expectGET(CLMLocations.getPolicyViolationsUrl() +'?maxResults=20&newest=true').respond(500, 'An error');
      $httpBackend.flush();
      expect(scope.error).toBeDefined();
      expect(scope.error.status).toBe(500);
      expect(scope.error.data).toBe('An error');

      scope.applyFilters();
      $httpBackend.expectGET(CLMLocations.getPolicyViolationsUrl() +'?maxResults=20').respond([policyViolations[0]]);
      $httpBackend.expectGET(CLMLocations.getPolicyViolationsUrl() +'?maxResults=20&newest=true').respond([newestViolations[0]]);
      $httpBackend.flush();
      expect(scope.error).toBeNull();
    }));
  });
});