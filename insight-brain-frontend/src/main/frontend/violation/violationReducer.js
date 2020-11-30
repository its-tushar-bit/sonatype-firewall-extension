/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { createReducerFromActionMap, propSetConst } from '../util/reduxUtil';
import {
  VIOLATION_LOAD_VIOLATION_DETAILS_REQUESTED,
  VIOLATION_FETCH_CROSS_STAGE_VIOLATION_FULFILLED,
  VIOLATION_FETCH_APPLICABLE_WAIVERS_FULFILLED,
  VIOLATION_LOAD_VIOLATION_DETAILS_FULFILLED,
  VIOLATION_LOAD_VIOLATION_DETAILS_FAILED,
  VIOLATION_LOAD_VULNERABILITY_DETAILS_REQUESTED,
  VIOLATION_LOAD_VULNERABILITY_DETAILS_FULFILLED,
  VIOLATION_LOAD_VULNERABILITY_DETAILS_FAILED
} from './violationActions';
import { UI_ROUTER_ON_FINISH } from '../reduxUiRouter/routerActions';

const initialState = Object.freeze({
  violationDetails: null,
  loading: true,
  violationDetailsError: null,
  vulnerabilityDetailsLoading: false,
  vulnerabilityDetails: null,
  vulnerabilityDetailsError: null,
  activeWaivers: Object.freeze([]),
  expiredWaivers: Object.freeze([]),
  selectedViolationId: null
});

const reducerActionMap = {
  [VIOLATION_LOAD_VIOLATION_DETAILS_REQUESTED]: loadViolationRequested,
  [VIOLATION_FETCH_CROSS_STAGE_VIOLATION_FULFILLED]: fetchCrossStageViolationFulfilled,
  [VIOLATION_FETCH_APPLICABLE_WAIVERS_FULFILLED]: fetchApplicableWaiversFulfilled,
  [VIOLATION_LOAD_VIOLATION_DETAILS_FULFILLED]: loadViolationFulfilled,
  [VIOLATION_LOAD_VIOLATION_DETAILS_FAILED]: loadViolationFailed,
  [VIOLATION_LOAD_VULNERABILITY_DETAILS_REQUESTED]: propSetConst('vulnerabilityDetailsLoading', true),
  [VIOLATION_LOAD_VULNERABILITY_DETAILS_FULFILLED]: loadVulnerabilityDetailsFulfilled,
  [VIOLATION_LOAD_VULNERABILITY_DETAILS_FAILED]: loadVulnerabilityDetailsFailed,
  [UI_ROUTER_ON_FINISH]: propSetConst('loading', true)
};

function loadViolationRequested() {
  return {
    ...initialState,
    loading: true
  };
}

function fetchCrossStageViolationFulfilled({ violationDetails, selectedViolationId }, state) {
  return {
    ...state,
    violationDetails,
    selectedViolationId
  };
}

function loadViolationFulfilled(payload, state) {
  return {
    ...state,
    loading: false,
    violationDetailsError: null
  };
}

function fetchApplicableWaiversFulfilled({activeWaivers, expiredWaivers}, state) {
  return {
    ...state,
    activeWaivers,
    expiredWaivers
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
