/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import axios from 'axios';

import { actions } from '../../../../main/frontend/componentDetails/ViolationsTableTile/policyViolationsSlice';
import {
  getComponentWaivers,
  getProductFeaturesUrl,
  getReportPolicyThreatsUrl,
} from '../../../../main/frontend/util/CLMLocation';
import { omit } from 'ramda';
import { getPermissionContextTestUrl } from '../../../../main/frontend/util/CLMContextLocation';

import 'TestRoot/SpecUtil';

describe('componentDetailsPolicyViolationsActions', () => {
  const mockAxiosCalls = SpecUtil.axiosMockerGenerator(axios);
  let store, state;

  beforeEach(function () {
    state = {
      router: {
        currentParams: {
          publicId: 'appPublicId',
          scanId: 'currentScanId',
          hash: 'currentComponentHash',
        },
      },
      applicationReport: { metadata: { application: { id: 'internalAppId' } } },
    };
    store = SpecUtil.mockReduxStore(state);
  });

  describe('load', () => {
    const { load } = actions;

    it('immediately dispatches a componentDetailsPolicyViolations/load/pending action and appropriate requests', () => {
      mockAxiosCalls({
        get: {
          [getReportPolicyThreatsUrl('appPublicId', 'currentScanId')]: Promise.resolve({}),
          [getComponentWaivers('application', 'appPublicId', 'currentComponentHash')]: Promise.resolve({}),
          [getProductFeaturesUrl()]: Promise.resolve({ data: [] }),
        },
        put: {
          [getPermissionContextTestUrl('application', 'internalAppId')]: Promise.resolve({
            data: ['WAIVE_POLICY_VIOLATIONS'],
          }),
        },
      });

      store.dispatch(load());

      const actions = store.getActions();
      expect(actions).toHaveAction({
        type: 'componentDetailsPolicyViolations/load/pending',
      });
      expect(axios.get).toHaveBeenCalledTimes(3);
      expect(axios.get).toHaveBeenCalledWith('/rest/report/appPublicId/currentScanId/browseReport/policythreats.json');
      expect(axios.get).toHaveBeenCalledWith(
        '/rest/policyWaiver/application/appPublicId/component/currentComponentHash'
      );
      expect(axios.get).toHaveBeenCalledWith('/rest/product/features');
    });

    it('dispatches a componentDetailsPolicyViolations/load/fulfilled action after successful requests', (done) => {
      const violationData = [{ policyViolationId: 'violation1' }];
      const waiversData = [{ id: 'waiver1' }];
      mockAxiosCalls({
        get: {
          [getReportPolicyThreatsUrl('appPublicId', 'currentScanId')]: Promise.resolve({ data: violationData }),
          [getComponentWaivers('application', 'appPublicId', 'currentComponentHash')]: Promise.resolve({
            data: waiversData,
          }),
          [getProductFeaturesUrl()]: Promise.resolve({ data: [] }),
        },
        put: {
          [getPermissionContextTestUrl('application', 'internalAppId')]: Promise.resolve({
            data: [],
          }),
        },
      });

      const expectedPendingAction = {
        type: 'componentDetailsPolicyViolations/load/pending',
      };
      const expectedFulfilledAction = {
        type: 'componentDetailsPolicyViolations/load/fulfilled',
        payload: {
          violationsResult: violationData,
          waiversResult: waiversData,
          permissionResult: false,
          innerSourceTransitiveWaiver: false,
          hash: 'currentComponentHash',
        },
      };

      store.dispatch(load()).then(() => {
        // Remove metadata and custom error information from redux toolkit before comparisons
        const actions = store.getActions().map((action) => omit(['meta', 'error'], action));
        expect(actions).toHaveActionsInOrder([expectedPendingAction, expectedFulfilledAction]);
        done();
      });
    });

    it('sets innerSourceTransitiveWaiver to true if the features includes it', (done) => {
      const violationData = [{ policyViolationId: 'violation1' }];
      const waiversData = [{ id: 'waiver1' }];
      mockAxiosCalls({
        get: {
          [getReportPolicyThreatsUrl('appPublicId', 'currentScanId')]: Promise.resolve({ data: violationData }),
          [getComponentWaivers('application', 'appPublicId', 'currentComponentHash')]: Promise.resolve({
            data: waiversData,
          }),
          [getProductFeaturesUrl()]: Promise.resolve({ data: ['inner-source-transitive-waiver'] }),
        },
        put: {
          [getPermissionContextTestUrl('application', 'internalAppId')]: Promise.resolve({
            data: [],
          }),
        },
      });

      const expectedPendingAction = {
        type: 'componentDetailsPolicyViolations/load/pending',
      };
      const expectedFulfilledAction = {
        type: 'componentDetailsPolicyViolations/load/fulfilled',
        payload: {
          violationsResult: violationData,
          waiversResult: waiversData,
          permissionResult: false,
          innerSourceTransitiveWaiver: true,
          hash: 'currentComponentHash',
        },
      };

      store.dispatch(load()).then(() => {
        // Remove metadata and custom error information from redux toolkit before comparisons
        const actions = store.getActions().map((action) => omit(['meta', 'error'], action));
        expect(actions).toHaveActionsInOrder([expectedPendingAction, expectedFulfilledAction]);
        done();
      });
    });

    it('dispatches a componentDetailsPolicyViolations/load/rejected action after an error occurs in the requests', (done) => {
      mockAxiosCalls({
        get: {
          [getReportPolicyThreatsUrl('appPublicId', 'currentScanId')]: () => Promise.reject('errorMessage'),
          [getComponentWaivers('application', 'appPublicId', 'currentComponentHash')]: Promise.resolve({
            data: [{ id: 'waiver1' }],
          }),
          [getProductFeaturesUrl()]: Promise.resolve({ data: [] }),
        },
        put: {
          [getPermissionContextTestUrl('application', 'internalAppId')]: Promise.resolve({
            data: ['WAIVE_POLICY_VIOLATIONS'],
          }),
        },
      });

      const expectedPendingAction = {
        type: 'componentDetailsPolicyViolations/load/pending',
      };
      const expectedFailedAction = {
        type: 'componentDetailsPolicyViolations/load/rejected',
        payload: 'errorMessage',
      };

      store.dispatch(load()).then(() => {
        // Remove metadata and custom error information from redux toolkit before comparisons
        const actions = store.getActions().map((action) => omit(['meta', 'error'], action));
        expect(actions).toHaveActionsInOrder([expectedPendingAction, expectedFailedAction]);
        done();
      });
    });
  });

  describe('goToWaivers', () => {
    const { goToWaivers } = actions;

    it('dispatches a @@reduxUiRouter/stateGo action to `applicationReport.violationWaivers`', () => {
      store.dispatch(goToWaivers('policyViolationId'));

      const actions = store.getActions();
      expect(actions).toHaveAction({
        type: '@@reduxUiRouter/stateGo',
        payload: {
          to: 'applicationReport.violationWaivers',
          params: { hash: 'currentComponentHash', violationId: 'policyViolationId' },
          options: undefined,
        },
      });
    });
  });
});
