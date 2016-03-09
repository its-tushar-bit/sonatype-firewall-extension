describe('owner.tree-view.directive.spec.js', function() {
  var scope;

  beforeEach(module(function($provide) {
    // $state stub for spying
    $provide.service('$state', function() {
      return {
        includes: function(state, params) {
        }
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

  function runTestsForOwnerTreeViewDirective(permissions) {
    describe('ownerTreeViewDirective', function() {
      var $httpBackend, $state, CLMLocations, CLMAppLocations,
          ownerList = SidebarResourceMockData.getOwnerListUrl();

      beforeEach(inject(function(_$rootScope_, _$httpBackend_, _$state_, _$timeout_, _$compile_, _CLMLocations_,
                                 _CLMAppLocations_)
      {
        $httpBackend = _$httpBackend_;
        $state = _$state_;
        CLMLocations = _CLMLocations_;
        CLMAppLocations = _CLMAppLocations_;

        scope = _$rootScope_.$new();
        var ownerTreeView = angular.element('<div owner-tree-view></div>');
        _$compile_(ownerTreeView)(scope);
        SpecUtil.respondWithTemplate($httpBackend,
            'owner.manager/navigation/owner.tree.view.directive.html?' + clmBuildTimestamp);
        scope.$digest();

        spyOn($state, 'includes').andReturn(false);

        $httpBackend.expectGET(CLMLocations.getOwnerListUrl()).respond(ownerList);
        $httpBackend.expectPUT(CLMAppLocations.getPermissionContextTestUrl('repository_container')).respond(permissions);
        scope.$digest();
        $httpBackend.flush();
        _$timeout_.flush();
      }));

      it('loads organizations and applications', function() {
        expect(scope.vm.showRepositories).toBe(permissions.length > 0);

        expect(scope.vm.organizations).toBeDefined();
        // Root organization is removed from vm.organizations
        expect(scope.vm.organizations.length).toBe(ownerList.organizations.length - 1);

        expect(scope.vm.organizations[0].id).toBe(ownerList.organizations[0].id);
        expect(scope.vm.organizations[0].name).toBe(ownerList.organizations[0].name);
        expect(scope.vm.organizations[0].isVisible).toBe(true);
        expect(scope.vm.organizations[0].isExpanded).toBe(false);
        expect(scope.vm.organizations[0].applications).toBeDefined();
        expect(scope.vm.organizations[0].applications.length).toBe(2);
        expect(scope.vm.organizations[0].applications[0].id).toBe(ownerList.organizations[0].applications[0].id);
        expect(scope.vm.organizations[0].applications[0].publicId).toBe(
            ownerList.organizations[0].applications[0].publicId);
        expect(scope.vm.organizations[0].applications[0].name).toBe(ownerList.organizations[0].applications[0].name);
        expect(scope.vm.organizations[0].applications[0].isVisible).toBe(true);
        expect(scope.vm.organizations[0].applications[1].id).toBe(ownerList.organizations[0].applications[1].id);
        expect(scope.vm.organizations[0].applications[1].publicId).toBe(
            ownerList.organizations[0].applications[1].publicId);
        expect(scope.vm.organizations[0].applications[1].name).toBe(ownerList.organizations[0].applications[1].name);
        expect(scope.vm.organizations[0].applications[1].isVisible).toBe(true);

        expect(scope.vm.organizations[1].id).toBe(ownerList.organizations[1].id);
        expect(scope.vm.organizations[1].name).toBe(ownerList.organizations[1].name);
        expect(scope.vm.organizations[1].isVisible).toBe(true);
        expect(scope.vm.organizations[1].isExpanded).toBe(false);
        expect(scope.vm.organizations[1].synthetic).toBe(true);
        expect(scope.vm.organizations[1].applications).toBeDefined();
        expect(scope.vm.organizations[1].applications.length).toBe(1);
        expect(scope.vm.organizations[1].applications[0].id).toBe(ownerList.organizations[1].applications[0].id);
        expect(scope.vm.organizations[1].applications[0].publicId).toBe(
            ownerList.organizations[1].applications[0].publicId);
        expect(scope.vm.organizations[1].applications[0].name).toBe(ownerList.organizations[1].applications[0].name);
        expect(scope.vm.organizations[1].applications[0].isVisible).toBe(true);

        //Mock data returns root organization as the third item
        expect(scope.vm.rootOrganization.id).toBe(ownerList.organizations[2].id);
        expect(scope.vm.rootOrganization.name).toBe(ownerList.organizations[2].name);
      });

      it('checks if an organization or application is selected', function() {
        // vaguely perf tracking, minor changes w/ Angular versions aren't an issue but large changes could indicate a
        // potential perf issue with a large number of apps+orgs
        expect(scope.vm.$state.includes.calls.length).toBe(permissions.length > 0 ? 30 : 29);
        expect(scope.vm.$state.includes).toHaveBeenCalledWith('management.view.organization', {
          organizationId: ownerList.organizations[2].id
        });
        expect(scope.vm.$state.includes).toHaveBeenCalledWith('management.view.organization', {
          organizationId: ownerList.organizations[0].id
        });
        expect(scope.vm.$state.includes).toHaveBeenCalledWith('management.view.organization', {
          organizationId: ownerList.organizations[1].id
        });
        expect(scope.vm.$state.includes).toHaveBeenCalledWith('management.view.organization', {
          organizationId: ownerList.organizations[0].applications[0].organizationId
        });
        expect(scope.vm.$state.includes).toHaveBeenCalledWith('management.view.application');
      });

      it('filters organizations', function() {
        scope.vm.filter.value = 'ONE';
        scope.$digest();

        expect(scope.vm.organizations).toBeDefined();
        expect(scope.vm.organizations.length).toBe(2);
        expect(scope.vm.organizations[0].id).toBe(ownerList.organizations[0].id);
        expect(scope.vm.organizations[0].name).toBe(ownerList.organizations[0].name);
        expect(scope.vm.organizations[0].isVisible).toBe(true);
        expect(scope.vm.organizations[0].isExpanded).toBe(true);

        expect(scope.vm.organizations[0].applications).toBeDefined();
        expect(scope.vm.organizations[0].applications.length).toBe(2);
        expect(scope.vm.organizations[0].applications[0].id).toBe(ownerList.organizations[0].applications[0].id);
        expect(scope.vm.organizations[0].applications[0].publicId).toBe(
            ownerList.organizations[0].applications[0].publicId);
        expect(scope.vm.organizations[0].applications[0].name).toBe(ownerList.organizations[0].applications[0].name);
        expect(scope.vm.organizations[0].applications[0].isVisible).toBe(true);
        expect(scope.vm.organizations[0].applications[1].id).toBe(ownerList.organizations[0].applications[1].id);
        expect(scope.vm.organizations[0].applications[1].publicId).toBe(
            ownerList.organizations[0].applications[1].publicId);
        expect(scope.vm.organizations[0].applications[1].name).toBe(ownerList.organizations[0].applications[1].name);
        expect(scope.vm.organizations[0].applications[1].isVisible).toBe(true);

        expect(scope.vm.organizations[1].id).toBe(ownerList.organizations[1].id);
        expect(scope.vm.organizations[1].name).toBe(ownerList.organizations[1].name);
        expect(scope.vm.organizations[1].isVisible).toBe(false);
        expect(scope.vm.organizations[1].isExpanded).toBe(false);
        expect(scope.vm.organizations[1].applications.length).toBe(1);
        expect(scope.vm.organizations[1].applications[0].id).toBe(ownerList.organizations[1].applications[0].id);
        expect(scope.vm.organizations[1].applications[0].publicId).toBe(
            ownerList.organizations[1].applications[0].publicId);
        expect(scope.vm.organizations[1].applications[0].name).toBe(ownerList.organizations[1].applications[0].name);
        expect(scope.vm.organizations[1].applications[0].isVisible).toBe(false);
      });

      it('filters applications', function() {
        scope.vm.filter.value = 'THREE';
        scope.$digest();

        expect(scope.vm.organizations).toBeDefined();
        expect(scope.vm.organizations.length).toBe(2);
        expect(scope.vm.organizations[0].id).toBe(ownerList.organizations[0].id);
        expect(scope.vm.organizations[0].name).toBe(ownerList.organizations[0].name);
        expect(scope.vm.organizations[0].isVisible).toBe(false);
        expect(scope.vm.organizations[0].isExpanded).toBe(false);

        expect(scope.vm.organizations[0].applications).toBeDefined();
        expect(scope.vm.organizations[0].applications.length).toBe(2);
        expect(scope.vm.organizations[0].applications[0].id).toBe(ownerList.organizations[0].applications[0].id);
        expect(scope.vm.organizations[0].applications[0].publicId).toBe(
            ownerList.organizations[0].applications[0].publicId);
        expect(scope.vm.organizations[0].applications[0].name).toBe(ownerList.organizations[0].applications[0].name);
        expect(scope.vm.organizations[0].applications[0].isVisible).toBe(false);
        expect(scope.vm.organizations[0].applications[1].id).toBe(ownerList.organizations[0].applications[1].id);
        expect(scope.vm.organizations[0].applications[1].publicId).toBe(
            ownerList.organizations[0].applications[1].publicId);
        expect(scope.vm.organizations[0].applications[1].name).toBe(ownerList.organizations[0].applications[1].name);
        expect(scope.vm.organizations[0].applications[1].isVisible).toBe(false);

        expect(scope.vm.organizations[1].id).toBe(ownerList.organizations[1].id);
        expect(scope.vm.organizations[1].name).toBe(ownerList.organizations[1].name);
        expect(scope.vm.organizations[1].isVisible).toBe(true);
        expect(scope.vm.organizations[1].isExpanded).toBe(true);
        expect(scope.vm.organizations[1].applications.length).toBe(1);
        expect(scope.vm.organizations[1].applications[0].id).toBe(ownerList.organizations[1].applications[0].id);
        expect(scope.vm.organizations[1].applications[0].publicId).toBe(
            ownerList.organizations[1].applications[0].publicId);
        expect(scope.vm.organizations[1].applications[0].name).toBe(ownerList.organizations[1].applications[0].name);
        expect(scope.vm.organizations[1].applications[0].isVisible).toBe(true);
      });

      it('handles new organization', function() {
        var newOrganization = {
          id: 'newOrganizationID',
          name: 'New Organization Name'
        };
        scope.$broadcast('owner.updated', newOrganization, 'organization', true);
        scope.$digest();

        expect(scope.vm.organizations).toBeDefined();
        expect(scope.vm.organizations.length).toBe(3);

        expect(scope.vm.organizations[2].id).toBe(newOrganization.id);
        expect(scope.vm.organizations[2].name).toBe(newOrganization.name);
        expect(scope.vm.organizations[2].applications).toBeDefined();
        expect(scope.vm.organizations[2].applications.length).toBe(0);
        expect(scope.vm.organizations[2].isVisible).toBe(true);
        expect(scope.vm.organizations[2].isExpanded).toBe(true);
      });

      it('handles removed organization', function() {
        scope.$broadcast('owner.deleted', ownerList.organizations[1], 'organization');
        scope.$digest();

        expect(scope.vm.organizations).toBeDefined();
        expect(scope.vm.organizations.length).toBe(1);

        expect(scope.vm.organizations[0].id).toBe(ownerList.organizations[0].id);
      });

      it('handles changes to organization', function() {
        scope.$broadcast('owner.updated', {
          id: ownerList.organizations[0].id,
          name: 'foo'
        }, 'organization', false);
        scope.$digest();

        expect(scope.vm.organizations[0].name).toBe('foo');
      });

      it('handles new application', function() {
        var newApplication = {
          id: 'newApplicationID',
          organizationId: ownerList.organizations[1].id,
          publicId: 'newApplicationPublicId',
          name: 'New Application'
        };
        scope.$broadcast('owner.updated', newApplication, 'application', true);
        scope.$digest();

        expect(scope.vm.organizations).toBeDefined();
        expect(scope.vm.organizations.length).toBe(2);

        expect(scope.vm.organizations[1].applications).toBeDefined();
        expect(scope.vm.organizations[1].applications.length).toBe(2);

        expect(scope.vm.organizations[1].applications[1].id).toBe(newApplication.id);
        expect(scope.vm.organizations[1].applications[1].publicId).toBe(newApplication.publicId);
        expect(scope.vm.organizations[1].applications[1].name).toBe(newApplication.name);
        expect(scope.vm.organizations[1].applications[1].isVisible).toBe(true);
      });

      it('handles removed application', function() {
        scope.$broadcast('owner.deleted', ownerList.organizations[0].applications[1], 'application');
        scope.$digest();

        expect(scope.vm.organizations).toBeDefined();
        expect(scope.vm.organizations.length).toBe(2);

        expect(scope.vm.organizations[0].applications).toBeDefined();
        expect(scope.vm.organizations[0].applications.length).toBe(1);
        expect(scope.vm.organizations[0].applications[0].id).toBe(ownerList.organizations[0].applications[0].id);
      });

      it('handles changes to application', function() {
        scope.$broadcast('owner.updated', {
          id: ownerList.organizations[0].applications[0].id,
          organizationId: ownerList.organizations[0].id,
          name: 'foo'
        }, 'application', false);
        scope.$digest();

        expect(scope.vm.organizations[0].applications[0].name).toBe('foo');
      });
    });
  }

  describe('ownerTreeViewDirective with repositories', function() {
    runTestsForOwnerTreeViewDirective(['READ']);
  });
  describe('ownerTreeViewDirective without repositories', function() {
    runTestsForOwnerTreeViewDirective([]);
  });
});
