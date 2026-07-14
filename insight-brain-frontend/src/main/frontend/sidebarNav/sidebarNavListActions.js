/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

import { loadFilter } from '../dashboard/filter/dashboardFilterActions';
import { payloadParamActionCreator } from '../util/reduxUtil';
import { stateGo } from '../reduxUiRouter/routerActions';
import { FIREWALL_WAIVER_DETAILS } from 'MainRoot/constants/states';
import { loadContainerWaiverList } from 'MainRoot/firewall/firewallActions';
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

  const isFirewall = selectIsStandaloneFirewall(getState());
  const containerPromise = isFirewall ? Promise.resolve(dispatch(loadContainerWaiverList())) : Promise.resolve();

  return Promise.all([filterPromise, containerPromise])
    .then(() => {
      const state = getState();
      const componentWaivers = state.dashboard.waivers.results;
      const containerList = isFirewall ? state.firewall?.containerWaiverGridState?.containerWaiverList || [] : [];
      // Only re-shape into a merged array when there are container waivers to fold in; otherwise
      // preserve the original component-waivers payload shape (may be an array OR the legacy
      // object emitted by dashboard filters) so downstream consumers of the sidebar list don't
      // regress.
      const data = containerList.length
        ? [
            ...(Array.isArray(componentWaivers) ? componentWaivers : []),
            ...containerList.map((w) => ({
              ...w,
              id: w.policyWaiverId,
              threatLevel: w.maxThreatLevel || 0,
              policyName: `Multiple-Policy-Types(${w.uniquePolicyCount || 0})`,
              ownerName: w.applicationScope,
              ownerType: 'application',
              forContainerImage: true,
            })),
          ]
        : componentWaivers;
      return dispatch(
        loadSidebarNavListFulfilled({
          data,
          contentType: 'waivers',
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
