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
  selectRouteParamsFromSecurityTab,
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

      expect(actualSelection).toBeTrue();
    });
  });

  describe('selectIsApplication', () => {
    it('is composed from the following selector', () => {
      expect(selectIsApplication.dependencies).toEqual([selectCurrentRouteName]);
    });

    it('selects if current url name includes `application`', () => {
      const actualSelection = selectIsApplication.resultFunc('management.edit.application');

      expect(actualSelection).toBeTrue();
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

      expect(selection).toBeTrue();
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
});
