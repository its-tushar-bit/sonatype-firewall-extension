/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import axios from 'axios';
import { getGlobalRoleMappingUrl } from 'MainRoot/utilAngular/CLMContextLocation';
import { getFindUsersUrl, getRoleMappingUrl } from 'MainRoot/util/CLMLocation';
import { loadRolesIfNeeded } from 'MainRoot/configuration/administrators/administratorsSlice';
import * as administratorsSelectors from 'MainRoot/configuration/administrators/administratorsSelectors';
import { SUBMIT_MASK_SUCCESS_VISIBLE_TIME_MS } from '@sonatype/react-shared-components';
import * as routerSelectors from 'MainRoot/reduxUiRouter/routerSelectors';

describe('administratorsConfigActionsSpec', () => {
  const mockAxiosCalls = SpecUtil.axiosMockerGenerator(axios);
  const mappingUrl = getGlobalRoleMappingUrl();
  const fetchUrl = getFindUsersUrl('search');
  const saveMembersUrl = getRoleMappingUrl('roleId');

  let load, goToAdministratorPage, checkPermissionsSpy, goToAdministrators, loadFetchUsers, saveMembers;

  beforeEach(() => {
    checkPermissionsSpy = jasmine.createSpy('checkPermissions');
    const module = require('inject-loader!../../../../main/frontend/configuration/administrators/administratorsSlice')({
      'MainRoot/util/authorizationUtil': {
        checkPermissions: checkPermissionsSpy,
      },
    });
    load = module.actions.load;
    loadFetchUsers = module.actions.loadFetchUsers;
    saveMembers = module.actions.saveMembers;
    goToAdministratorPage = module.actions.goToAdministratorPage;
    goToAdministrators = module.actions.goToAdministrators;
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
        checkPermissionsSpy.and.returnValue(Promise.resolve());
      });

      it('fires administratorsConfig/load/fulfilled action on success', (done) => {
        spyOn(routerSelectors, 'selectRouterCurrentParams').and.returnValue({ roleId: 'roleId' });
        mockAxiosCalls({
          get: {
            [mappingUrl]: Promise.resolve({ data: {} }),
          },
        });

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
        checkPermissionsSpy.and.callFake(() => Promise.reject('Administrator config page: authorization error'));
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
      mockAxiosCalls({
        get: {
          [fetchUrl]: Promise.resolve({ data: {} }),
        },
      });

      store.dispatch(loadFetchUsers('search')).then(() => {
        const actions = store.getActions();
        expect(actions.length).toBe(2);
        expect(actions).toHaveActionType('administratorsConfig/loadFetchUsers/pending');
        expect(actions).toHaveActionType('administratorsConfig/loadFetchUsers/fulfilled');
        done();
      });
    });

    it('fires administratorsConfig/loadFetchUsers/rejected action on reject', (done) => {
      mockAxiosCalls({
        get: {
          [fetchUrl]: () => Promise.reject('some error'),
        },
      });

      store.dispatch(loadFetchUsers('search')).then(() => {
        const actions = store.getActions();
        expect(actions.length).toBe(2);
        expect(actions).toHaveActionType('administratorsConfig/loadFetchUsers/pending');
        expect(actions).toHaveAction({
          type: 'administratorsConfig/loadFetchUsers/rejected',
          payload: 'some error',
        });

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
      jasmine.clock().install();
      spyOn(routerSelectors, 'selectRouterCurrentParams').and.returnValue({ roleId: 'roleId' });
      spyOn(administratorsSelectors, 'selectAddedUsers').and.returnValue([]);
      mockAxiosCalls({
        put: {
          [saveMembersUrl]: Promise.resolve(),
        },
      });

      store.dispatch(saveMembers([])).then(() => {
        jasmine.clock().tick(SUBMIT_MASK_SUCCESS_VISIBLE_TIME_MS);
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
        jasmine.clock().uninstall();
        done();
      });
    });

    it('fires administratorsConfig/loadFetchUsers/rejected action on reject', (done) => {
      mockAxiosCalls({
        get: {
          [fetchUrl]: () => Promise.reject('some error'),
        },
      });

      store.dispatch(loadFetchUsers('search')).then(() => {
        const actions = store.getActions();
        expect(actions.length).toBe(2);
        expect(actions[0].type).toBe('administratorsConfig/loadFetchUsers/pending');
        expect(actions[1].type).toBe('administratorsConfig/loadFetchUsers/rejected');
        expect(actions[1].payload).toBe('some error');

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
      spyOn(administratorsSelectors, 'selectRoleToEdit').and.returnValue(null);

      store.dispatch(loadRolesIfNeeded());
      const actions = store.getActions();

      expect(actions.length).toBe(2);
      expect(actions).toHaveAction({
        type: 'administratorsConfig/load/pending',
      });
      expect(actions).toHaveAction({
        type: 'administratorsConfig/clearErrors',
      });
    });

    it('does not dispatch load() if matching role exists in memory', () => {
      const store = SpecUtil.mockReduxStore({});
      spyOn(administratorsSelectors, 'selectRoleToEdit').and.returnValue({ roleId: 'roleId' });

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
