/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

import { createReducerFromActionMap, propSetConst } from '../util/reduxUtil';
import {
  LOAD_USER_FULFILLED,
  DEFAULT_ADMIN_PASSWORD_CHANGED,
  CHANGE_PASSWORD_REQUESTED,
  CHANGE_PASSWORD_FULFILLED,
  CHANGE_PASSWORD_FAILED,
  CHANGE_PASSWORD_STATUS_RESET,
} from './userActions';

// Initial User state
const initialState = Object.freeze({
  currentUser: null,
  isDefaultUser: false,
  shouldDisplayNotice: false,
  canChangePassword: false,
  changePasswordStatus: 'idle', // 'pending', 'success', 'failure'
  changePasswordErrorMessage: null,
});

/**
 * Maps ActionTypes to Reducer functions
 */
const reducerActionMap = {
  [DEFAULT_ADMIN_PASSWORD_CHANGED]: propSetConst('shouldDisplayNotice', false),
  [LOAD_USER_FULFILLED]: loadCurrentUser,
  [CHANGE_PASSWORD_REQUESTED]: setChangePasswordStatus('pending'),
  [CHANGE_PASSWORD_FULFILLED]: setChangePasswordStatus('success'),
  [CHANGE_PASSWORD_STATUS_RESET]: setChangePasswordStatus('idle'),
  [CHANGE_PASSWORD_FAILED]: setChangePasswordFailure,
};

function loadCurrentUser({ currentUser, shouldDisplayWarning }, state) {
  return {
    ...state,
    currentUser,
    canChangePassword: currentUser && currentUser.internalUser,
    isDefaultUser: currentUser && currentUser.username === 'admin',
    shouldDisplayNotice: shouldDisplayWarning,
  };
}

function setChangePasswordStatus(status) {
  return (action, state) => ({
    ...state,
    changePasswordStatus: status,
    changePasswordErrorMessage: null,
  });
}

function setChangePasswordFailure(action, state) {
  return {
    ...state,
    changePasswordStatus: 'failure',
    changePasswordErrorMessage: action.message,
  };
}

/**
 * Main reducer function for this file.
 * Looks up the action by type and then executes using the payload and the state.
 */
const manageUserReducer = createReducerFromActionMap(reducerActionMap, initialState);
export default manageUserReducer;
