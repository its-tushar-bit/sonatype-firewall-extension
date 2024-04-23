/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import reducer, {
  initialState,
} from 'MainRoot/sbomManager/features/dashboard/totalSbomsStoredTile/totalSbomsStoredTileSlice';
import { UI_ROUTER_ON_FINISH } from 'MainRoot/reduxUiRouter/routerActions';

describe('totalSbomsStoredTile reducers have the correct state when the following reducer is dispatched', function () {
  describe('totalSbomsStoredTile/loadTotalSbomsStored', function () {
    it('/pending', () => {
      const state = {
        loading: true,
        errorMessage: null,
        total: null,
        threshold: null,
      };

      const newState = reducer(state, {
        type: 'totalSbomsStoredTile/loadTotalSbomsStored/pending',
      });

      expect(newState.loading).toBe(true);
      expect(newState.errorMessage).toBe(null);
      expect(newState.total).toBe(null);
      expect(newState.threshold).toBe(null);
    });

    it('/failed', () => {
      const state = {
        loading: true,
        errorMessage: null,
        total: null,
        threshold: null,
      };

      const payload = {
        response: {
          data: 'payload error',
        },
      };

      const newState = reducer(state, {
        type: 'totalSbomsStoredTile/loadTotalSbomsStored/rejected',
        payload: payload,
      });

      expect(newState.errorMessage).toBe('payload error');
      expect(newState.loading).toBe(false);
      expect(newState.total).toBe(null);
      expect(newState.threshold).toBe(null);
    });

    it('/fulfilled', () => {
      const state = {
        loading: true,
        errorMessage: null,
        total: null,
        threshold: null,
      };

      const newState = reducer(state, {
        type: 'totalSbomsStoredTile/loadTotalSbomsStored/fulfilled',
        payload: { total: 123, threshold: 246 },
      });

      expect(newState.loading).toBe(false);
      expect(newState.errorMessage).toBe(null);
      expect(newState.total).toBe(123);
      expect(newState.threshold).toBe(246);
    });
  });

  describe('UI_ROUTER_ON_FINISH', () => {
    it('clears state on onFinish', () => {
      const state = Object.freeze({
        loading: false,
        errorMessage: null,
        total: 123,
        threshold: 246,
      });

      const newState = reducer(state, { type: UI_ROUTER_ON_FINISH });
      expect(newState).toEqual(initialState);
    });
  });
});
