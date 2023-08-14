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
  VIOLATION_LOAD_VULNERABILITY_DETAILS_FAILED,
  VIOLATION_RESET_VIOLATION_DETAILS_REQUESTED,
} from './violationActions';

const initialState = Object.freeze({
  violationDetails: null,
  loading: false,
  violationDetailsError: null,
  vulnerabilityDetailsLoading: false,
  vulnerabilityDetails: null,
  vulnerabilityDetailsError: null,
  hasEditIqPermission: false,
  activeWaivers: Object.freeze([]),
  expiredWaivers: Object.freeze([]),
  selectedViolationId: null,
  hasPermissionForAppWaivers: false,
  isVulnerabilityDetailsOutdated: false,
});

const reducerActionMap = {
  [VIOLATION_RESET_VIOLATION_DETAILS_REQUESTED]: resetViolation,
  [VIOLATION_LOAD_VIOLATION_DETAILS_REQUESTED]: loadViolationRequested,
  [VIOLATION_FETCH_CROSS_STAGE_VIOLATION_FULFILLED]: fetchCrossStageViolationFulfilled,
  [VIOLATION_FETCH_APPLICABLE_WAIVERS_FULFILLED]: fetchApplicableWaiversFulfilled,
  [VIOLATION_LOAD_VIOLATION_DETAILS_FULFILLED]: loadViolationFulfilled,
  [VIOLATION_LOAD_VIOLATION_DETAILS_FAILED]: loadViolationFailed,
  [VIOLATION_LOAD_VULNERABILITY_DETAILS_REQUESTED]: propSetConst('vulnerabilityDetailsLoading', true),
  [VIOLATION_LOAD_VULNERABILITY_DETAILS_FULFILLED]: loadVulnerabilityDetailsFulfilled,
  [VIOLATION_LOAD_VULNERABILITY_DETAILS_FAILED]: loadVulnerabilityDetailsFailed,
};

function resetViolation() {
  return {
    ...initialState,
  };
}

function loadViolationRequested(_, state) {
  return {
    ...state,
    loading: true,
    violationDetailsError: null,
  };
}

function fetchCrossStageViolationFulfilled({ violationDetails, selectedViolationId }, state) {
  return {
    ...state,
    violationDetails,
    selectedViolationId,
  };
}

function loadViolationFulfilled(payload, state) {
  return {
    ...state,
    loading: false,
    violationDetailsError: null,
    hasPermissionForAppWaivers: payload,
  };
}

function fetchApplicableWaiversFulfilled({ activeWaivers, expiredWaivers }, state) {
  return {
    ...state,
    activeWaivers,
    expiredWaivers,
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
    isVulnerabilityDetailsOutdated: false,
    vulnerabilityDetails: payload,
    hasEditIqPermission: payload.hasEditIqPermission,
  };
}

function loadVulnerabilityDetailsFailed(payload, state) {
  const httpStatusCode = payload?.response?.status ?? null;
  return {
    ...state,
    isVulnerabilityDetailsOutdated: httpStatusCode === 404,
    vulnerabilityDetailsLoading: false,
    vulnerabilityDetailsError: httpStatusCode !== 404 ? payload : null,
  };
}

const reducer = createReducerFromActionMap(reducerActionMap, initialState);
export default reducer;
