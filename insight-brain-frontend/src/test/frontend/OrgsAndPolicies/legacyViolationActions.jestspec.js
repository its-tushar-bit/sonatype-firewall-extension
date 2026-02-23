/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { omit } from 'ramda';
import { actions } from 'MainRoot/OrgsAndPolicies/legacyViolationSlice';
import * as routerSelectors from 'MainRoot/reduxUiRouter/routerSelectors';

import { getLegacyViolationURL } from 'MainRoot/util/CLMLocation';
import { SUBMIT_MASK_SUCCESS_VISIBLE_TIME_MS } from '@sonatype/react-shared-components';
import { axiosMockAdapter } from 'TestRoot/SpecUtil';

import 'TestRoot/SpecUtil';

describe('legacViolationActions', () => {
  const legacyViolationUrl = getLegacyViolationURL('organization', 'ROOT_ORGANIZATION_ID');
  let store, state, setLegacyViolationStatus, mock;
  beforeAll(() => {
    mock = axiosMockAdapter();
  });

  beforeEach(function () {
    setLegacyViolationStatus = actions.setLegacyViolationStatus;
    state = {
      router: {
        currentParams: {
          organizationId: 'ROOT_ORGANIZATION_ID',
        },
        currentState: {
          name: 'management.edit.organization.legacy-violation-react',
          url: '/legacyViolationsReact',
        },
      },
      orgsAndPolicies: {
        legacyViolations: {
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

    jest.spyOn(routerSelectors, 'selectRouterCurrentParams').mockReturnValue({
      applicationPublicId: 'application',
    });
  });

  describe('loadLegacyViolation/fulfilled', () => {
    it('loads legacy violation successfully', (done) => {
      mock.onGet(legacyViolationUrl).reply(200, {});

      const expectedPendingAction = {
        type: 'legacyViolation/loadLegacyViolation/pending',
      };

      const expectedFulfilledAction = {
        type: 'legacyViolation/loadLegacyViolation/fulfilled',
        payload: {},
      };

      store.dispatch(actions.loadLegacyViolation()).then(() => {
        const actions = store.getActions().map((action) => omit(['meta', 'error'], action));
        expect(actions).toHaveActionsInOrder([expectedPendingAction, expectedFulfilledAction]);
        done();
      });
    });

    it('dispatches rejected action if loadLegacyViolation request fails', (done) => {
      mock.onGet(legacyViolationUrl).reply(() => Promise.reject('Some error'));

      const expectedPendingAction = {
        type: 'legacyViolation/loadLegacyViolation/pending',
      };

      const expectedFailedAction = {
        type: 'legacyViolation/loadLegacyViolation/rejected',
        payload: 'Some error',
      };

      store.dispatch(actions.loadLegacyViolation()).then(() => {
        const actions = store.getActions().map((action) => omit(['meta', 'error'], action));
        expect(actions).toHaveActionsInOrder([expectedPendingAction, expectedFailedAction]);
        done();
      });
    });
  });

  describe('saveLegacyViolation', () => {
    beforeEach(function () {
      state = {
        router: {
          currentParams: {
            organizationId: 'ROOT_ORGANIZATION_ID',
          },
          currentState: {
            name: 'management.edit.organization.legacy-violation-react',
            url: '/legacyViolationsReact',
          },
        },
        orgsAndPolicies: {
          legacyViolations: {
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
      jest.useFakeTimers();
    });

    afterEach(() => {
      jest.useRealTimers();
    });

    it('saves legacy violation successfully', (done) => {
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

      mock.onPut(legacyViolationUrl, putData).reply(200, {
        data: putDataResponse,
      });

      store.dispatch(actions.saveLegacyViolation()).then(() => {
        jest.advanceTimersByTime(SUBMIT_MASK_SUCCESS_VISIBLE_TIME_MS);
        expect(mock.history.put.length).toBe(1);
        expect(mock.history.put[0].url).toBe('/rest/legacyViolations/organization/ROOT_ORGANIZATION_ID', putData);
        expect(mock.history.put[0].data).toEqual(JSON.stringify(putData));
        const actions = store.getActions();
        expect(actions.length).toBe(3);
        expect(actions).toHaveActionTypesInOrder([
          'legacyViolation/saveLegacyViolation/pending',
          'legacyViolation/saveLegacyViolation/fulfilled',
          'legacyViolation/saveMaskTimerDone',
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
      mock.onPut(legacyViolationUrl, putData).reply(() => Promise.reject('could not save updates'));

      store.dispatch(actions.saveLegacyViolation()).then(() => {
        expect(mock.history.put.length).toBe(1);
        expect(mock.history.put[0].url).toBe('/rest/legacyViolations/organization/ROOT_ORGANIZATION_ID', putData);

        const actions = store.getActions();

        expect(actions.length).toBe(2);
        expect(actions).toHaveActionTypesInOrder([
          'legacyViolation/saveLegacyViolation/pending',
          'legacyViolation/saveLegacyViolation/rejected',
        ]);

        expect(actions[1].payload).toEqual('could not save updates');

        done();
      });
    });
  });

  describe('setLegacyViolationStatus action', () => {
    it('setLegacyViolationStatus seted successfully', () => {
      store.dispatch(setLegacyViolationStatus('inherit'));
      const setAction = store.getActions();
      expect(setAction.length).toBe(1);
      expect(setAction).toHaveAction({
        type: 'legacyViolation/setLegacyViolationStatus',
        payload: 'inherit',
      });
    });
  });
});
