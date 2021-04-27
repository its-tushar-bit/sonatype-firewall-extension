/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import axios from 'axios';
import { SUBMIT_MASK_SUCCESS_VISIBLE_TIME_MS } from '@sonatype/react-shared-components';

import { checkUserTokenExistenceUrl, userTokenUrl } from '../../../../util/CLMLocation';
import { noPayloadActionCreator, payloadParamActionCreator } from '../../../../util/reduxUtil';

export const USER_TOKEN_CHECK_TOKEN_EXISTENCE_REQUESTED = 'USER_TOKEN_CHECK_TOKEN_EXISTENCE_REQUESTED';
export const USER_TOKEN_CHECK_TOKEN_EXISTENCE_FULFILLED = 'USER_TOKEN_CHECK_TOKEN_EXISTENCE_FULFILLED';
export const USER_TOKEN_CHECK_TOKEN_EXISTENCE_FAILED = 'USER_TOKEN_CHECK_TOKEN_EXISTENCE_FAILED';
export const USER_TOKEN_GENERATE_TOKEN_REQUESTED = 'USER_TOKEN_GENERATE_TOKEN_REQUESTED';
export const USER_TOKEN_GENERATE_TOKEN_FAILED = 'USER_TOKEN_GENERATE_TOKEN_FAILED';
export const USER_TOKEN_GENERATE_TOKEN_FULFILLED = 'USER_TOKEN_GENERATE_TOKEN_FULFILLED';
export const USER_TOKEN_DELETE_TOKEN_REQUESTED = 'USER_TOKEN_DELETE_TOKEN_REQUESTED';
export const USER_TOKEN_DELETE_TOKEN_FAILED = 'USER_TOKEN_DELETE_TOKEN_FAILED';
export const USER_TOKEN_DELETE_TOKEN_FULFILLED = 'USER_TOKEN_DELETE_TOKEN_FULFILLED';
export const USER_TOKEN_SHOW_MODAL = 'USER_TOKEN_SHOW_MODAL';
export const USER_TOKEN_HIDE_MODAL = 'USER_TOKEN_HIDE_MODAL';
export const USER_TOKEN_MASK_TIMER_DONE = 'USER_TOKEN_MASK_TIMER_DONE';
export const USER_TOKEN_COPY_TO_CLIPBOARD = 'USER_TOKEN_COPY_TO_CLIPBOARD';

export const showUserTokenModal = noPayloadActionCreator(USER_TOKEN_SHOW_MODAL);
export const hideUserTokenModal = noPayloadActionCreator(USER_TOKEN_HIDE_MODAL);

const checkTokenExistenceRequested = noPayloadActionCreator(USER_TOKEN_CHECK_TOKEN_EXISTENCE_REQUESTED);
const checkTokenExistenceFulfilled = payloadParamActionCreator(USER_TOKEN_CHECK_TOKEN_EXISTENCE_FULFILLED);
const checkTokenExistenceFailed = payloadParamActionCreator(USER_TOKEN_CHECK_TOKEN_EXISTENCE_FAILED);

function startMaskTimer(dispatch) {
  setTimeout(() => {
    dispatch({ type: USER_TOKEN_MASK_TIMER_DONE });
  }, SUBMIT_MASK_SUCCESS_VISIBLE_TIME_MS);
}

export function checkUserTokenExistence() {
  return (dispatch) => {
    dispatch(checkTokenExistenceRequested());

    return axios
      .get(checkUserTokenExistenceUrl())
      .then(({ data }) => {
        const { userTokenExists } = data;
        return dispatch(checkTokenExistenceFulfilled(userTokenExists));
      })
      .catch((err) => {
        return dispatch(checkTokenExistenceFailed(err));
      });
  };
}

const generateTokenRequested = noPayloadActionCreator(USER_TOKEN_GENERATE_TOKEN_REQUESTED);
const generateTokenFailed = payloadParamActionCreator(USER_TOKEN_GENERATE_TOKEN_FAILED);
const generateTokenFulfilled = payloadParamActionCreator(USER_TOKEN_GENERATE_TOKEN_FULFILLED);

export function generateUserToken() {
  return (dispatch) => {
    dispatch(generateTokenRequested());

    return axios
      .post(userTokenUrl())
      .then(({ data }) => {
        startMaskTimer(dispatch);
        return dispatch(generateTokenFulfilled(data));
      })
      .catch((err) => {
        return dispatch(generateTokenFailed(err));
      });
  };
}

const deleteTokenRequested = noPayloadActionCreator(USER_TOKEN_DELETE_TOKEN_REQUESTED);
const deleteTokenFailed = payloadParamActionCreator(USER_TOKEN_DELETE_TOKEN_FAILED);
const deleteTokenFulfilled = noPayloadActionCreator(USER_TOKEN_DELETE_TOKEN_FULFILLED);

export function deleteUserToken() {
  return (dispatch) => {
    dispatch(deleteTokenRequested());

    return axios
      .delete(userTokenUrl())
      .then(() => {
        startMaskTimer(dispatch);
        dispatch(deleteTokenFulfilled());
      })
      .catch((err) => {
        return dispatch(deleteTokenFailed(err));
      });
  };
}
