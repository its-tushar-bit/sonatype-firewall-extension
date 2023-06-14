/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import * as textInputStateHelpers from '@sonatype/react-shared-components/components/NxTextInput/stateHelpers';
import reduce, { initialState } from '../../../../main/frontend/security/users/usersReducer';
import {
  CREATE_USER_LOAD_REQUESTED,
  CREATE_USER_LOAD_FULFILLED,
  CREATE_USER_LOAD_FAILED,
  CREATE_USER_SAVE_REQUESTED,
  CREATE_USER_SAVE_FULFILLED,
  CREATE_USER_SAVE_FAILED,
  USER_FORM_SUBMIT_MASK_TIMER_DONE,
  USER_FORM_DELETE_MASK_TIMER_DONE,
  USER_SET_FIRST_NAME,
  USER_SET_LAST_NAME,
  USER_SET_EMAIL,
  USER_SET_USERNAME,
  USER_SET_PASSWORD,
  USER_SET_MATCH_PASSWORD,
  EDIT_USER_LOAD_REQUESTED,
  EDIT_USER_LOAD_FAILED,
  EDIT_USER_LOAD_FULFILLED,
  EDIT_USER_UPDATE_REQUESTED,
  EDIT_USER_UPDATE_FULFILLED,
  EDIT_USER_UPDATE_FAILED,
  DELETE_USER_REQUESTED,
  DELETE_USER_FULFILLED,
  DELETE_USER_FAILED,
  RESET_USER_PASSWORD_REQUESTED,
  RESET_USER_PASSWORD_FULFILLED,
  RESET_USER_PASSWORD_FAILED,
  RESET_USER_PASSWORD_RESET_VALUE,
} from '../../../../main/frontend/security/users/usersActions';

describe('usersReducer', () => {
  let otherObject;

  beforeEach(() => {
    otherObject = { value: 'that is no moon' };
  });

  describe(`${CREATE_USER_LOAD_REQUESTED} action`, () => {
    it('resets to initial state', () => {
      const state = {
        other: otherObject,
      };

      const newState = reduce(state, { type: CREATE_USER_LOAD_REQUESTED });
      expect(newState).toBe(initialState);
    });
  });

  describe(`${CREATE_USER_LOAD_FULFILLED} action`, () => {
    it('resets loading and errors and sets fetched users', () => {
      const state = {
        users: [],
        loading: true,
        saveError: 'save error',
        loadError: 'load error',
        other: otherObject,
      };

      const payload = {
        users: [
          {
            id: 'ADMIN',
            username: 'admin',
            usernameLowercase: 'admin',
            password: '#~FAKE~PASSWORD~#',
            firstName: 'Admin',
            lastName: 'BuiltIn',
            email: 'admin@localhost',
          },
        ],
      };

      const { loading, users, saveError, loadError, other } = reduce(state, {
        type: CREATE_USER_LOAD_FULFILLED,
        payload,
      });

      expect(users.length).toBe(1);
      expect(users).toEqual(payload.users);
      expect(loading).toBe(false);
      expect(saveError).toBe(null);
      expect(loadError).toBe(null);
      expect(other).toBe(otherObject);
    });

    it('sets all inputFields when inviteMode is set to false', () => {
      const state = {
        users: [],
        loading: true,
      };

      const payload = {
        users: [],
        inviteMode: false,
      };

      const { inputFields } = reduce(state, {
        type: CREATE_USER_LOAD_FULFILLED,
        payload,
      });

      expect(inputFields).toEqual({
        firstName: textInputStateHelpers.initialState(''),
        lastName: textInputStateHelpers.initialState(''),
        email: textInputStateHelpers.initialState(''),
        username: textInputStateHelpers.initialState(''),
        password: textInputStateHelpers.initialState(''),
        matchPassword: textInputStateHelpers.initialState(''),
      });
    });

    it('sets only necessary inputFields when inviteMode is set to true', () => {
      const state = {
        users: [],
        loading: true,
      };

      const payload = {
        users: [],
        inviteMode: true,
      };

      const { inputFields } = reduce(state, {
        type: CREATE_USER_LOAD_FULFILLED,
        payload,
      });

      expect(inputFields).toEqual({
        firstName: textInputStateHelpers.initialState(''),
        lastName: textInputStateHelpers.initialState(''),
        email: textInputStateHelpers.initialState(''),
      });
      expect(inputFields.username).toBe(undefined);
      expect(inputFields.password).toBe(undefined);
      expect(inputFields.matchPassword).toBe(undefined);
    });
  });

  describe(`${CREATE_USER_LOAD_FAILED} action`, () => {
    it('resets loading and sets error to loadError', () => {
      const state = {
        users: [],
        loading: true,
        saveError: 'save error',
        loadError: null,
        other: otherObject,
      };

      const { loading, users, saveError, loadError, other } = reduce(state, {
        type: CREATE_USER_LOAD_FAILED,
        payload: 'load error occurred',
      });

      expect(users.length).toBe(0);
      expect(loading).toBe(false);
      expect(saveError).toBe(state.saveError);
      expect(loadError).toBe('load error occurred');
      expect(other).toBe(otherObject);
    });
  });

  describe(`${CREATE_USER_SAVE_REQUESTED} action`, () => {
    it('resets errors and sets mask to false', () => {
      const state = {
        saveError: 'save error',
        loadError: null,
        submitMaskState: true,
        other: otherObject,
      };

      const { submitMaskState, saveError, loadError, other } = reduce(state, {
        type: CREATE_USER_SAVE_REQUESTED,
      });

      expect(submitMaskState).toBe(false);
      expect(saveError).toBe(null);
      expect(loadError).toBe(null);
      expect(other).toBe(otherObject);
    });
  });

  describe(`${CREATE_USER_SAVE_FULFILLED} action`, () => {
    it('resets inputs to initial state, sets mask to true, dirty to false, clears errors', () => {
      const state = {
        submitMaskState: null,
        isDirty: true,
        inputFields: {
          firstName: 'fake',
          lastName: 'fake',
          email: 'fake',
          username: 'fake',
          password: 'fake',
          matchPassword: 'fake',
        },
        other: otherObject,
      };

      const { submitMaskState, isDirty, other, inputFields } = reduce(state, {
        type: CREATE_USER_SAVE_FULFILLED,
      });

      expect(submitMaskState).toBe(true);
      expect(isDirty).toBe(false);
      expect(inputFields).toEqual(initialState.inputFields);
      expect(other).toBe(otherObject);
    });
  });

  describe(`${CREATE_USER_SAVE_FAILED} action`, () => {
    it('resets mask, sets saveError', () => {
      const state = {
        saveError: null,
        loadError: 'error',
        submitMaskState: true,
        other: otherObject,
      };

      const { submitMaskState, saveError, loadError, other } = reduce(state, {
        type: CREATE_USER_SAVE_FAILED,
        payload: 'save error occurred',
      });

      expect(submitMaskState).toBeNull();
      expect(saveError).toBe('save error occurred');
      expect(loadError).toBe(state.loadError);
      expect(other).toBe(otherObject);
    });
  });

  describe(`${USER_FORM_SUBMIT_MASK_TIMER_DONE} action`, () => {
    it('resets submitMaskState', () => {
      const state = {
        submitMaskState: true,
        other: otherObject,
      };

      const { submitMaskState, other } = reduce(state, { type: USER_FORM_SUBMIT_MASK_TIMER_DONE });

      expect(submitMaskState).toBe(null);
      expect(other).toBe(otherObject);
    });
  });

  describe(`${USER_FORM_DELETE_MASK_TIMER_DONE} action`, () => {
    it('resets deleteMaskState', () => {
      const state = {
        deleteMaskState: true,
        other: otherObject,
      };

      const { deleteMaskState, other } = reduce(state, { type: USER_FORM_DELETE_MASK_TIMER_DONE });

      expect(deleteMaskState).toBe(null);
      expect(other).toBe(otherObject);
    });
  });

  describe(`${EDIT_USER_LOAD_REQUESTED} action`, () => {
    it('resets to initial state', () => {
      const state = {
        other: otherObject,
      };

      const newState = reduce(state, { type: EDIT_USER_LOAD_REQUESTED });
      expect(newState).toBe(initialState);
    });
  });

  describe(`${EDIT_USER_LOAD_FULFILLED} action`, () => {
    it('resets loading and errors and sets fetched users', () => {
      const state = {
        loading: true,
        saveError: 'update error',
        loadError: 'load error',
        other: otherObject,
      };

      const fetchedUser = {
        id: 'ADMIN',
        username: 'admin',
        usernameLowercase: 'admin',
        password: '#~FAKE~PASSWORD~#',
        firstName: 'Admin',
        lastName: 'BuiltIn',
        email: 'admin@localhost',
      };

      const { loading, selectedUserServerData, saveError, loadError, other, inputFields } = reduce(state, {
        type: EDIT_USER_LOAD_FULFILLED,
        payload: fetchedUser,
      });

      expect(selectedUserServerData).toEqual(fetchedUser);
      expect(loading).toBe(false);
      expect(saveError).toBe(null);
      expect(loadError).toBe(null);
      expect(other).toBe(otherObject);
      expect(inputFields).toEqual({
        firstName: textInputStateHelpers.initialState(fetchedUser.firstName),
        lastName: textInputStateHelpers.initialState(fetchedUser.lastName),
        email: textInputStateHelpers.initialState(fetchedUser.email),
      });
    });
  });

  describe(`${EDIT_USER_LOAD_FAILED} action`, () => {
    it('resets loading and sets error to loadError', () => {
      const state = {
        serverData: {},
        loading: true,
        updateError: 'save error',
        loadError: null,
        other: otherObject,
      };

      const { loading, updateError, loadError, other } = reduce(state, {
        type: EDIT_USER_LOAD_FAILED,
        payload: 'load error occurred',
      });

      expect(loading).toBe(false);
      expect(updateError).toBe(state.updateError);
      expect(loadError).toBe('load error occurred');
      expect(other).toBe(otherObject);
    });
  });

  describe(`${EDIT_USER_UPDATE_REQUESTED} action`, () => {
    it('resets errors and sets mask to false', () => {
      const state = {
        saveError: 'save error',
        loadError: null,
        submitMaskState: true,
        other: otherObject,
      };

      const { submitMaskState, saveError, loadError, other } = reduce(state, {
        type: EDIT_USER_UPDATE_REQUESTED,
      });

      expect(submitMaskState).toBe(false);
      expect(saveError).toBe(null);
      expect(loadError).toBe(null);
      expect(other).toBe(otherObject);
    });
  });

  describe(`${EDIT_USER_UPDATE_FULFILLED} action`, () => {
    it('sets mask to true, dirty to false, clears errors', () => {
      const state = {
        submitMaskState: null,
        isDirty: true,
        saveError: 'error',
        inputFields: {
          firstName: 'fake',
          lastName: 'fake',
          email: 'fake',
        },
        other: otherObject,
      };

      const { submitMaskState, isDirty, other, inputFields, saveError } = reduce(state, {
        type: EDIT_USER_UPDATE_FULFILLED,
      });

      expect(submitMaskState).toBe(true);
      expect(isDirty).toBe(false);
      expect(inputFields).toEqual(state.inputFields);
      expect(saveError).toBeNull();
      expect(other).toBe(otherObject);
    });
  });

  describe(`${EDIT_USER_UPDATE_FAILED} action`, () => {
    it('resets mask, sets updateError', () => {
      const state = {
        saveError: null,
        loadError: 'error',
        submitMaskState: true,
        other: otherObject,
      };

      const { submitMaskState, saveError, loadError, other } = reduce(state, {
        type: EDIT_USER_UPDATE_FAILED,
        payload: 'update error occurred',
      });

      expect(submitMaskState).toBeNull();
      expect(saveError).toBe('update error occurred');
      expect(loadError).toBe(state.loadError);
      expect(other).toBe(otherObject);
    });
  });

  describe(`${DELETE_USER_REQUESTED} action`, () => {
    it('sets delete mask to false', () => {
      const state = {
        deleteMaskState: true,
        other: otherObject,
      };

      const { deleteMaskState, other } = reduce(state, {
        type: DELETE_USER_REQUESTED,
      });

      expect(deleteMaskState).toBe(false);
      expect(other).toBe(otherObject);
    });
  });

  describe(`${DELETE_USER_FULFILLED} action`, () => {
    it('sets delete mask to true, dirty to false', () => {
      const state = {
        isDirty: true,
        deleteMaskState: null,
        other: otherObject,
      };

      const { deleteMaskState, isDirty, other } = reduce(state, {
        type: DELETE_USER_FULFILLED,
      });

      expect(deleteMaskState).toBe(true);
      expect(isDirty).toBe(false);
      expect(other).toBe(otherObject);
    });
  });

  describe(`${DELETE_USER_FAILED} action`, () => {
    it('resets delete mask, sets deleteError', () => {
      const state = {
        deleteError: null,
        deleteMaskState: true,
        other: otherObject,
      };

      const { deleteMaskState, deleteError, other } = reduce(state, {
        type: DELETE_USER_FAILED,
        payload: 'delete error occurred',
      });

      expect(deleteMaskState).toBeNull();
      expect(deleteError).toBe('delete error occurred');
      expect(other).toBe(otherObject);
    });
  });

  describe('input field actions', () => {
    let state;

    beforeEach(() => {
      state = {
        users: [
          {
            id: 'ADMIN',
            username: 'admin',
            usernameLowercase: 'admin',
            password: '#~FAKE~PASSWORD~#',
            firstName: 'Admin',
            lastName: 'BuiltIn',
            email: 'admin@localhost',
          },
        ],
        selectedUserServerData: {},
        inputFields: {
          firstName: textInputStateHelpers.initialState(''),
          lastName: textInputStateHelpers.initialState(''),
          username: textInputStateHelpers.initialState(''),
          email: textInputStateHelpers.initialState(''),
          password: textInputStateHelpers.initialState(''),
          matchPassword: textInputStateHelpers.initialState(''),
          other: otherObject,
        },
        other: otherObject,
      };
    });

    describe(`${USER_SET_FIRST_NAME} action`, () => {
      it('sets firstName userInput', () => {
        const { inputFields, other } = reduce(state, {
          type: USER_SET_FIRST_NAME,
          payload: 'John',
        });

        expect(other).toBe(otherObject);
        expect(inputFields.other).toBe(otherObject);
        expect(inputFields.firstName.value).toBe('John');
      });

      it('sets firstName userInput with "Must be non-empty" error', () => {
        const { inputFields, other } = reduce(state, {
          type: USER_SET_FIRST_NAME,
          payload: '',
        });

        expect(other).toBe(otherObject);
        expect(inputFields.other).toBe(otherObject);
        expect(inputFields.firstName.value).toBe('');
        expect(inputFields.firstName.validationErrors).toEqual(['Must be non-empty']);
      });

      it('sets firstName userInput with "Use valid characters" error', () => {
        const { inputFields, other } = reduce(state, {
          type: USER_SET_FIRST_NAME,
          payload: '&',
        });

        expect(other).toBe(otherObject);
        expect(inputFields.other).toBe(otherObject);
        expect(inputFields.firstName.value).toBe('&');
        expect(inputFields.firstName.validationErrors).toEqual([
          'Use valid characters: alphanumeric, "_", ".", "-", or spaces',
        ]);
      });

      it('sets firstName userInput with "No leading, trailing or double spaces or tabs" error', () => {
        const { inputFields, other } = reduce(state, {
          type: USER_SET_FIRST_NAME,
          payload: 'a  a',
        });

        expect(other).toBe(otherObject);
        expect(inputFields.other).toBe(otherObject);
        expect(inputFields.firstName.value).toBe('a  a');
        expect(inputFields.firstName.validationErrors).toEqual(['No leading, trailing or double spaces or tabs']);
      });
    });

    describe(`${USER_SET_LAST_NAME} action`, () => {
      it('sets lastName userInput', () => {
        const { inputFields, other } = reduce(state, {
          type: USER_SET_LAST_NAME,
          payload: 'Doe',
        });

        expect(other).toBe(otherObject);
        expect(inputFields.other).toBe(otherObject);
        expect(inputFields.lastName.value).toBe('Doe');
      });

      it('sets lastName userInput with "Must be non-empty" error', () => {
        const { inputFields, other } = reduce(state, {
          type: USER_SET_LAST_NAME,
          payload: '',
        });

        expect(other).toBe(otherObject);
        expect(inputFields.other).toBe(otherObject);
        expect(inputFields.lastName.value).toBe('');
        expect(inputFields.lastName.validationErrors).toEqual(['Must be non-empty']);
      });

      it('sets lastName userInput with "Use valid characters" error', () => {
        const { inputFields, other } = reduce(state, {
          type: USER_SET_LAST_NAME,
          payload: '^',
        });

        expect(other).toBe(otherObject);
        expect(inputFields.other).toBe(otherObject);
        expect(inputFields.lastName.value).toBe('^');
        expect(inputFields.lastName.validationErrors).toEqual([
          'Use valid characters: alphanumeric, "_", ".", "-", or spaces',
        ]);
      });

      it('sets lastName userInput with "No leading, trailing or double spaces or tabs" error', () => {
        const { inputFields, other } = reduce(state, {
          type: USER_SET_LAST_NAME,
          payload: 'b  b',
        });

        expect(other).toBe(otherObject);
        expect(inputFields.other).toBe(otherObject);
        expect(inputFields.lastName.value).toBe('b  b');
        expect(inputFields.lastName.validationErrors).toEqual(['No leading, trailing or double spaces or tabs']);
      });
    });

    describe(`${USER_SET_EMAIL} action`, () => {
      it('sets email userInput', () => {
        const { inputFields, other } = reduce(state, {
          type: USER_SET_EMAIL,
          payload: 'john@doe.com',
        });

        expect(other).toBe(otherObject);
        expect(inputFields.other).toBe(otherObject);
        expect(inputFields.email.value).toBe('john@doe.com');
      });

      it('sets email userInput with "Must be non-empty" error', () => {
        const { inputFields, other } = reduce(state, {
          type: USER_SET_EMAIL,
          payload: '',
        });

        expect(other).toBe(otherObject);
        expect(inputFields.other).toBe(otherObject);
        expect(inputFields.email.value).toBe('');
        expect(inputFields.email.validationErrors[0]).toEqual('Must be non-empty');
      });

      it('sets email userInput with "Use valid format: abc@xyz.com" error', () => {
        const { inputFields, other } = reduce(state, {
          type: USER_SET_EMAIL,
          payload: '@.com',
        });

        expect(other).toBe(otherObject);
        expect(inputFields.other).toBe(otherObject);
        expect(inputFields.email.value).toBe('@.com');
        expect(inputFields.email.validationErrors).toEqual(['Use valid format: abc@xyz.com']);
      });
    });

    describe(`${USER_SET_USERNAME} action`, () => {
      it('sets username userInput', () => {
        const { inputFields, other } = reduce(state, {
          type: USER_SET_USERNAME,
          payload: 'johnDoe',
        });

        expect(other).toBe(otherObject);
        expect(inputFields.other).toBe(otherObject);
        expect(inputFields.username.value).toBe('johnDoe');
      });

      it('sets username userInput with "Must be non-empty" error', () => {
        const { inputFields, other } = reduce(state, {
          type: USER_SET_USERNAME,
          payload: '',
        });

        expect(other).toBe(otherObject);
        expect(inputFields.other).toBe(otherObject);
        expect(inputFields.username.value).toBe('');
        expect(inputFields.username.validationErrors).toEqual(['Must be non-empty']);
      });

      it('sets username userInput with "Use valid characters" error', () => {
        const { inputFields, other } = reduce(state, {
          type: USER_SET_USERNAME,
          payload: '^',
        });

        expect(other).toBe(otherObject);
        expect(inputFields.other).toBe(otherObject);
        expect(inputFields.username.value).toBe('^');
        expect(inputFields.username.validationErrors).toEqual(['Use valid characters: alphanumeric, "_", "." or "-"']);
      });

      it('sets username userInput with "Use valid characters" error', () => {
        const { inputFields, other } = reduce(state, {
          type: USER_SET_USERNAME,
          payload: 'f g',
        });

        expect(other).toBe(otherObject);
        expect(inputFields.other).toBe(otherObject);
        expect(inputFields.username.value).toBe('f g');
        expect(inputFields.username.validationErrors).toEqual(['Use valid characters: alphanumeric, "_", "." or "-"']);
      });

      it('sets username userInput with "Username already taken" error', () => {
        const { inputFields, other } = reduce(state, {
          type: USER_SET_USERNAME,
          payload: 'admin',
        });

        expect(other).toBe(otherObject);
        expect(inputFields.other).toBe(otherObject);
        expect(inputFields.username.value).toBe('admin');
        expect(inputFields.username.validationErrors).toEqual(['Username already taken']);
      });
    });

    describe(`${USER_SET_PASSWORD} action`, () => {
      it('sets password userInput', () => {
        const { inputFields, other } = reduce(state, {
          type: USER_SET_PASSWORD,
          payload: '1234',
        });

        expect(other).toBe(otherObject);
        expect(inputFields.other).toBe(otherObject);
        expect(inputFields.password.value).toBe('1234');
      });

      it('sets username userInput with "Must be non-empty" error', () => {
        const { inputFields, other } = reduce(state, {
          type: USER_SET_PASSWORD,
          payload: '',
        });

        expect(other).toBe(otherObject);
        expect(inputFields.other).toBe(otherObject);
        expect(inputFields.password.value).toBe('');
        expect(inputFields.password.validationErrors).toEqual('Must be non-empty');
      });
    });

    describe(`${USER_SET_MATCH_PASSWORD} action`, () => {
      it('sets matchPassword userInput', () => {
        const { inputFields, other } = reduce(state, {
          type: USER_SET_MATCH_PASSWORD,
          payload: '1234',
        });

        expect(other).toBe(otherObject);
        expect(inputFields.other).toBe(otherObject);
        expect(inputFields.matchPassword.value).toBe('1234');
      });

      it('sets matchPassword userInput with "Must be non-empty" error', () => {
        const { inputFields, other } = reduce(state, {
          type: USER_SET_MATCH_PASSWORD,
          payload: '',
        });

        expect(other).toBe(otherObject);
        expect(inputFields.other).toBe(otherObject);
        expect(inputFields.matchPassword.value).toBe('');
        expect(inputFields.matchPassword.validationErrors).toEqual('Must be non-empty');
      });

      it('sets matchPassword userInput with "Passwords must match!" error', () => {
        const { inputFields, other } = reduce(state, {
          type: USER_SET_MATCH_PASSWORD,
          payload: 'qwe',
        });

        expect(other).toBe(otherObject);
        expect(inputFields.other).toBe(otherObject);
        expect(inputFields.matchPassword.value).toBe('qwe');
        expect(inputFields.matchPassword.validationErrors).toEqual('Passwords must match!');
      });
    });

    describe('password and matchPassword behavior', () => {
      it('matchPassword does not show error if pristine and password field was changed', () => {
        const { inputFields, other } = reduce(state, {
          type: USER_SET_PASSWORD,
          payload: 'asdf',
        });

        expect(other).toBe(otherObject);
        expect(inputFields.other).toBe(otherObject);
        expect(inputFields.password.value).toBe('asdf');
        expect(inputFields.password.validationErrors).toBeNull();
        expect(inputFields.matchPassword.validationErrors).toBeNull();
      });

      it('matchPassword shows error if password and matchPassword fields were changed', () => {
        state = reduce(state, {
          type: USER_SET_MATCH_PASSWORD,
          payload: '',
        });

        const { inputFields, other } = reduce(state, {
          type: USER_SET_PASSWORD,
          payload: 'asdf',
        });

        expect(other).toBe(otherObject);
        expect(inputFields.other).toBe(otherObject);
        expect(inputFields.matchPassword.value).toBe('');
        expect(inputFields.password.value).toBe('asdf');
        expect(inputFields.matchPassword.validationErrors).toEqual('Must be non-empty');
        expect(inputFields.password.validationErrors).toBeNull();
      });

      it('shows error if password and matchPassword fields both were changed to empty values', () => {
        state = reduce(state, {
          type: USER_SET_PASSWORD,
          payload: '',
        });

        const { inputFields, other } = reduce(state, {
          type: USER_SET_MATCH_PASSWORD,
          payload: '',
        });

        expect(other).toBe(otherObject);
        expect(inputFields.other).toBe(otherObject);
        expect(inputFields.matchPassword.value).toBe('');
        expect(inputFields.password.value).toBe('');
        expect(inputFields.matchPassword.validationErrors).toEqual('Must be non-empty');
        expect(inputFields.matchPassword.validationErrors).toEqual('Must be non-empty');
      });
    });
  });

  describe(`${RESET_USER_PASSWORD_REQUESTED} action`, () => {
    it('sets resetMaskState to false', () => {
      const state = {
        resetMaskState: true,
        other: otherObject,
      };

      const { resetMaskState, other } = reduce(state, {
        type: RESET_USER_PASSWORD_REQUESTED,
      });

      expect(resetMaskState).toBe(false);
      expect(other).toBe(otherObject);
    });
  });

  describe(`${RESET_USER_PASSWORD_FULFILLED} action`, () => {
    it('sets resetMaskState to true, resets resetError to null, newPassword to payload', () => {
      const state = {
        resetMaskState: null,
        newPassword: null,
        resetError: 'error',
        other: otherObject,
      };

      const { resetMaskState, newPassword, resetError, other } = reduce(state, {
        type: RESET_USER_PASSWORD_FULFILLED,
        payload: { newPassword: 'weAreDoomed' },
      });

      expect(resetMaskState).toBe(true);
      expect(newPassword).toBe('weAreDoomed');
      expect(resetError).toBeNull();
      expect(other).toBe(otherObject);
    });
  });

  describe(`${RESET_USER_PASSWORD_FAILED} action`, () => {
    it('resets resetMaskState, sets resetError', () => {
      const state = {
        resetError: null,
        resetMaskState: true,
        other: otherObject,
      };

      const { resetMaskState, resetError, other } = reduce(state, {
        type: RESET_USER_PASSWORD_FAILED,
        payload: 'error occurred',
      });

      expect(resetMaskState).toBeNull();
      expect(resetError).toBe('error occurred');
      expect(other).toBe(otherObject);
    });
  });

  describe(`${RESET_USER_PASSWORD_RESET_VALUE} action`, () => {
    it('resets newPassword, resetMaskState and resetError', () => {
      const state = {
        resetError: 'error',
        resetMaskState: true,
        newPassword: 'weAreDoomed',
        other: otherObject,
      };

      const { resetMaskState, resetError, newPassword, other } = reduce(state, {
        type: RESET_USER_PASSWORD_RESET_VALUE,
      });

      expect(resetMaskState).toBeNull();
      expect(resetError).toBeNull();
      expect(newPassword).toBeNull();
      expect(other).toBe(otherObject);
    });
  });
});
