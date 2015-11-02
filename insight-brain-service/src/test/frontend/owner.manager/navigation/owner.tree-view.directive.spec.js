describe('owner.tree-view.directive.spec.js', function() {
  var scope;

  beforeEach(module(function($provide) {
    // $state stub for spying
    $provide.service('$state', function() {
      return {
        includes: function(state, params) {}
      };
    });
  }));
  beforeEach(module('owner.manager.module'));

  afterEach(inject(function(_$httpBackend_) {
    _$httpBackend_.verifyNoOutstandingExpectation();
    _$httpBackend_.verifyNoOutstandingRequest();

    if (scope && scope.$destroy) {
      scope.$destroy();
    }
  }));

  describe('ownerTreeViewDirective', function() {
    var $httpBackend, $state, CLMLocations, OrganizationStore, ApplicationStore,
        organizations = StoreMockData.getOrganizations(),
        applications = StoreMockData.getApplications();

    beforeEach(inject(function(_$rootScope_, _$httpBackend_, _$state_, _$timeout_, _$compile_, _CLMLocations_,
        _OrganizationStore_, _ApplicationStore_)
    {
      $httpBackend = _$httpBackend_;
      $state = _$state_;
      CLMLocations = _CLMLocations_;
      OrganizationStore = _OrganizationStore_;
      ApplicationStore = _ApplicationStore_;

      scope = _$rootScope_.$new();
      var ownerTreeView = angular.element('<div owner-tree-view></div>');
      _$compile_(ownerTreeView)(scope);
      SpecUtil.respondWithTemplate($httpBackend, 'owner.manager/navigation/owner.tree.view.directive.html');
      scope.$digest();

      spyOn($state, 'includes').andReturn(false);

      $httpBackend.expectGET(CLMLocations.getOrganizationsUrl()).respond(organizations);
      $httpBackend.expectGET(CLMLocations.getApplicationsUrl()).respond(applications);
      scope.$digest();
      $httpBackend.flush();
      _$timeout_.flush();
    }));

    it('loads organizations and applications', function() {
      expect(scope.vm.organizations).toBeDefined();
      expect(scope.vm.organizations.length).toBe(organizations.length);

      expect(scope.vm.organizations[0].id).toBe(organizations[0].id);
      expect(scope.vm.organizations[0].name).toBe(organizations[0].name);
      expect(scope.vm.organizations[0].isVisible).toBe(true);
      expect(scope.vm.organizations[0].isExpanded).toBe(false);
      expect(scope.vm.organizations[0].applications).toBeDefined();
      expect(scope.vm.organizations[0].applications.length).toBe(2);
      expect(scope.vm.organizations[0].applications[0].id).toBe(applications[0].id);
      expect(scope.vm.organizations[0].applications[0].publicId).toBe(applications[0].publicId);
      expect(scope.vm.organizations[0].applications[0].name).toBe(applications[0].name);
      expect(scope.vm.organizations[0].applications[0].isVisible).toBe(true);
      expect(scope.vm.organizations[0].applications[1].id).toBe(applications[1].id);
      expect(scope.vm.organizations[0].applications[1].publicId).toBe(applications[1].publicId);
      expect(scope.vm.organizations[0].applications[1].name).toBe(applications[1].name);
      expect(scope.vm.organizations[0].applications[1].isVisible).toBe(true);

      expect(scope.vm.organizations[1].id).toBe(organizations[1].id);
      expect(scope.vm.organizations[1].name).toBe(organizations[1].name);
      expect(scope.vm.organizations[1].applications).toBeDefined();
      expect(scope.vm.organizations[1].applications.length).toBe(0);
      expect(scope.vm.organizations[1].isVisible).toBe(true);
      expect(scope.vm.organizations[1].isExpanded).toBe(false);

      expect(scope.vm.organizations[2].id).toBe(applications[2].organizationId);
      expect(scope.vm.organizations[2].name).toBe(applications[2].organizationName);
      expect(scope.vm.organizations[2].isVisible).toBe(true);
      expect(scope.vm.organizations[2].isExpanded).toBe(false);
      expect(scope.vm.organizations[2].synthetic).toBe(true);
      expect(scope.vm.organizations[2].applications).toBeDefined();
      expect(scope.vm.organizations[2].applications.length).toBe(1);
      expect(scope.vm.organizations[2].applications[0].id).toBe(applications[2].id);
      expect(scope.vm.organizations[2].applications[0].publicId).toBe(applications[2].publicId);
      expect(scope.vm.organizations[2].applications[0].name).toBe(applications[2].name);
      expect(scope.vm.organizations[2].applications[0].isVisible).toBe(true);

      expect(scope.vm.rootOrganization.id).toBe('rootOrg');
      expect(scope.vm.rootOrganization.name).toBe('Root org');
    });

    it('checks if an organization or application is selected', function() {
      expect(scope.vm.$state.includes.calls.length).toBe(59);
      expect(scope.vm.$state.includes).toHaveBeenCalledWith('management.view.organization', {
        organizationId: 'rootOrg'
      });
      expect(scope.vm.$state.includes).toHaveBeenCalledWith('management.view.organization', {
        organizationId: organizations[0].id
      });
      expect(scope.vm.$state.includes).toHaveBeenCalledWith('management.view.organization', {
        organizationId: organizations[1].id
      });
      expect(scope.vm.$state.includes).toHaveBeenCalledWith('management.view.organization', {
        organizationId: applications[2].organizationId
      });
      expect(scope.vm.$state.includes).toHaveBeenCalledWith('management.view.application');
    });

    it('filters organizations', function() {
      scope.vm.filter.value = 'ONE';
      scope.$digest();

      expect(scope.vm.organizations).toBeDefined();
      expect(scope.vm.organizations.length).toBe(3);
      expect(scope.vm.organizations[0].id).toBe(organizations[0].id);
      expect(scope.vm.organizations[0].name).toBe(organizations[0].name);
      expect(scope.vm.organizations[0].isVisible).toBe(true);
      expect(scope.vm.organizations[0].isExpanded).toBe(true);

      expect(scope.vm.organizations[0].applications).toBeDefined();
      expect(scope.vm.organizations[0].applications.length).toBe(2);
      expect(scope.vm.organizations[0].applications[0].id).toBe(applications[0].id);
      expect(scope.vm.organizations[0].applications[0].publicId).toBe(applications[0].publicId);
      expect(scope.vm.organizations[0].applications[0].name).toBe(applications[0].name);
      expect(scope.vm.organizations[0].applications[0].isVisible).toBe(true);
      expect(scope.vm.organizations[0].applications[1].id).toBe(applications[1].id);
      expect(scope.vm.organizations[0].applications[1].publicId).toBe(applications[1].publicId);
      expect(scope.vm.organizations[0].applications[1].name).toBe(applications[1].name);
      expect(scope.vm.organizations[0].applications[1].isVisible).toBe(true);

      expect(scope.vm.organizations[1].id).toBe(organizations[1].id);
      expect(scope.vm.organizations[1].name).toBe(organizations[1].name);
      expect(scope.vm.organizations[1].isVisible).toBe(false);
      expect(scope.vm.organizations[1].isExpanded).toBe(false);
      expect(scope.vm.organizations[1].applications).toBeDefined();
      expect(scope.vm.organizations[1].applications.length).toBe(0);

      expect(scope.vm.organizations[2].id).toBe(applications[2].organizationId);
      expect(scope.vm.organizations[2].name).toBe(applications[2].organizationName);
      expect(scope.vm.organizations[2].isVisible).toBe(false);
      expect(scope.vm.organizations[2].isExpanded).toBe(false);
      expect(scope.vm.organizations[2].applications.length).toBe(1);
      expect(scope.vm.organizations[2].applications[0].id).toBe(applications[2].id);
      expect(scope.vm.organizations[2].applications[0].publicId).toBe(applications[2].publicId);
      expect(scope.vm.organizations[2].applications[0].name).toBe(applications[2].name);
      expect(scope.vm.organizations[2].applications[0].isVisible).toBe(false);
    });

    it('filters applications', function() {
      scope.vm.filter.value = 'TEN';
      scope.$digest();

      expect(scope.vm.organizations).toBeDefined();
      expect(scope.vm.organizations.length).toBe(3);
      expect(scope.vm.organizations[0].id).toBe(organizations[0].id);
      expect(scope.vm.organizations[0].name).toBe(organizations[0].name);
      expect(scope.vm.organizations[0].isVisible).toBe(true);
      expect(scope.vm.organizations[0].isExpanded).toBe(true);

      expect(scope.vm.organizations[0].applications).toBeDefined();
      expect(scope.vm.organizations[0].applications.length).toBe(2);
      expect(scope.vm.organizations[0].applications[0].id).toBe(applications[0].id);
      expect(scope.vm.organizations[0].applications[0].publicId).toBe(applications[0].publicId);
      expect(scope.vm.organizations[0].applications[0].name).toBe(applications[0].name);
      expect(scope.vm.organizations[0].applications[0].isVisible).toBe(true);
      expect(scope.vm.organizations[0].applications[1].id).toBe(applications[1].id);
      expect(scope.vm.organizations[0].applications[1].publicId).toBe(applications[1].publicId);
      expect(scope.vm.organizations[0].applications[1].name).toBe(applications[1].name);
      expect(scope.vm.organizations[0].applications[1].isVisible).toBe(false);

      expect(scope.vm.organizations[1].id).toBe(organizations[1].id);
      expect(scope.vm.organizations[1].name).toBe(organizations[1].name);
      expect(scope.vm.organizations[1].applications).toBeDefined();
      expect(scope.vm.organizations[1].applications.length).toBe(0);
      expect(scope.vm.organizations[1].isVisible).toBe(false);
      expect(scope.vm.organizations[1].isExpanded).toBe(false);

      expect(scope.vm.organizations[2].id).toBe(applications[2].organizationId);
      expect(scope.vm.organizations[2].name).toBe(applications[2].organizationName);
      expect(scope.vm.organizations[2].isVisible).toBe(false);
      expect(scope.vm.organizations[2].isExpanded).toBe(false);
      expect(scope.vm.organizations[2].applications.length).toBe(1);
      expect(scope.vm.organizations[2].applications[0].id).toBe(applications[2].id);
      expect(scope.vm.organizations[2].applications[0].publicId).toBe(applications[2].publicId);
      expect(scope.vm.organizations[2].applications[0].name).toBe(applications[2].name);
      expect(scope.vm.organizations[2].applications[0].isVisible).toBe(false);
    });

    it('handles new organization', function() {
      var newOrganizationRaw = StoreMockData.newOrganization();
      var newOrganization = OrganizationStore.create();
      newOrganization.$save();
      $httpBackend.expectPOST(CLMLocations.getOrganizationsUrl()).respond(newOrganizationRaw);
      $httpBackend.flush();

      expect(scope.vm.organizations).toBeDefined();
      expect(scope.vm.organizations.length).toBe(4);

      expect(scope.vm.organizations[3].id).toBe(newOrganization.id);
      expect(scope.vm.organizations[3].name).toBe(newOrganization.name);
      expect(scope.vm.organizations[3].applications).toBeDefined();
      expect(scope.vm.organizations[3].applications.length).toBe(0);
      expect(scope.vm.organizations[3].isVisible).toBe(true);
      expect(scope.vm.organizations[3].isExpanded).toBe(false);
    });

    it('handles removed organization', function() {
      OrganizationStore.get().then(function(organizations) {
        organizations[1].$delete();
        $httpBackend.expectDELETE(CLMLocations.getOrganizationsUrl() + '/' + organizations[1].id).respond({});
      });
      scope.$digest();
      $httpBackend.flush();

      expect(scope.vm.organizations).toBeDefined();
      expect(scope.vm.organizations.length).toBe(2);

      expect(scope.vm.organizations[0].id).toBe(organizations[0].id);
      expect(scope.vm.organizations[1].id).toBe(applications[2].organizationId);
    });

    it('handles changes to organization', function() {
      OrganizationStore.get().then(function(organizations) {
        organizations[0].name = 'foo';
      });
      scope.$digest();

      expect(scope.vm.organizations[0].name).toBe('foo');
    });

    it('handles new application', function() {
      var newApplicationRaw = StoreMockData.newApplication();
      var newApplication = ApplicationStore.create();
      newApplication.$save();
      $httpBackend.expectPOST(CLMLocations.getApplicationsUrl()).respond(newApplicationRaw);
      $httpBackend.flush();

      expect(scope.vm.organizations).toBeDefined();
      expect(scope.vm.organizations.length).toBe(3);

      expect(scope.vm.organizations[1].applications).toBeDefined();
      expect(scope.vm.organizations[1].applications.length).toBe(1);

      expect(scope.vm.organizations[1].applications[0].id).toBe(newApplicationRaw.id);
      expect(scope.vm.organizations[1].applications[0].publicId).toBe(newApplicationRaw.publicId);
      expect(scope.vm.organizations[1].applications[0].name).toBe(newApplicationRaw.name);
      expect(scope.vm.organizations[1].applications[0].isVisible).toBe(true);
    });

    it('handles removed application', function() {
      ApplicationStore.get().then(function(applications) {
        applications[0].$delete();
        $httpBackend.expectDELETE(CLMLocations.getApplicationsUrl() + '/' + applications[0].publicId).respond({});
      });
      scope.$digest();
      $httpBackend.flush();

      expect(scope.vm.organizations).toBeDefined();
      expect(scope.vm.organizations.length).toBe(3);

      expect(scope.vm.organizations[0].applications).toBeDefined();
      expect(scope.vm.organizations[0].applications.length).toBe(1);
      expect(scope.vm.organizations[0].applications[0].id).toBe(applications[1].id);
    });

    it('handles changes to application', function() {
      ApplicationStore.get().then(function(applications) {
        applications[0].name = 'foo';
      });
      scope.$digest();

      expect(scope.vm.organizations[0].applications[0].name).toBe('foo');
    });
  });
});
