/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import axios from 'axios';
import { actions } from 'MainRoot/productFeatures/productFeaturesSlice';
import {
  getProductFeaturesUrl,
  getQuarantinedComponentViewAnonymousAccessEnabledState,
} from 'MainRoot/util/CLMLocation';
import * as productFeaturesSelectors from 'MainRoot/productFeatures/productFeaturesSelectors';

import 'TestRoot/SpecUtil';

describe('productFeaturesActions', () => {
  const mockAxiosCalls = SpecUtil.axiosMockerGenerator(axios);
  let store, state;

  beforeEach(() => {
    state = {
      productFeatures: { productFeatures: {} },
    };
    store = SpecUtil.mockReduxStore(state);
  });

  describe('fetchProductFeaturesIfNeeded', () => {
    let selectProductFeaturesSpy;
    beforeEach(() => {
      selectProductFeaturesSpy = jest.spyOn(productFeaturesSelectors, 'selectProductFeatures').mockReturnValue({});
    });

    it('load product features successfully if no product features were loaded before', (done) => {
      mockAxiosCalls({
        get: {
          [getProductFeaturesUrl()]: Promise.resolve({
            data: [
              'enforcement',
              'firewall',
              'policy-monitoring',
              'policy-grandfathering',
              'notifications',
              'webhooks-for-applications',
            ],
          }),
        },
      });

      store.dispatch(actions.fetchProductFeaturesIfNeeded()).then(() => {
        expect(axios.get).toHaveBeenCalledTimes(1);
        expect(axios.get).toHaveBeenCalledWith('/rest/product/features');

        const actions = store.getActions();
        expect(actions.length).toBe(4);
        expect(actions).toHaveActionTypesInOrder([
          'productFeatures/fetchProductFeaturesIfNeeded/pending',
          'productFeatures/fetchProductFeatures/pending',
          'productFeatures/fetchProductFeatures/fulfilled',
          'productFeatures/fetchProductFeaturesIfNeeded/fulfilled',
        ]);

        expect(actions[2].payload).toEqual([
          'enforcement',
          'firewall',
          'policy-monitoring',
          'policy-grandfathering',
          'notifications',
          'webhooks-for-applications',
        ]);
        expect(actions[3].payload).toEqual({
          enforcement: true,
          firewall: true,
          'policy-monitoring': true,
          'policy-grandfathering': true,
          notifications: true,
          'webhooks-for-applications': true,
        });

        done();
      });
    });

    it('does not load product features if product features data is available', (done) => {
      selectProductFeaturesSpy.mockReturnValue({
        enforcement: true,
        firewall: true,
        'policy-monitoring': true,
        'policy-grandfathering': true,
        notifications: true,
        'webhooks-for-applications': true,
      });

      store.dispatch(actions.fetchProductFeaturesIfNeeded()).then(() => {
        const actions = store.getActions();
        expect(actions.length).toBe(2);
        expect(actions).toHaveActionTypesInOrder([
          'productFeatures/fetchProductFeaturesIfNeeded/pending',
          'productFeatures/fetchProductFeaturesIfNeeded/fulfilled',
        ]);

        expect(actions[1].payload).toEqual(undefined);

        done();
      });
    });

    it('dispatches rejected action if load request fails', (done) => {
      mockAxiosCalls({
        get: {
          [getProductFeaturesUrl()]: () => Promise.reject('rejected'),
        },
      });

      store.dispatch(actions.fetchProductFeaturesIfNeeded()).then(() => {
        expect(axios.get).toHaveBeenCalledTimes(1);
        expect(axios.get).toHaveBeenCalledWith('/rest/product/features');

        const actions = store.getActions();
        expect(actions.length).toBe(4);

        expect(actions).toHaveActionTypesInOrder([
          'productFeatures/fetchProductFeaturesIfNeeded/pending',
          'productFeatures/fetchProductFeatures/pending',
          'productFeatures/fetchProductFeatures/rejected',
          'productFeatures/fetchProductFeaturesIfNeeded/rejected',
        ]);
        expect(actions[2].payload).toBe('rejected');
        expect(actions[2].payload).toBe('rejected');

        done();
      });
    });
  });

  describe('loadIsQuarantinedComponentViewAnonymousAccessEnabled', () => {
    it('does load flag configuration successfully', (done) => {
      mockAxiosCalls({
        get: {
          [getQuarantinedComponentViewAnonymousAccessEnabledState()]: Promise.resolve({
            data: true,
          }),
        },
      });

      store.dispatch(actions.loadIsQuarantinedComponentViewAnonymousAccessEnabled()).then(() => {
        expect(axios.get).toHaveBeenCalledTimes(1);
        expect(axios.get).toHaveBeenCalledWith(getQuarantinedComponentViewAnonymousAccessEnabledState());

        const actions = store.getActions();
        expect(actions.length).toBe(2);
        expect(actions).toHaveActionTypesInOrder([
          'productFeatures/loadIsQuarantinedComponentViewAnonymousAccessEnabled/pending',
          'productFeatures/loadIsQuarantinedComponentViewAnonymousAccessEnabled/fulfilled',
        ]);

        expect(actions[1].payload).toEqual(true);
        done();
      });
    });

    it('dispatches rejected action if load request fails', (done) => {
      mockAxiosCalls({
        get: {
          [getQuarantinedComponentViewAnonymousAccessEnabledState()]: () => Promise.reject('rejected'),
        },
      });

      store.dispatch(actions.loadIsQuarantinedComponentViewAnonymousAccessEnabled()).then(() => {
        expect(axios.get).toHaveBeenCalledTimes(1);
        expect(axios.get).toHaveBeenCalledWith(getQuarantinedComponentViewAnonymousAccessEnabledState());

        const actions = store.getActions();
        expect(actions.length).toBe(2);
        expect(actions).toHaveActionTypesInOrder([
          'productFeatures/loadIsQuarantinedComponentViewAnonymousAccessEnabled/pending',
          'productFeatures/loadIsQuarantinedComponentViewAnonymousAccessEnabled/rejected',
        ]);

        expect(actions[1].payload).toEqual('rejected');
        done();
      });
    });
  });
});
