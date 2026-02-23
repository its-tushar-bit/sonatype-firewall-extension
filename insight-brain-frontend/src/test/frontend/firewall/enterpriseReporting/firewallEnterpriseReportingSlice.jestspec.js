/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

import { axiosMockAdapter } from 'TestRoot/SpecUtil';
import configureMockStore from 'redux-mock-store';
import thunk from 'redux-thunk';
import reducer, { actions, initialState } from 'MainRoot/firewall/enterpriseReporting/firewallEnterpriseReportingSlice';
import {
  getEnterpriseReportingDashboardsUrl,
  getEnterpriseReportingBaseUrl,
  getIqVersion,
} from 'MainRoot/util/CLMLocation';

const middlewares = [thunk];
const mockStore = configureMockStore(middlewares);

describe('firewallEnterpriseReportingSlice', () => {
  let axiosMock;

  const mockDashboards = [
    {
      dashboardId: 'malware-insights',
      title: 'Malware Insights',
      category: 'firewall',
      dashboardPath: 'firewall_malware::firewall_malware',
    },
  ];

  beforeAll(() => {
    axiosMock = axiosMockAdapter();
  });

  afterEach(() => {
    axiosMock.reset();
  });

  describe('loadDashboards', () => {
    it('should set loading to true when loadDashboards is pending', () => {
      const action = { type: actions.loadDashboards.pending.type };
      const state = reducer(initialState, action);

      expect(state.loading).toBe(true);
      expect(state.loadError).toBe(null);
    });

    it('should load dashboards and iqVersion successfully', async () => {
      axiosMock.onGet(getEnterpriseReportingDashboardsUrl()).reply(200, {
        dashboardMetadata: mockDashboards,
      });
      axiosMock.onGet(getIqVersion()).reply(200, {
        version: '1.170.0',
      });

      const store = mockStore({ firewallEnterpriseReporting: initialState });
      await store.dispatch(actions.loadDashboards());

      const dispatchedActions = store.getActions();
      expect(dispatchedActions[0].type).toBe(actions.loadDashboards.pending.type);
      expect(dispatchedActions[1].type).toBe(actions.loadDashboards.fulfilled.type);
      expect(dispatchedActions[1].payload).toEqual({
        dashboards: mockDashboards,
        iqVersion: '1.170.0',
      });
    });

    it('should set dashboards and iqVersion when loadDashboards is fulfilled', () => {
      const action = {
        type: actions.loadDashboards.fulfilled.type,
        payload: {
          dashboards: mockDashboards,
          iqVersion: '1.170.0',
        },
      };
      const state = reducer(initialState, action);

      expect(state.loading).toBe(false);
      expect(state.dashboards).toEqual(mockDashboards);
      expect(state.iqVersion).toBe('1.170.0');
      expect(state.loadError).toBe(null);
    });

    it('should handle loadDashboards failure', async () => {
      axiosMock.onGet(getEnterpriseReportingDashboardsUrl()).reply(500, { message: 'Server Error' });
      axiosMock.onGet(getIqVersion()).reply(500, { message: 'Server Error' });

      const store = mockStore({ firewallEnterpriseReporting: initialState });
      await store.dispatch(actions.loadDashboards());

      const dispatchedActions = store.getActions();
      expect(dispatchedActions[0].type).toBe(actions.loadDashboards.pending.type);
      expect(dispatchedActions[1].type).toBe(actions.loadDashboards.rejected.type);
    });

    it('should set error when loadDashboards is rejected', () => {
      const action = {
        type: actions.loadDashboards.rejected.type,
        payload: { message: 'Server Error' },
      };
      const state = reducer(initialState, action);

      expect(state.loading).toBe(false);
      expect(state.loadError).toBeTruthy();
    });
  });

  describe('loadDashboardDetail', () => {
    it('should load dashboard detail with baseUrl and dashboards', async () => {
      axiosMock.onGet(getEnterpriseReportingDashboardsUrl()).reply(200, {
        dashboardMetadata: mockDashboards,
      });
      axiosMock.onGet(getEnterpriseReportingBaseUrl()).reply(200, 'https://looker.example.com');

      const store = mockStore({ firewallEnterpriseReporting: initialState });
      await store.dispatch(actions.loadDashboardDetail());

      const dispatchedActions = store.getActions();
      expect(dispatchedActions[0].type).toBe(actions.loadDashboardDetail.pending.type);
      expect(dispatchedActions[1].type).toBe(actions.loadDashboardDetail.fulfilled.type);
    });

    it('should extract host from baseUrl when fulfilled', () => {
      const action = {
        type: actions.loadDashboardDetail.fulfilled.type,
        payload: {
          dashboards: { dashboardMetadata: mockDashboards },
          baseUrl: 'https://looker.example.com/path',
        },
      };
      const state = reducer(initialState, action);

      expect(state.baseUrl).toBe('looker.example.com');
      expect(state.dashboards).toEqual(mockDashboards);
    });
  });

  describe('updateSelectedDashboard', () => {
    it('should update selected dashboard when dashboard exists', () => {
      const stateWithDashboards = {
        ...initialState,
        dashboards: mockDashboards,
      };

      const store = mockStore({ firewallEnterpriseReporting: stateWithDashboards });
      store.dispatch(actions.updateSelectedDashboard('malware-insights'));

      const dispatchedActions = store.getActions();
      expect(dispatchedActions[0].type).toBe(actions.setSelectedDashboard.type);
      expect(dispatchedActions[0].payload).toEqual(
        expect.objectContaining({
          dashboardId: 'malware-insights',
          dashboardPath: 'firewall_malware::firewall_malware',
          category: 'firewall',
        })
      );
    });

    it('should not dispatch action when dashboards are empty', () => {
      const store = mockStore({ firewallEnterpriseReporting: initialState });
      store.dispatch(actions.updateSelectedDashboard('malware-insights'));

      const dispatchedActions = store.getActions();
      expect(dispatchedActions).toHaveLength(0);
    });

    it('should reset selected dashboard when dashboard not found', () => {
      const stateWithDashboards = {
        ...initialState,
        dashboards: mockDashboards,
      };

      const store = mockStore({ firewallEnterpriseReporting: stateWithDashboards });
      store.dispatch(actions.updateSelectedDashboard('non-existent-id'));

      const dispatchedActions = store.getActions();
      expect(dispatchedActions[0].type).toBe(actions.resetSelectedDashboard.type);
    });
  });

  describe('setSelectedDashboard', () => {
    it('should set selected dashboard and name', () => {
      const dashboard = {
        dashboardId: 'malware-insights',
        dashboardPath: 'dashboards/firewall_malware::firewall_malware',
        category: 'firewall',
        title: 'Malware Insights',
      };

      const action = {
        type: actions.setSelectedDashboard.type,
        payload: dashboard,
      };
      const state = reducer(initialState, action);

      expect(state.selectedDashboard).toEqual({
        dashboardId: 'malware-insights',
        dashboardPath: 'firewall_malware::firewall_malware',
        category: 'firewall',
      });
      expect(state.selectedDashboardName).toBe('Malware Insights');
    });

    it('should strip "dashboards/" prefix from dashboardPath', () => {
      const dashboard = {
        dashboardId: 'test',
        dashboardPath: 'dashboards/test::test',
        category: 'firewall',
        title: 'Test',
      };

      const action = {
        type: actions.setSelectedDashboard.type,
        payload: dashboard,
      };
      const state = reducer(initialState, action);

      expect(state.selectedDashboard.dashboardPath).toBe('test::test');
    });
  });

  describe('resetSelectedDashboard', () => {
    it('should reset selected dashboard to null', () => {
      const stateWithSelection = {
        ...initialState,
        selectedDashboard: { dashboardId: 'test', dashboardPath: 'test::test', category: 'firewall' },
        selectedDashboardName: 'Test Dashboard',
      };

      const action = { type: actions.resetSelectedDashboard.type };
      const state = reducer(stateWithSelection, action);

      expect(state.selectedDashboard).toBe(null);
      expect(state.selectedDashboardName).toBe(null);
    });
  });

  describe('reset', () => {
    it('should reset state to initial state', () => {
      const modifiedState = {
        dashboards: mockDashboards,
        loading: true,
        loadError: 'Error',
        iqVersion: '1.170.0',
        baseUrl: 'looker.example.com',
        selectedDashboard: { dashboardId: 'test' },
        selectedDashboardName: 'Test',
      };

      const action = { type: actions.reset.type };
      const state = reducer(modifiedState, action);

      expect(state).toEqual(initialState);
    });
  });
});
