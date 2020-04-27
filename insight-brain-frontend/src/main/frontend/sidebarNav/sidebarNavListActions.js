/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import axios from 'axios';

import { payloadParamActionCreator } from '../util/reduxUtil';
import { getNewestRisksUrl } from '../util/CLMLocation';
import { stateGo } from '../reduxUiRouter/routerActions';
import { createDashboardDataRequestPayload } from '../dashboard/utils/dashboard.utils.module';
import { MAX_RESULTS } from '../dashboard/services/dashboard.data.service';
import { translateViolationsSortFields } from '../dashboard/services/sortFieldsUtils';

export const LOAD_SIDEBAR_NAV_LIST_REQUESTED = 'LOAD_SIDEBAR_NAV_LIST_REQUESTED';
export const LOAD_SIDEBAR_NAV_LIST_FULFILLED = 'LOAD_SIDEBAR_NAV_LIST_FULFILLED';
export const LOAD_SIDEBAR_NAV_LIST_FAILED = 'LOAD_SIDEBAR_NAV_LIST_FAILED';

function sidebarNavDoesNotNeedLoading(state, sidebarReference, sidebarId) {
  return sidebarId === state.sidebarNavList.sidebarId && sidebarReference === state.sidebarNavList.sidebarReference;
}

export function loadSidebarNav({type = null, sidebarReference = null, sidebarId = null}) {
  return function(dispatch, getState) {
    if (sidebarNavDoesNotNeedLoading(getState(), sidebarReference, sidebarId)) {
      return Promise.resolve();
    }

    dispatch(loadSidebarNavListRequested({ sidebarReference, sidebarId }));

    switch (type) {
      case 'violation':
        return loadViolations(dispatch, getState, sidebarReference, sidebarId);
      default:
        return dispatch(loadSidebarNavListFailed(`Unknown type: ${type}`));
    }
  };
}

function getFilterViolationRequest(state) {
  let sortFields = translateViolationsSortFields(state.dashboard.violations.sortFields);
  let filter = state.dashboardFilter.appliedFilter;
  return createDashboardDataRequestPayload(filter, MAX_RESULTS, sortFields);
}

function loadViolations(dispatch, getState, sidebarReference) {
  let dataPromise = null;

  switch (sidebarReference) {
    case 'filter':
      dataPromise = axios.post(getNewestRisksUrl(), getFilterViolationRequest(getState()));
      break;
    default:
      return dispatch(loadSidebarNavListFailed(`Unknown sidebarReference: ${sidebarReference}`));
  }

  return dataPromise
      .then(({ data }) => dispatch(loadSidebarNavListFulfilled({
        data: data.dashboardResults,
        contentType: 'violations',
        backButtonStateName: 'dashboard.overview.violations'
      })))
      .catch(err => dispatch(loadSidebarNavListFailed(err)));
}

export function gotoNewVulnerability(id) {
  return stateGo('violation', { id });
}

const loadSidebarNavListRequested = payloadParamActionCreator(LOAD_SIDEBAR_NAV_LIST_REQUESTED);
const loadSidebarNavListFulfilled = payloadParamActionCreator(LOAD_SIDEBAR_NAV_LIST_FULFILLED);
const loadSidebarNavListFailed = payloadParamActionCreator(LOAD_SIDEBAR_NAV_LIST_FAILED);
