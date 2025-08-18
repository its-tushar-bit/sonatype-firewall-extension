/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import reducer from 'MainRoot/enterpriseReporting/enterpriseReportingLandingPageSlice';
import { mockData } from './enterpriseReportingMockData';

describe('EnterpriseReportingReducer', () => {
  let initialState;

  const fakeData = {
    dashboardsData: mockData,
    iqVersion: '1.188.0-SNAPSHOT',
  };

  beforeEach(() => {
    const dummyAction = { type: 'DUMMY_ACTION' };
    initialState = reducer(undefined, dummyAction);
  });

  describe('initial state', () => {
    it('has default field values', function () {
      expect(initialState.loading).toBeFalsy();
      expect(initialState.loadError).toBeNull();
      expect(initialState.dashboardsData).toBeNull();
      expect(initialState.iqVersion).toBeNull();
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

  describe('enterpriseReportingLandingPage/load/pending', () => {
    it('should return the initial state', () => {
      const action = { type: 'enterpriseReportingLandingPage/load/pending' };
      const newState = reducer(undefined, action);

      expect(newState).toEqual({ ...initialState, loading: true });
    });
  });

  describe('enterpriseReportingLandingPage/load/fulfilled', () => {
    it('should set the data to the state ', () => {
      const oldState = Object.freeze({
        loading: false,
        loadError: null,
      });
      const newState = reducer(oldState, {
        type: 'enterpriseReportingLandingPage/load/fulfilled',
        payload: fakeData,
      });

      expect(newState).toEqual({
        dashboardsData: fakeData.dashboardsData,
        iqVersion: fakeData.iqVersion,
        loadError: null,
        loading: false,
      });
    });
  });

  describe('enterpriseReportingLandingPage/load/rejected', () => {
    it('should set the data to the state ', () => {
      const oldState = Object.freeze({ loading: false, loadError: null, dashboardsData: null, iqVersion: null });
      const newState = reducer(oldState, {
        type: 'enterpriseReportingLandingPage/load/rejected',
        payload: 'Error on load',
      });

      expect(newState).toEqual({
        loadError: 'Error on load',
        loading: false,
        dashboardsData: null,
        iqVersion: null,
      });
    });
  });
});
