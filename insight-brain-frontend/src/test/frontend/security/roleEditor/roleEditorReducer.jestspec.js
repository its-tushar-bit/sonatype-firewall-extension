/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

import { nxTextInputStateHelpers } from '@sonatype/react-shared-components';

import reduce from '../../../../main/frontend/security/roleEditor/roleEditorReducer';

const { initialState: initUserInput } = nxTextInputStateHelpers;

describe('RoleEditorReducer', () => {
  let otherObject;
  let initialState;

  beforeEach(() => {
    otherObject = { value: 'OTHER-VALUE' };
    initialState = Object.freeze({
      builtIn: false,
      deleteError: null,
      updateError: null,
      readonly: false,
      isDirty: false,
      loading: true,
      updateMaskState: null,
      deleteMaskState: null,
      loadError: null,
      formState: {
        id: null,
        name: initUserInput(''),
        description: initUserInput(''),
        permissionCategories: [],
      },
      formStateFromServer: {},
    });
  });

  describe('ROLE_EDITOR_SET_ROLE_NAME action', () => {
    it('changes the name prop value', () => {
      const state = {
        otherObject,
        formState: {
          name: initUserInput(''),
          description: initUserInput(''),
          otherObject,
        },
      };
      const roleName = 'Role name';
      const action = {
        type: 'ROLE_EDITOR_SET_ROLE_NAME',
        payload: { value: roleName, roles: [] },
      };
      const newState = reduce(state, action);
      expect(newState.formState.name.trimmedValue).toBe(roleName);
      expect(newState.otherObject).toBe(otherObject);
      expect(newState.formState.otherObject).toBe(otherObject);
    });

    it('has duplication error', () => {
      const state = {
        otherObject,
        formState: {
          id: 'Rol-Id',
          name: initUserInput(''),
          description: initUserInput(''),
          otherObject,
        },
      };
      const roleName = 'Role name';
      const action = {
        type: 'ROLE_EDITOR_SET_ROLE_NAME',
        payload: { value: roleName, roles: [{ name: roleName, id: 'OTHER-ID' }] },
      };
      const newState = reduce(state, action);
      expect(newState.formState.name.validationErrors).toEqual(['Name is already in use']);
      expect(newState.otherObject).toBe(otherObject);
      expect(newState.formState.otherObject).toBe(otherObject);
    });

    it('has non-empty validation', () => {
      const state = {
        otherObject,
        formState: {
          id: 'Rol-Id',
          name: initUserInput(''),
          description: initUserInput(''),
          otherObject,
        },
      };
      const roleName = '';
      const action = {
        type: 'ROLE_EDITOR_SET_ROLE_NAME',
        payload: { value: roleName, roles: [] },
      };
      const newState = reduce(state, action);
      expect(newState.formState.name.validationErrors).toEqual(['Must be non-empty']);
      expect(newState.otherObject).toBe(otherObject);
      expect(newState.formState.otherObject).toBe(otherObject);
    });

    it('does not have errors because it is the same role', () => {
      const state = {
        otherObject,
        formState: {
          id: 'Rol-Id',
          name: initUserInput(''),
          description: initUserInput(''),
          otherObject,
        },
      };
      const roleName = 'Role name';
      const action = {
        type: 'ROLE_EDITOR_SET_ROLE_NAME',
        payload: { value: roleName, roles: [{ id: 'Rol-Id', name: roleName }] },
      };
      const newState = reduce(state, action);
      expect(newState.formState.name.validationErrors).toEqual([]);
      expect(newState.otherObject).toBe(otherObject);
      expect(newState.formState.otherObject).toBe(otherObject);
    });
  });

  describe('ROLE_EDITOR_SET_ROLE_DESCRIPTION action', () => {
    it('changes the description prop value', () => {
      const roleDescription = 'Role description';
      const state = {
        otherObject,
        formState: {
          description: initUserInput(''),
          name: initUserInput(''),
          otherObject,
        },
      };
      const action = {
        type: 'ROLE_EDITOR_SET_ROLE_DESCRIPTION',
        payload: { value: roleDescription },
      };
      const newState = reduce(state, action);

      expect(newState.formState.description.trimmedValue).toBe(roleDescription);

      expect(newState.otherObject).toBe(otherObject);
      expect(newState.formState.otherObject).toBe(otherObject);
    });

    it('has non-empty validation', () => {
      const roleDescription = '';
      const state = {
        otherObject,
        formState: {
          description: initUserInput(''),
          name: initUserInput(''),
          otherObject,
        },
      };
      const action = {
        type: 'ROLE_EDITOR_SET_ROLE_DESCRIPTION',
        payload: { value: roleDescription },
      };
      const newState = reduce(state, action);

      expect(newState.formState.description.validationErrors).toBe('Must be non-empty');

      expect(newState.otherObject).toBe(otherObject);
      expect(newState.formState.otherObject).toBe(otherObject);
    });
  });

  describe('ROLE_EDITOR_TOGGLE_VALUE action', () => {
    it('changes a permission value', () => {
      const state = {
        otherObject,
        formState: {
          otherObject,
          permissionCategories: [
            {
              displayName: 'CATEGORY_DISPLAY_NAME',
              permissions: [{ id: 'PERMISSION_ID', allowed: false }],
            },
            {
              displayName: 'OTHER_CATEGORY_DISPLAY_NAME',
              permissions: [{ id: 'OTHER_PERMISSION_ID', allowed: false }],
            },
            otherObject,
          ],
        },
      };
      const action = {
        type: 'ROLE_EDITOR_TOGGLE_VALUE',
        payload: { category: 'CATEGORY_DISPLAY_NAME', id: 'PERMISSION_ID' },
      };
      const newState = reduce(state, action);

      expect(newState.formState.permissionCategories[0].permissions[0].allowed).toBe(true);
      expect(newState.formState.permissionCategories[1].permissions[0].allowed).toBe(false);

      expect(newState.otherObject).toBe(otherObject);
      expect(newState.formState.otherObject).toBe(otherObject);
      expect(newState.formState.permissionCategories[2]).toBe(otherObject);
    });
  });

  describe('ROLE_EDITOR_UPDATE_REQUESTED action', () => {
    it('sets false to updateMaskState', () => {
      const state = {
        otherObject,
        updateMaskState: null,
        formState: {
          otherObject,
        },
      };
      const action = {
        type: 'ROLE_EDITOR_UPDATE_REQUESTED',
      };
      const newState = reduce(state, action);

      expect(newState.updateMaskState).toBe(false);

      expect(newState.otherObject).toBe(otherObject);
      expect(newState.formState.otherObject).toBe(otherObject);
    });
  });

  describe('ROLE_EDITOR_UPDATE_FULFILLED action', () => {
    it('resets the state and sets true to updateMaskState', () => {
      const state = {
        otherObject,
        prop: 'prop value',
      };
      const action = {
        type: 'ROLE_EDITOR_UPDATE_FULFILLED',
      };
      const newState = reduce(state, action);
      expect(newState).toEqual({
        ...state,
        isDirty: false,
        updateMaskState: true,
      });
    });
  });

  describe('ROLE_EDITOR_UPDATE_FAILED action', () => {
    let newState, error;

    beforeEach(() => {
      error = 'some error happened';
      const state = {
        otherObject,
        formState: {
          otherObject,
        },
      };
      const action = { type: 'ROLE_EDITOR_UPDATE_FAILED', payload: error };
      newState = reduce(state, action);
    });

    it('does not affect to other props', () => {
      expect(newState.otherObject).toBe(otherObject);
      expect(newState.formState.otherObject).toBe(otherObject);
    });

    it('sets updateError', () => {
      expect(newState.updateError).toBe(error);
    });

    it('sets false to loading', () => {
      expect(newState.loading).toBe(false);
    });

    it('sets null to updateMaskState', () => {
      expect(newState.updateMaskState).toBeNull();
    });
  });

  describe('ROLE_EDITOR_SAVE_SUBMIT_MASK_TIMER_DONE action', () => {
    it('sets null to updateMaskState', () => {
      const state = {
        otherObject,
        updateMaskState: true,
        formState: {
          otherObject,
        },
      };
      const action = {
        type: 'ROLE_EDITOR_SAVE_SUBMIT_MASK_TIMER_DONE',
      };
      const newState = reduce(state, action);

      expect(newState.updateMaskState).toBeNull();
      expect(newState.otherObject).toBe(otherObject);
      expect(newState.formState.otherObject).toBe(otherObject);
    });
  });

  describe('ROLE_EDITOR_LOAD_REQUESTED action', () => {
    it('returns initial state', () => {
      const action = {
        type: 'ROLE_EDITOR_LOAD_REQUESTED',
      };
      const newState = reduce(initialState, action);
      expect(newState).toEqual(initialState);
    });
  });

  describe('ROLE_EDITOR_LOAD_FULFILLED action', () => {
    let state, newState, action, permissionCategories;

    beforeEach(() => {
      permissionCategories = [];
      state = {
        otherObject,
        formState: {
          otherObject,
        },
        formStateFromService: {},
      };
      const payload = {
        permissionCategories,
        id: 'TEST-ID',
        name: 'Role Name',
        description: 'Role Description',
      };
      action = {
        type: 'ROLE_EDITOR_LOAD_FULFILLED',
        payload,
      };

      newState = reduce(state, action);
    });

    it('sets permissionCategories in formStateFromService and in formState', () => {
      expect(newState.formStateFromServer.permissionCategories).toEqual(permissionCategories);
      expect(newState.formState.permissionCategories).toEqual(permissionCategories);
    });

    it('sets initial value to name, id and description', () => {
      expect(newState.formState.name).toEqual(initUserInput('Role Name'));
      expect(newState.formState.description).toEqual(initUserInput('Role Description'));
      expect(newState.formState.id).toBe('TEST-ID');
    });

    it('sets false to loading and null to loadError', () => {
      expect(newState.loading).toBe(false);
      expect(newState.loadError).toBeNull();
    });
  });

  describe('ROLE_EDITOR_SET_READONLY action', () => {
    it('sets true to readonly', () => {
      const state = {
        otherObject,
        readonly: false,
        formState: {
          otherObject,
        },
      };
      const action = {
        type: 'ROLE_EDITOR_SET_READONLY',
        payload: true,
      };
      const newState = reduce(state, action);
      expect(newState.readonly).toBe(true);

      expect(newState.otherObject).toBe(otherObject);
      expect(newState.formState.otherObject).toBe(otherObject);
    });
  });

  describe('ROLE_EDITOR_LOAD_FAILED action', () => {
    let state, action, newState, error;

    beforeEach(() => {
      error = 'some error happened';
      state = {
        otherObject,
        formState: {
          otherObject,
        },
      };
      action = {
        type: 'ROLE_EDITOR_LOAD_FAILED',
        payload: error,
      };
      newState = reduce(state, action);
    });

    it('sets error in payload to loadError', () => {
      expect(newState.loadError).toBe(error);
    });

    it('sets false to loading', () => {
      expect(newState.loading).toBe(false);
    });

    it('does not affect to other objects', () => {
      expect(newState.otherObject).toBe(otherObject);
      expect(newState.formState.otherObject).toBe(otherObject);
    });
  });

  describe('ROLE_EDITOR_DELETE_FAILED action', () => {
    it('fills the deleteError property', () => {
      const error = 'some error happened';
      const state = {
        otherObject,
        deleteError: null,
        deleteMaskState: true,
      };
      const action = {
        type: 'ROLE_EDITOR_DELETE_FAILED',
        payload: error,
      };
      const newState = reduce(state, action);

      expect(newState.deleteError).toBe(error);
      expect(newState.deleteMaskState).toBeNull();

      expect(newState.otherObject).toBe(otherObject);
    });
  });

  describe('ROLE_EDITOR_DELETE_REQUESTED action', () => {
    let newState;

    beforeEach(() => {
      const action = {
        type: 'ROLE_EDITOR_DELETE_REQUESTED',
      };
      const state = {
        otherObject,
        deleteMaskState: true,
      };
      newState = reduce(state, action);
    });

    it('sets false to deleteMaskState', () => {
      expect(newState.deleteMaskState).toBe(false);
    });
  });

  describe('ROLE_EDITOR_DELETE_FULFILLED action', () => {
    let newState;

    beforeEach(() => {
      const action = {
        type: 'ROLE_EDITOR_DELETE_FULFILLED',
      };
      const state = {
        otherObject,
        deleteMaskState: false,
        isDirty: true,
      };
      newState = reduce(state, action);
    });

    it('sets true to deleteMaskState', () => {
      expect(newState.deleteMaskState).toBe(true);
    });

    it('sets false to isDirty property', () => {
      expect(newState.isDirty).toBe(false);
    });
  });

  describe('ROLE_EDITOR_DELETE_MASK_TIMER_DONE', () => {
    it('sets null to deleteMaskState', () => {
      const state = {
        otherObject,
        deleteMaskState: true,
        formState: {
          otherObject,
        },
      };
      const action = {
        type: 'ROLE_EDITOR_DELETE_MASK_TIMER_DONE',
      };
      const newState = reduce(state, action);

      expect(newState.deleteMaskState).toBeNull();
      expect(newState.otherObject).toBe(otherObject);
      expect(newState.formState.otherObject).toBe(otherObject);
    });
  });
});
