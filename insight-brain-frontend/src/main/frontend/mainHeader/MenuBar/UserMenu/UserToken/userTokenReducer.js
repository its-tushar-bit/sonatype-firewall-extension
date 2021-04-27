/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

import { always } from 'ramda';

import { UI_ROUTER_ON_FINISH } from '../../../../reduxUiRouter/routerActions';
import { createReducerFromActionMap, propSetConst } from '../../../../util/reduxUtil';
import {
  USER_TOKEN_CHECK_TOKEN_EXISTENCE_REQUESTED,
  USER_TOKEN_CHECK_TOKEN_EXISTENCE_FULFILLED,
  USER_TOKEN_CHECK_TOKEN_EXISTENCE_FAILED,
  USER_TOKEN_GENERATE_TOKEN_REQUESTED,
  USER_TOKEN_GENERATE_TOKEN_FAILED,
  USER_TOKEN_GENERATE_TOKEN_FULFILLED,
  USER_TOKEN_DELETE_TOKEN_REQUESTED,
  USER_TOKEN_DELETE_TOKEN_FAILED,
  USER_TOKEN_DELETE_TOKEN_FULFILLED,
  USER_TOKEN_SHOW_MODAL,
  USER_TOKEN_HIDE_MODAL,
  USER_TOKEN_MASK_TIMER_DONE,
} from './userTokenActions';

const initialState = Object.freeze({
  // ui state
  isUserTokenModalVisible: false,
  // read state
  userToken: null,
  checkUserTokenError: null,
  checkUserTokenLoading: false,
  // generate state
  generateUserTokenError: null,
  generateUserTokenLoading: null,
  // delete state
  deleteUserTokenError: null,
  deleteUserTokenLoading: null,
});

const checkTokenRequested = (payload, state) => ({
  ...state,
  userToken: null,
  checkUserTokenError: null,
  checkUserTokenLoading: true,
});

const checkTokenFailed = (payload, state) => ({
  ...state,
  userToken: null,
  checkUserTokenLoading: false,
  checkUserTokenError: payload,
});

const checkTokenFulfilled = (payload, state) => ({
  ...state,
  userToken: payload,
  checkUserTokenLoading: false,
  checkUserTokenError: null,
});

const generateTokenRequested = (payload, state) => ({
  ...state,
  generateUserTokenLoading: false,
  generateUserTokenError: null,
});

const generateTokenFailed = (payload, state) => ({
  ...state,
  generateUserTokenLoading: null,
  generateUserTokenError: payload,
});

const generateTokenFulfilled = (payload, state) => ({
  ...state,
  userToken: payload,
  generateUserTokenLoading: null,
  generateUserTokenError: null,
});

const deleteTokenRequested = (payload, state) => ({
  ...state,
  deleteUserTokenLoading: false,
  deleteUserTokenError: null,
});

const deleteTokenFailed = (payload, state) => ({
  ...state,
  deleteUserTokenLoading: null,
  deleteUserTokenError: payload,
});

const deleteUserTokenFulfilled = (payload, state) => ({
  ...state,
  deleteUserTokenLoading: true,
  deleteUserTokenError: null,
  userToken: null,
});

const reducerActionMap = {
  [UI_ROUTER_ON_FINISH]: always(initialState),
  [USER_TOKEN_HIDE_MODAL]: always(initialState),
  [USER_TOKEN_SHOW_MODAL]: propSetConst('isUserTokenModalVisible', true),
  [USER_TOKEN_CHECK_TOKEN_EXISTENCE_REQUESTED]: checkTokenRequested,
  [USER_TOKEN_CHECK_TOKEN_EXISTENCE_FAILED]: checkTokenFailed,
  [USER_TOKEN_CHECK_TOKEN_EXISTENCE_FULFILLED]: checkTokenFulfilled,
  [USER_TOKEN_GENERATE_TOKEN_REQUESTED]: generateTokenRequested,
  [USER_TOKEN_GENERATE_TOKEN_FAILED]: generateTokenFailed,
  [USER_TOKEN_GENERATE_TOKEN_FULFILLED]: generateTokenFulfilled,
  [USER_TOKEN_DELETE_TOKEN_REQUESTED]: deleteTokenRequested,
  [USER_TOKEN_DELETE_TOKEN_FAILED]: deleteTokenFailed,
  [USER_TOKEN_DELETE_TOKEN_FULFILLED]: deleteUserTokenFulfilled,
  [USER_TOKEN_MASK_TIMER_DONE]: propSetConst('deleteUserTokenLoading', null),
};

const userTokenReducer = createReducerFromActionMap(reducerActionMap, initialState);
export default userTokenReducer;
