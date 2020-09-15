/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { createReducerFromActionMap, propSetConst } from '../util/reduxUtil';
import {
  LOAD_VIOLATION_REQUESTED,
  LOAD_VIOLATION_FULFILLED,
  LOAD_VIOLATION_FAILED,
  LOAD_VULNERABILITY_DETAILS_REQUESTED,
  LOAD_VULNERABILITY_DETAILS_FULFILLED,
  LOAD_VULNERABILITY_DETAILS_FAILED
} from './violationPageActions';
import { UI_ROUTER_ON_FINISH } from '../reduxUiRouter/routerActions';

const initialState = Object.freeze({
  violationDetails: null,
  loading: true,
  violationDetailsError: null,
  vulnerabilityDetailsLoading: false,
  vulnerabilityDetails: null,
  vulnerabilityDetailsError: null,
  activeWaivers: Object.freeze([])
});

const reducerActionMap = {
  [LOAD_VIOLATION_REQUESTED]: loadViolationRequested,
  [LOAD_VIOLATION_FULFILLED]: loadViolationFulfilled,
  [LOAD_VIOLATION_FAILED]: loadViolationFailed,
  [LOAD_VULNERABILITY_DETAILS_REQUESTED]: propSetConst('vulnerabilityDetailsLoading', true),
  [LOAD_VULNERABILITY_DETAILS_FULFILLED]: loadVulnerabilityDetailsFulfilled,
  [LOAD_VULNERABILITY_DETAILS_FAILED]: loadVulnerabilityDetailsFailed,
  [UI_ROUTER_ON_FINISH]: propSetConst('loading', true)
};

function loadViolationRequested() {
  return {
    ...initialState,
    loading: true
  };
}

function loadViolationFulfilled({ violationDetails, applicableWaivers }, state) {
  return {
    ...state,
    loading: false,
    violationDetailsError: null,
    violationDetails,
    activeWaivers: [...applicableWaivers.activeWaivers]
  };
}

function loadViolationFailed(payload, state) {
  return { ...state, loading: false, violationDetailsError: payload };
}

function loadVulnerabilityDetailsFulfilled(payload, state) {
  return {
    ...state,
    vulnerabilityDetailsLoading: false,
    vulnerabilityDetailsError: null,
    vulnerabilityDetails: payload
  };
}

function loadVulnerabilityDetailsFailed(payload, state) {
  return {
    ...state,
    vulnerabilityDetailsLoading: false,
    vulnerabilityDetailsError: payload
  };
}

const reducer = createReducerFromActionMap(reducerActionMap, initialState);
export default reducer;
