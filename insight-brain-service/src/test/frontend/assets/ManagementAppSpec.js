describe('ManagementModule', function() {
  var scope;

  beforeEach(module(function($provide) {
    // $state stub for spying
    $provide.service('$state', function() {
      return {
        includes: function(state, params) {}
      }
    })
  }));
  beforeEach(module('ManagementModule'));

  afterEach(inject(function($httpBackend) {
    $httpBackend.verifyNoOutstandingExpectation();
    $httpBackend.verifyNoOutstandingRequest();

    if (scope && scope.$destroy) {
      scope.$destroy();
    }
  }));

  describe('OwnerTreeViewController', function() {
    var $controller, $httpBackend, $state, CLMLocations,
        organizations = StoreMockData.getOrganizations(),
        applications = StoreMockData.getApplications();

    beforeEach(inject(function(_$controller_, _$rootScope_, _$httpBackend_, _$state_, _CLMLocations_) {
      $controller = _$controller_;
      $httpBackend = _$httpBackend_;
      $state = _$state_;
      CLMLocations = _CLMLocations_;

      scope = _$rootScope_.$new();
      $controller('OwnerTreeViewController', { $scope: scope });

      spyOn($state, 'includes').andReturn(false);

      $httpBackend.expectGET(CLMLocations.getOrganizationsUrl()).respond(angular.copy(organizations));
      $httpBackend.expectGET(CLMLocations.getApplicationsUrl()).respond(angular.copy(applications));
      scope.$digest();
      $httpBackend.flush();
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
      expect(scope.organizations[0].applications[0].id).toBe(applications[1].id);
      expect(scope.organizations[0].applications[0].publicId).toBe(applications[1].publicId);
      expect(scope.organizations[0].applications[0].name).toBe(applications[1].name);
      expect(scope.organizations[0].applications[0].isVisible).toBe(true);
      expect(scope.organizations[0].applications[1].id).toBe(applications[0].id);
      expect(scope.organizations[0].applications[1].publicId).toBe(applications[0].publicId);
      expect(scope.organizations[0].applications[1].name).toBe(applications[0].name);
      expect(scope.organizations[0].applications[1].isVisible).toBe(true);

      expect(scope.organizations[1].id).toBe(organizations[1].id);
      expect(scope.organizations[1].name).toBe(organizations[1].name);
      expect(scope.organizations[1].applications).toBeDefined();
      expect(scope.organizations[1].applications.length).toBe(0);
      expect(scope.organizations[1].isVisible).toBe(true);
      expect(scope.organizations[1].isExpanded).toBe(false);
    });

    it('checks if an organization or application is selected', function() {
      expect($state.includes.calls.length).toBe(4);
      expect($state.includes).toHaveBeenCalledWith('management.organization-view', {
        organizationId: organizations[0].id
      });
      expect($state.includes).toHaveBeenCalledWith('management.organization-view', {
        organizationId: organizations[1].id
      });
      expect($state.includes).toHaveBeenCalledWith('management.application-view');
    });

    it('filters organizations', function() {
      scope.filter = 'ONE';
      scope.$digest();

      expect(scope.organizations).toBeDefined();
      expect(scope.organizations.length).toBe(2);
      expect(scope.organizations[0].id).toBe(organizations[0].id);
      expect(scope.organizations[0].name).toBe(organizations[0].name);
      expect(scope.organizations[0].isVisible).toBe(true);
      expect(scope.organizations[0].isExpanded).toBe(true);

      expect(scope.organizations[0].applications).toBeDefined();
      expect(scope.organizations[0].applications.length).toBe(2);
      expect(scope.organizations[0].applications[0].id).toBe(applications[1].id);
      expect(scope.organizations[0].applications[0].publicId).toBe(applications[1].publicId);
      expect(scope.organizations[0].applications[0].name).toBe(applications[1].name);
      expect(scope.organizations[0].applications[0].isVisible).toBe(true);
      expect(scope.organizations[0].applications[1].id).toBe(applications[0].id);
      expect(scope.organizations[0].applications[1].publicId).toBe(applications[0].publicId);
      expect(scope.organizations[0].applications[1].name).toBe(applications[0].name);
      expect(scope.organizations[0].applications[1].isVisible).toBe(true);

      expect(scope.organizations[1].id).toBe(organizations[1].id);
      expect(scope.organizations[1].name).toBe(organizations[1].name);
      expect(scope.organizations[1].applications).toBeDefined();
      expect(scope.organizations[1].applications.length).toBe(0);
      expect(scope.organizations[1].isVisible).toBe(false);
      expect(scope.organizations[1].isExpanded).toBe(false);
    });

    it('filters applications', function() {
      scope.filter = 'TEN';
      scope.$digest();

      expect(scope.organizations).toBeDefined();
      expect(scope.organizations.length).toBe(2);
      expect(scope.organizations[0].id).toBe(organizations[0].id);
      expect(scope.organizations[0].name).toBe(organizations[0].name);
      expect(scope.organizations[0].isVisible).toBe(true);
      expect(scope.organizations[0].isExpanded).toBe(true);

      expect(scope.organizations[0].applications).toBeDefined();
      expect(scope.organizations[0].applications.length).toBe(2);
      expect(scope.organizations[0].applications[0].id).toBe(applications[1].id);
      expect(scope.organizations[0].applications[0].publicId).toBe(applications[1].publicId);
      expect(scope.organizations[0].applications[0].name).toBe(applications[1].name);
      expect(scope.organizations[0].applications[0].isVisible).toBe(false);
      expect(scope.organizations[0].applications[1].id).toBe(applications[0].id);
      expect(scope.organizations[0].applications[1].publicId).toBe(applications[0].publicId);
      expect(scope.organizations[0].applications[1].name).toBe(applications[0].name);
      expect(scope.organizations[0].applications[1].isVisible).toBe(true);

      expect(scope.organizations[1].id).toBe(organizations[1].id);
      expect(scope.organizations[1].name).toBe(organizations[1].name);
      expect(scope.organizations[1].applications).toBeDefined();
      expect(scope.organizations[1].applications.length).toBe(0);
      expect(scope.organizations[1].isVisible).toBe(false);
      expect(scope.organizations[1].isExpanded).toBe(false);
    });
  });
});
