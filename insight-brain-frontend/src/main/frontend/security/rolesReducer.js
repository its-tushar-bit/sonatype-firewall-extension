/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { always } from 'ramda';
import { createReducerFromActionMap } from '../util/reduxUtil';
import { ROLES_LIST_LOAD_REQUESTED, ROLES_LIST_LOAD_FULFILLED, ROLES_LIST_LOAD_FAILED } from './rolesActions';

export const initialState = Object.freeze({
  roles: [],
  loading: true,
  loadError: null,
  readOnly: true,
});

function loadFulfilled(payload, state) {
  return {
    ...state,
    roles: payload.roles,
    loading: false,
    readOnly: payload.readOnly,
  };
}

function loadFailed(payload, state) {
  return {
    ...state,
    loading: false,
    loadError: payload,
  };
}

const reducerActionMap = {
  [ROLES_LIST_LOAD_REQUESTED]: always(initialState),
  [ROLES_LIST_LOAD_FULFILLED]: loadFulfilled,
  [ROLES_LIST_LOAD_FAILED]: loadFailed,
};

const reducer = createReducerFromActionMap(reducerActionMap, initialState);
export default reducer;
