/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import ownerManagerModule from '../../main/frontend/owner.manager/owner.manager.module';
import legacyConfigurationModule from '../../main/frontend/LegacyConfigurationModule';

describe('owner.tree.view.directive.spec.js', function() {
  var scope, $httpBackend, $state, $timeout, CLMLocations, CLMContextLocations, EventNameConstant;

  beforeEach(angular.mock.module(function($provide) {
    // $state stub for spying
    $provide.service('$state', function() {
      return {
        includes: function() {
        }
      };
    });
  }));
  beforeEach(angular.mock.module(ownerManagerModule.name, legacyConfigurationModule.name, function($provide) {
    SpecUtil.mockNgRedux($provide);
    $provide.factory('scmOnboardingActions', function() {
      return {
        loadConfig: jasmine.createSpy('loadConfig')
      };
    });
  }));

  afterEach(inject(function(_$httpBackend_) {
    _$httpBackend_.verifyNoOutstandingExpectation();
    _$httpBackend_.verifyNoOutstandingRequest();

    if (scope && scope.$destroy) {
      scope.$destroy();
    }
  }));

  function runTestsForOwnerTreeViewDirective(permissions) {
    describe('ownerTreeViewDirective', function() {
      var ownerList = SidebarResourceMockData.getOwnerListUrl();

      beforeEach(inject(function(_$rootScope_, _$httpBackend_, _$state_, _$timeout_, _$compile_, _CLMLocations_,
                                 _CLMContextLocations_, $injector) {
        $timeout = _$timeout_;
        $httpBackend = _$httpBackend_;
        $state = _$state_;
        CLMLocations = _CLMLocations_;
        CLMContextLocations = _CLMContextLocations_;
        EventNameConstant = $injector.get('event.name.constant');

        $httpBackend.expectGET(CLMLocations.getOwnerListUrl()).respond(ownerList);
        $httpBackend.expectPUT(CLMContextLocations.getPermissionContextTestUrl('repository_container'))
            .respond(permissions);

        scope = _$rootScope_.$new();
        var ownerTreeView = angular.element('<div owner-tree-view></div>');
        _$compile_(ownerTreeView)(scope);
        scope.$digest();

        spyOn($state, 'includes').and.returnValue(false);

        scope.$digest();
        $httpBackend.flush();
        $timeout.flush();
      }));

      it('loads feature flag', () => {
        expect(scope.vm.loadConfig).toHaveBeenCalledTimes(1);
      });

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
        expect(scope.vm.organizations[0].applications.length).toBe(7);
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
        expect(scope.vm.$state.includes.calls.count()).toBe(permissions.length > 0 ? 30 : 29);
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
        scope.vm.filter.value = 'ORGANIZATION ONE';
        scope.$digest();

        expect(scope.vm.organizations).toBeDefined();
        expect(scope.vm.organizations.length).toBe(2);
        expect(scope.vm.organizations[0].id).toBe(ownerList.organizations[0].id);
        expect(scope.vm.organizations[0].name).toBe(ownerList.organizations[0].name);
        expect(scope.vm.organizations[0].isVisible).toBe(true);
        expect(scope.vm.organizations[0].isExpanded).toBe(true);

        expect(scope.vm.organizations[0].applications).toBeDefined();
        expect(scope.vm.organizations[0].applications.length).toBe(7);
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

        scope.vm.filter.value = 'ORGANIZOTION ONE';
        scope.$digest();

        expect(scope.vm.organizations).toBeDefined();
        expect(scope.vm.organizations.length).toBe(2);
        expect(scope.vm.organizations[0].id).toBe(ownerList.organizations[0].id);
        expect(scope.vm.organizations[0].name).toBe(ownerList.organizations[0].name);
        expect(scope.vm.organizations[0].isVisible).toBe(true);
        expect(scope.vm.organizations[0].isExpanded).toBe(true);

        expect(scope.vm.organizations[0].applications).toBeDefined();
        expect(scope.vm.organizations[0].applications.length).toBe(7);
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
        scope.vm.filter.value = 'APPLICATION THREE';
        scope.$digest();

        expect(scope.vm.organizations).toBeDefined();
        expect(scope.vm.organizations.length).toBe(2);
        expect(scope.vm.organizations[0].id).toBe(ownerList.organizations[0].id);
        expect(scope.vm.organizations[0].name).toBe(ownerList.organizations[0].name);
        expect(scope.vm.organizations[0].isVisible).toBe(false);
        expect(scope.vm.organizations[0].isExpanded).toBe(false);

        expect(scope.vm.organizations[0].applications).toBeDefined();
        expect(scope.vm.organizations[0].applications.length).toBe(7);
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

        scope.vm.filter.value = 'analytics-gateway-2.2.0';
        scope.$digest();

        expect(scope.vm.organizations).toBeDefined();
        expect(scope.vm.organizations.length).toBe(2);
        expect(scope.vm.organizations[0].id).toBe(ownerList.organizations[0].id);
        expect(scope.vm.organizations[0].name).toBe(ownerList.organizations[0].name);
        expect(scope.vm.organizations[0].isVisible).toBe(true);
        expect(scope.vm.organizations[0].isExpanded).toBe(true);

        expect(scope.vm.organizations[0].applications).toBeDefined();
        expect(scope.vm.organizations[0].applications.length).toBe(7);
        expect(scope.vm.organizations[0].applications[0].isVisible).toBe(false);
        expect(scope.vm.organizations[0].applications[1].isVisible).toBe(false);
        expect(scope.vm.organizations[0].applications[2].isVisible).toBe(false);
        expect(scope.vm.organizations[0].applications[3].isVisible).toBe(true);
        expect(scope.vm.organizations[0].applications[4].isVisible).toBe(false);
        expect(scope.vm.organizations[0].applications[5].isVisible).toBe(true);
        expect(scope.vm.organizations[0].applications[6].isVisible).toBe(false);

        expect(scope.vm.organizations[1].id).toBe(ownerList.organizations[1].id);
        expect(scope.vm.organizations[1].name).toBe(ownerList.organizations[1].name);
        expect(scope.vm.organizations[1].isVisible).toBe(false);
        expect(scope.vm.organizations[1].isExpanded).toBe(false);

        scope.vm.filter.value = 'zamarchiva';
        scope.$digest();

        expect(scope.vm.organizations).toBeDefined();
        expect(scope.vm.organizations.length).toBe(2);
        expect(scope.vm.organizations[0].id).toBe(ownerList.organizations[0].id);
        expect(scope.vm.organizations[0].name).toBe(ownerList.organizations[0].name);
        expect(scope.vm.organizations[0].isVisible).toBe(true);
        expect(scope.vm.organizations[0].isExpanded).toBe(true);

        expect(scope.vm.organizations[0].applications).toBeDefined();
        expect(scope.vm.organizations[0].applications.length).toBe(7);
        expect(scope.vm.organizations[0].applications[0].isVisible).toBe(false);
        expect(scope.vm.organizations[0].applications[1].isVisible).toBe(false);
        expect(scope.vm.organizations[0].applications[2].isVisible).toBe(false);
        expect(scope.vm.organizations[0].applications[3].isVisible).toBe(false);
        expect(scope.vm.organizations[0].applications[4].isVisible).toBe(false);
        expect(scope.vm.organizations[0].applications[5].isVisible).toBe(false);
        expect(scope.vm.organizations[0].applications[6].isVisible).toBe(true);

        expect(scope.vm.organizations[1].id).toBe(ownerList.organizations[1].id);
        expect(scope.vm.organizations[1].name).toBe(ownerList.organizations[1].name);
        expect(scope.vm.organizations[1].isVisible).toBe(false);
        expect(scope.vm.organizations[1].isExpanded).toBe(false);

        scope.vm.filter.value = 'adma';
        scope.$digest();

        expect(scope.vm.organizations).toBeDefined();
        expect(scope.vm.organizations.length).toBe(2);
        expect(scope.vm.organizations[0].id).toBe(ownerList.organizations[0].id);
        expect(scope.vm.organizations[0].name).toBe(ownerList.organizations[0].name);
        expect(scope.vm.organizations[0].isVisible).toBe(false);
        expect(scope.vm.organizations[0].isExpanded).toBe(false);

        expect(scope.vm.organizations[1].id).toBe(ownerList.organizations[1].id);
        expect(scope.vm.organizations[1].name).toBe(ownerList.organizations[1].name);
        expect(scope.vm.organizations[1].isVisible).toBe(false);
        expect(scope.vm.organizations[1].isExpanded).toBe(false);
      });

      it('handles new organization', function() {
        var newOrganization = {
          id: 'newOrganizationID',
          name: 'New Organization Name'
        };
        scope.$broadcast(EventNameConstant.OWNER_UPDATED, newOrganization, 'organization', true);
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
        scope.$broadcast(EventNameConstant.OWNER_UPDATED, {
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
        scope.$broadcast(EventNameConstant.OWNER_UPDATED, newApplication, 'application', true);
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
        expect(scope.vm.organizations[0].applications.length).toBe(6);
        expect(scope.vm.organizations[0].applications[0].id).toBe(ownerList.organizations[0].applications[0].id);
      });

      it('handles changes to application', function() {
        scope.$broadcast(EventNameConstant.OWNER_UPDATED, {
          id: ownerList.organizations[0].applications[0].id,
          organizationId: ownerList.organizations[0].id,
          name: 'foo'
        }, 'application', false);
        scope.$digest();

        expect(scope.vm.organizations[0].applications[0].name).toBe('foo');
      });

      it('Reloads on broadcasted owner summary reload event', inject(function($rootScope, $injector) {
        var EventNameConstant = $injector.get('event.name.constant');

        $rootScope.$broadcast(EventNameConstant.RELOAD_OWNER_TREE_DATA);

        $httpBackend.expectGET(CLMLocations.getOwnerListUrl()).respond(ownerList);
        $httpBackend.expectPUT(CLMContextLocations.getPermissionContextTestUrl('repository_container'))
            .respond(permissions);
        scope.$digest();
        expect($httpBackend.flush).not.toThrow();
        $timeout.flush();
      }));

      describe('handleOrganizationTwistyClick', function() {
        it('expands an unexpanded organization', function() {
          var evt = jasmine.createSpyObj('event', ['preventDefault', 'stopPropagation']),
              organization = { id: 'asdf', isExpanded: false };

          scope.vm.handleOrganizationTwistyClick(evt, organization);

          expect(organization.isExpanded).toBe(true);
          expect(evt.stopPropagation).toHaveBeenCalled();
          expect(evt.preventDefault).toHaveBeenCalled();
          expect($state.includes).toHaveBeenCalledWith('management.view.organization', { organizationId: 'asdf' });
        });

        it('unexpands an expanded organization that isn\'t the active one', function() {
          var evt = jasmine.createSpyObj('event', ['preventDefault', 'stopPropagation']),
              organization = { id: 'asdf', isExpanded: true };

          scope.vm.handleOrganizationTwistyClick(evt, organization);

          expect(organization.isExpanded).toBe(false);
          expect(evt.stopPropagation).toHaveBeenCalled();
          expect(evt.preventDefault).toHaveBeenCalled();
          expect($state.includes).toHaveBeenCalledWith('management.view.organization', { organizationId: 'asdf' });
        });

        it('does not unexpand the currently selected organization', function() {
          var evt = jasmine.createSpyObj('event', ['preventDefault', 'stopPropagation']),
              organization = { id: 'asdf', isExpanded: true };

          // $state.includes is already a spy, so we can't use spyOn.  We can however adjust the spy behavior
          $state.includes.and.returnValue(true);

          scope.vm.handleOrganizationTwistyClick(evt, organization);

          expect(organization.isExpanded).toBe(true);
          expect(evt.stopPropagation).toHaveBeenCalled();
          expect(evt.preventDefault).toHaveBeenCalled();
          expect($state.includes).toHaveBeenCalledWith('management.view.organization', { organizationId: 'asdf' });
        });

        it('does not unexpand the parent org of the currently selected application', function() {
          var evt = jasmine.createSpyObj('event', ['preventDefault', 'stopPropagation']),
              organization = { id: 'asdf', isExpanded: true };

          scope.vm.selectedParentOrganization = { id: 'asdf' };

          scope.vm.handleOrganizationTwistyClick(evt, organization);

          expect(organization.isExpanded).toBe(true);
          expect(evt.stopPropagation).toHaveBeenCalled();
          expect(evt.preventDefault).toHaveBeenCalled();
          expect($state.includes).toHaveBeenCalledWith('management.view.organization', { organizationId: 'asdf' });
        });
      });
    });
  }

  describe('ownerTreeViewDirective with repositories', function() {
    runTestsForOwnerTreeViewDirective(['READ']);
  });
  describe('ownerTreeViewDirective without repositories', function() {
    runTestsForOwnerTreeViewDirective([]);
  });

  describe('organization and policy link', function() {
    var $timeout,
        options = { location: 'replace' };

    beforeEach(inject(function(_$rootScope_, _$httpBackend_, _$state_, _$timeout_, _$compile_, _CLMLocations_,
                               _CLMContextLocations_) {
      $httpBackend = _$httpBackend_;
      $state = _$state_;
      $timeout = _$timeout_;
      CLMLocations = _CLMLocations_;
      CLMContextLocations = _CLMContextLocations_;

      spyOn($state, 'is').and.returnValue(true);
      scope = _$rootScope_.$new();
      var ownerTreeView = angular.element('<div owner-tree-view></div>');
      _$compile_(ownerTreeView)(scope);

      spyOn($state, 'go');
    }));

    it('redirects to the root org if accessible', function() {
      doLoadWithOwnerList(SidebarResourceMockData.getOwnerListUrl());
      expect($state.go).toHaveBeenCalledWith('.organization', {organizationId: 'ROOT_ORGANIZATION_ID'}, options);
    });

    it('redirects to the first non-synthetic org', function() {
      doLoadWithOwnerList(SidebarResourceMockData.getOwnerListUrl_noRoot());
      expect($state.go).toHaveBeenCalledWith('.organization', {organizationId: 'nonSynthOrgID'}, options);
    });

    it('redirects to the first application if no non-syntehtic orgs present', function() {
      doLoadWithOwnerList(SidebarResourceMockData.getOwnerListUrl_onlySynthetic());
      expect($state.go).toHaveBeenCalledWith('.application', {applicationPublicId: 'applicationOnePublicID'}, options);
    });

    it('handles load error', function() {
      $httpBackend.expectGET(CLMLocations.getOwnerListUrl()).respond(400, 'Bad Request');
      $httpBackend.expectPUT(CLMContextLocations.getPermissionContextTestUrl('repository_container')).respond(false);
      $httpBackend.flush();

      expect(scope.vm.error).toBeDefined();
      expect(scope.vm.error.data).toEqual('Bad Request');
      expect(scope.vm.error.status).toEqual(400);

      scope.vm.doLoad();
      doLoadWithOwnerList(SidebarResourceMockData.getOwnerListUrl_onlySynthetic());
      expect(scope.vm.error).toBeUndefined();
    });

    function doLoadWithOwnerList(ownerList) {
      $httpBackend.expectGET(CLMLocations.getOwnerListUrl()).respond(ownerList);
      $httpBackend.expectPUT(CLMContextLocations.getPermissionContextTestUrl('repository_container')).respond(false);
      scope.$digest();
      $httpBackend.flush();
      $timeout.flush();
    }

  });

  describe('manifest scan feature flag', function() {

    beforeEach(inject((_$rootScope_, _$httpBackend_, _$state_, _$compile_, _CLMLocations_, _CLMContextLocations_) => {
      $httpBackend = _$httpBackend_;
      $state = _$state_;
      CLMLocations = _CLMLocations_;
      CLMContextLocations = _CLMContextLocations_;

      spyOn($state, 'is').and.returnValue(true);
      scope = _$rootScope_.$new();
      const ownerTreeView = angular.element('<div owner-tree-view></div>');
      _$compile_(ownerTreeView)(scope);
    }));

    describe('doLoad', () => {
      it('subscribes to ngRedux', () => {
        scope.vm.doLoad();
        $httpBackend.whenGET(CLMLocations.getOwnerListUrl()).respond(SidebarResourceMockData.getOwnerListUrl());
        $httpBackend.whenPUT(CLMContextLocations.getPermissionContextTestUrl('repository_container')).respond(false);
        $httpBackend.flush();

        expect(scope.vm.unsubscribe).toBeDefined();
      });
    });

    describe('$destroy', () => {
      it('unsubscribes from ngRedux', () => {
        expect(scope.vm.unsubscribe).not.toHaveBeenCalled();
        scope.$destroy();
        $httpBackend.whenGET(CLMLocations.getOwnerListUrl()).respond(SidebarResourceMockData.getOwnerListUrl());
        $httpBackend.whenPUT(CLMContextLocations.getPermissionContextTestUrl('repository_container')).respond(false);
        $httpBackend.flush();
        expect(scope.vm.unsubscribe).toHaveBeenCalledTimes(1);
      });
    });
  });
});
