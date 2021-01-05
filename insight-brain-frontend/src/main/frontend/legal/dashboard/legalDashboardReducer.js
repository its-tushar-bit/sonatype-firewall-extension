/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

import { createReducerFromActionMap } from '../../util/reduxUtil';
import {
  LEGAL_DASHBOARD_LOAD_APPLICATIONS_FAILED,
  LEGAL_DASHBOARD_LOAD_APPLICATIONS_FULFILLED,
  LEGAL_DASHBOARD_LOAD_APPLICATIONS_REQUESTED
} from './legalDashboardActions';

const initialState = {
  applications: [],
  components: [],
  loading: false,
  loadError: null
};

function loadApplicationsRequested() {
  return {
    ...initialState,
    loading: true,
    loadError: null
  };
}

function loadApplicationsFulfilled(payload, state) {
  return {
    ...state,
    applications: payload,
    loading: false,
    loadError: null
  };
}

function loadApplicationsFailed(payload, state) {
  return {
    ...state,
    loading: false,
    loadError: payload
  };
}

const reducerActionMap = {
  [LEGAL_DASHBOARD_LOAD_APPLICATIONS_REQUESTED]: loadApplicationsRequested,
  [LEGAL_DASHBOARD_LOAD_APPLICATIONS_FULFILLED]: loadApplicationsFulfilled,
  [LEGAL_DASHBOARD_LOAD_APPLICATIONS_FAILED]: loadApplicationsFailed
};

const legalDashboardReducer = createReducerFromActionMap(reducerActionMap, initialState);
export default legalDashboardReducer;
