/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import reducer, { initialState } from 'MainRoot/sbomManager/features/billOfMaterials/billOfMaterialsSlice';
import { UI_ROUTER_ON_FINISH } from 'MainRoot/reduxUiRouter/routerActions';

describe('billOfMaterialsPage reducers have the correct state when the following reducer is dispatched', function () {
  it('billOfMaterialsPage/setPublicAppId', () => {
    const state = {
      publicAppId: null,
    };

    const payload = 'app_123';

    const newState = reducer(state, {
      type: 'billOfMaterialsPage/setPublicAppId',
      payload: payload,
    });

    expect(newState.publicAppId).toBe('app_123');
  });

  describe('billOfMaterialsPage/loadSbomTableData', function () {
    it('/pending', () => {
      const state = {
        results: null,
        errorInternalAppId: null,
      };

      const newState = reducer(state, {
        type: 'billOfMaterialsPage/loadInternalApplicationId/pending',
      });

      expect(newState.results).toBe(null);
      expect(newState.loading).toBe(true);
    });

    it('/failed', () => {
      const state = {
        loading: false,
        errorInternalAppId: null,
        internalAppId: null,
        publicAppId: null,
      };

      const payload = {
        response: {
          data: 'payload error',
        },
      };

      const newState = reducer(state, {
        type: 'billOfMaterialsPage/loadInternalApplicationId/rejected',
        payload: payload,
      });

      expect(newState.errorInternalAppId).toBe('payload error');
      expect(newState.loading).toBe(false);
      expect(newState.internalAppId).toBe(null);
      expect(newState.publicAppId).toBe(null);
    });

    it('/fulfilled', () => {
      const state = {
        loading: false,
        errorInternalAppId: null,
        internalAppId: null,
      };

      const newState = reducer(state, {
        type: 'billOfMaterialsPage/loadInternalApplicationId/fulfilled',
        payload: { id: 'abc123' },
      });

      expect(newState.loading).toBe(false);
      expect(newState.errorInternalAppId).toBe(null);
      expect(newState.internalAppId).toBe('abc123');
    });
  });

  describe('UI_ROUTER_ON_FINISH', () => {
    it('clears state on onFinish', () => {
      const state = Object.freeze({
        loading: true,
        errorInternalAppId: 'some error',
        publicAppId: 'test-app-public',
        internalAppId: 'test-app-internal',
      });

      const newState = reducer(state, { type: UI_ROUTER_ON_FINISH });
      expect(newState).toEqual(initialState);
    });
  });
});
