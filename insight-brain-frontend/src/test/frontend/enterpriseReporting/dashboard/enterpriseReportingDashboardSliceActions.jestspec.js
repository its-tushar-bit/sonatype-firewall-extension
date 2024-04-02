/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { actions, initialState } from 'MainRoot/enterpriseReporting/dashboard/enterpriseReportingDashboardSlice';
import { axiosMockAdapter } from 'TestRoot/SpecUtil';
import { omit } from 'ramda';
import { getEnterpriseReportingEmbedUrl, getProductFeaturesUrl } from 'MainRoot/util/CLMLocation';

describe('enterpriseReportingDashboardSliceAction', () => {
  let store, state, axiosMock;

  const ssoEmbedUrl = { url: 'http://sonatypeinstance.looker.com', baseUrl: 'http://sonatypeinstance.looker.com' };

  beforeEach(() => {
    state = { enterpriseReportingDashboard: initialState };
    store = SpecUtil.mockReduxStore(state);
    axiosMock = axiosMockAdapter();
    axiosMock.onGet(getProductFeaturesUrl()).reply(200, ['integrated-enterprise-reporting']);
  });

  describe('load', () => {
    it('immediately dispatches a enterpriseReportingDashboard/load/pending', (done) => {
      axiosMock.onPost(getEnterpriseReportingEmbedUrl()).reply(200, ssoEmbedUrl);
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
      axiosMock.onPost(getEnterpriseReportingEmbedUrl()).reply(409, errorMessage);

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
      const dashboardId = 'rolling-recap';
      axiosMock.onPost(getEnterpriseReportingEmbedUrl()).reply(200, ssoEmbedUrl);

      store.dispatch(actions.load(dashboardId)).then(() => {
        const actions = store.getActions().map((action) => omit(['meta', 'error'], action));
        expect(actions[0].type).toBe('enterpriseReportingDashboard/load/pending');
        expect(actions[1].type).toBe('productFeatures/fetchProductFeaturesIfNeeded/pending');
        expect(actions[2].type).toBe('productFeatures/fetchProductFeaturesIfNeeded/fulfilled');
        expect(actions[3]).toEqual({
          type: 'enterpriseReportingDashboard/load/fulfilled',
          payload: { url: ssoEmbedUrl.url, baseUrl: ssoEmbedUrl.baseUrl },
        });
        done();
      });
    });
  });
});
