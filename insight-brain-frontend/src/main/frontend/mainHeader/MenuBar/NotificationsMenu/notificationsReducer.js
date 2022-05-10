/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { createReducerFromActionMap } from '../../../util/reduxUtil';
import {
  MAIN_MENU_LOAD_NOTIFICATIONS_FAILED,
  MAIN_MENU_LOAD_NOTIFICATIONS_FULFILLED,
  MAIN_MENU_LOAD_NOTIFICATIONS_REQUESTED,
  MAIN_MENU_SET_NOTIFICATION_SEEN,
} from './notificationsActions';
import { Messages } from '../../../utilAngular/CommonServices';

const initialState = Object.freeze({
  loading: false,
  error: null,
  notificationsToDisplay: null,
});

const loadNotificationsRequested = (payload, state) => ({
  ...state,
  loading: true,
  error: null,
  notificationsToDisplay: null,
});

const loadNotificationsFulfilled = (payload, state) => ({
  ...state,
  loading: false,
  error: null,
  notificationsToDisplay: payload,
});

const loadNotificationsFailed = (payload, state) => ({
  ...state,
  loading: false,
  error: Messages.getHttpErrorMessage(payload),
  notificationsToDisplay: null,
});

const setNotificationSeen = (payload, state) => {
  const newNotifications =
    state.notificationsToDisplay &&
    state.notificationsToDisplay.map((notification) => {
      if (notification.id === payload) {
        return {
          ...notification,
          viewed: true,
        };
      }
      return notification;
    });

  return {
    ...state,
    notificationsToDisplay: newNotifications,
  };
};

const reducerActionMap = {
  [MAIN_MENU_LOAD_NOTIFICATIONS_REQUESTED]: loadNotificationsRequested,
  [MAIN_MENU_LOAD_NOTIFICATIONS_FULFILLED]: loadNotificationsFulfilled,
  [MAIN_MENU_LOAD_NOTIFICATIONS_FAILED]: loadNotificationsFailed,
  [MAIN_MENU_SET_NOTIFICATION_SEEN]: setNotificationSeen,
};

const notificationsReducer = createReducerFromActionMap(reducerActionMap, initialState);
export default notificationsReducer;
