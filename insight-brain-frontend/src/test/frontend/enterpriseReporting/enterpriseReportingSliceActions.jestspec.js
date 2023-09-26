/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

import axios from 'axios';
import { initialState } from 'MainRoot/enterpriseReporting/enterpriseReportingSlice';
import { axiosMockAdapter } from 'TestRoot/SpecUtil';
import { actions } from 'MainRoot/enterpriseReporting/enterpriseReportingSlice';
import { omit } from 'ramda';
import { getEnterpriseReportingUrl } from 'MainRoot/util/CLMLocation';

describe('enterpriseReportingSliceAction', () => {
  // eslint-disable-next-line no-unused-vars
  let store, state, mock;

  beforeEach(() => {
    state = initialState;
    store = SpecUtil.mockReduxStore(state);
  });

  describe('load', () => {
    it('immediately dispatches a enterpriseReporting/load/pending', (done) => {
      jest.spyOn(axios, 'post').mockResolvedValue({
        url: 'http://sonatypeinstance.looker.com',
        baseUrl: 'http://sonatypeinstance.looker.com',
      });
      store.dispatch(actions.load()).then(() => {
        const actions = store.getActions();
        expect(actions).toHaveAction({
          type: 'enterpriseReporting/load/pending',
        });
        done();
      });
    });

    it('dispatches enterpriseReporting/load/rejected on loading error', (done) => {
      const errorMessage = 'error on load';
      jest.spyOn(axios, 'post').mockRejectedValue(errorMessage);

      store.dispatch(actions.load()).then(() => {
        const actions = store.getActions();
        expect(actions[0].type).toBe('enterpriseReporting/load/pending');
        expect(actions[1].type).toBe('enterpriseReporting/load/rejected');
        expect(actions[1].payload).toEqual(errorMessage);
        done();
      });
    });

    it('dispatches enterpriseReporting/load/fulfilled', (done) => {
      const ssoEmbedUrl = { url: 'http://sonatypeinstance.looker.com', baseUrl: 'http://sonatypeinstance.looker.com' };

      mock = axiosMockAdapter();
      mock.onPost(`${getEnterpriseReportingUrl()}`, { dashboard: 'rolling-recap' }).reply(200, ssoEmbedUrl);

      store.dispatch(actions.load()).then(() => {
        const actions = store.getActions().map((action) => omit(['meta', 'error'], action));
        expect(actions[0].type).toBe('enterpriseReporting/load/pending');
        expect(actions[1]).toEqual({
          type: 'enterpriseReporting/load/fulfilled',
          payload: { url: ssoEmbedUrl.url, baseUrl: ssoEmbedUrl.baseUrl },
        });
        done();
      });
    });
  });
});
