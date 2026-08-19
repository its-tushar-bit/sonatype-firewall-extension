/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

// Mock the authorizationUtil module before importing administrators slice
jest.mock('MainRoot/util/authorizationUtil', () => ({
  checkPermissions: jest.fn(),
}));

import '../../SpecUtil';
import { axiosMockAdapter } from 'TestRoot/SpecUtil';
import { checkPermissions } from 'MainRoot/util/authorizationUtil';
import { getGlobalRoleMappingUrl } from 'MainRoot/util/CLMContextLocation';
import { getFindUsersUrl, getRoleMappingUrl } from 'MainRoot/util/CLMLocation';
import { loadRolesIfNeeded, actions } from 'MainRoot/configuration/administrators/administratorsSlice';
import * as administratorsSelectors from 'MainRoot/configuration/administrators/administratorsSelectors';
import { SUBMIT_MASK_SUCCESS_VISIBLE_TIME_MS } from '@sonatype/react-shared-components';
import * as routerSelectors from 'MainRoot/reduxUiRouter/routerSelectors';

describe('administratorsConfigActionsSpec', () => {
  let axiosMock;

  beforeAll(() => {
    axiosMock = axiosMockAdapter();
  });
  const mappingUrl = getGlobalRoleMappingUrl();
  const fetchUrl = getFindUsersUrl('search');
  const saveMembersUrl = getRoleMappingUrl('roleId');

  // Import actions directly from the slice
  const { load, loadFetchUsers, saveMembers, goToAdministratorPage, goToAdministrators } = actions;

  beforeEach(() => {
    // Clear all mocks before each test
    jest.clearAllMocks();
    checkPermissions.mockClear();
  });

  describe('load', () => {
    let store, state;

    beforeEach(() => {
      state = {
        administratorsConfig: {
          serverData: null,
          loading: false,
          loadError: null,
        },
      };

      store = SpecUtil.mockReduxStore(state);
    });

    describe('when authorized', () => {
      beforeEach(() => {
        checkPermissions.mockReturnValue(Promise.resolve());
      });

      it('fires administratorsConfig/load/fulfilled action on success', (done) => {
        jest.spyOn(routerSelectors, 'selectRouterCurrentParams').mockReturnValue({ roleId: 'roleId' });
        axiosMock.onGet(mappingUrl).reply(200, {});

        store.dispatch(load()).then(() => {
          const actions = store.getActions();
          expect(actions.length).toBe(4);
          expect(actions).toHaveActionType('administratorsConfig/load/pending');
          expect(actions).toHaveActionType('administratorsConfig/setAddedUsers');
          expect(actions).toHaveActionType('administratorsConfig/setServerAddedUsers');
          expect(actions).toHaveActionType('administratorsConfig/load/fulfilled');
          done();
        });
      });
    });

    describe('when not authorized', () => {
      it('does not load configure administrators page', (done) => {
        checkPermissions.mockImplementation(() => Promise.reject('Administrator config page: authorization error'));
        store.dispatch(load()).then(() => {
          const actions = store.getActions();

          expect(actions.length).toBe(2);
          expect(actions).toHaveActionType('administratorsConfig/load/pending');
          expect(actions).toHaveAction({
            type: 'administratorsConfig/load/rejected',
            payload: 'Administrator config page: authorization error',
          });

          done();
        });
      });
    });
  });

  describe('loadFetchUsers', () => {
    let store, state;

    beforeEach(() => {
      state = {
        administratorsConfig: {
          fetchUsers: { data: [], loading: false, loadError: null },
        },
      };

      store = SpecUtil.mockReduxStore(state);
    });

    it('fires administratorsConfig/loadFetchUsers/fulfilled action on success', (done) => {
      axiosMock.onGet(fetchUrl).reply(200, {});

      store.dispatch(loadFetchUsers('search')).then(() => {
        const actions = store.getActions();
        expect(actions.length).toBe(2);
        expect(actions).toHaveActionType('administratorsConfig/loadFetchUsers/pending');
        expect(actions).toHaveActionType('administratorsConfig/loadFetchUsers/fulfilled');
        done();
      });
    });

    it('fires administratorsConfig/loadFetchUsers/rejected action on reject', (done) => {
      axiosMock.onGet(fetchUrl).reply(500, 'some error');

      store.dispatch(loadFetchUsers('search')).then(() => {
        const actions = store.getActions();
        expect(actions.length).toBe(2);
        expect(actions).toHaveActionType('administratorsConfig/loadFetchUsers/pending');
        expect(actions[1].type).toBe('administratorsConfig/loadFetchUsers/rejected');
        expect(actions[1].payload.message).toBe('Request failed with status code 500');

        done();
      });
    });
  });

  describe('saveMembers', () => {
    let store, state;

    beforeEach(() => {
      state = {
        administratorsConfig: {
          submitMaskState: null,
          submitError: null,
        },
      };

      store = SpecUtil.mockReduxStore(state);
    });

    it('fires administratorsConfig/saveMembers/fulfilled action on success', (done) => {
      jest.useFakeTimers();
      jest.spyOn(routerSelectors, 'selectRouterCurrentParams').mockReturnValue({ roleId: 'roleId' });
      jest.spyOn(administratorsSelectors, 'selectAddedUsers').mockReturnValue([]);
      axiosMock.onPut(saveMembersUrl).reply(200);

      store.dispatch(saveMembers([])).then(() => {
        jest.advanceTimersByTime(SUBMIT_MASK_SUCCESS_VISIBLE_TIME_MS);
        const actions = store.getActions();
        expect(actions.length).toBe(4);
        expect(actions).toHaveActionType('administratorsConfig/saveMembers/pending');
        expect(actions).toHaveActionType('administratorsConfig/saveMembers/fulfilled');
        expect(actions).toHaveActionType('administratorsConfig/resetSubmitMaskState');
        expect(actions).toHaveAction({
          type: '@@reduxUiRouter/stateGo',
          payload: {
            to: 'administrators',
            params: undefined,
            options: undefined,
          },
        });
        jest.useRealTimers();
        done();
      });
    });

    it('fires administratorsConfig/loadFetchUsers/rejected action on reject', (done) => {
      axiosMock.onGet(fetchUrl).reply(500, 'some error');

      store.dispatch(loadFetchUsers('search')).then(() => {
        const actions = store.getActions();
        expect(actions.length).toBe(2);
        expect(actions[0].type).toBe('administratorsConfig/loadFetchUsers/pending');
        expect(actions[1].type).toBe('administratorsConfig/loadFetchUsers/rejected');
        expect(actions[1].payload.message).toBe('Request failed with status code 500');

        done();
      });
    });
  });

  describe('goToAdministratorPage', () => {
    it('calls stateGo with the appropriate parameters', () => {
      const store = SpecUtil.mockReduxStore({});

      store.dispatch(goToAdministratorPage('roleId'));

      expect(store.getActions()).toHaveAction({
        type: '@@reduxUiRouter/stateGo',
        payload: {
          to: 'administratorsEdit',
          params: { roleId: 'roleId' },
          options: undefined,
        },
      });
    });
  });

  describe('goToAdministrators', () => {
    it('calls stateGo with the appropriate parameters', () => {
      const store = SpecUtil.mockReduxStore({});

      store.dispatch(goToAdministrators());

      expect(store.getActions()).toHaveAction({
        type: '@@reduxUiRouter/stateGo',
        payload: {
          to: 'administrators',
          params: undefined,
          options: undefined,
        },
      });
    });
  });

  describe('loadRolesIfNeeded', () => {
    it('dispatches load() if matching role doesn`t exist in memory', () => {
      const store = SpecUtil.mockReduxStore({});
      jest.spyOn(administratorsSelectors, 'selectRoleToEdit').mockReturnValue(null);

      store.dispatch(loadRolesIfNeeded());
      const actions = store.getActions();

      expect(actions.length).toBe(3);
      expect(actions).toHaveAction({
        type: 'administratorsConfig/load/pending',
      });
      expect(actions).toHaveAction({
        type: 'administratorsConfig/clearErrors',
      });
    });

    it('does not dispatch load() if matching role exists in memory', () => {
      const store = SpecUtil.mockReduxStore({});
      jest.spyOn(administratorsSelectors, 'selectRoleToEdit').mockReturnValue({ roleId: 'roleId' });

      store.dispatch(loadRolesIfNeeded());
      const actions = store.getActions();

      expect(actions.length).toBe(3);
      expect(actions).not.toHaveAction({
        type: 'administratorsConfig/load/pending',
      });
      expect(actions).toHaveAction({
        type: 'administratorsConfig/setAddedUsers',
        payload: [],
      });
      expect(actions).toHaveAction({
        type: 'administratorsConfig/setServerAddedUsers',
        payload: [],
      });
      expect(actions).toHaveAction({
        type: 'administratorsConfig/clearErrors',
      });
    });
  });
});
