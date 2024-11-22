/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import reducer from 'MainRoot/api/apiPageSlice';

describe('apiPageSliceReducer', () => {
  describe('initial state', () => {
    it('returns the initial state given an undefined state', function () {
      const state = undefined;

      const newState = reducer(state, {});

      expect(newState).toEqual({ loading: false, loadError: null, publicOpenApi: {}, experimentalOpenApi: {} });
    });
  });

  describe('unknown action', () => {
    it('returns original state', function () {
      const state = Object.freeze({ foo: 'bar' });
      const action = {
        type: 'UNKNOWN',
      };

      const newState = reducer(state, action);

      expect(newState).toBe(state);
    });
  });

  describe('apiPage/loadOpenApi/pending action', () => {
    it('sets the initial state', () => {
      const state = {};

      const newState = reducer(state, {
        type: 'apiPage/loadOpenApi/pending',
      });

      expect(newState).toEqual({ loading: true });
    });
  });

  describe('apiPage/loadOpenApi/fulfilled action', () => {
    it('sets `loading` to false and `openApi` to the payload', () => {
      const state = {};
      const payload = { endpoint: 'endpointType', data: 'somePayload' };

      const newState = reducer(state, {
        type: 'apiPage/loadOpenApi/fulfilled',
        payload,
      });

      expect(newState).toEqual({
        loading: false,
        endpointTypeOpenApi: 'somePayload',
      });
    });
  });

  describe('apiPage/loadOpenApi/rejected action', () => {
    it('sets `loading` to false and `loadError` to the payload http error message', () => {
      const state = {};
      const error = 'someError';

      const newState = reducer(state, {
        type: 'apiPage/loadOpenApi/rejected',
        payload: error,
      });

      expect(newState).toEqual({
        loading: false,
        loadError: error,
      });
    });
  });
});
