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

  describe('organizationTreeViewController', function() {
    var $controller, $httpBackend, organizationTreeViewFactory, CLMLocations,
        organizations = StoreMockData.getOrganizations(),
        applications = StoreMockData.getApplications();

    beforeEach(inject(function(_$controller_, _$rootScope_, _$httpBackend_, _organizationTreeViewFactory_, _CLMLocations_) {
      $controller = _$controller_;
      $httpBackend = _$httpBackend_;
      organizationTreeViewFactory = _organizationTreeViewFactory_;
      CLMLocations = _CLMLocations_;

      scope = _$rootScope_.$new();
      $controller('organizationTreeViewController', { $scope: scope });

      spyOn(organizationTreeViewFactory, 'loader').andCallThrough();

      $httpBackend.expectGET(CLMLocations.getOrganizationsUrl()).respond(angular.copy(organizations));
      $httpBackend.expectGET(CLMLocations.getApplicationsUrl()).respond(angular.copy(applications));
      scope.$digest();
      $httpBackend.flush();
    }));

    it('loads organizations and applications', function() {
      expect(scope.organizations).toBeDefined();
      expect(organizationTreeViewFactory.create).toHaveBeenCalled();
    });

    it('filters organization and applications', function() {
      scope.filter = '';
      spyOn(organizationTreeViewFactory, 'filter').andCallThrough();
      scope.filter = 'foo';
      scope.$digest();

      expect(organizationTreeViewFactory.filter).toHaveBeenCalled();
      expect(organizationTreeViewFactory.filter).toHaveBeenCalledWith(scope.organizations, 'foo');
    })
  });

  describe('organizationTreeViewFactory', function() {
    var organizationTreeViewFactory, $state,
        organizations = StoreMockData.getOrganizations(),
        applications = StoreMockData.getApplications();

    beforeEach(inject(function(_$state_, _organizationTreeViewFactory_) {
      $state = _$state_;
      organizationTreeViewFactory = _organizationTreeViewFactory_;
    }));

    it('selects organizations', function() {
      scope = {};
      scope.organizations = organizationTreeViewFactory.build(angular.copy(organizations), angular.copy(applications));

      spyOn($state, 'includes').andReturn(true);
      var isSelected = organizationTreeViewFactory.isOrganizationOrChildSelected(scope.organizations[0]);
      expect(isSelected).toBe(true);
      expect($state.includes.calls.length).toBe(1);
      expect($state.includes).toHaveBeenCalledWith('management.organization.view', {
        organizationId: organizations[0].id
      });
      $state.includes.reset();

      $state.includes.andReturn(false);
      isSelected = organizationTreeViewFactory.isOrganizationOrChildSelected(scope.organizations[0]);
      expect(isSelected).toBe(false);
      expect($state.includes.calls.length).toBe(3);
      expect($state.includes).toHaveBeenCalledWith('management.organization.view', {
        organizationId: organizations[0].id
      });
      expect($state.includes).toHaveBeenCalledWith('management.application.view', {
        applicationPublicId: applications[0].publicId
      });
      expect($state.includes).toHaveBeenCalledWith('management.application.view', {
        applicationPublicId: applications[1].publicId
      });
    });

    it('loads applications', function() {
      scope = {};

      spyOn(organizationTreeViewFactory, 'isOrganizationOrChildSelected').andReturn(true);

      scope.organizations = organizationTreeViewFactory.build(angular.copy(organizations), angular.copy(applications));

      expect(scope.organizations).toBeDefined();
      expect(scope.organizations.length).toBe(organizations.length);
      expect(scope.organizations[0].id).toBe(organizations[0].id);
      expect(scope.organizations[0].name).toBe(organizations[0].name);
      expect(scope.organizations[0].isVisible).toBe(true);
      expect(scope.organizations[0].isExpanded).toBe(true);
      expect(organizationTreeViewFactory.isOrganizationOrChildSelected.calls.length).toBe(2);
      expect(organizationTreeViewFactory.isOrganizationOrChildSelected).toHaveBeenCalledWith(scope.organizations[0]);

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
      expect(scope.organizations[1].isExpanded).toBe(true);
      expect(organizationTreeViewFactory.isOrganizationOrChildSelected).toHaveBeenCalledWith(scope.organizations[1]);
    });

    it('filters organizations', function() {
      scope = {};
      scope.organizations = organizationTreeViewFactory.build(angular.copy(organizations), angular.copy(applications));

      organizationTreeViewFactory.filter(scope.organizations, 'ONE');
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
      scope = {};
      scope.organizations = organizationTreeViewFactory.build(angular.copy(organizations), angular.copy(applications));

      organizationTreeViewFactory.filter(scope.organizations, 'TEN');
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
