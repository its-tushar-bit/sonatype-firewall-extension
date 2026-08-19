/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import {
  selectRouterSlice,
  selectRouterCurrentParams,
  selectRouterState,
  selectRouterPrevState,
  selectCurrentRouteName,
  selectPreviousRouteName,
  selectIsOrganization,
  selectIsApplication,
  selectOrganizationId,
  selectApplicationId,
  selectIsRootOrganization,
  selectIsRepositoryManager,
  selectIsRepositoryContainer,
  selectRouteParamsFromSecurityTab,
  selectIsSbomManager,
  selectIsManagementViewRouterState,
  selectIsStandaloneFirewall,
} from 'MainRoot/reduxUiRouter/routerSelectors';

describe('routerSelectors', function () {
  describe('selectRouterSlice', () => {
    it('selects `router`', () => {
      const state = { router: 'router' };
      expect(selectRouterSlice(state)).toBe('router');
    });
  });

  describe('selectRouterCurrentParams', () => {
    it('is composed from the following selector', () => {
      expect(selectRouterCurrentParams.dependencies).toEqual([selectRouterSlice]);
    });

    it('selects `currentParams`', () => {
      const actualSelection = selectRouterCurrentParams.resultFunc({ currentParams: 'currentParams' });

      expect(actualSelection).toBe('currentParams');
    });
  });

  describe('selectRouterState', () => {
    it('is composed from the following selector', () => {
      expect(selectRouterState.dependencies).toEqual([selectRouterSlice]);
    });

    it('selects `currentParams`', () => {
      const actualSelection = selectRouterState.resultFunc({ currentState: 'currentState' });

      expect(actualSelection).toBe('currentState');
    });
  });

  describe('selectCurrentRouteName', () => {
    it('is composed from the following selector', () => {
      expect(selectCurrentRouteName.dependencies).toEqual([selectRouterState]);
    });

    it('selects `name`', () => {
      const actualSelection = selectCurrentRouteName.resultFunc({ name: 'name' });

      expect(actualSelection).toBe('name');
    });
  });

  describe('selectRouterPrevState', () => {
    it('is composed from the following selector', () => {
      expect(selectRouterPrevState.dependencies).toEqual([selectRouterSlice]);
    });

    it('selects `prevState`', () => {
      const actualSelection = selectRouterPrevState.resultFunc({ prevState: 'prevState' });

      expect(actualSelection).toBe('prevState');
    });
  });

  describe('selectPreviousRouteName', () => {
    it('is composed from the following selector', () => {
      expect(selectPreviousRouteName.dependencies).toEqual([selectRouterPrevState]);
    });

    it('selects `name`', () => {
      const actualSelection = selectPreviousRouteName.resultFunc({ name: 'name' });

      expect(actualSelection).toBe('name');
    });
  });

  describe('selectIsOrganization', () => {
    it('is composed from the following selector', () => {
      expect(selectIsOrganization.dependencies).toEqual([selectCurrentRouteName]);
    });

    it('selects if current url name includes `organization`', () => {
      const actualSelection = selectIsOrganization.resultFunc('management.edit.organization.policy');

      expect(actualSelection).toBeTruthy();
    });
  });

  describe('selectIsApplication', () => {
    it('is composed from the following selector', () => {
      expect(selectIsApplication.dependencies).toEqual([selectCurrentRouteName]);
    });

    it('selects if current url name includes `application`', () => {
      const actualSelection = selectIsApplication.resultFunc('management.edit.application');

      expect(actualSelection).toBeTruthy();
    });
  });

  describe('selectOrganizationId', () => {
    it('is composed from the following selector', () => {
      expect(selectOrganizationId.dependencies).toEqual([selectRouterCurrentParams]);
    });

    it('selects if current url name includes `organizationId`', () => {
      const actualSelection = selectOrganizationId.resultFunc({ organizationId: 'orgId' });

      expect(actualSelection).toBe('orgId');
    });
  });

  describe('selectApplicationId', () => {
    it('is composed from the following selector', () => {
      expect(selectApplicationId.dependencies).toEqual([selectRouterCurrentParams]);
    });

    it('selects if current url name includes `applicationId`', () => {
      const actualSelection = selectApplicationId.resultFunc({ applicationPublicId: 'appId' });

      expect(actualSelection).toBe('appId');
    });
  });

  describe('selectIsRootOrganization', () => {
    it('is composed from the following selector', () => {
      expect(selectIsRootOrganization.dependencies).toEqual([selectRouterCurrentParams]);
    });

    it('returns true if organizationId is ROOT_ORGANIZATION_ID', () => {
      const currentRouterParams = {
        organizationId: 'ROOT_ORGANIZATION_ID',
      };
      const selection = selectIsRootOrganization.resultFunc(currentRouterParams);

      expect(selection).toBeTruthy();
    });
  });

  describe('selectIsRepositoryContainer', () => {
    it('is composed from the following selector', () => {
      expect(selectIsRepositoryContainer.dependencies).toEqual([selectCurrentRouteName]);
    });

    it('selects if current url name includes `organization`', () => {
      expect(selectIsRepositoryContainer.resultFunc('management.edit.repository_container.policy')).toBeTruthy();
      expect(selectIsRepositoryContainer.resultFunc('management.edit.repository_manager.policy')).toBeFalsy();
      expect(selectIsRepositoryContainer.resultFunc('management.edit.organization.policy')).toBeFalsy();
      expect(selectIsRepositoryContainer.resultFunc('management.edit.application.policy')).toBeFalsy();
    });
  });

  describe('selectIsRepositoryManager', () => {
    it('is composed from the following selector', () => {
      expect(selectIsRepositoryManager.dependencies).toEqual([selectCurrentRouteName]);
    });

    it('selects if current url name includes `organization`', () => {
      expect(selectIsRepositoryManager.resultFunc('management.edit.repository_manager.policy')).toBeTruthy();
      expect(selectIsRepositoryManager.resultFunc('management.edit.repository_container.policy')).toBeFalsy();
      expect(selectIsRepositoryManager.resultFunc('management.edit.organization.policy')).toBeFalsy();
      expect(selectIsRepositoryManager.resultFunc('management.edit.application.policy')).toBeFalsy();
    });
  });

  describe('selectRouteParamsFromSecurityTab', () => {
    it('maps the route params to proper common properties in application securityTab components', () => {
      const currentRouterParams = {
        publicId: 'applicationId',
        hash: 'componentHash',
      };
      expect(selectRouteParamsFromSecurityTab.dependencies).toEqual([selectRouterCurrentParams]);
      const selection = selectRouteParamsFromSecurityTab.resultFunc(currentRouterParams);

      expect(selection).toEqual({
        ownerId: currentRouterParams.publicId,
        hash: currentRouterParams.hash,
        isRepositoryComponent: false,
      });
    });

    it('maps the route params to proper common properties in repository securityTab components', () => {
      const currentRouterParams = {
        repositoryId: 'repositoryId',
        componentHash: 'componentHash',
      };
      expect(selectRouteParamsFromSecurityTab.dependencies).toEqual([selectRouterCurrentParams]);
      const selection = selectRouteParamsFromSecurityTab.resultFunc(currentRouterParams);

      expect(selection).toEqual({
        ownerId: currentRouterParams.repositoryId,
        hash: currentRouterParams.componentHash,
        isRepositoryComponent: true,
      });
    });
  });

  describe('selectIsSbomManager', function () {
    it('returns true if the route state name starts with "sbomManager."', function () {
      const state = {
        router: {
          currentState: {
            name: 'sbomManager.foo',
          },
        },
      };
      const state2 = {
        router: {
          currentState: {
            name: 'sbomManager',
          },
        },
      };

      expect(selectIsSbomManager(state)).toBe(true);
      expect(selectIsSbomManager(state2)).toBe(true);
    });

    it('returns false if the route state name does not start with "sbomManager."', function () {
      const state = {
        router: {
          currentState: {
            name: 'asdf.foo',
          },
        },
      };
      const state2 = {
        router: {
          currentState: {
            name: 'foo.sbomManager',
          },
        },
      };

      expect(selectIsSbomManager(state)).toBe(false);
      expect(selectIsSbomManager(state2)).toBe(false);
    });
  });

  describe('selectIsStandaloneFirewall', function () {
    it('returns true if the route state name starts with "firewall."', function () {
      const state = {
        router: {
          currentState: {
            name: 'firewall.foo',
          },
        },
      };
      const state2 = {
        router: {
          currentState: {
            name: 'firewall',
          },
        },
      };

      expect(selectIsStandaloneFirewall(state)).toBe(true);
      expect(selectIsStandaloneFirewall(state2)).toBe(true);
    });

    it('returns false if the route state name does not start with "firewall."', function () {
      const state = {
        router: {
          currentState: {
            name: 'asdf.foo',
          },
        },
      };
      const state2 = {
        router: {
          currentState: {
            name: 'foo.firewall',
          },
        },
      };

      expect(selectIsStandaloneFirewall(state)).toBe(false);
      expect(selectIsStandaloneFirewall(state2)).toBe(false);
    });
  });

  describe('selectIsManagementViewRouterState', () => {
    it('is composed from the following selector', () => {
      expect(selectIsManagementViewRouterState.dependencies).toEqual([selectRouterState]);
    });

    it('returns true for management.view`', () => {
      const actualSelection = selectIsManagementViewRouterState.resultFunc({
        name: 'management.view',
      });

      expect(actualSelection).toBe(true);
    });

    it('returns true for sbomManager.management.view`', () => {
      const actualSelection = selectIsManagementViewRouterState.resultFunc({
        name: 'sbomManager.management.view',
      });

      expect(actualSelection).toBe(true);
    });

    it('returns false `', () => {
      const actualSelection = selectIsManagementViewRouterState.resultFunc({
        name: 'sbomManager.',
      });

      expect(actualSelection).toBe(false);
    });
  });
});
