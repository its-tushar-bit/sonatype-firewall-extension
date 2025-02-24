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
  getProductFeaturesUrl,
} from 'MainRoot/util/CLMLocation';

describe('enterpriseReportingDashboardSliceAction', () => {
  let store, state, axiosMock;

  const baseUrl = 'http://sonatypeinstance.looker.com';
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
    state = { enterpriseReportingDashboard: initialState };
    store = SpecUtil.mockReduxStore(state);
    axiosMock = axiosMockAdapter();
    axiosMock.onGet(getProductFeaturesUrl()).reply(200, ['integrated-enterprise-reporting']);
  });

  describe('load', () => {
    it('immediately dispatches a enterpriseReportingDashboard/load/pending', (done) => {
      axiosMock.onGet(getEnterpriseReportingDashboardsUrl()).reply(200, { dashboardMetadata: dashboardMetadata });
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
      axiosMock.onGet(getEnterpriseReportingDashboardsUrl()).reply(200, { dashboardMetadata: dashboardMetadata });
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
      axiosMock.onGet(getEnterpriseReportingDashboardsUrl()).reply(200, { dashboardMetadata: dashboardMetadata });

      store.dispatch(actions.load()).then(() => {
        const actions = store.getActions().map((action) => omit(['meta', 'error'], action));
        expect(actions[0].type).toBe('enterpriseReportingDashboard/load/pending');
        expect(actions[1].type).toBe('productFeatures/fetchProductFeaturesIfNeeded/pending');
        expect(actions[2].type).toBe('productFeatures/fetchProductFeaturesIfNeeded/fulfilled');
        expect(actions[3].type).toBe('enterpriseReportingDashboard/load/fulfilled');
        expect(actions[3]).toEqual({
          type: 'enterpriseReportingDashboard/load/fulfilled',
          payload: { baseUrl: baseUrl, dashboards: dashboardMetadata },
        });
        done();
      });
    });
  });
});
