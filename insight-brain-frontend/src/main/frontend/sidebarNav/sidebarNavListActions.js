/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

import { loadFilter } from '../dashboard/filter/dashboardFilterActions';
import { payloadParamActionCreator } from '../util/reduxUtil';
import { stateGo } from '../reduxUiRouter/routerActions';

export const LOAD_SIDEBAR_NAV_LIST_REQUESTED =
  'LOAD_SIDEBAR_NAV_LIST_REQUESTED';
export const LOAD_SIDEBAR_NAV_LIST_FULFILLED =
  'LOAD_SIDEBAR_NAV_LIST_FULFILLED';
export const LOAD_SIDEBAR_NAV_LIST_FAILED = 'LOAD_SIDEBAR_NAV_LIST_FAILED';

export function loadSidebarNav({
  type = null,
  sidebarReference = null,
  sidebarId = null,
}) {
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
          return loadViolations(
            dispatch,
            getState,
            sidebarReference,
            sidebarId
          );
        default:
          return dispatch(loadSidebarNavListFailed(`Unknown type: ${type}`));
      }
    }
  };
}

function loadViolations(dispatch, getState, sidebarReference) {
  let filterPromise = null;

  switch (sidebarReference) {
    case 'filter':
      filterPromise = dispatch(loadFilter('violations'));
      break;
    default:
      return dispatch(
        loadSidebarNavListFailed(
          `Unknown sidebarReference: ${sidebarReference}`
        )
      );
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

export function gotoNewVulnerability(id) {
  return stateGo('sidebarView.violation', { id });
}

const loadSidebarNavListRequested = payloadParamActionCreator(
  LOAD_SIDEBAR_NAV_LIST_REQUESTED
);
const loadSidebarNavListFulfilled = payloadParamActionCreator(
  LOAD_SIDEBAR_NAV_LIST_FULFILLED
);
const loadSidebarNavListFailed = payloadParamActionCreator(
  LOAD_SIDEBAR_NAV_LIST_FAILED
);
