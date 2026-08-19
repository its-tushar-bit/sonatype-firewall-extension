/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import reducer from '../../../../../../main/frontend/mainHeader/MenuBar/UserMenu/UserToken/userTokenReducer';
import {
  USER_TOKEN_CHECK_TOKEN_EXISTENCE_FAILED,
  USER_TOKEN_CHECK_TOKEN_EXISTENCE_FULFILLED,
  USER_TOKEN_CHECK_TOKEN_EXISTENCE_REQUESTED,
  USER_TOKEN_DELETE_TOKEN_FAILED,
  USER_TOKEN_DELETE_TOKEN_FULFILLED,
  USER_TOKEN_DELETE_TOKEN_REQUESTED,
  USER_TOKEN_GENERATE_TOKEN_FAILED,
  USER_TOKEN_GENERATE_TOKEN_FULFILLED,
  USER_TOKEN_GENERATE_TOKEN_REQUESTED,
  USER_TOKEN_HIDE_MODAL,
  USER_TOKEN_MASK_TIMER_DONE,
  USER_TOKEN_SHOW_MODAL,
} from '../../../../../../main/frontend/mainHeader/MenuBar/UserMenu/UserToken/userTokenActions';
import { UI_ROUTER_ON_FINISH } from '../../../../../../main/frontend/reduxUiRouter/routerActions';

describe('userTokenReducer', function () {
  describe('unknown action', function () {
    it('returns original state', function () {
      const state = Object.freeze({ foo: 'bar' });
      const action = { type: 'UNKNOWN' };
      const newState = reducer(state, action);
      expect(newState).toBe(state);
    });
  });

  describe('initial state', function () {
    it('is used if no state is provided', function () {
      const action = { type: 'UNKNOWN' };
      const newState = reducer(undefined, action);
      expect(newState).not.toBeUndefined();
    });

    it('has default fields', function () {
      const action = { type: 'UNKNOWN' };
      const newState = reducer(undefined, action);
      expect(newState).not.toBeUndefined();
      expect(newState.isUserTokenModalVisible).toBe(false);
      expect(newState.userToken).toBeNull();
      expect(newState.checkUserTokenError).toBeNull();
      expect(newState.checkUserTokenLoading).toBe(false);
      expect(newState.generateUserTokenError).toBeNull();
      expect(newState.generateUserTokenLoading).toBeNull();
      expect(newState.deleteUserTokenError).toBeNull();
      expect(newState.deleteUserTokenLoading).toBeNull();
    });

    it('is immutable', function () {
      const action = { type: 'UNKNOWN' };
      const state = reducer(undefined, action);

      expect(() => {
        state.newProp = 'newProp';
      }).toThrow(TypeError);

      expect(() => {
        state.isUserTokenModalVisible = true;
      }).toThrow(TypeError);

      expect(() => {
        state.userToken = 'userToken';
      }).toThrow(TypeError);

      expect(() => {
        state.checkUserTokenError = 'error';
      }).toThrow(TypeError);

      expect(() => {
        state.checkUserTokenLoading = true;
      }).toThrow(TypeError);

      expect(() => {
        state.generateUserTokenError = 'error';
      }).toThrow(TypeError);

      expect(() => {
        state.generateUserTokenLoading = true;
      }).toThrow(TypeError);

      expect(() => {
        state.deleteUserTokenError = 'error';
      }).toThrow(TypeError);

      expect(() => {
        state.deleteUserTokenLoading = true;
      }).toThrow(TypeError);
    });
  });

  describe('USER_TOKEN_SHOW_MODAL action', function () {
    it('sets isUserTokenModalVisible to true', function () {
      const state = {
        isUserTokenModalVisible: false,
        otherProp: 'foo',
      };
      const action = { type: USER_TOKEN_SHOW_MODAL };
      const newState = reducer(state, action);
      expect(newState.isUserTokenModalVisible).toBe(true);
      expect(newState.otherProp).toBe('foo');
    });
  });

  describe('USER_TOKEN_HIDE_MODAL action', function () {
    it('sets isUserTokenModalVisible to false and restores the state to the init state', function () {
      const reducerInitState = reducer(undefined, 'unknownActionToReturnInitState');

      const state = {
        isUserTokenModalVisible: true,
        otherProp: 'foo',
      };
      const action = { type: USER_TOKEN_HIDE_MODAL };
      const newState = reducer(state, action);
      expect(newState.isUserTokenModalVisible).toBe(false);
      expect(newState).toEqual(reducerInitState);
    });
  });

  describe('UI_ROUTER_ON_FINISH action', function () {
    it('resets state to initState', function () {
      const reducerInitState = reducer(undefined, 'unknownActionToReturnInitState');

      const state = {
        isUserTokenModalVisible: true,
        otherProp: 'foo',
        userToken: 'userToken',
        checkUserTokenError: null,
        checkUserTokenLoading: false,
      };
      const action = { type: UI_ROUTER_ON_FINISH };
      const newState = reducer(state, action);
      expect(newState).toEqual(reducerInitState);
    });
  });

  describe('USER_TOKEN_MASK_TIMER_DONE action', function () {
    it('unsets deleteUserTokenLoading', function () {
      const state = {
        otherProp: 'foo',
        deleteUserTokenLoading: true,
      };
      const action = { type: USER_TOKEN_MASK_TIMER_DONE };
      const newState = reducer(state, action);
      expect(newState.deleteUserTokenLoading).toBeNull();
      expect(newState.otherProp).toBe('foo');
    });
  });

  describe('USER_TOKEN_CHECK_TOKEN_EXISTENCE_REQUESTED action', function () {
    it('unsets userToken and checkUserTokenError, also sets checkUserTokenLoading to true', function () {
      const state = {
        userToken: 'userToken',
        checkUserTokenError: 'Err',
        checkUserTokenLoading: false,
        otherProp: 'foo',
      };
      const action = { type: USER_TOKEN_CHECK_TOKEN_EXISTENCE_REQUESTED };
      const newState = reducer(state, action);
      expect(newState.userToken).toBeNull();
      expect(newState.checkUserTokenError).toBeNull();
      expect(newState.checkUserTokenLoading).toBe(true);
      expect(newState.otherProp).toBe('foo');
    });
  });

  describe('USER_TOKEN_CHECK_TOKEN_EXISTENCE_FAILED action', function () {
    it('unsets userToken, also sets checkUserTokenLoading to false and checkUserTokenError to the payload', function () {
      const state = {
        userToken: 'userToken',
        checkUserTokenError: null,
        checkUserTokenLoading: true,
        otherProp: 'foo',
      };
      const action = {
        type: USER_TOKEN_CHECK_TOKEN_EXISTENCE_FAILED,
        payload: 'Err',
      };
      const newState = reducer(state, action);
      expect(newState.userToken).toBeNull();
      expect(newState.checkUserTokenError).toBe('Err');
      expect(newState.checkUserTokenLoading).toBe(false);
      expect(newState.otherProp).toBe('foo');
    });
  });

  describe('USER_TOKEN_CHECK_TOKEN_EXISTENCE_FULFILLED action', function () {
    it('unsets checkUserTokenError, also sets checkUserTokenLoading to false and userToken to the payload', function () {
      const state = {
        userToken: null,
        checkUserTokenError: 'Err',
        checkUserTokenLoading: true,
        otherProp: 'foo',
      };
      const action = {
        type: USER_TOKEN_CHECK_TOKEN_EXISTENCE_FULFILLED,
        payload: true,
      };
      const newState = reducer(state, action);
      expect(newState.userToken).toBe(true);
      expect(newState.checkUserTokenError).toBeNull();
      expect(newState.checkUserTokenLoading).toBe(false);
      expect(newState.otherProp).toBe('foo');
    });
  });

  describe('USER_TOKEN_GENERATE_TOKEN_REQUESTED action', function () {
    it('unsets generateUserTokenError, also sets generateUserTokenLoading to false', function () {
      const state = {
        generateUserTokenError: 'Err',
        generateUserTokenLoading: true,
        otherProp: 'foo',
      };
      const action = { type: USER_TOKEN_GENERATE_TOKEN_REQUESTED };
      const newState = reducer(state, action);
      expect(newState.generateUserTokenError).toBeNull();
      expect(newState.generateUserTokenLoading).toBe(false);
      expect(newState.otherProp).toBe('foo');
    });
  });

  describe('USER_TOKEN_GENERATE_TOKEN_FAILED action', function () {
    it('unsets generateUserTokenLoading, also sets generateUserTokenError to the payload', function () {
      const state = {
        generateUserTokenError: null,
        generateUserTokenLoading: false,
        otherProp: 'foo',
      };
      const action = { type: USER_TOKEN_GENERATE_TOKEN_FAILED, payload: 'Err' };
      const newState = reducer(state, action);
      expect(newState.generateUserTokenError).toBe('Err');
      expect(newState.generateUserTokenLoading).toBeNull();
      expect(newState.otherProp).toBe('foo');
    });
  });

  describe('USER_TOKEN_GENERATE_TOKEN_FULFILLED action', function () {
    it('unsets generateUserTokenLoading and generateUserTokenError, also sets userToken to the payload', function () {
      const state = {
        generateUserTokenError: 'Err',
        generateUserTokenLoading: false,
        otherProp: 'foo',
      };
      const action = {
        type: USER_TOKEN_GENERATE_TOKEN_FULFILLED,
        payload: 'userToken',
      };
      const newState = reducer(state, action);
      expect(newState.userToken).toBe('userToken');
      expect(newState.generateUserTokenError).toBeNull();
      expect(newState.generateUserTokenLoading).toBeNull();
      expect(newState.otherProp).toBe('foo');
    });
  });

  describe('USER_TOKEN_DELETE_TOKEN_REQUESTED action', function () {
    it('unsets deleteUserTokenError, also sets deleteUserTokenLoading to false', function () {
      const state = {
        deleteUserTokenError: 'Err',
        deleteUserTokenLoading: true,
        otherProp: 'foo',
      };
      const action = { type: USER_TOKEN_DELETE_TOKEN_REQUESTED };
      const newState = reducer(state, action);
      expect(newState.deleteUserTokenError).toBeNull();
      expect(newState.deleteUserTokenLoading).toBe(false);
      expect(newState.otherProp).toBe('foo');
    });
  });

  describe('USER_TOKEN_DELETE_TOKEN_FAILED action', function () {
    it('unsets deleteUserTokenLoading, also sets deleteUserTokenError to the payload', function () {
      const state = {
        deleteUserTokenError: null,
        deleteUserTokenLoading: false,
        otherProp: 'foo',
      };
      const action = { type: USER_TOKEN_DELETE_TOKEN_FAILED, payload: 'Err' };
      const newState = reducer(state, action);
      expect(newState.deleteUserTokenError).toBe('Err');
      expect(newState.deleteUserTokenLoading).toBeNull();
      expect(newState.otherProp).toBe('foo');
    });
  });

  describe('USER_TOKEN_DELETE_TOKEN_FULFILLED action', function () {
    it('unsets userToken and deleteUserTokenError, also sets deleteUserTokenLoading to true', function () {
      const state = {
        userToken: 'userToken',
        deleteUserTokenError: 'Err',
        deleteUserTokenLoading: false,
        otherProp: 'foo',
      };
      const action = {
        type: USER_TOKEN_DELETE_TOKEN_FULFILLED,
        payload: 'userToken',
      };
      const newState = reducer(state, action);
      expect(newState.userToken).toBeNull();
      expect(newState.deleteUserTokenError).toBeNull();
      expect(newState.deleteUserTokenLoading).toBe(true);
      expect(newState.otherProp).toBe('foo');
    });
  });
});
