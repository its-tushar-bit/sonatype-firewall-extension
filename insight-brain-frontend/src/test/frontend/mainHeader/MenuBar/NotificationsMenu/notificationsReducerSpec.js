/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import reducer from '../../../../../main/frontend/mainHeader/MenuBar/NotificationsMenu/notificationsReducer';
import {
  MAIN_MENU_LOAD_NOTIFICATIONS_FAILED,
  MAIN_MENU_LOAD_NOTIFICATIONS_FULFILLED,
  MAIN_MENU_LOAD_NOTIFICATIONS_REQUESTED,
  MAIN_MENU_SET_NOTIFICATION_SEEN,
} from '../../../../../main/frontend/mainHeader/MenuBar/NotificationsMenu/notificationsActions';

describe('notifications reducer', function () {
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
      expect(newState.loading).toBe(false);
      expect(newState.error).toBeNull();
      expect(newState.notificationsToDisplay).toBeNull();
    });
  });

  describe('MAIN_MENU_LOAD_NOTIFICATIONS_REQUESTED', function () {
    it('unsets the error and current notifications and sets the loading flag to true', function () {
      const state = {
        loading: false,
        error: 'some previous error',
        notificationsToDisplay: ['impossible notification with error'],
        otherProp: 'foo',
      };

      const action = { type: MAIN_MENU_LOAD_NOTIFICATIONS_REQUESTED };
      const newState = reducer(state, action);

      expect(newState.loading).toBe(true);
      expect(newState.error).toBeNull();
      expect(newState.notificationsToDisplay).toBeNull();
      expect(newState.otherProp).toBe('foo');
    });
  });

  describe('MAIN_MENU_LOAD_NOTIFICATIONS_FULFILLED', function () {
    it('unsets the error, sets loading flag to false and sets the notifications to the payload', function () {
      const state = {
        loading: true,
        error: 'some previous impossible error with loading true',
        notificationsToDisplay: null,
        otherProp: 'foo',
      };

      const action = {
        type: MAIN_MENU_LOAD_NOTIFICATIONS_FULFILLED,
        payload: ['notification from the server'],
      };
      const newState = reducer(state, action);

      expect(newState.loading).toBe(false);
      expect(newState.error).toBeNull();
      expect(newState.notificationsToDisplay).toEqual(['notification from the server']);
      expect(newState.otherProp).toBe('foo');
    });
  });

  describe('MAIN_MENU_LOAD_NOTIFICATIONS_FAILED', function () {
    it('sets loading flag to false and unsets current notifications, sets the error to the payload', function () {
      const state = {
        loading: true,
        error: null,
        notificationsToDisplay: ['previous notifications'],
        otherProp: 'foo',
      };

      const action = {
        type: MAIN_MENU_LOAD_NOTIFICATIONS_FAILED,
        payload: 'error occurred while loading notifications',
      };
      const newState = reducer(state, action);

      expect(newState.loading).toBe(false);
      expect(newState.notificationsToDisplay).toBeNull();
      expect(newState.error).toEqual('error occurred while loading notifications');
      expect(newState.otherProp).toBe('foo');
    });

    it('sets the error to the message in the payload', function () {
      const state = {
        error: null,
      };

      const action = {
        type: MAIN_MENU_LOAD_NOTIFICATIONS_FAILED,
        payload: { response: 'error occurred while loading notifications' },
      };
      const newState = reducer(state, action);

      expect(newState.error).toEqual('error occurred while loading notifications');
    });
  });

  describe('MAIN_MENU_SET_NOTIFICATION_SEEN', function () {
    it('sets the appropriate notification as seen', function () {
      const state = {
        notificationsToDisplay: [
          { viewed: false, id: 'id1' },
          { viewed: false, id: 'id2' },
        ],
      };

      const action = {
        type: MAIN_MENU_SET_NOTIFICATION_SEEN,
        payload: 'id2',
      };
      const newState = reducer(state, action);

      expect(newState.notificationsToDisplay).toEqual([
        { viewed: false, id: 'id1' },
        { viewed: true, id: 'id2' },
      ]);
    });
  });
});
