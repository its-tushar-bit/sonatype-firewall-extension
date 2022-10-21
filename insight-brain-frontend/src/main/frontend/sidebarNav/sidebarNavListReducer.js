/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { createReducerFromActionMap } from '../util/reduxUtil';
import {
  LOAD_SIDEBAR_NAV_LIST_FAILED,
  LOAD_SIDEBAR_NAV_LIST_FULFILLED,
  LOAD_SIDEBAR_NAV_LIST_REQUESTED,
  SET_SIDEBAR_NAV_LIST_DATA,
} from './sidebarNavListActions';

const initialState = Object.freeze({
  data: Object.freeze([]),
  contentType: '',
  loading: false,
  error: null,
  sidebarId: null,
  sidebarReference: null,
});

const reducerActionMap = {
  [LOAD_SIDEBAR_NAV_LIST_REQUESTED]: loadRequested,
  [LOAD_SIDEBAR_NAV_LIST_FULFILLED]: loadFulfilled,
  [LOAD_SIDEBAR_NAV_LIST_FAILED]: loadFailed,
  [SET_SIDEBAR_NAV_LIST_DATA]: setSidebarNavListData,
};

function loadFulfilled(payload, state) {
  return {
    ...state,
    ...payload,
    error: null,
    loading: false,
  };
}

function loadFailed(error, state) {
  return { ...state, loading: false, error };
}

function loadRequested({ contentType, sidebarReference, sidebarId }, state) {
  return {
    ...state,
    contentType,
    sidebarReference,
    sidebarId,
    loading: true,
  };
}

function setSidebarNavListData(data, state) {
  return {
    ...state,
    data,
  };
}

const reducer = createReducerFromActionMap(reducerActionMap, initialState);
export default reducer;
