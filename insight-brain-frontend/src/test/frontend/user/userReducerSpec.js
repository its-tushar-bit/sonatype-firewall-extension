/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import changeDefaultAdminPasswordNoticeModule from '../../../main/frontend/changeDefaultAdminPasswordNotice/module';

describe('userReducer', function() {
  let reduce;

  beforeEach(angular.mock.module(changeDefaultAdminPasswordNoticeModule.name));

  beforeEach(inject((userReducer) => {
    reduce = userReducer;
  }));

  describe('unknown action', () => {
    it('returns original state', () => {
      const state = Object.freeze({
        currentUser: null,
        isDefaultUser: false,
        shouldDisplayNotice: false,
        canChangePassword: false,
        other: {
          random: 'prop'
        }
      });
      const action = {
        type: 'UNKNOWN'
      };
      const newState = reduce(state, action);
      expect(newState.currentUser).toBe(state.currentUser);
      expect(newState.isDefaultUser).toBe(state.isDefaultUser);
      expect(newState.shouldDisplayNotice).toBe(state.shouldDisplayNotice);
      expect(newState.canChangePassword).toBe(state.canChangePassword);
      expect(newState.other).toBe(state.other);
    });
  });

  describe('initial state', function() {
    it('is used if no state is provided', () => {
      const action = {
        type: 'UNKNOWN'
      };
      const newState = reduce(undefined, action);
      expect(newState.currentUser).toBe(null);
      expect(newState.isDefaultUser).toBe(false);
      expect(newState.shouldDisplayNotice).toBe(false);
      expect(newState.canChangePassword).toBe(false);
    });
  });

  describe('DEFAULT_ADMIN_PASSWORD_CHANGED action', () => {
    it('sets shouldDisplayNotice to false', () => {
      const state = Object.freeze({
        shouldDisplayNotice: true,
        other: {
          random: 'prop'
        }
      });
      const action = {
        type: 'DEFAULT_ADMIN_PASSWORD_CHANGED'
      };
      const newState = reduce(state, action);
      expect(newState.shouldDisplayNotice).toBe(false);
      expect(newState.other).toBe(state.other);
    });
  });

  describe('LOAD_USER_FULFILLED action', () => {
    it('should set currentUser', () => {
      const user = {
        username: 'admin',
        internalUser: true
      };
      const state = Object.freeze({
        currentUser: null,
        other: {
          random: 'prop'
        }
      });
      const action = {
        type: 'LOAD_USER_FULFILLED',
        payload: {
          currentUser: user
        }
      };
      const newState = reduce(state, action);
      expect(newState.currentUser).toBe(user);
      expect(newState.other).toBe(state.other);
    });

    it('should set isDefaultUser according to the username: admin', () => {
      const user = {
        username: 'admin'
      };
      const state = Object.freeze({
        currentUser: null,
        isDefaultUser: false,
        other: {
          random: 'prop'
        }
      });
      const action = {
        type: 'LOAD_USER_FULFILLED',
        payload: {
          currentUser: user
        }
      };
      const newState = reduce(state, action);
      expect(newState.currentUser.username).toBe(user.username);
      expect(newState.isDefaultUser).toBe(true);
      expect(newState.other).toBe(state.other);
    });

    it('should set isDefaultUser according to the username: foo', () => {
      const user = {
        username: 'foo'
      };
      const state = Object.freeze({
        currentUser: null,
        isDefaultUser: false,
        other: {
          random: 'prop'
        }
      });
      const action = {
        type: 'LOAD_USER_FULFILLED',
        payload: {
          currentUser: user
        }
      };
      const newState = reduce(state, action);
      expect(newState.currentUser.username).toBe(user.username);
      expect(newState.isDefaultUser).toBe(false);
      expect(newState.other).toBe(state.other);
    });

    it('should set canChangePassword true for internal users', () => {
      const user = {
        internalUser: true
      };
      const state = Object.freeze({
        canChangePassword: false,
        other: {
          random: 'prop'
        }
      });
      const action = {
        type: 'LOAD_USER_FULFILLED',
        payload: {
          currentUser: user
        }
      };
      const newState = reduce(state, action);
      expect(newState.canChangePassword).toBe(true);
      expect(newState.currentUser.internalUser).toBe(user.internalUser);
      expect(newState.other).toBe(state.other);
    });

    it('should set canChangePassword false for non-internal users', () => {
      const user = {
        internalUser: false
      };
      const state = Object.freeze({
        canChangePassword: true,
        other: {
          random: 'prop'
        }
      });
      const action = {
        type: 'LOAD_USER_FULFILLED',
        payload: {
          currentUser: user
        }
      };
      const newState = reduce(state, action);
      expect(newState.canChangePassword).toBe(false);
      expect(newState.currentUser.internalUser).toBe(user.internalUser);
      expect(newState.other).toBe(state.other);
    });

    it('should set shouldDisplayNotice based on the payload shouldDisplayWarning field: true', () => {
      const state = Object.freeze({
        shouldDisplayNotice: false,
        other: {
          random: 'prop'
        }
      });
      const action = {
        type: 'LOAD_USER_FULFILLED',
        payload: {
          shouldDisplayWarning: true
        }
      };
      const newState = reduce(state, action);
      expect(newState.shouldDisplayNotice).toBe(true);
      expect(newState.other).toBe(state.other);
    });

    it('should set shouldDisplayNotice based on the payload shouldDisplayWarning field: false', () => {
      const state = Object.freeze({
        shouldDisplayNotice: true,
        other: {
          random: 'prop'
        }
      });
      const action = {
        type: 'LOAD_USER_FULFILLED',
        payload: {
          shouldDisplayWarning: false
        }
      };
      const newState = reduce(state, action);
      expect(newState.shouldDisplayNotice).toBe(false);
      expect(newState.other).toBe(state.other);
    });
  });
});
