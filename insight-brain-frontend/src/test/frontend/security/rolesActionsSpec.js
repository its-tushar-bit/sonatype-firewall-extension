/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import axios from 'axios';
import { getRoleListUrl } from '../../../main/frontend/util/CLMLocation';
import { getGlobalPermissionTestUrl } from '../../../main/frontend/util/CLMContextLocation';
import { load, permissions, authErrorMessage } from '../../../main/frontend/security/rolesActions';

describe('rolesActions', () => {
  const mockAxiosCalls = SpecUtil.axiosMockerGenerator(axios);
  const permissionUrl = getGlobalPermissionTestUrl();
  const rolesListUrl = getRoleListUrl();

  describe('load', () => {
    let store, state;

    beforeEach(() => {
      state = {
        roles: {
          roles: [],
          loading: true,
          loadError: null,
          readOnly: true,
        },
      };

      store = SpecUtil.mockReduxStore(state);
    });

    afterEach(function () {
      expect(axios.put).toHaveBeenCalledWith(permissionUrl, permissions);
    });

    describe('after a successful PUT permission call', function () {
      it('dispatches an ROLES_LIST_LOAD_FULFILLED action', function (done) {
        const role = {
          id: 'roleIdOne',
          name: 'Role Name One',
          description: 'Role Description One',
          builtIn: false,
        };

        mockAxiosCalls({
          put: {
            [permissionUrl]: Promise.resolve({
              data: permissions,
            }),
          },
          get: {
            [rolesListUrl]: Promise.resolve({
              data: [{ ...role }],
            }),
          },
        });

        store.dispatch(load()).then(() => {
          actions = store.getActions();
          expect(actions.length).toBe(2);
          expect(actions[1].type).toBe('ROLES_LIST_LOAD_FULFILLED');
          expect(actions[1].payload).toEqual({ roles: [role], readOnly: false });
          done();
        });

        let actions = store.getActions();
        expect(actions.length).toBe(1);
        expect(actions[0].type).toBe('ROLES_LIST_LOAD_REQUESTED');
      });

      it('dispatches an ROLES_LIST_LOAD_FULFILLED action with readOnly true', (done) => {
        const role = {
          id: 'roleIdOne',
          name: 'Role Name One',
          description: 'Role Description One',
          builtIn: false,
        };

        mockAxiosCalls({
          put: {
            [permissionUrl]: Promise.resolve({
              data: ['VIEW_ROLES'],
            }),
          },
          get: {
            [rolesListUrl]: Promise.resolve({
              data: [{ ...role }],
            }),
          },
        });

        store.dispatch(load()).then(() => {
          actions = store.getActions();
          expect(actions.length).toBe(2);
          expect(actions[1].type).toBe('ROLES_LIST_LOAD_FULFILLED');
          expect(actions[1].payload).toEqual({ roles: [role], readOnly: true });
          done();
        });

        let actions = store.getActions();
        expect(actions.length).toBe(1);
        expect(actions[0].type).toBe('ROLES_LIST_LOAD_REQUESTED');
      });
    });

    describe('after a failed PUT permission call', function () {
      it('dispatches an ROLES_LIST_LOAD_FAILED action', function (done) {
        mockAxiosCalls({
          put: {
            [permissionUrl]: Promise.resolve({
              data: [],
            }),
          },
        });

        store.dispatch(load()).then(() => {
          actions = store.getActions();
          expect(actions.length).toBe(2);
          expect(actions[1].type).toBe('ROLES_LIST_LOAD_FAILED');
          expect(actions[1].payload).toEqual(authErrorMessage);
          done();
        });

        let actions = store.getActions();
        expect(actions.length).toBe(1);
        expect(actions[0].type).toBe('ROLES_LIST_LOAD_REQUESTED');
      });
    });

    describe('after a failed GET roles call', function () {
      it('dispatches an ROLES_LIST_LOAD_FAILED action', function (done) {
        mockAxiosCalls({
          put: {
            [permissionUrl]: Promise.resolve({
              data: permissions,
            }),
          },
          get: {
            [rolesListUrl]: Promise.reject('error'),
          },
        });

        store.dispatch(load()).then(() => {
          actions = store.getActions();
          expect(actions.length).toBe(2);
          expect(actions[1].type).toBe('ROLES_LIST_LOAD_FAILED');
          expect(actions[1].payload).toBe('error');
          done();
        });

        let actions = store.getActions();
        expect(actions.length).toBe(1);
        expect(actions[0].type).toBe('ROLES_LIST_LOAD_REQUESTED');
        expect(actions[0].payload).toBeUndefined();
      });
    });
  });
});
