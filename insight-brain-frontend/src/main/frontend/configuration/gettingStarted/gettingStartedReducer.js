/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

import { createReducerFromActionMap } from '../../util/reduxUtil';

import {
  GETTING_STARTED_LOAD_REQUESTED,
  GETTING_STARTED_LOAD_FULFILLED,
  GETTING_STARTED_LOAD_FAILED,
} from './gettingStartedActions';

export const initialState = Object.freeze({
  loading: true,
  loadError: null,
  validPermissions: [],
  isAuthorizedToViewSystemSetup: false,
  shouldDisplayHdsUnreachable: false,
  hdsUnreachableErrorMessage: null,
  hdsUnreachableIncidentId: null,
  license: null,
  isAdmin: false,
});

function loadFulfilled(payload, state) {
  return {
    ...state,
    loading: false,
    loadError: null,
    ...payload,
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
  [GETTING_STARTED_LOAD_REQUESTED]: () => initialState,
  [GETTING_STARTED_LOAD_FULFILLED]: loadFulfilled,
  [GETTING_STARTED_LOAD_FAILED]: loadFailed,
};

const reducer = createReducerFromActionMap(reducerActionMap, initialState);
export default reducer;
