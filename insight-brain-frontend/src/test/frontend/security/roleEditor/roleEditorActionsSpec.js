/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

import axios from 'axios';
import {
  authErrorMessage,
  save,
  load,
  deleteRole,
} from '../../../../main/frontend/security/roleEditor/roleEditorActions';
import { getGlobalPermissionTestUrl } from '../../../../main/frontend/util/CLMContextLocation';
import { getRoleByIdUrl, getRoleForNewUrl, getRoleListUrl } from '../../../../main/frontend/util/CLMLocation';

describe('RoleEditorActions', function () {
  const mockAxiosCalls = SpecUtil.axiosMockerGenerator(axios);
  const roleListUrl = getRoleListUrl();
  const globalPermissionTestUrl = getGlobalPermissionTestUrl();
  const roleForNewUrl = getRoleForNewUrl();
  let state, store, actions;

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
    store = SpecUtil.mockReduxStore(state);
    actions = store.getActions();
  });

  describe('save', () => {
    it('maps the formState in store before call the service', () => {
      const spy = spyOn(axios, 'post').and.returnValue(Promise.resolve({}));
      store.dispatch(save());
      expect(spy).toHaveBeenCalledWith(roleListUrl, {
        name: 'TEST-NAME',
        description: 'TEST-DESCRIPTION',
        otherValue: 'OTHER-STATE-PROP',
      });
    });

    it('dispatches a ROLE_EDITOR_UPDATE_FULFILLED action', (done) => {
      mockAxiosCalls({
        post: {
          [roleListUrl]: Promise.resolve({}),
        },
      });
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
      mockAxiosCalls({
        post: {
          [roleListUrl]: Promise.reject(error),
        },
      });
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
      store = SpecUtil.mockReduxStore(state);
      const spy = spyOn(axios, 'put').and.returnValue(Promise.resolve({}));
      store.dispatch(save());
      expect(spy).toHaveBeenCalledWith(roleListUrl, {
        id,
        name: 'TEST-NAME',
        description: 'TEST-DESCRIPTION',
        otherValue: 'OTHER-STATE-PROP',
      });
    });
  });

  describe('load', () => {
    it('dispatches a ROLE_EDITOR_LOAD_FAILED when permissions do not include VIEW_ROLES', (done) => {
      mockAxiosCalls({
        put: {
          [globalPermissionTestUrl]: Promise.resolve({ data: ['FIRST_PERMISSION', 'SECOND_PERMISSION'] }),
        },
      });
      store.dispatch(load()).then(() => {
        const [, { type: actionType, payload: actionPayload }] = actions;
        expect(actionType).toBe('ROLE_EDITOR_LOAD_FAILED');
        expect(actionPayload).toBe(authErrorMessage);
        done();
      });
      const [{ type: actionType }] = actions;
      expect(actionType).toBe('ROLE_EDITOR_LOAD_REQUESTED');
    });

    it('dispatches a ROLE_EDITOR_LOAD_FAILED when permissions do not include EDIT_ROLES', (done) => {
      mockAxiosCalls({
        put: {
          [globalPermissionTestUrl]: Promise.resolve({ data: ['VIEW_ROLES'] }),
        },
        get: {
          [roleListUrl]: Promise.resolve({}),
        },
      });
      store.dispatch(load()).then(() => {
        const [, { type: actionType, payload: actionPayload }] = actions;
        expect(actionType).toBe('ROLE_EDITOR_LOAD_FAILED');
        expect(actionPayload).toBe(authErrorMessage);
        done();
      });
      const [{ type: actionType }] = actions;
      expect(actionType).toBe('ROLE_EDITOR_LOAD_REQUESTED');
    });

    it('dispatches a ROLE_EDITOR_LOAD_FULFILLED action', (done) => {
      let data = {};
      mockAxiosCalls({
        put: {
          [globalPermissionTestUrl]: Promise.resolve({ data: ['VIEW_ROLES', 'EDIT_ROLES'] }),
        },
        get: {
          [roleForNewUrl]: Promise.resolve({ data }),
        },
      });
      store.dispatch(load()).then(() => {
        const [, , { type: actionType, payload: actionPayload }] = actions;
        expect(actionType).toBe('ROLE_EDITOR_LOAD_FULFILLED');
        expect(actionPayload).toBe(data);
        done();
      });
      const [{ type: actionType }] = actions;
      expect(actionType).toBe('ROLE_EDITOR_LOAD_REQUESTED');
    });

    it('dispatches a ROLE_EDITOR_LOAD_FAILED action when the get role for new url service fail', (done) => {
      let data = 'some error happened';
      mockAxiosCalls({
        put: {
          [globalPermissionTestUrl]: Promise.resolve({ data: ['VIEW_ROLES', 'EDIT_ROLES'] }),
        },
        get: {
          [roleForNewUrl]: Promise.reject(data),
        },
      });
      store.dispatch(load()).then(() => {
        const [, { type: actionType, payload: actionPayload }] = actions;
        expect(actionType).toBe('ROLE_EDITOR_LOAD_FAILED');
        expect(actionPayload).toBe(data);
        done();
      });
      const [{ type: actionType }] = actions;
      expect(actionType).toBe('ROLE_EDITOR_LOAD_REQUESTED');
    });

    it('loads info from getRoleByIdUrl', (done) => {
      const id = 'ROLE-ID';
      const roleByIdUrl = getRoleByIdUrl(id);
      const spy = spyOn(axios, 'get').and.returnValue(Promise.resolve({}));
      mockAxiosCalls({
        put: {
          [globalPermissionTestUrl]: Promise.resolve({ data: ['VIEW_ROLES', 'EDIT_ROLES'] }),
        },
      });
      store.dispatch(load(id)).then(() => {
        expect(spy).toHaveBeenCalledWith(roleByIdUrl);
        done();
      });
    });

    it('dispatches a ROLE_EDITOR_SET_READONLY action with true', (done) => {
      const id = 'ROLE-ID';
      const roleByIdUrl = getRoleByIdUrl(id);
      state.roleEditor.formState.id = id;
      store = SpecUtil.mockReduxStore(state);
      actions = store.getActions();
      mockAxiosCalls({
        put: {
          [globalPermissionTestUrl]: Promise.resolve({ data: ['VIEW_ROLES'] }),
        },
        get: {
          [roleByIdUrl]: Promise.resolve({}),
        },
      });
      store.dispatch(load(id)).then(() => {
        const [, { type: actionType, payload: actionPayload }] = actions;
        expect(actionType).toBe('ROLE_EDITOR_SET_READONLY');
        expect(actionPayload).toBe(true);
        done();
      });
      const [{ type: actionType }] = actions;
      expect(actionType).toBe('ROLE_EDITOR_LOAD_REQUESTED');
    });
  });

  describe('delete', () => {
    it('dispatches a ROLE_EDITOR_DELETE_FULFILLED action', (done) => {
      const id = 'ROLE-ID';
      const roleByIdUrl = getRoleByIdUrl(id);
      mockAxiosCalls({
        del: {
          [roleByIdUrl]: Promise.resolve({}),
        },
      });
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
      mockAxiosCalls({
        del: {
          [roleByIdUrl]: Promise.reject(error),
        },
      });
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
