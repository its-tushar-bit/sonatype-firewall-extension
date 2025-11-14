/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import axios from 'axios';
import { getRoleListUrl } from '../../../main/frontend/util/CLMLocation';

describe('rolesActions', () => {
  const mockAxiosCalls = SpecUtil.axiosMockerGenerator(axios);
  const rolesListUrl = getRoleListUrl();

  describe('load', () => {
    let getPermissionsSpy, load, store, state;

    beforeEach(() => {
      getPermissionsSpy = jasmine.createSpy('getPermissions');
      const module = require('inject-loader!../../../main/frontend/security/rolesActions')({
        '../util/authorizationUtil': {
          getPermissions: getPermissionsSpy,
        },
      });

      load = module.load;

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

    describe('when authorized', () => {
      beforeEach(() => {
        getPermissionsSpy.and.returnValue(Promise.resolve(['VIEW_ROLES', 'EDIT_ROLES']));
      });

      it('fires an ROLES_LIST_LOAD_FULFILLED action', (done) => {
        const role = {
          id: 'roleIdOne',
          name: 'Role Name One',
          description: 'Role Description One',
          builtIn: false,
        };

        mockAxiosCalls({
          get: {
            [rolesListUrl]: Promise.resolve({
              data: { roles: [{ ...role }] },
            }),
          },
        });

        store.dispatch(load()).then(() => {
          const actions = store.getActions();
          expect(actions.length).toBe(2);

          expect(actions).toHaveActionsInOrder([
            { type: 'ROLES_LIST_LOAD_REQUESTED' },
            { type: 'ROLES_LIST_LOAD_FULFILLED', payload: { roles: [role], readOnly: false } },
          ]);
          done();
        });
      });

      it('fires an ROLES_LIST_LOAD_FULFILLED action with readOnly true', (done) => {
        getPermissionsSpy.and.returnValue(Promise.resolve(['VIEW_ROLES']));
        const role = {
          id: 'roleIdOne',
          name: 'Role Name One',
          description: 'Role Description One',
          builtIn: false,
        };

        mockAxiosCalls({
          get: {
            [rolesListUrl]: Promise.resolve({
              data: { roles: [{ ...role }] },
            }),
          },
        });

        store.dispatch(load()).then(() => {
          const actions = store.getActions();

          expect(actions.length).toBe(2);
          expect(actions).toHaveActionsInOrder([
            { type: 'ROLES_LIST_LOAD_REQUESTED' },
            { type: 'ROLES_LIST_LOAD_FULFILLED', payload: { roles: [role], readOnly: true } },
          ]);
          done();
        });
      });
    });

    describe('when not authorized', () => {
      it('fires an ROLES_LIST_LOAD_FAILED action', (done) => {
        getPermissionsSpy.and.returnValue(Promise.resolve(['EDIT_ROLES']));

        store.dispatch(load()).then(() => {
          const actions = store.getActions();
          expect(actions.length).toBe(2);
          expect(actions).toHaveActionsInOrder([
            { type: 'ROLES_LIST_LOAD_REQUESTED' },
            { type: 'ROLES_LIST_LOAD_FAILED' },
          ]);
          done();
        });
      });
    });

    describe('after a failed GET roles call', () => {
      it('dispatches an ROLES_LIST_LOAD_FAILED action', (done) => {
        getPermissionsSpy.and.returnValue(Promise.resolve(['VIEW_ROLES', 'EDIT_ROLES']));
        mockAxiosCalls({
          get: {
            [rolesListUrl]: () => Promise.reject('error'),
          },
        });

        store.dispatch(load()).then(() => {
          const actions = store.getActions();
          expect(actions.length).toBe(2);
          expect(actions).toHaveActionsInOrder([
            { type: 'ROLES_LIST_LOAD_REQUESTED' },
            { type: 'ROLES_LIST_LOAD_FAILED', payload: 'error' },
          ]);
          done();
        });
      });
    });
  });
});
