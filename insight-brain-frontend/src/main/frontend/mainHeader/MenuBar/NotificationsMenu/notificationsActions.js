/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import axios from 'axios';

import { noPayloadActionCreator, payloadParamActionCreator } from '../../../util/reduxUtil';
import { getNotificationUrl, getNotificationViewedUrl } from '../../../util/CLMLocation';

export const MAIN_MENU_LOAD_NOTIFICATIONS_REQUESTED = 'MAIN_MENU_LOAD_NOTIFICATIONS_REQUESTED';
export const MAIN_MENU_LOAD_NOTIFICATIONS_FULFILLED = 'MAIN_MENU_LOAD_NOTIFICATIONS_FULFILLED';
export const MAIN_MENU_LOAD_NOTIFICATIONS_FAILED = 'MAIN_MENU_LOAD_NOTIFICATIONS_FAILED';
export const MAIN_MENU_SET_NOTIFICATION_SEEN = 'MAIN_MENU_SET_NOTIFICATION_SEEN';

const loadNotificationsRequested = noPayloadActionCreator(MAIN_MENU_LOAD_NOTIFICATIONS_REQUESTED);
const loadNotificationsFulfilled = payloadParamActionCreator(MAIN_MENU_LOAD_NOTIFICATIONS_FULFILLED);
const loadNotificationsFailed = payloadParamActionCreator(MAIN_MENU_LOAD_NOTIFICATIONS_FAILED);
const setNotificationSeen = payloadParamActionCreator(MAIN_MENU_SET_NOTIFICATION_SEEN);

export function loadNotifications() {
  return (dispatch) => {
    dispatch(loadNotificationsRequested());

    return axios
      .get(getNotificationUrl())
      .then(({ data }) => {
        const { notifications } = data;
        return dispatch(loadNotificationsFulfilled(notifications));
      })
      .catch((err) => {
        return dispatch(loadNotificationsFailed(err));
      });
  };
}

export function setNotificationViewed({ id }) {
  return (dispatch) => {
    return axios.post(getNotificationViewedUrl(), { id }).then(() => dispatch(setNotificationSeen(id)));
  };
}
