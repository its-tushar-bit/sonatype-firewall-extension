/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { axiosMockAdapter } from 'TestRoot/SpecUtil';
import { getRoleListUrl } from 'MainRoot/util/CLMLocation';
import * as authorizationUtil from 'MainRoot/util/authorizationUtil';
import { load } from 'MainRoot/security/rolesActions';

describe('rolesActions', () => {
  let axiosMock;
  const rolesListUrl = getRoleListUrl();

  beforeAll(() => {
    axiosMock = axiosMockAdapter();
  });

  describe('load', () => {
    let getPermissionsSpy, dispatch, getState;

    beforeEach(() => {
      getPermissionsSpy = jest.spyOn(authorizationUtil, 'getPermissions');

      // Mock Redux dispatch and getState
      dispatch = jest.fn((action) => {
        if (typeof action === 'function') {
          return action(dispatch, getState);
        }
        return Promise.resolve(action);
      });

      getState = jest.fn().mockReturnValue({
        roles: {
          roles: [],
          loading: true,
          loadError: null,
          readOnly: true,
        },
      });
    });

    afterEach(() => {
      jest.restoreAllMocks();
    });

    describe('when authorized', () => {
      beforeEach(() => {
        getPermissionsSpy.mockReturnValue(Promise.resolve(['VIEW_ROLES', 'EDIT_ROLES']));
      });

      it('fires an ROLES_LIST_LOAD_FULFILLED action', async () => {
        const role = {
          id: 'roleIdOne',
          name: 'Role Name One',
          description: 'Role Description One',
          builtIn: false,
        };

        axiosMock.onGet(rolesListUrl).reply(200, {
          roles: [{ ...role }],
        });

        await load()(dispatch, getState);

        expect(dispatch).toHaveBeenCalledTimes(2);
        expect(dispatch).toHaveBeenNthCalledWith(1, { type: 'ROLES_LIST_LOAD_REQUESTED' });
        expect(dispatch).toHaveBeenNthCalledWith(2, {
          type: 'ROLES_LIST_LOAD_FULFILLED',
          payload: { roles: [role], readOnly: false },
        });
      });

      it('fires an ROLES_LIST_LOAD_FULFILLED action with readOnly true', async () => {
        getPermissionsSpy.mockReturnValue(Promise.resolve(['VIEW_ROLES']));
        const role = {
          id: 'roleIdOne',
          name: 'Role Name One',
          description: 'Role Description One',
          builtIn: false,
        };

        axiosMock.onGet(rolesListUrl).reply(200, {
          roles: [{ ...role }],
        });

        await load()(dispatch, getState);

        expect(dispatch).toHaveBeenCalledTimes(2);
        expect(dispatch).toHaveBeenNthCalledWith(1, { type: 'ROLES_LIST_LOAD_REQUESTED' });
        expect(dispatch).toHaveBeenNthCalledWith(2, {
          type: 'ROLES_LIST_LOAD_FULFILLED',
          payload: { roles: [role], readOnly: true },
        });
      });
    });

    describe('when not authorized', () => {
      it('fires an ROLES_LIST_LOAD_FAILED action', async () => {
        getPermissionsSpy.mockReturnValue(Promise.resolve(['EDIT_ROLES']));

        await load()(dispatch, getState);

        expect(dispatch).toHaveBeenCalledTimes(2);
        expect(dispatch).toHaveBeenNthCalledWith(1, { type: 'ROLES_LIST_LOAD_REQUESTED' });
        expect(dispatch.mock.calls[1][0]).toMatchObject({
          type: 'ROLES_LIST_LOAD_FAILED',
        });
        expect(dispatch.mock.calls[1][0].payload).toBeDefined();
      });
    });

    describe('after a failed GET roles call', () => {
      it('dispatches an ROLES_LIST_LOAD_FAILED action', async () => {
        getPermissionsSpy.mockReturnValue(Promise.resolve(['VIEW_ROLES', 'EDIT_ROLES']));
        axiosMock.onGet(rolesListUrl).reply(500, 'error');

        await load()(dispatch, getState);

        expect(dispatch).toHaveBeenCalledTimes(2);
        expect(dispatch).toHaveBeenNthCalledWith(1, { type: 'ROLES_LIST_LOAD_REQUESTED' });
        expect(dispatch.mock.calls[1][0]).toMatchObject({
          type: 'ROLES_LIST_LOAD_FAILED',
        });
        expect(dispatch.mock.calls[1][0].payload).toBeDefined();
      });
    });
  });
});
