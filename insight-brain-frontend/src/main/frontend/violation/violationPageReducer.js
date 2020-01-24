/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { createReducerFromActionMap, propSetConst } from '../util/reduxUtil';
import { LOAD_VIOLATION_REQUESTED, LOAD_VIOLATION_FULFILLED, LOAD_VIOLATION_FAILED } from './violationPageActions';
import { Messages } from '../util/CommonServices';

const initialState = Object.freeze({
  loading: false,
  error: null
});

const reducerActionMap = {
  [LOAD_VIOLATION_REQUESTED]: propSetConst('loading', true),
  [LOAD_VIOLATION_FULFILLED]: loadFulfilled,
  [LOAD_VIOLATION_FAILED]: loadFailed
};

function loadFulfilled(payload, state) {
  return { ...state, loading: false, error: null };
}

function loadFailed(payload, state) {
  return { ...state, loading: false, error: Messages.getHttpErrorMessage(payload) };
}

const reducer = createReducerFromActionMap(reducerActionMap, initialState);
export default reducer;
