/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { clone } from 'ramda';
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
  VIOLATION_SORT_SIMILAR_WAIVERS,
  VIOLATION_SET_FILTER_IDS_SIMILAR_WAIVERS,
  VIOLATION_FETCH_APPLICABLE_WAIVERS_REQUESTED,
  VIOLATION_FETCH_APPLICABLE_WAIVERS_FAILED,
  VIOLATION_FETCH_APPLICABLE_AUTO_WAIVER_FULFILLED,
  VIOLATION_FETCH_APPLICABLE_AUTO_WAIVER_REQUESTED,
  VIOLATION_FETCH_APPLICABLE_AUTO_WAIVER_FAILED,
} from './violationActions';
import { UI_ROUTER_ON_FINISH } from 'MainRoot/reduxUiRouter/routerActions';
import {
  WAIVERS_LOAD_SIMILAR_WAIVERS_FAILED,
  WAIVERS_LOAD_SIMILAR_WAIVERS_FULFILLED,
  WAIVERS_LOAD_SIMILAR_WAIVERS_REQUESTED,
} from '../waivers/waiverActions';

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
  autoWaiver: null,
  similarWaivers: Object.freeze([]),
  similarWaiversFilterSelectedIds: new Set([]),
  selectedViolationId: null,
  hasPermissionForAppWaivers: false,
  isVulnerabilityDetailsOutdated: false,
  loadingApplicableWaivers: false,
  loadApplicableWaiversError: null,
  loadingSimilarWaivers: false,
  loadSimilarWaiversError: null,
});

const sortByCreateTime = (a, b) => (a.createTime > b.createTime ? 1 : a.createTime === b.createTime ? 0 : -1);

function resetViolation() {
  return {
    ...initialState,
  };
}

function loadViolationRequested(_, state) {
  return {
    ...state,
    loading: true,
    vulnerabilityDetails: null,
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

function loadApplicableWaiversFulfilled({ activeWaivers, expiredWaivers }, state) {
  return {
    ...state,
    activeWaivers,
    expiredWaivers,
    loadingApplicableWaivers: false,
    loadApplicableWaiversError: null,
  };
}

const loadApplicableWaiversRequested = (payload, state) => ({
  ...state,
  loadingApplicableWaivers: true,
  loadApplicableWaiversError: null,
});

const loadApplicableWaiversFailed = (payload, state) => ({
  ...state,
  loadingApplicableWaivers: false,
  loadApplicableWaiversError: payload,
});

const loadApplicableAutoWaiverFulfilled = (payload, state) => ({
  ...state,
  autoWaiver: payload,
  loadingApplicableAutoWaiver: false,
  loadApplicableAutoWaiverError: null,
});

const loadApplicableAutoWaiverRequested = (payload, state) => ({
  ...state,
  loadingApplicableAutoWaiver: true,
  loadApplicableAutoWaiverError: null,
});

const loadApplicableAutoWaiverFailed = (payload, state) => ({
  ...state,
  loadingApplicableAutoWaiver: false,
  loadApplicableAutoWaiverError: payload,
});

const loadSimilarWaiversRequested = (payload, state) => ({
  ...state,
  loadingSimilarWaivers: true,
  loadSimilarWaiversError: null,
});

const loadSimilarWaiversFailed = (payload, state) => ({
  ...state,
  loadingSimilarWaivers: false,
  loadSimilarWaiversError: payload,
});

const loadSimilarWaiversFulfilled = (payload, state) => ({
  ...state,
  loadingSimilarWaivers: false,
  loadSimilarWaiversError: null,
  similarWaivers: payload,
});

function sortSimilarWaives(sortDir, state) {
  const sortedSimilarWaivers = clone(state.similarWaivers);
  if (!sortDir || sortDir === 'asc') {
    sortedSimilarWaivers?.sort(sortByCreateTime);
  } else {
    sortedSimilarWaivers?.sort(sortByCreateTime).reverse();
  }
  return {
    ...state,
    similarWaivers: sortedSimilarWaivers,
  };
}

function setFilterIdsSimilarWivers(similarWaiversFilterSelectedIds, state) {
  return {
    ...state,
    similarWaiversFilterSelectedIds,
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

const reducerActionMap = {
  [VIOLATION_RESET_VIOLATION_DETAILS_REQUESTED]: resetViolation,
  [VIOLATION_LOAD_VIOLATION_DETAILS_REQUESTED]: loadViolationRequested,
  [VIOLATION_FETCH_CROSS_STAGE_VIOLATION_FULFILLED]: fetchCrossStageViolationFulfilled,
  [VIOLATION_FETCH_APPLICABLE_WAIVERS_FULFILLED]: loadApplicableWaiversFulfilled,
  [VIOLATION_FETCH_APPLICABLE_WAIVERS_REQUESTED]: loadApplicableWaiversRequested,
  [VIOLATION_FETCH_APPLICABLE_WAIVERS_FAILED]: loadApplicableWaiversFailed,
  [VIOLATION_FETCH_APPLICABLE_AUTO_WAIVER_FULFILLED]: loadApplicableAutoWaiverFulfilled,
  [VIOLATION_FETCH_APPLICABLE_AUTO_WAIVER_REQUESTED]: loadApplicableAutoWaiverRequested,
  [VIOLATION_FETCH_APPLICABLE_AUTO_WAIVER_FAILED]: loadApplicableAutoWaiverFailed,
  [VIOLATION_SORT_SIMILAR_WAIVERS]: sortSimilarWaives,
  [VIOLATION_SET_FILTER_IDS_SIMILAR_WAIVERS]: setFilterIdsSimilarWivers,
  [VIOLATION_LOAD_VIOLATION_DETAILS_FULFILLED]: loadViolationFulfilled,
  [VIOLATION_LOAD_VIOLATION_DETAILS_FAILED]: loadViolationFailed,
  [VIOLATION_LOAD_VULNERABILITY_DETAILS_REQUESTED]: propSetConst('vulnerabilityDetailsLoading', true),
  [VIOLATION_LOAD_VULNERABILITY_DETAILS_FULFILLED]: loadVulnerabilityDetailsFulfilled,
  [VIOLATION_LOAD_VULNERABILITY_DETAILS_FAILED]: loadVulnerabilityDetailsFailed,
  [WAIVERS_LOAD_SIMILAR_WAIVERS_REQUESTED]: loadSimilarWaiversRequested,
  [WAIVERS_LOAD_SIMILAR_WAIVERS_FAILED]: loadSimilarWaiversFailed,
  [WAIVERS_LOAD_SIMILAR_WAIVERS_FULFILLED]: loadSimilarWaiversFulfilled,
  [UI_ROUTER_ON_FINISH]: (_, state) => setFilterIdsSimilarWivers(new Set([]), state),
};

const reducer = createReducerFromActionMap(reducerActionMap, initialState);
export default reducer;
