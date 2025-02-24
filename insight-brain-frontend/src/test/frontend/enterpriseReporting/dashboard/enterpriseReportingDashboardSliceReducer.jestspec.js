/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import reducer from 'MainRoot/enterpriseReporting/dashboard/enterpriseReportingDashboardSlice';

describe('enterpriseReportingDashboardReducer', () => {
  let initialState;

  const baseUrl = 'http://looker.com/';
  const dashboardMetadata = [
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
  ];

  beforeEach(() => {
    const dummyAction = { type: 'DUMMY_ACTION' };
    initialState = reducer(undefined, dummyAction);
  });

  describe('initial state', () => {
    it('has default field values', function () {
      expect(initialState.loading).toBe(true);
      expect(initialState.loadError).toBeNull();
      expect(initialState.baseUrl).toBeNull();
      expect(initialState.selectedDashboard).toBeNull();
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

  describe('enterpriseReportingDashboard/reset', () => {
    it('returns initial state', function () {
      const state = Object.freeze({ foo: 'bar' });
      const action = {
        type: 'enterpriseReportingDashboard/reset',
      };

      const newState = reducer(state, action);
      expect(newState).toBe(initialState);
    });
  });

  describe('enterpriseReportingDashboard/load/pending', () => {
    it('should return the initial state', () => {
      const action = { type: 'enterpriseReportingDashboard/load/pending' };
      const newState = reducer(undefined, action);

      expect(newState).toEqual({ ...initialState, loading: true });
    });
  });

  describe('enterpriseReportingDashboard/load/fulfilled', () => {
    it('should set the data to the state', () => {
      const oldState = {};
      const newState = reducer(oldState, {
        type: 'enterpriseReportingDashboard/load/fulfilled',
        payload: { baseUrl: baseUrl, dashboards: dashboardMetadata },
      });

      expect(newState).toEqual({
        baseUrl: 'looker.com',
        dashboardsData: dashboardMetadata,
        loading: false,
      });
    });
  });

  describe('enterpriseReportingDashboard/load/rejected', () => {
    it('should set the data to the state', () => {
      const oldState = {};
      const newState = reducer(oldState, {
        type: 'enterpriseReportingDashboard/load/rejected',
        payload: 'Error on load',
      });

      expect(newState).toEqual({
        loadError: 'Error on load',
        loading: false,
      });
    });
  });

  describe('setSelectedDashboard action', () => {
    it('should save the selected report dashboard id to the state', () => {
      const newState = reducer(initialState, {
        type: 'enterpriseReportingDashboard/setSelectedDashboard',
        payload: { dashboardId: 'dashboardId', dashboardPath: 'dashboards/dashboardPath' },
      });
      expect(newState.selectedDashboard).toEqual({
        dashboardId: 'dashboardId',
        dashboardPath: 'dashboardPath',
      });
    });
  });
});
