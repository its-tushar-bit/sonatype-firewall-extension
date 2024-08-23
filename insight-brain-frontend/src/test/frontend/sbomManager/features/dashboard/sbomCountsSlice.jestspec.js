/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import reducer, { initialState } from 'MainRoot/sbomManager/features/dashboard/sbomCountsSlice';
import { UI_ROUTER_ON_FINISH } from 'MainRoot/reduxUiRouter/routerActions';

describe('sbomCountsSlice reducers have the correct state when the following reducer is dispatched', function () {
  describe('sbomCounts/load', function () {
    it('/pending', () => {
      const state = Object.freeze({
        loading: true,
        loadError: null,
        releaseReadyCount: null,
        partiallyReadyCount: null,
        needsAttentionCount: null,
        totalSbomCount: null,
        sbomMaxThreshold: null,
      });

      const newState = reducer(state, {
        type: 'sbomCounts/load/pending',
      });

      expect(newState.loading).toBe(true);
      expect(newState.loadError).toBe(null);
      expect(newState.releaseReadyCount).toBe(null);
      expect(newState.partiallyReadyCount).toBe(null);
      expect(newState.needsAttentionCount).toBe(null);
      expect(newState.totalSbomCount).toBe(null);
      expect(newState.sbomMaxThreshold).toBe(null);
    });

    it('/failed', () => {
      const state = {
        loading: true,
        loadError: null,
        releaseReadyCount: null,
        partiallyReadyCount: null,
        needsAttentionCount: null,
        totalSbomCount: null,
        sbomMaxThreshold: null,
      };

      const payload = {
        response: {
          data: 'payload error',
        },
      };

      const newState = reducer(state, {
        type: 'sbomCounts/load/rejected',
        payload: payload,
      });

      expect(newState.loadError).toEqual({ response: { data: 'payload error' } });
      expect(newState.loading).toBe(false);
      expect(newState.releaseReadyCount).toBe(null);
      expect(newState.partiallyReadyCount).toBe(null);
      expect(newState.needsAttentionCount).toBe(null);
      expect(newState.totalSbomCount).toBe(null);
      expect(newState.sbomMaxThreshold).toBe(null);
    });

    it('/fulfilled', () => {
      const state = Object.freeze({
        loading: true,
        loadError: null,
        releaseReadyCount: null,
        partiallyReadyCount: null,
        needsAttentionCount: null,
        totalSbomCount: null,
        sbomMaxThreshold: null,
      });

      const newState = reducer(state, {
        type: 'sbomCounts/load/fulfilled',
        payload: {
          releaseReadyCount: 1,
          partiallyReadyCount: 2,
          needsAttentionCount: 3,
          total: 4,
          threshold: 5,
        },
      });

      expect(newState.loading).toBe(false);
      expect(newState.loadError).toBe(null);
      expect(newState.releaseReadyCount).toBe(1);
      expect(newState.partiallyReadyCount).toBe(2);
      expect(newState.needsAttentionCount).toBe(3);
      expect(newState.totalSbomCount).toBe(4);
      expect(newState.sbomMaxThreshold).toBe(5);
    });
  });

  describe('UI_ROUTER_ON_FINISH', () => {
    it('clears state on onFinish', () => {
      const state = Object.freeze({
        loading: false,
        loadError: null,
        releaseReadyCount: 1,
        partiallyReadyCount: 2,
        needsAttentionCount: 3,
        totalSbomCount: 4,
        sbomMaxThreshold: 5,
      });

      const newState = reducer(state, { type: UI_ROUTER_ON_FINISH });
      expect(newState).toEqual(initialState);
    });
  });
});
