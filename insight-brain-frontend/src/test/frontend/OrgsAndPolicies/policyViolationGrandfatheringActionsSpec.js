/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { omit } from 'ramda';
import { actions } from 'MainRoot/OrgsAndPolicies/policyViolationGrandfatheringSlice';
import * as routerSelectors from 'MainRoot/reduxUiRouter/routerSelectors';

import { getGrandfatheringUrl } from 'MainRoot/util/CLMLocation';
import { SUBMIT_MASK_SUCCESS_VISIBLE_TIME_MS } from '@sonatype/react-shared-components';
import { axiosMockAdapter } from 'TestRoot/SpecUtil';

describe('policyViolationGrandfatheringActions', () => {
  const grandfatheringUrl = getGrandfatheringUrl('organization', 'ROOT_ORGANIZATION_ID');
  let store, state, setGrandfatheringStatus, mock;
  beforeAll(() => {
    mock = axiosMockAdapter();
  });

  beforeEach(function () {
    setGrandfatheringStatus = actions.setGrandfatheringStatus;
    state = {
      router: {
        currentParams: {
          organizationId: 'ROOT_ORGANIZATION_ID',
        },
        currentState: {
          name: 'management.edit.organization.violation-grandfathering-policy-react',
          url: '/grandfatheringReact',
        },
      },
      orgsAndPolicies: {
        policyViolationGrandfathering: {
          isDirty: false,
          loadError: null,
          loading: false,
          submitError: null,
          submitMaskState: null,
          data: {
            allowChange: true,
            allowOverride: true,
            enabled: false,
            inheritedFromOrganizationName: null,
          },
          serverData: {
            allowChange: true,
            allowOverride: true,
            enabled: false,
            inheritedFromOrganizationName: null,
          },
        },
      },
    };
    store = SpecUtil.mockReduxStore(state);

    spyOn(routerSelectors, 'selectRouterCurrentParams').and.returnValue({
      applicationPublicId: 'application',
    });
  });

  describe('loadPolicyViolationGrandfathering/fulfilled', () => {
    it('loads policy violation grandfathering successfully', (done) => {
      mock.onGet(grandfatheringUrl).reply(200, {});

      const expectedPendingAction = {
        type: 'policyViolationGrandfathering/loadPolicyViolationGrandfathering/pending',
      };

      const expectedFulfilledAction = {
        type: 'policyViolationGrandfathering/loadPolicyViolationGrandfathering/fulfilled',
        payload: {},
      };

      store.dispatch(actions.loadPolicyViolationGrandfathering()).then(() => {
        const actions = store.getActions().map((action) => omit(['meta', 'error'], action));
        expect(actions).toHaveActionsInOrder([expectedPendingAction, expectedFulfilledAction]);
        done();
      });
    });

    it('dispatches rejected action if loadPolicyViolationGrandfathering request fails', (done) => {
      mock.onGet(grandfatheringUrl).reply(() => Promise.reject('Some error'));

      const expectedPendingAction = {
        type: 'policyViolationGrandfathering/loadPolicyViolationGrandfathering/pending',
      };

      const expectedFailedAction = {
        type: 'policyViolationGrandfathering/loadPolicyViolationGrandfathering/rejected',
        payload: 'Some error',
      };

      store.dispatch(actions.loadPolicyViolationGrandfathering()).then(() => {
        const actions = store.getActions().map((action) => omit(['meta', 'error'], action));
        expect(actions).toHaveActionsInOrder([expectedPendingAction, expectedFailedAction]);
        done();
      });
    });
  });

  describe('savePolicyViolationGrandfathering', () => {
    beforeEach(function () {
      state = {
        router: {
          currentParams: {
            organizationId: 'ROOT_ORGANIZATION_ID',
          },
          currentState: {
            name: 'management.edit.organization.violation-grandfathering-policy-react',
            url: '/grandfatheringReact',
          },
        },
        orgsAndPolicies: {
          policyViolationGrandfathering: {
            isDirty: false,
            loadError: null,
            loading: false,
            submitError: null,
            submitMaskState: null,
            data: {
              allowChange: true,
              allowOverride: false,
              enabled: true,
              inheritedFromOrganizationName: null,
            },
            serverData: {
              allowChange: true,
              allowOverride: true,
              enabled: false,
              inheritedFromOrganizationName: null,
            },
          },
        },
      };
      store = SpecUtil.mockReduxStore(state);
    });

    beforeEach(() => {
      jasmine.clock().install();
    });

    afterEach(() => {
      jasmine.clock().uninstall();
    });

    it('saves policy violation grandfathering successfully', (done) => {
      const putData = {
        allowOverride: false,
        enabled: true,
      };

      const putDataResponse = {
        enabled: true,
        inheritedFromOrganizationName: null,
        allowOverride: false,
        allowChange: false,
      };

      mock.onPut(grandfatheringUrl, putData).reply(200, {
        data: putDataResponse,
      });

      store.dispatch(actions.savePolicyViolationGrandfathering()).then(() => {
        jasmine.clock().tick(SUBMIT_MASK_SUCCESS_VISIBLE_TIME_MS);
        expect(mock.history.put.length).toBe(1);
        expect(mock.history.put[0].url).toBe(
          '/rest/policyViolationGrandfathering/organization/ROOT_ORGANIZATION_ID',
          putData
        );
        expect(mock.history.put[0].data).toEqual(JSON.stringify(putData));
        const actions = store.getActions();
        expect(actions.length).toBe(3);
        expect(actions).toHaveActionTypesInOrder([
          'policyViolationGrandfathering/savePolicyViolationGrandfathering/pending',
          'policyViolationGrandfathering/savePolicyViolationGrandfathering/fulfilled',
          'policyViolationGrandfathering/saveMaskTimerDone',
        ]);
        expect(actions[0].payload).toEqual(undefined);
        expect(actions[1].payload).toEqual({
          data: putDataResponse,
        });
        done();
      });
    });

    it('dispatches rejected action if save request fails', (done) => {
      const putData = {
        allowOverride: false,
        enabled: true,
      };
      mock.onPut(grandfatheringUrl, putData).reply(() => Promise.reject('could not save updates'));

      store.dispatch(actions.savePolicyViolationGrandfathering()).then(() => {
        expect(mock.history.put.length).toBe(1);
        expect(mock.history.put[0].url).toBe(
          '/rest/policyViolationGrandfathering/organization/ROOT_ORGANIZATION_ID',
          putData
        );

        const actions = store.getActions();

        expect(actions.length).toBe(2);
        expect(actions).toHaveActionTypesInOrder([
          'policyViolationGrandfathering/savePolicyViolationGrandfathering/pending',
          'policyViolationGrandfathering/savePolicyViolationGrandfathering/rejected',
        ]);

        expect(actions[1].payload).toEqual('could not save updates');

        done();
      });
    });
  });

  describe('setGrandfatheringStatus action', () => {
    it('setGrandfatheringStatus seted successfully', () => {
      store.dispatch(setGrandfatheringStatus('inherit'));
      const setAction = store.getActions();
      expect(setAction.length).toBe(1);
      expect(setAction).toHaveAction({
        type: 'policyViolationGrandfathering/setGrandfatheringStatus',
        payload: 'inherit',
      });
    });
  });
});
