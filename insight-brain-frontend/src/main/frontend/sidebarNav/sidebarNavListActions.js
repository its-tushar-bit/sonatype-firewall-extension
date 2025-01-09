/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

import { loadFilter } from '../dashboard/filter/dashboardFilterActions';
import { payloadParamActionCreator } from '../util/reduxUtil';
import { stateGo } from '../reduxUiRouter/routerActions';
import { FIREWALL_FIREWALLPAGE_WAIVERS, FIREWALL_WAIVER_DETAILS } from 'MainRoot/constants/states';
import { selectIsStandaloneFirewall } from 'MainRoot/reduxUiRouter/routerSelectors';

export const LOAD_SIDEBAR_NAV_LIST_REQUESTED = 'LOAD_SIDEBAR_NAV_LIST_REQUESTED';
export const LOAD_SIDEBAR_NAV_LIST_FULFILLED = 'LOAD_SIDEBAR_NAV_LIST_FULFILLED';
export const LOAD_SIDEBAR_NAV_LIST_FAILED = 'LOAD_SIDEBAR_NAV_LIST_FAILED';
export const SET_SIDEBAR_NAV_LIST_DATA = 'SET_SIDEBAR_NAV_LIST_DATA';

export function loadSidebarNav({ type = null, sidebarReference = null, sidebarId = null }) {
  return function (dispatch, getState) {
    dispatch(
      loadSidebarNavListRequested({
        contentType: type,
        sidebarReference,
        sidebarId,
      })
    );

    if (type) {
      switch (type) {
        case 'violation':
          return loadViolations(dispatch, getState, sidebarReference, sidebarId);
        case 'waiver':
        case 'autoWaiver':
          return loadWaivers(dispatch, getState, sidebarReference, sidebarId);
        default:
          return dispatch(loadSidebarNavListFailed(`Unknown type: ${type}`));
      }
    }
  };
}

function loadViolations(dispatch, getState, sidebarReference) {
  let filterPromise = null;
  const { results } = getState().dashboard.violations;
  switch (sidebarReference) {
    case 'filter':
      filterPromise = results ? Promise.resolve() : dispatch(loadFilter('violations', true));
      break;
    default:
      return dispatch(loadSidebarNavListFailed(`Unknown sidebarReference: ${sidebarReference}`));
  }

  return filterPromise
    .then(() => {
      const { dashboard } = getState();
      return dispatch(
        loadSidebarNavListFulfilled({
          data: dashboard.violations.results,
          contentType: 'violations',
          backButtonStateName: 'dashboard.overview.violations',
        })
      );
    })
    .catch((err) => dispatch(loadSidebarNavListFailed(err)));
}

function loadWaivers(dispatch, getState, sidebarReference) {
  let filterPromise = null;

  switch (sidebarReference) {
    case 'filter':
      filterPromise = dispatch(loadFilter('waivers', true));
      break;
    default:
      return dispatch(loadSidebarNavListFailed(`Unknown sidebarReference: ${sidebarReference}`));
  }

  const state = getState();
  const isStandaloneFirewall = selectIsStandaloneFirewall(state);
  const backButtonStateName = isStandaloneFirewall ? FIREWALL_FIREWALLPAGE_WAIVERS : 'dashboard.overview.waivers';

  return filterPromise
    .then(() => {
      const state = getState();
      const { dashboard } = state;
      return dispatch(
        loadSidebarNavListFulfilled({
          data: dashboard.waivers.results,
          contentType: 'waivers',
          backButtonStateName: backButtonStateName,
        })
      );
    })
    .catch((err) => dispatch(loadSidebarNavListFailed(err)));
}

export function gotoNewVulnerability(id) {
  return stateGo('sidebarView.violation', { id });
}

export function gotoWaiver(ownerId, ownerType, waiverId, isStandaloneFirewall) {
  const stateToGo = isStandaloneFirewall ? FIREWALL_WAIVER_DETAILS : 'waiver.details';
  return stateGo(stateToGo, { ownerId, ownerType, waiverId });
}

export function goToWaiverWithType(ownerId, ownerType, waiverId, type, isStandaloneFirewall) {
  const stateToGo = isStandaloneFirewall ? FIREWALL_WAIVER_DETAILS : 'waiver.details';
  return stateGo(stateToGo, { ownerId, ownerType, waiverId, type });
}

export const setSidebarNavListData = payloadParamActionCreator(SET_SIDEBAR_NAV_LIST_DATA);

const loadSidebarNavListRequested = payloadParamActionCreator(LOAD_SIDEBAR_NAV_LIST_REQUESTED);
const loadSidebarNavListFulfilled = payloadParamActionCreator(LOAD_SIDEBAR_NAV_LIST_FULFILLED);
const loadSidebarNavListFailed = payloadParamActionCreator(LOAD_SIDEBAR_NAV_LIST_FAILED);
