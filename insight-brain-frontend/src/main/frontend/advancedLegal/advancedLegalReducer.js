/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

import { createReducerFromActionMap } from '../util/reduxUtil';
import {
  ADVANCED_LEGAL_LOAD_APPLICATIONS_REQUESTED,
  ADVANCED_LEGAL_LOAD_APPLICATIONS_FULFILLED,
  ADVANCED_LEGAL_LOAD_APPLICATIONS_FAILED,
  ADVANCED_LEGAL_LOAD_APPLICATION_REPORT_REQUESTED,
  ADVANCED_LEGAL_LOAD_APPLICATION_REPORT_FULFILLED,
  ADVANCED_LEGAL_LOAD_APPLICATION_REPORT_FAILED,
  ADVANCED_LEGAL_LOAD_COMPONENT_REQUESTED,
  ADVANCED_LEGAL_LOAD_COMPONENT_FULFILLED,
  ADVANCED_LEGAL_LOAD_COMPONENT_FAILED,
  ADVANCED_LEGAL_LOAD_AVAILABLE_SCOPES_REQUESTED,
  ADVANCED_LEGAL_LOAD_AVAILABLE_SCOPES_FULFILLED,
  ADVANCED_LEGAL_LOAD_AVAILABLE_SCOPES_FAILED
} from './advancedLegalActions';

const initialState = {
  viewStateApplications: {
    loading: false,
    error: null
  },
  applications: [],
  viewStateApplicationReport: {
    loading: false,
    error: null
  },
  applicationReport: null,
  component: {
    loading: true
  },
  availableScopes: {
    loading: false,
    error: null
  }
};

function loadApplicationsRequested() {
  return {
    ...initialState,
    viewStateApplications: {
      loading: true,
      error: null
    }
  };
}

function loadApplicationsFulfilled(payload, state) {
  return {
    ...state,
    viewStateApplications: {
      ...state.viewStateApplications,
      loading: false
    },
    applications: payload
  };
}

function loadApplicationsFailed(payload, state) {
  return {
    ...state,
    viewStateApplications: {
      ...state.viewStateApplications,
      loading: false,
      error: payload
    }
  };
}

function loadApplicationReportRequested(_, state) {
  return {
    ...state,
    viewStateApplicationReport: {
      loading: true,
      error: null
    },
    applicationReport: null
  };
}

function loadApplicationReportFulfilled(payload, state) {
  return {
    ...state,
    viewStateApplicationReport: {
      ...state.viewStateApplicationReport,
      loading: false
    },
    applicationReport: payload
  };
}

function loadApplicationReportFailed(payload, state) {
  return {
    ...state,
    viewStateApplicationReport: {
      ...state.viewStateApplicationReport,
      loading: false,
      error: payload
    }
  };
}

function loadComponentRequested() {
  return {
    ...initialState,
    component: {
      loading: true,
      error: null
    }
  };
}

function loadComponentFulfilled(payload, state) {
  return {
    ...state,
    component: {
      loading: false,
      ...payload
    }
  };
}

function loadComponentFailed(payload, state) {
  return {
    ...state,
    component: {
      loading: false,
      error: payload
    }
  };
}

function loadAvailableScopesRequested(_, state) {
  return {
    ...state,
    availableScopes: {
      loading: true,
      error: null
    }
  };
}

function loadAvailableScopesFulfilled(payload, state) {
  return {
    ...state,
    availableScopes: {
      loading: false,
      error: null,
      ...payload
    }
  };
}

function loadAvailableScopesFailed(payload, state) {
  return {
    ...state,
    availableScopes: {
      loading: false,
      error: payload
    }
  };
}

const reducerActionMap = {
  [ADVANCED_LEGAL_LOAD_APPLICATIONS_REQUESTED]: loadApplicationsRequested,
  [ADVANCED_LEGAL_LOAD_APPLICATIONS_FULFILLED]: loadApplicationsFulfilled,
  [ADVANCED_LEGAL_LOAD_APPLICATIONS_FAILED]: loadApplicationsFailed,
  [ADVANCED_LEGAL_LOAD_APPLICATION_REPORT_REQUESTED]: loadApplicationReportRequested,
  [ADVANCED_LEGAL_LOAD_APPLICATION_REPORT_FULFILLED]: loadApplicationReportFulfilled,
  [ADVANCED_LEGAL_LOAD_APPLICATION_REPORT_FAILED]: loadApplicationReportFailed,
  [ADVANCED_LEGAL_LOAD_COMPONENT_REQUESTED]: loadComponentRequested,
  [ADVANCED_LEGAL_LOAD_COMPONENT_FULFILLED]: loadComponentFulfilled,
  [ADVANCED_LEGAL_LOAD_COMPONENT_FAILED]: loadComponentFailed,
  [ADVANCED_LEGAL_LOAD_AVAILABLE_SCOPES_REQUESTED]: loadAvailableScopesRequested,
  [ADVANCED_LEGAL_LOAD_AVAILABLE_SCOPES_FULFILLED]: loadAvailableScopesFulfilled,
  [ADVANCED_LEGAL_LOAD_AVAILABLE_SCOPES_FAILED]: loadAvailableScopesFailed
};

const advancedLegalReducer = createReducerFromActionMap(reducerActionMap, initialState);
export default advancedLegalReducer;
