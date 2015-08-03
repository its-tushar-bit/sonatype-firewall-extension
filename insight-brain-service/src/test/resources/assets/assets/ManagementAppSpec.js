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

  describe('OwnerTreeViewController', function() {
    var $controller, $httpBackend, $state, CLMLocations, OrganizationStore, ApplicationStore,
        organizations = StoreMockData.getOrganizations(),
        applications = StoreMockData.getApplications();

    beforeEach(inject(function(_$controller_, _$rootScope_, _$httpBackend_, _$state_, _$timeout_, _CLMLocations_,
                               _OrganizationStore_, _ApplicationStore_)
    {
      $controller = _$controller_;
      $httpBackend = _$httpBackend_;
      $state = _$state_;
      CLMLocations = _CLMLocations_;
      OrganizationStore = _OrganizationStore_;
      ApplicationStore = _ApplicationStore_;

      scope = _$rootScope_.$new();
      $controller('OwnerTreeViewController', { $scope: scope });

      spyOn($state, 'includes').andReturn(false);

      $httpBackend.expectGET(CLMLocations.getOrganizationsUrl()).respond(organizations);
      $httpBackend.expectGET(CLMLocations.getApplicationsUrl()).respond(applications);
      scope.$digest();
      $httpBackend.flush();
      _$timeout_.flush();
    }));

    it('loads organizations and applications', function() {
      expect(scope.organizations).toBeDefined();
      expect(scope.organizations.length).toBe(organizations.length);

      expect(scope.organizations[0].id).toBe(organizations[0].id);
      expect(scope.organizations[0].name).toBe(organizations[0].name);
      expect(scope.organizations[0].isVisible).toBe(true);
      expect(scope.organizations[0].isExpanded).toBe(false);
      expect(scope.organizations[0].applications).toBeDefined();
      expect(scope.organizations[0].applications.length).toBe(2);
      expect(scope.organizations[0].applications[0].id).toBe(applications[0].id);
      expect(scope.organizations[0].applications[0].publicId).toBe(applications[0].publicId);
      expect(scope.organizations[0].applications[0].name).toBe(applications[0].name);
      expect(scope.organizations[0].applications[0].isVisible).toBe(true);
      expect(scope.organizations[0].applications[1].id).toBe(applications[1].id);
      expect(scope.organizations[0].applications[1].publicId).toBe(applications[1].publicId);
      expect(scope.organizations[0].applications[1].name).toBe(applications[1].name);
      expect(scope.organizations[0].applications[1].isVisible).toBe(true);

      expect(scope.organizations[1].id).toBe(organizations[1].id);
      expect(scope.organizations[1].name).toBe(organizations[1].name);
      expect(scope.organizations[1].applications).toBeDefined();
      expect(scope.organizations[1].applications.length).toBe(0);
      expect(scope.organizations[1].isVisible).toBe(true);
      expect(scope.organizations[1].isExpanded).toBe(false);

      expect(scope.organizations[2].id).toBe(applications[2].organizationId);
      expect(scope.organizations[2].name).toBe(applications[2].organizationName);
      expect(scope.organizations[2].isVisible).toBe(true);
      expect(scope.organizations[2].isExpanded).toBe(false);
      expect(scope.organizations[2].synthetic).toBe(true);
      expect(scope.organizations[2].applications).toBeDefined();
      expect(scope.organizations[2].applications.length).toBe(1);
      expect(scope.organizations[2].applications[0].id).toBe(applications[2].id);
      expect(scope.organizations[2].applications[0].publicId).toBe(applications[2].publicId);
      expect(scope.organizations[2].applications[0].name).toBe(applications[2].name);
      expect(scope.organizations[2].applications[0].isVisible).toBe(true);

      //validate that the synthesized root org is in the scope
      expect(scope.rootOrganization.id).toBe('rootOrg');
      expect(scope.rootOrganization.name).toBe('Root org');
    });

    it('checks if an organization or application is selected', function() {
      expect($state.includes.calls.length).toBe(8);
      expect($state.includes).toHaveBeenCalledWith('management.organization', {
        organizationId: 'rootOrg'
      });
      expect($state.includes).toHaveBeenCalledWith('management.organization', {
        organizationId: organizations[0].id
      });
      expect($state.includes).toHaveBeenCalledWith('management.organization', {
        organizationId: organizations[1].id
      });
      expect($state.includes).toHaveBeenCalledWith('management.organization', {
        organizationId: applications[2].organizationId
      });
      expect($state.includes).toHaveBeenCalledWith('management.application');
    });

    it('filters organizations', function() {
      scope.filter.value = 'ONE';
      scope.$digest();

      expect(scope.organizations).toBeDefined();
      expect(scope.organizations.length).toBe(3);
      expect(scope.organizations[0].id).toBe(organizations[0].id);
      expect(scope.organizations[0].name).toBe(organizations[0].name);
      expect(scope.organizations[0].isVisible).toBe(true);
      expect(scope.organizations[0].isExpanded).toBe(true);

      expect(scope.organizations[0].applications).toBeDefined();
      expect(scope.organizations[0].applications.length).toBe(2);
      expect(scope.organizations[0].applications[0].id).toBe(applications[0].id);
      expect(scope.organizations[0].applications[0].publicId).toBe(applications[0].publicId);
      expect(scope.organizations[0].applications[0].name).toBe(applications[0].name);
      expect(scope.organizations[0].applications[0].isVisible).toBe(true);
      expect(scope.organizations[0].applications[1].id).toBe(applications[1].id);
      expect(scope.organizations[0].applications[1].publicId).toBe(applications[1].publicId);
      expect(scope.organizations[0].applications[1].name).toBe(applications[1].name);
      expect(scope.organizations[0].applications[1].isVisible).toBe(true);

      expect(scope.organizations[1].id).toBe(organizations[1].id);
      expect(scope.organizations[1].name).toBe(organizations[1].name);
      expect(scope.organizations[1].isVisible).toBe(false);
      expect(scope.organizations[1].isExpanded).toBe(false);
      expect(scope.organizations[1].applications).toBeDefined();
      expect(scope.organizations[1].applications.length).toBe(0);

      expect(scope.organizations[2].id).toBe(applications[2].organizationId);
      expect(scope.organizations[2].name).toBe(applications[2].organizationName);
      expect(scope.organizations[2].isVisible).toBe(false);
      expect(scope.organizations[2].isExpanded).toBe(false);
      expect(scope.organizations[2].applications.length).toBe(1);
      expect(scope.organizations[2].applications[0].id).toBe(applications[2].id);
      expect(scope.organizations[2].applications[0].publicId).toBe(applications[2].publicId);
      expect(scope.organizations[2].applications[0].name).toBe(applications[2].name);
      expect(scope.organizations[2].applications[0].isVisible).toBe(false);
    });

    it('filters applications', function() {
      scope.filter.value = 'TEN';
      scope.$digest();

      expect(scope.organizations).toBeDefined();
      expect(scope.organizations.length).toBe(3);
      expect(scope.organizations[0].id).toBe(organizations[0].id);
      expect(scope.organizations[0].name).toBe(organizations[0].name);
      expect(scope.organizations[0].isVisible).toBe(true);
      expect(scope.organizations[0].isExpanded).toBe(true);

      expect(scope.organizations[0].applications).toBeDefined();
      expect(scope.organizations[0].applications.length).toBe(2);
      expect(scope.organizations[0].applications[0].id).toBe(applications[0].id);
      expect(scope.organizations[0].applications[0].publicId).toBe(applications[0].publicId);
      expect(scope.organizations[0].applications[0].name).toBe(applications[0].name);
      expect(scope.organizations[0].applications[0].isVisible).toBe(true);
      expect(scope.organizations[0].applications[1].id).toBe(applications[1].id);
      expect(scope.organizations[0].applications[1].publicId).toBe(applications[1].publicId);
      expect(scope.organizations[0].applications[1].name).toBe(applications[1].name);
      expect(scope.organizations[0].applications[1].isVisible).toBe(false);

      expect(scope.organizations[1].id).toBe(organizations[1].id);
      expect(scope.organizations[1].name).toBe(organizations[1].name);
      expect(scope.organizations[1].applications).toBeDefined();
      expect(scope.organizations[1].applications.length).toBe(0);
      expect(scope.organizations[1].isVisible).toBe(false);
      expect(scope.organizations[1].isExpanded).toBe(false);

      expect(scope.organizations[2].id).toBe(applications[2].organizationId);
      expect(scope.organizations[2].name).toBe(applications[2].organizationName);
      expect(scope.organizations[2].isVisible).toBe(false);
      expect(scope.organizations[2].isExpanded).toBe(false);
      expect(scope.organizations[2].applications.length).toBe(1);
      expect(scope.organizations[2].applications[0].id).toBe(applications[2].id);
      expect(scope.organizations[2].applications[0].publicId).toBe(applications[2].publicId);
      expect(scope.organizations[2].applications[0].name).toBe(applications[2].name);
      expect(scope.organizations[2].applications[0].isVisible).toBe(false);
    });

    it('handles new organization', function() {
      var newOrganizationRaw = StoreMockData.newOrganization();
      var newOrganization = OrganizationStore.create();
      newOrganization.$save();
      $httpBackend.expectPOST(CLMLocations.getOrganizationsUrl()).respond(newOrganizationRaw);
      $httpBackend.flush();

      expect(scope.organizations).toBeDefined();
      expect(scope.organizations.length).toBe(4);

      expect(scope.organizations[3].id).toBe(newOrganization.id);
      expect(scope.organizations[3].name).toBe(newOrganization.name);
      expect(scope.organizations[3].applications).toBeDefined();
      expect(scope.organizations[3].applications.length).toBe(0);
      expect(scope.organizations[3].isVisible).toBe(true);
      expect(scope.organizations[3].isExpanded).toBe(false);
    });

    it('handles removed organization', function() {
      OrganizationStore.get().then(function(organizations) {
        organizations[1].$delete();
        $httpBackend.expectDELETE(CLMLocations.getOrganizationsUrl() + '/' + organizations[1].id).respond({});
      });
      scope.$digest();
      $httpBackend.flush();

      expect(scope.organizations).toBeDefined();
      expect(scope.organizations.length).toBe(2);
    });

    it('handles changes to organization', function() {
      OrganizationStore.get().then(function(organizations) {
        organizations[0].name = 'foo';
      });
      scope.$digest();

      expect(scope.organizations[0].name).toBe('foo');
    });

    it('handles new application', function() {
      var newApplicationRaw = StoreMockData.newApplication();
      var newApplication = ApplicationStore.create();
      newApplication.$save();
      $httpBackend.expectPOST(CLMLocations.getApplicationsUrl()).respond(newApplicationRaw);
      $httpBackend.flush();

      expect(scope.organizations).toBeDefined();
      expect(scope.organizations.length).toBe(3);

      expect(scope.organizations[1].applications).toBeDefined();
      expect(scope.organizations[1].applications.length).toBe(1);

      expect(scope.organizations[1].applications[0].id).toBe(newApplicationRaw.id);
      expect(scope.organizations[1].applications[0].publicId).toBe(newApplicationRaw.publicId);
      expect(scope.organizations[1].applications[0].name).toBe(newApplicationRaw.name);
      expect(scope.organizations[1].applications[0].isVisible).toBe(true);
    });

    it('handles removed application', function() {
      ApplicationStore.get().then(function(applications) {
        applications[0].$delete();
        $httpBackend.expectDELETE(CLMLocations.getApplicationsUrl() + '/' + applications[0].publicId).respond({});
      });
      scope.$digest();
      $httpBackend.flush();

      expect(scope.organizations).toBeDefined();
      expect(scope.organizations.length).toBe(3);

      expect(scope.organizations[0].applications).toBeDefined();
      expect(scope.organizations[0].applications.length).toBe(1);
    });

    it('handles changes to application', function() {
      ApplicationStore.get().then(function(applications) {
        applications[0].name = 'foo';
      });
      scope.$digest();

      expect(scope.organizations[0].applications[0].name).toBe('foo');
    });
  });
});
