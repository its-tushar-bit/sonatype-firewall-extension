/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

import { createReducerFromActionMap } from '../util/reduxUtil';

import {
  LOAD_POLICY_VIOLATIONS_FAILED,
  LOAD_POLICY_VIOLATIONS_FULFILLED,
  LOAD_POLICY_VIOLATIONS_REQUESTED,
  QUARANTINED_REPORT_LOAD_QUARANTINE_COMPONENT_OVERVIEW_FAILED,
  QUARANTINED_REPORT_LOAD_QUARANTINE_COMPONENT_OVERVIEW_FULFILLED,
  QUARANTINED_REPORT_LOAD_QUARANTINE_COMPONENT_OVERVIEW_REQUESTED,
} from './quarantinedComponentReportActions';

const initialState = Object.freeze({
  viewState: Object.freeze({
    loadError: null,
    componentOverview: {
      componentIdentifier: null,
      componentHash: '',
      matchState: '',
      pathname: '',
      componentOverviewLoading: true,
      componentDisplayName: '',
      isQuarantined: false,
      quarantinedPolicyViolationsCount: 0,
      repositoryId: '',
      repositoryName: '',
      quarantinedDate: '',
      componentVersion: '',
    },
    violations: [],
  }),
});

const loadComponentOverviewRequested = (payload, state) => ({
  ...state,
  viewState: {
    ...state.viewState,
    componentOverview: {
      ...state.viewState.componentOverview,
      componentOverviewLoading: true,
    },
  },
});

const loadComponentOverviewFulfilled = (payload, state) => ({
  ...state,
  viewState: {
    ...state.viewState,
    loadError: null,
    componentOverview: {
      ...payload,
      componentOverviewLoading: false,
    },
  },
});

const loadComponentOverviewFailed = (payload, state) => ({
  ...state,
  viewState: {
    ...state.viewState,
    loadError: payload,
    componentOverview: {
      ...state.viewState.componentOverview,
      componentOverviewLoading: false,
    },
  },
});

const loadPolicyViolationsRequested = (payload, state) => ({
  ...state,
  viewState: {
    ...state.viewState,
    violationsLoading: true,
  },
});

const loadPolicyViolationsFulfilled = (payload, state) => {
  return {
    ...state,
    viewState: {
      ...state.viewState,
      violations: payload,
      violationsLoading: false,
      violationsLoadError: null,
    },
  };
};

const loadPolicyViolationsFailed = (payload, state) => {
  return {
    ...state,
    viewState: {
      ...state.viewState,
      violations: [...state.viewState.violations],
      violationsLoading: false,
      violationsLoadError: payload,
    },
  };
};

const reducerActionMap = {
  [QUARANTINED_REPORT_LOAD_QUARANTINE_COMPONENT_OVERVIEW_FAILED]: loadComponentOverviewFailed,
  [QUARANTINED_REPORT_LOAD_QUARANTINE_COMPONENT_OVERVIEW_FULFILLED]: loadComponentOverviewFulfilled,
  [QUARANTINED_REPORT_LOAD_QUARANTINE_COMPONENT_OVERVIEW_REQUESTED]: loadComponentOverviewRequested,
  [LOAD_POLICY_VIOLATIONS_REQUESTED]: loadPolicyViolationsRequested,
  [LOAD_POLICY_VIOLATIONS_FULFILLED]: loadPolicyViolationsFulfilled,
  [LOAD_POLICY_VIOLATIONS_FAILED]: loadPolicyViolationsFailed,
};

const reducer = createReducerFromActionMap(reducerActionMap, initialState);
export default reducer;
