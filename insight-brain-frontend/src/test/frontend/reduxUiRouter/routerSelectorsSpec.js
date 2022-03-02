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
});
