/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import 'TestRoot/SpecUtil';
import axios from 'axios';
import { axiosMockAdapter } from 'TestRoot/SpecUtil';
import { save, deleteRole } from 'MainRoot/security/roleEditor/roleEditorActions';
import { getRoleByIdUrl, getRoleForNewUrl, getRoleListUrl } from 'MainRoot/util/CLMLocation';
import * as authorizationUtil from 'MainRoot/util/authorizationUtil';

// Mock the authorizationUtil module
jest.mock('MainRoot/util/authorizationUtil', () => ({
  getPermissions: jest.fn(),
}));

// Import the mocked module to get access to load action
// We need to import this after the mock is set up
import { load } from 'MainRoot/security/roleEditor/roleEditorActions';

// Create a simple mock store that records dispatched actions
function createMockStore(state) {
  const actions = [];
  const store = {
    dispatch: jest.fn((action) => {
      if (typeof action === 'function') {
        return action(store.dispatch, store.getState);
      }
      actions.push(action);
      return action;
    }),
    getState: jest.fn(() => state),
    getActions: () => actions,
    subscribe: jest.fn(),
  };
  return store;
}

describe('RoleEditorActions', () => {
  let axiosMock;
  const roleListUrl = getRoleListUrl();
  const roleForNewUrl = getRoleForNewUrl();
  let state, store, actions;

  beforeAll(() => {
    axiosMock = axiosMockAdapter();
  });

  afterEach(() => {
    axiosMock.reset();
  });

  beforeEach(() => {
    state = {
      roleEditor: {
        formState: {
          name: {
            value: 'TEST-NAME ',
            trimmedValue: 'TEST-NAME',
          },
          description: {
            value: 'TEST-DESCRIPTION ',
            trimmedValue: 'TEST-DESCRIPTION',
          },
          otherValue: 'OTHER-STATE-PROP',
        },
      },
    };
    store = createMockStore(state);
    actions = store.getActions();
  });

  describe('save', () => {
    it('maps the formState in store before call the service', () => {
      const spy = jest.spyOn(axios, 'post').mockReturnValue(Promise.resolve({}));
      store.dispatch(save());
      expect(spy).toHaveBeenCalledWith(roleListUrl, {
        name: 'TEST-NAME',
        description: 'TEST-DESCRIPTION',
        otherValue: 'OTHER-STATE-PROP',
      });
    });

    it('dispatches a ROLE_EDITOR_UPDATE_FULFILLED action', (done) => {
      axiosMock.onPost(roleListUrl).reply(200, {});

      store.dispatch(save()).then(() => {
        const [, { type: actionType }] = actions;
        expect(actionType).toBe('ROLE_EDITOR_UPDATE_FULFILLED');
        done();
      });
      const [{ type: actionType }] = actions;
      expect(actionType).toBe('ROLE_EDITOR_UPDATE_REQUESTED');
    });

    it('dispatches a ROLE_EDITOR_UPDATE_FAILED action', (done) => {
      let error = 'some error happened';
      axiosMock.onPost(roleListUrl).reply(() => Promise.reject(error));

      store.dispatch(save()).then(() => {
        const [, { type: actionType, payload: actionPayload }] = actions;
        expect(actionType).toBe('ROLE_EDITOR_UPDATE_FAILED');
        expect(actionPayload).toBe(error);
        done();
      });
      const [{ type: actionType }] = actions;
      expect(actionType).toBe('ROLE_EDITOR_UPDATE_REQUESTED');
    });

    it('calls the service to update a role', () => {
      const id = 'ROLE-ID';
      state.roleEditor.formState.id = id;
      store = createMockStore(state);
      const spy = jest.spyOn(axios, 'put').mockReturnValue(Promise.resolve({}));
      store.dispatch(save());
      expect(spy).toHaveBeenCalledWith(getRoleByIdUrl(id), {
        id,
        name: 'TEST-NAME',
        description: 'TEST-DESCRIPTION',
        otherValue: 'OTHER-STATE-PROP',
      });
    });
  });

  describe('load', () => {
    let getPermissionsSpy;

    beforeEach(() => {
      getPermissionsSpy = authorizationUtil.getPermissions;
      getPermissionsSpy.mockReturnValue(Promise.resolve(['VIEW_ROLES', 'EDIT_ROLES']));
    });

    it('dispatches a ROLE_EDITOR_LOAD_FAILED when permissions do not include VIEW_ROLES', (done) => {
      getPermissionsSpy.mockReturnValue(Promise.resolve(['EDIT_ROLES']));

      store.dispatch(load()).then(() => {
        expect(actions.length).toBe(2);

        expect(actions).toHaveActionsInOrder([
          { type: 'ROLE_EDITOR_LOAD_REQUESTED' },
          { type: 'ROLE_EDITOR_LOAD_FAILED' },
        ]);

        done();
      });
    });

    it('dispatches a ROLE_EDITOR_LOAD_FAILED when permissions do not include EDIT_ROLES', (done) => {
      getPermissionsSpy.mockReturnValue(Promise.resolve(['VIEW_ROLES']));
      axiosMock.onGet(roleListUrl).reply(200, {});

      store.dispatch(load()).then(() => {
        expect(actions.length).toBe(2);

        expect(actions).toHaveActionsInOrder([
          { type: 'ROLE_EDITOR_LOAD_REQUESTED' },
          { type: 'ROLE_EDITOR_LOAD_FAILED' },
        ]);

        done();
      });
    });

    it('dispatches a ROLE_EDITOR_LOAD_FULFILLED action', (done) => {
      axiosMock.onGet(roleForNewUrl).reply(200, {});

      store.dispatch(load()).then(() => {
        expect(actions.length).toBe(3);

        expect(actions).toHaveActionsInOrder([
          { type: 'ROLE_EDITOR_LOAD_REQUESTED' },
          { type: 'ROLE_EDITOR_SET_READONLY', payload: undefined },
          { type: 'ROLE_EDITOR_LOAD_FULFILLED', payload: {} },
        ]);
        done();
      });
    });

    it('dispatches a ROLE_EDITOR_LOAD_FAILED action when the get role for new url service fail', (done) => {
      let data = 'some error happened';
      axiosMock.onGet(roleForNewUrl).reply(() => Promise.reject(data));

      store.dispatch(load()).then(() => {
        expect(actions.length).toBe(2);

        expect(actions).toHaveActionsInOrder([
          { type: 'ROLE_EDITOR_LOAD_REQUESTED' },
          { type: 'ROLE_EDITOR_LOAD_FAILED', payload: data },
        ]);
        done();
      });
    });

    it('loads info from getRoleByIdUrl', (done) => {
      const id = 'ROLE-ID';
      const roleByIdUrl = getRoleByIdUrl(id);
      const spy = jest.spyOn(axios, 'get').mockReturnValue(Promise.resolve({}));

      store.dispatch(load(id)).then(() => {
        expect(spy).toHaveBeenCalledWith(roleByIdUrl);
        done();
      });
    });

    it('dispatches a ROLE_EDITOR_SET_READONLY action with true', (done) => {
      getPermissionsSpy.mockReturnValue(Promise.resolve(['VIEW_ROLES']));
      const id = 'ROLE-ID';
      state.roleEditor.formState.id = id;
      store = createMockStore(state);
      actions = store.getActions();

      jest.spyOn(axios, 'get').mockReturnValue(Promise.resolve({}));

      store.dispatch(load(id)).then(() => {
        expect(actions.length).toBe(3);

        expect(actions).toHaveActionsInOrder([
          { type: 'ROLE_EDITOR_LOAD_REQUESTED' },
          { type: 'ROLE_EDITOR_SET_READONLY', payload: true },
          { type: 'ROLE_EDITOR_LOAD_FULFILLED', payload: undefined },
        ]);

        done();
      });
    });
  });

  describe('delete', () => {
    it('dispatches a ROLE_EDITOR_DELETE_FULFILLED action', (done) => {
      const id = 'ROLE-ID';
      const roleByIdUrl = getRoleByIdUrl(id);
      axiosMock.onDelete(roleByIdUrl).reply(200, {});

      store.dispatch(deleteRole(id)).then(() => {
        const [, { type: actionType }] = actions;
        expect(actionType).toBe('ROLE_EDITOR_DELETE_FULFILLED');
        done();
      });
      const [{ type: actionType }] = actions;
      expect(actionType).toBe('ROLE_EDITOR_DELETE_REQUESTED');
    });

    it('dispatches a ROLE_EDITOR_DELETE_FAILED action', (done) => {
      const error = 'some error happened';
      const id = 'ROLE-ID';
      const roleByIdUrl = getRoleByIdUrl(id);
      axiosMock.onDelete(roleByIdUrl).reply(() => Promise.reject(error));

      store.dispatch(deleteRole(id)).then(() => {
        const [, { type: actionType, payload }] = actions;
        expect(actionType).toBe('ROLE_EDITOR_DELETE_FAILED');
        expect(payload).toBe(error);
        done();
      });
      const [{ type: actionType }] = actions;
      expect(actionType).toBe('ROLE_EDITOR_DELETE_REQUESTED');
    });
  });
});
