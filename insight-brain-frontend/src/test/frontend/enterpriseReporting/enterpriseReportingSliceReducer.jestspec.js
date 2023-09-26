/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import reducer from 'MainRoot/enterpriseReporting/enterpriseReportingSlice';

describe('EnterpriseReportingReducer', () => {
  let initialState;

  const fakeData = {
    url: 'http://looker.com/embed',
    baseUrl: 'http://looker.com/',
  };

  beforeEach(() => {
    const dummyAction = { type: 'DUMMY_ACTION' };
    initialState = reducer(undefined, dummyAction);
  });

  describe('initial state', () => {
    it('has default field values', function () {
      expect(initialState.loading).toBeFalsy();
      expect(initialState.loadError).toBeNull();
      expect(initialState.embedUrlData).toBeNull();
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

  describe('enterpriseReporting/load/pending', () => {
    it('should return the initial state', () => {
      const action = { type: 'enterpriseReporting/load/pending' };
      const newState = reducer(undefined, action);

      expect(newState).toEqual({ ...initialState, loading: true });
    });
  });

  describe('enterpriseReporting/load/fulfilled', () => {
    it('should set the data to the state ', () => {
      const oldState = {};
      const newState = reducer(oldState, {
        type: 'enterpriseReporting/load/fulfilled',
        payload: fakeData,
      });

      expect(newState).toEqual({
        embedUrlData: {
          ...fakeData,
        },
        loadError: null,
        loading: false,
      });
    });
  });

  describe('enterpriseReporting/load/rejected', () => {
    it('should set the data to the state ', () => {
      const oldState = {};
      const newState = reducer(oldState, {
        type: 'enterpriseReporting/load/rejected',
        payload: 'Error on load',
      });

      expect(newState).toEqual({
        loadError: 'Error on load',
        loading: false,
      });
    });
  });
});
