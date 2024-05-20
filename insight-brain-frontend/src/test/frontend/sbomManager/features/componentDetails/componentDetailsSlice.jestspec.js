/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import reducer from 'MainRoot/sbomManager/features/componentDetails/componentDetailsSlice';

describe('sbomComponentDetailsPage reducers have the correct state when the following reducer is dispatched', function () {
  describe('sbomComponentDetailsPage/loadComponentDetails', function () {
    it('/pending', () => {
      const state = {
        publicAppId: null,
        componentDetails: null,
        loadError: null,
      };

      const newState = reducer(state, {
        type: 'sbomComponentDetailsPage/loadComponentDetails/pending',
      });

      expect(newState.publicAppId).toBe(null);
      expect(newState.componentDetails).toBe(null);
      expect(newState.loadError).toBe(null);
      expect(newState.loading).toBe(true);
    });

    it('/failed', () => {
      const state = {
        loading: false,
        loadError: null,
        publicAppId: null,
        componentDetails: null,
      };

      const payload = {
        response: {
          data: 'payload error',
        },
      };

      const newState = reducer(state, {
        type: 'sbomComponentDetailsPage/loadComponentDetails/rejected',
        payload: payload,
      });

      expect(newState.loading).toBe(false);
      expect(newState.loadError).toBe('payload error');
      expect(newState.publicAppId).toBe(null);
      expect(newState.componentDetails).toBe(null);
    });

    it('/fulfilled', () => {
      const state = {
        loading: false,
        loadError: null,
        componentDetails: null,
      };

      const newState = reducer(state, {
        type: 'sbomComponentDetailsPage/loadComponentDetails/fulfilled',
        payload: { name: 'abc123' },
      });

      expect(newState.loading).toBe(false);
      expect(newState.loadError).toBe(null);
      expect(newState.componentDetails.name).toBe('abc123');
    });
  });
});
