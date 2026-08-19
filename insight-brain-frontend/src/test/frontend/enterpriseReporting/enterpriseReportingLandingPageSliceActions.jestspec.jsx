/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { actions, initialState } from 'MainRoot/enterpriseReporting/enterpriseReportingLandingPageSlice';
import { omit } from 'ramda';
import '../SpecUtil';
import { axiosMockAdapter } from 'TestRoot/SpecUtil';
import { getEnterpriseReportingDashboardsUrl, getProductFeaturesUrl, getIqVersion } from 'MainRoot/util/CLMLocation';

describe('enterpriseReportingLandingPageSliceAction', () => {
  let store, axiosMock;

  beforeEach(() => {
    axiosMock = axiosMockAdapter();
    store = SpecUtil.mockReduxStore(initialState);
    axiosMock.onGet(getProductFeaturesUrl()).reply(200, ['integrated-reportin-enterprise']);
  });

  describe('load', () => {
    it('immediately dispatches a enterpriseReportingLandingPage/load/pending', (done) => {
      store.dispatch(actions.load()).then(() => {
        const actions = store.getActions();
        expect(actions).toHaveAction({
          type: 'enterpriseReportingLandingPage/load/pending',
        });
        done();
      });
    });

    it('dispatches enterpriseReportingLandingPage/load/rejected on loading error', (done) => {
      const errorMessage = 'error on load';
      axiosMock.onGet(getEnterpriseReportingDashboardsUrl()).reply(500, errorMessage);
      axiosMock.onGet(getIqVersion()).reply(200, '1.188.0-SNAPSHOT');

      store.dispatch(actions.load()).then(() => {
        const actions = store.getActions();
        expect(actions[0].type).toBe('enterpriseReportingLandingPage/load/pending');
        expect(actions[1].type).toBe('productFeatures/fetchProductFeaturesIfNeeded/pending');
        expect(actions[2].type).toBe('productFeatures/fetchProductFeaturesIfNeeded/fulfilled');
        expect(actions[3].type).toBe('enterpriseReportingLandingPage/load/rejected');
        expect(actions[3].payload.response.data).toEqual(errorMessage);
        done();
      });
    });

    it('dispatches enterpriseReportingLandingPage/load/fulfilled', (done) => {
      const dashboardResponse = {
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
        ],
        dashboardGroupMetadata: [],
      };
      const iqVersionResponse = { version: '1.188.0-SNAPSHOT' };

      axiosMock.onGet(getEnterpriseReportingDashboardsUrl()).reply(200, dashboardResponse);
      axiosMock.onGet(getIqVersion()).reply(200, iqVersionResponse);

      store.dispatch(actions.load()).then(() => {
        const actions = store.getActions().map((action) => omit(['meta', 'error'], action));
        expect(actions[0].type).toBe('enterpriseReportingLandingPage/load/pending');
        expect(actions[1].type).toBe('productFeatures/fetchProductFeaturesIfNeeded/pending');
        expect(actions[2].type).toBe('productFeatures/fetchProductFeaturesIfNeeded/fulfilled');
        expect(actions[3]).toEqual({
          type: 'enterpriseReportingLandingPage/load/fulfilled',
          payload: {
            dashboardsData: dashboardResponse,
            iqVersion: iqVersionResponse.version,
          },
        });
        done();
      });
    });
  });
});
