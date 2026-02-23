/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import axios from 'axios';
import { actions } from 'MainRoot/OrgsAndPolicies/policyMonitoringSlice';
import * as routerSelectors from 'MainRoot/reduxUiRouter/routerSelectors';
import * as productFeaturesSelectors from 'MainRoot/productFeatures/productFeaturesSelectors';
import 'TestRoot/SpecUtil';

import {
  getPolicyMonitoringUrl,
  getApplicablePolicyMonitoringUrl,
  getProductFeaturesUrl,
  getSbomStageUrl,
} from 'MainRoot/util/CLMLocation';
import { SUBMIT_MASK_SUCCESS_VISIBLE_TIME_MS } from '@sonatype/react-shared-components';

import { getCliStageUrl } from 'MainRoot/util/CLMLocation';

const responseData = [
  { stageTypeId: 1, stageName: 'name 1' },
  { stageTypeId: 2, stageName: 'name 2' },
  { stageTypeId: 3, stageName: 'name 3' },
];

const responseDataSbom = [{ stageTypeId: 'compliance', stageName: 'Compliance' }];

describe('policyMonitoringActions', () => {
  const mockAxiosCalls = SpecUtil.axiosMockerGenerator(axios);
  const policyMonitoringUrl = getPolicyMonitoringUrl('application', 'application');
  const applicablePolicyMonitoringUrl = getApplicablePolicyMonitoringUrl('application', 'application');
  const cliStageUrl = getCliStageUrl('application', 'application');
  const sbomStageUrl = getSbomStageUrl('application', 'application');
  let store, state, productFeaturesSpy;

  beforeEach(function () {
    jest.useFakeTimers();
    state = {
      router: {
        currentParams: {
          applicationPublicId: 'application',
        },
      },
      orgsAndPolicies: {
        policyMonitoring: {
          monitoredStage: {},
          originalStage: {},
        },
      },
      stages: {
        cli: {
          stageTypes: null,
        },
        sbom: {
          stageTypes: null,
        },
      },
    };
    store = SpecUtil.mockReduxStore(state);

    jest.spyOn(routerSelectors, 'selectRouterCurrentParams').mockReturnValue({
      applicationPublicId: 'application',
    });
    productFeaturesSpy = jest.spyOn(productFeaturesSelectors, 'selectProductFeatures').mockReturnValue({});
  });
  afterEach(function () {
    jest.useRealTimers();
  });

  describe('loadApplicablePolicyMonitoring', () => {
    it('loads policy monitoring successfully', (done) => {
      mockAxiosCalls({
        get: {
          [applicablePolicyMonitoringUrl]: Promise.resolve({ data: {} }),
          [getProductFeaturesUrl()]: Promise.resolve({
            data: ['enforcement', 'firewall', 'policy-monitoring', 'policy-grandfathering'],
          }),
          [cliStageUrl]: Promise.resolve({ data: responseData }),
          [sbomStageUrl]: Promise.resolve({ data: responseDataSbom }),
        },
      });

      store.dispatch(actions.loadApplicablePolicyMonitoring()).then(() => {
        expect(axios.get).toHaveBeenCalledTimes(3);
        expect(axios.get).toHaveBeenCalledWith('/rest/policyMonitoring/application/application/applicable');

        const actions = store.getActions();
        expect(actions.length).toBe(6);
        expect(actions).toHaveActionTypesInOrder([
          'policyMonitoring/loadApplicablePolicyMonitoring/pending',
          'stages/loadStageTypes/pending',
          'stages/loadStageTypes/pending',
          'stages/loadStageTypes/fulfilled',
          'stages/loadStageTypes/fulfilled',
          'policyMonitoring/loadApplicablePolicyMonitoring/fulfilled',
        ]);
        done();
      });
    });

    it('dispatches rejected action if loadApplicablePolicyMonitoring request fails', (done) => {
      productFeaturesSpy.mockReturnValue({
        enforcement: true,
        firewall: true,
        'policy-monitoring': true,
        'policy-grandfathering': true,
      });
      mockAxiosCalls({
        get: {
          [applicablePolicyMonitoringUrl]: () => Promise.reject('something went wrong'),
          [cliStageUrl]: Promise.resolve({ data: responseData }),
          [sbomStageUrl]: Promise.resolve({ data: responseDataSbom }),
        },
      });

      store.dispatch(actions.loadApplicablePolicyMonitoring()).then(() => {
        expect(axios.get).toHaveBeenCalledTimes(3);
        expect(axios.get).toHaveBeenCalledWith('/rest/policyMonitoring/application/application/applicable');
        const actions = store.getActions();

        expect(actions.length).toBe(6);
        expect(actions).toHaveActionTypesInOrder([
          'policyMonitoring/loadApplicablePolicyMonitoring/pending',
          'stages/loadStageTypes/pending',
          'stages/loadStageTypes/pending',
          'stages/loadStageTypes/fulfilled',
          'stages/loadStageTypes/fulfilled',
          'policyMonitoring/loadApplicablePolicyMonitoring/rejected',
        ]);
        done();
      });
    });
  });

  describe('savePolicyMonitoring', () => {
    it('saves policy monitoring successfully', (done) => {
      mockAxiosCalls({
        put: { [policyMonitoringUrl]: Promise.resolve({ data: {} }) },
        get: {
          [applicablePolicyMonitoringUrl]: Promise.resolve({ data: {} }),
          [getProductFeaturesUrl()]: Promise.resolve({
            data: ['enforcement', 'firewall', 'policy-monitoring', 'policy-grandfathering'],
          }),
          [cliStageUrl]: Promise.resolve({ data: responseData }),
          [sbomStageUrl]: Promise.resolve({ data: responseDataSbom }),
        },
      });

      store.dispatch(actions.savePolicyMonitoring({})).then(() => {
        expect(axios.put).toHaveBeenCalledTimes(1);
        expect(axios.put).toHaveBeenCalledWith('/rest/policyMonitoring/application/application', {});

        const actions = store.getActions();
        expect(actions.length).toBe(8);
        expect(actions).toHaveActionTypesInOrder([
          'policyMonitoring/savePolicyMonitoring/pending',
          'policyMonitoring/loadApplicablePolicyMonitoring/pending',
          'stages/loadStageTypes/pending',
          'stages/loadStageTypes/pending',
          'policyMonitoring/savePolicyMonitoring/fulfilled',
          'stages/loadStageTypes/fulfilled',
          'stages/loadStageTypes/fulfilled',
          'policyMonitoring/loadApplicablePolicyMonitoring/fulfilled',
        ]);
        expect(actions[0].payload).toEqual(undefined);
        expect(actions[4].payload).toEqual({});

        jest.advanceTimersByTime(SUBMIT_MASK_SUCCESS_VISIBLE_TIME_MS);
        expect(actions.length).toBe(9);
        expect(actions).toHaveActionTypesInOrder([
          'policyMonitoring/savePolicyMonitoring/pending',
          'policyMonitoring/loadApplicablePolicyMonitoring/pending',
          'stages/loadStageTypes/pending',
          'stages/loadStageTypes/pending',
          'policyMonitoring/savePolicyMonitoring/fulfilled',
          'stages/loadStageTypes/fulfilled',
          'stages/loadStageTypes/fulfilled',
          'policyMonitoring/loadApplicablePolicyMonitoring/fulfilled',
          'policyMonitoring/saveMaskTimerDone',
        ]);

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
      mockAxiosCalls({
        del: { [policyMonitoringUrl]: Promise.resolve({ data: {} }) },
        get: {
          [applicablePolicyMonitoringUrl]: Promise.resolve({ data: {} }),
          [getProductFeaturesUrl()]: Promise.resolve({
            data: ['enforcement', 'firewall', 'policy-monitoring', 'policy-grandfathering'],
          }),
          [cliStageUrl]: Promise.resolve({ data: responseData }),
          [sbomStageUrl]: Promise.resolve({ data: responseDataSbom }),
        },
      });

      store.dispatch(actions.removePolicyMonitoring()).then(() => {
        expect(axios.delete).toHaveBeenCalledTimes(1);
        expect(axios.delete).toHaveBeenCalledWith('/rest/policyMonitoring/application/application');

        const actions = store.getActions();
        expect(actions.length).toBe(8);
        expect(actions).toHaveActionTypesInOrder([
          'policyMonitoring/removePolicyMonitoring/pending',
          'policyMonitoring/loadApplicablePolicyMonitoring/pending',
          'stages/loadStageTypes/pending',
          'stages/loadStageTypes/pending',
          'policyMonitoring/removePolicyMonitoring/fulfilled',
          'stages/loadStageTypes/fulfilled',
          'stages/loadStageTypes/fulfilled',
          'policyMonitoring/loadApplicablePolicyMonitoring/fulfilled',
        ]);

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
