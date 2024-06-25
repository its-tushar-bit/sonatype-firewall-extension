/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import reducer, {
  initialState,
} from 'MainRoot/sbomManager/features/dashboard/sbomReleaseStatusTile/sbomReleaseStatusTileSlice';
import { UI_ROUTER_ON_FINISH } from 'MainRoot/reduxUiRouter/routerActions';

describe('sbomReleaseStatusTile reducers have the correct state when the following reducer is dispatched', function () {
  describe('sbomReleaseStatusTile/sbomReleaseStatusTile', function () {
    it('/pending', () => {
      const state = {
        loading: true,
        loadError: null,
        releaseReadyCount: null,
        partiallyReadyCount: null,
        needsAttentionCount: null,
      };

      const newState = reducer(state, {
        type: 'sbomReleaseStatusTile/sbomReleaseStatusTile/pending',
      });

      expect(newState.loading).toBe(true);
      expect(newState.releaseReadyCount).toBe(null);
      expect(newState.partiallyReadyCount).toBe(null);
      expect(newState.needsAttentionCount).toBe(null);
    });

    it('/failed', () => {
      const state = {
        loading: true,
        loadError: null,
        releaseReadyCount: null,
        partiallyReadyCount: null,
        needsAttentionCount: null,
      };

      const payload = {
        response: {
          data: 'payload error',
        },
      };

      const newState = reducer(state, {
        type: 'sbomReleaseStatusTile/sbomReleaseStatusTile/rejected',
        payload: payload,
      });

      expect(newState.loadError).toEqual({ response: { data: 'payload error' } });
      expect(newState.loading).toBe(false);
      expect(newState.releaseReadyCount).toBe(null);
      expect(newState.partiallyReadyCount).toBe(null);
      expect(newState.needsAttentionCount).toBe(null);
    });

    it('/fulfilled', () => {
      const state = {
        loading: true,
        releaseReadyCount: null,
        partiallyReadyCount: null,
        needsAttentionCount: null,
      };

      const newState = reducer(state, {
        type: 'sbomReleaseStatusTile/sbomReleaseStatusTile/fulfilled',
        payload: {
          releaseReadyCount: 10,
          partiallyReadyCount: 20,
          needsAttentionCount: 30,
        },
      });

      expect(newState.loading).toBe(false);
      expect(newState.loadError).toBe(null);
      expect(newState.releaseReadyCount).toBe(10);
      expect(newState.partiallyReadyCount).toBe(20);
      expect(newState.needsAttentionCount).toBe(30);
    });
  });

  describe('UI_ROUTER_ON_FINISH', () => {
    it('clears state on onFinish', () => {
      const state = Object.freeze({
        loading: false,
        loadError: null,
        releaseReadyCount: 10,
        partiallyReadyCount: 20,
        needsAttentionCount: 30,
      });

      const newState = reducer(state, { type: UI_ROUTER_ON_FINISH });
      expect(newState).toEqual(initialState);
    });
  });
});
