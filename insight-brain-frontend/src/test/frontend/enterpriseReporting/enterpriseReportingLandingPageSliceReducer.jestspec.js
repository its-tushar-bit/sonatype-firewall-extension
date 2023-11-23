/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import reducer from 'MainRoot/enterpriseReporting/enterpriseReportingLandingPageSlice';

describe('EnterpriseReportingReducer', () => {
  let initialState;

  const fakeData = {
    dashboardMetadata: [
      {
        dashboardId: 'rolling-recap',
        title: 'Rolling Recap Dashboard: Past 365 Days',
        description: 'Unlock trends by comparing your usage with the rest of the industry, over the past year.',
        features: ['Analyze app performance', 'Compare initial & latest scans', 'View security experts’ rating'],
        accessButtonText: 'View Rolling Recap',
        previewImage: '',
        priority: 1,
        spotlight: false,
      },
      {
        dashboardId: 'ai-consumption',
        title: 'ML/AI: Apps Using Machine Learning',
        description: 'Observe Machine Learning (ML) components and integrations within your software.',
        features: ['Sort components by AI type', 'Monitor AI within your apps', 'Isolate exact locations of AI'],
        accessButtonText: 'View ML/AI',
        previewImage: '',
        priority: 2,
        spotlight: true,
      },
      {
        dashboardId: 'component-eol',
        title: 'Component EOL: Retiring Old Code',
        description: 'Learn the specifics about the components that have the status of End of Life (EOL) ',
        features: ['Note ratings by version', 'Notice apps using versions', 'Sort cumulative lists by type'],
        accessButtonText: 'View Component EOL',
        previewImage: '',
        priority: 3,
        spotlight: false,
      },
    ],
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
        dashboardsData: fakeData,
        loadError: null,
        loading: false,
      });
    });
  });

  describe('enterpriseReportingLandingPage/load/rejected', () => {
    it('should set the data to the state ', () => {
      const oldState = Object.freeze({ loading: false, dashboardsData: null });
      const newState = reducer(oldState, {
        type: 'enterpriseReportingLandingPage/load/rejected',
        payload: 'Error on load',
      });

      expect(newState).toEqual({
        loadError: 'Error on load',
        loading: false,
        dashboardsData: null,
      });
    });
  });
});
