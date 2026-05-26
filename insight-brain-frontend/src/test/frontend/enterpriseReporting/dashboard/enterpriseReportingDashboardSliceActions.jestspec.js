/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { actions, initialState } from 'MainRoot/enterpriseReporting/dashboard/enterpriseReportingDashboardSlice';
import { axiosMockAdapter } from 'TestRoot/SpecUtil';
import { omit } from 'ramda';
import {
  getEnterpriseReportingBaseUrl,
  getEnterpriseReportingDashboardsUrl,
  getIqVersion,
  getProductFeaturesUrl,
} from 'MainRoot/util/CLMLocation';
import { mockData } from '../enterpriseReportingMockData';

describe('enterpriseReportingDashboardSliceAction', () => {
  let store, state, axiosMock;

  const baseUrl = 'http://sonatypeinstance.looker.com';

  beforeEach(() => {
    state = { enterpriseReportingDashboard: initialState };
    store = SpecUtil.mockReduxStore(state);
    axiosMock = axiosMockAdapter();
    axiosMock.onGet(getProductFeaturesUrl()).reply(200, ['integrated-enterprise-reporting']);
    axiosMock.onGet(getIqVersion()).reply(200, { version: '1.204.0' });
  });

  describe('load', () => {
    it('immediately dispatches a enterpriseReportingDashboard/load/pending', (done) => {
      axiosMock.onGet(getEnterpriseReportingDashboardsUrl()).reply(200, mockData);
      axiosMock.onGet(getEnterpriseReportingBaseUrl()).reply(200, baseUrl);
      store.dispatch(actions.load()).then(() => {
        const actions = store.getActions();
        expect(actions).toHaveAction({
          type: 'enterpriseReportingDashboard/load/pending',
        });
        done();
      });
    });

    it('dispatches enterpriseReportingDashboard/load/rejected on loading error', (done) => {
      const errorMessage = 'error on load';
      axiosMock.onGet(getEnterpriseReportingDashboardsUrl()).reply(200, mockData);
      axiosMock.onGet(getEnterpriseReportingBaseUrl()).reply(409, errorMessage);

      store.dispatch(actions.load()).then(() => {
        const actions = store.getActions();
        expect(actions[0].type).toBe('enterpriseReportingDashboard/load/pending');
        expect(actions[1].type).toBe('productFeatures/fetchProductFeaturesIfNeeded/pending');
        expect(actions[2].type).toBe('productFeatures/fetchProductFeaturesIfNeeded/fulfilled');
        expect(actions[3].type).toBe('enterpriseReportingDashboard/load/rejected');
        expect(actions[3].payload.response.data).toEqual(errorMessage);
        done();
      });
    });

    it('dispatches enterpriseReportingDashboard/load/fulfilled', (done) => {
      axiosMock.onGet(getEnterpriseReportingBaseUrl()).reply(200, baseUrl);
      axiosMock.onGet(getEnterpriseReportingDashboardsUrl()).reply(200, mockData);

      store.dispatch(actions.load()).then(() => {
        const actions = store.getActions().map((action) => omit(['meta', 'error'], action));
        expect(actions[0].type).toBe('enterpriseReportingDashboard/load/pending');
        expect(actions[1].type).toBe('productFeatures/fetchProductFeaturesIfNeeded/pending');
        expect(actions[2].type).toBe('productFeatures/fetchProductFeaturesIfNeeded/fulfilled');
        expect(actions[3].type).toBe('enterpriseReportingDashboard/load/fulfilled');
        expect(actions[3]).toEqual({
          type: 'enterpriseReportingDashboard/load/fulfilled',
          payload: { baseUrl: baseUrl, dashboards: mockData, iqVersion: '1.204.0' },
        });
        done();
      });
    });

    it('dispatches load/fulfilled with iqVersion null when version endpoint fails', (done) => {
      axiosMock.onGet(getEnterpriseReportingBaseUrl()).reply(200, baseUrl);
      axiosMock.onGet(getEnterpriseReportingDashboardsUrl()).reply(200, mockData);
      axiosMock.onGet(getIqVersion()).reply(500);

      store.dispatch(actions.load()).then(() => {
        const dispatchedActions = store.getActions().map((action) => omit(['meta', 'error'], action));
        expect(dispatchedActions[3].type).toBe('enterpriseReportingDashboard/load/fulfilled');
        expect(dispatchedActions[3].payload.iqVersion).toBeNull();
        done();
      });
    });
  });
});
