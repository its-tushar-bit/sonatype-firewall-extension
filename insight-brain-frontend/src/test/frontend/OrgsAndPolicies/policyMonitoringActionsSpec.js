/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import axios from 'axios';
import { actions } from 'MainRoot/OrgsAndPolicies/policyMonitoringSlice';
import * as routerSelectors from 'MainRoot/reduxUiRouter/routerSelectors';
import * as productFeaturesSelectors from 'MainRoot/productFeatures/productFeaturesSelectors';
import {
  getPolicyMonitoringUrl,
  getApplicablePolicyMonitoringUrl,
  getProductFeaturesUrl,
} from 'MainRoot/util/CLMLocation';

describe('policyMonitoringActions', () => {
  const mockAxiosCalls = SpecUtil.axiosMockerGenerator(axios);
  const policyMonitoringUrl = getPolicyMonitoringUrl('application', 'application');
  const applicablePolicyMonitoringUrl = getApplicablePolicyMonitoringUrl('application', 'application');
  let store, state, productFeaturesSpy;

  beforeEach(function () {
    state = {
      router: {
        currentParams: {
          applicationPublicId: 'application',
        },
      },
      orgsAndPolicies: {
        policyMonitoring: {
          monitoredStage: {},
        },
      },
    };
    store = SpecUtil.mockReduxStore(state);
    spyOn(routerSelectors, 'selectRouterCurrentParams').and.returnValue({
      applicationPublicId: 'application',
    });
    productFeaturesSpy = spyOn(productFeaturesSelectors, 'selectProductFeatures').and.returnValue({});
  });

  describe('loadApplicablePolicyMonitoring', () => {
    it('loads policy monitoring successfully', (done) => {
      mockAxiosCalls({
        get: {
          [applicablePolicyMonitoringUrl]: Promise.resolve({ data: {} }),
          [getProductFeaturesUrl()]: Promise.resolve({
            data: ['enforcement', 'firewall', 'policy-monitoring', 'policy-grandfathering'],
          }),
        },
      });

      store.dispatch(actions.loadApplicablePolicyMonitoring()).then(() => {
        expect(axios.get).toHaveBeenCalledTimes(2);
        expect(axios.get).toHaveBeenCalledWith('/rest/policyMonitoring/application/application/applicable');
        expect(axios.get).toHaveBeenCalledWith('/rest/product/features');

        const actions = store.getActions();

        expect(actions.length).toBe(6);
        expect(actions).toHaveActionTypesInOrder([
          'policyMonitoring/loadApplicablePolicyMonitoring/pending',
          'productFeatures/fetchProductFeaturesIfNeeded/pending',
          'productFeatures/fetchProductFeatures/pending',
          'productFeatures/fetchProductFeatures/fulfilled',
          'productFeatures/fetchProductFeaturesIfNeeded/fulfilled',
          'policyMonitoring/loadApplicablePolicyMonitoring/fulfilled',
        ]);
        done();
      });
    });

    it('dispatches rejected action if loadApplicablePolicyMonitoring request fails', (done) => {
      productFeaturesSpy.and.returnValue({
        enforcement: true,
        firewall: true,
        'policy-monitoring': true,
        'policy-grandfathering': true,
      });
      mockAxiosCalls({ get: { [applicablePolicyMonitoringUrl]: () => Promise.reject('something went wrong') } });

      store.dispatch(actions.loadApplicablePolicyMonitoring()).then(() => {
        expect(axios.get).toHaveBeenCalledOnceWith('/rest/policyMonitoring/application/application/applicable');

        const actions = store.getActions();

        expect(actions.length).toBe(4);
        expect(actions).toHaveActionTypesInOrder([
          'policyMonitoring/loadApplicablePolicyMonitoring/pending',
          'productFeatures/fetchProductFeaturesIfNeeded/pending',
          'productFeatures/fetchProductFeaturesIfNeeded/fulfilled',
          'policyMonitoring/loadApplicablePolicyMonitoring/rejected',
        ]);
        done();
      });
    });
  });

  describe('savePolicyMonitoring', () => {
    it('saves policy monitoring successfully', (done) => {
      mockAxiosCalls({ put: { [policyMonitoringUrl]: Promise.resolve({ data: {} }) } });

      store.dispatch(actions.savePolicyMonitoring({})).then(() => {
        expect(axios.put).toHaveBeenCalledTimes(1);
        expect(axios.put).toHaveBeenCalledWith('/rest/policyMonitoring/application/application', {});

        const actions = store.getActions();

        expect(actions.length).toBe(2);
        expect(actions).toHaveActionTypesInOrder([
          'policyMonitoring/savePolicyMonitoring/pending',
          'policyMonitoring/savePolicyMonitoring/fulfilled',
        ]);

        expect(actions[1].payload).toEqual({});

        done();
      });
    });

    it('dispatches rejected action if save request fails', (done) => {
      mockAxiosCalls({ put: { [policyMonitoringUrl]: () => Promise.reject('could not save label') } });

      store.dispatch(actions.savePolicyMonitoring({})).then(() => {
        expect(axios.put).toHaveBeenCalledTimes(1);
        expect(axios.put).toHaveBeenCalledWith('/rest/policyMonitoring/application/application', {});

        const actions = store.getActions();

        expect(actions.length).toBe(2);
        expect(actions).toHaveActionTypesInOrder([
          'policyMonitoring/savePolicyMonitoring/pending',
          'policyMonitoring/savePolicyMonitoring/rejected',
        ]);

        expect(actions[1].payload).toEqual('could not save label');

        done();
      });
    });
  });

  describe('removePolicyMonitoring', () => {
    it('deletes policy monitoring successfully', (done) => {
      mockAxiosCalls({ del: { [policyMonitoringUrl]: Promise.resolve({ data: {} }) } });

      store.dispatch(actions.removePolicyMonitoring()).then(() => {
        expect(axios.delete).toHaveBeenCalledTimes(1);
        expect(axios.delete).toHaveBeenCalledWith('/rest/policyMonitoring/application/application');

        const actions = store.getActions();

        expect(actions.length).toBe(2);
        expect(actions).toHaveActionTypesInOrder([
          'policyMonitoring/removePolicyMonitoring/pending',
          'policyMonitoring/removePolicyMonitoring/fulfilled',
        ]);

        expect(actions[1].payload).toEqual({});

        done();
      });
    });

    it('dispatches rejected action if remove request fails', (done) => {
      mockAxiosCalls({ del: { [policyMonitoringUrl]: () => Promise.reject('could not remove label') } });

      store.dispatch(actions.removePolicyMonitoring()).then(() => {
        expect(axios.delete).toHaveBeenCalledTimes(1);
        expect(axios.delete).toHaveBeenCalledWith('/rest/policyMonitoring/application/application');

        const actions = store.getActions();

        expect(actions.length).toBe(2);
        expect(actions).toHaveActionTypesInOrder([
          'policyMonitoring/removePolicyMonitoring/pending',
          'policyMonitoring/removePolicyMonitoring/rejected',
        ]);

        expect(actions[1].payload).toBe('could not remove label');

        done();
      });
    });
  });
});
