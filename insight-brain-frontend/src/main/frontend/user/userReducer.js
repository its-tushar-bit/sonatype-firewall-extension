/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

import { createReducerFromActionMap, propSetConst } from '../util/reduxUtil';
import { LOAD_USER_FULFILLED, DEFAULT_ADMIN_PASSWORD_CHANGED } from './userActions';

// Initial User state
const initialState = Object.freeze({
  currentUser: null,
  isDefaultUser: false,
  shouldDisplayNotice: false,
  canChangePassword: false,
});

/**
 * Maps ActionTypes to Reducer functions
 */
const reducerActionMap = {
  [DEFAULT_ADMIN_PASSWORD_CHANGED]: propSetConst('shouldDisplayNotice', false),
  [LOAD_USER_FULFILLED]: loadCurrentUser,
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

/**
 * Main reducer function for this file.
 * Looks up the action by type and then executes using the payload and the state.
 */
const manageUserReducer = createReducerFromActionMap(reducerActionMap, initialState);
export default manageUserReducer;
