/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import axios from 'axios';

import { getNotificationUrl, getNotificationViewedUrl } from '../../../../../main/frontend/util/CLMLocation';
import {
  loadNotifications,
  MAIN_MENU_LOAD_NOTIFICATIONS_FAILED,
  MAIN_MENU_LOAD_NOTIFICATIONS_FULFILLED,
  MAIN_MENU_LOAD_NOTIFICATIONS_REQUESTED,
  MAIN_MENU_SET_NOTIFICATION_SEEN,
  setNotificationViewed,
} from '../../../../../main/frontend/mainHeader/MenuBar/NotificationsMenu/notificationsActions';

import 'TestRoot/SpecUtil';

describe('notifications actions', function () {
  let store, mockAxiosCalls;

  beforeEach(function () {
    const state = {};
    store = SpecUtil.mockReduxStore(state);
    mockAxiosCalls = SpecUtil.axiosMockerGenerator(axios);
  });

  describe('loadNotifications', function () {
    let loadNotificationsUrl;

    beforeEach(function () {
      loadNotificationsUrl = getNotificationUrl();
    });

    it('immediately dispatches an MAIN_MENU_LOAD_NOTIFICATIONS_REQUESTED action', function () {
      mockAxiosCalls({
        get: {
          [loadNotificationsUrl]: Promise.resolve(),
        },
      });
      store.dispatch(loadNotifications());

      expect(store.getActions().length).toBe(1);
      expect(store.getActions()[0].type).toEqual(MAIN_MENU_LOAD_NOTIFICATIONS_REQUESTED);
      expect(store.getActions()[0].payload).toBeUndefined();
    });

    it('sends a GET request with proper config', function () {
      const expectedUrl = '/rest/product/notifications';

      mockAxiosCalls({
        get: {
          [loadNotificationsUrl]: Promise.resolve(),
        },
      });

      store.dispatch(loadNotifications());
      expect(axios.get).toHaveBeenCalledWith(expectedUrl);
    });

    describe('after a successful GET', function () {
      it('dispatches the MAIN_MENU_LOAD_NOTIFICATIONS_FULFILLED action with result data', function (done) {
        mockAxiosCalls({
          get: {
            [loadNotificationsUrl]: Promise.resolve({ data: { notifications: ['notification'] } }),
          },
        });

        store.dispatch(loadNotifications()).then(() => {
          expect(store.getActions().length).toBe(2);
          expect(store.getActions()[1].type).toBe(MAIN_MENU_LOAD_NOTIFICATIONS_FULFILLED);
          expect(store.getActions()[1].payload).toEqual(['notification']);
          done();
        });

        expect(store.getActions().length).toBe(1);
        expect(store.getActions()[0].type).toBe(MAIN_MENU_LOAD_NOTIFICATIONS_REQUESTED);
      });
    });

    describe('after a failed GET', function () {
      it('dispatches the MAIN_MENU_LOAD_NOTIFICATIONS_FAILED action', function (done) {
        mockAxiosCalls({
          get: {
            [loadNotificationsUrl]: () => Promise.reject('Err'),
          },
        });

        store.dispatch(loadNotifications()).then(() => {
          expect(store.getActions().length).toBe(2);
          expect(store.getActions()[1].type).toBe(MAIN_MENU_LOAD_NOTIFICATIONS_FAILED);
          expect(store.getActions()[1].payload).toBe('Err');
          done();
        });

        expect(store.getActions().length).toBe(1);
        expect(store.getActions()[0].type).toBe(MAIN_MENU_LOAD_NOTIFICATIONS_REQUESTED);
      });
    });
  });

  describe('setNotificationViewed', function () {
    let setNotificationViewedUrl;

    beforeEach(function () {
      setNotificationViewedUrl = getNotificationViewedUrl();
    });

    it('sends a POST request with proper config', function () {
      const expectedUrl = '/rest/product/notifications/viewed';

      mockAxiosCalls({
        post: {
          [setNotificationViewedUrl]: Promise.resolve({ data: {} }),
        },
      });

      store.dispatch(setNotificationViewed({ id: 'id' }));
      expect(axios.post).toHaveBeenCalledWith(expectedUrl, { id: 'id' });
    });

    describe('after a successful POST', function () {
      it('dispatches the MAIN_MENU_SET_NOTIFICATION_SEEN action with the id of the notification', function (done) {
        mockAxiosCalls({
          post: {
            [setNotificationViewedUrl]: Promise.resolve({ data: {} }),
          },
        });

        store.dispatch(setNotificationViewed({ id: 'id' })).then(() => {
          expect(store.getActions().length).toBe(1);
          expect(store.getActions()[0].type).toBe(MAIN_MENU_SET_NOTIFICATION_SEEN);
          expect(store.getActions()[0].payload).toEqual('id');
          done();
        });
      });
    });
  });
});
