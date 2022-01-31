/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import axios from 'axios';

import { getLegalDashboardApplicationsUrl, getLegalDashboardComponentsUrl } from '../../util/CLMLocation';
import { payloadParamActionCreator } from '../../util/reduxUtil';
import { DASHBOARD } from '../advancedLegalConstants';
import { loadFilter } from 'MainRoot/legal/dashboard/filter/legalDashboardFilterActions';

export const LEGAL_DASHBOARD_LOAD_RESULTS_REQUESTED = 'LEGAL_DASHBOARD_LOAD_RESULTS_REQUESTED';
export const LEGAL_DASHBOARD_LOAD_RESULTS_FULFILLED = 'LEGAL_DASHBOARD_LOAD_RESULTS_FULFILLED';
export const LEGAL_DASHBOARD_LOAD_RESULTS_FAILED = 'LEGAL_DASHBOARD_LOAD_RESULTS_FAILED';
export const LEGAL_DASHBOARD_SET_PAGE = 'LEGAL_DASHBOARD_SET_PAGE';
export const LEGAL_DASHBOARD_FETCH_BACKEND_PAGE = 'LEGAL_DASHBOARD_FETCH_BACKEND_PAGE';
export const LEGAL_DASHBOARD_CHANGE_SORT_FIELD = 'LEGAL_DASHBOARD_CHANGE_SORT_FIELD';
export const LEGAL_DASHBOARD_COMPONENT_SEARCH = 'LEGAL_DASHBOARD_COMPONENT_SEARCH';
export const LEGAL_DASHBOARD_COMPONENT_SET_SEARCH_INPUT_VALUE = 'LEGAL_DASHBOARD_COMPONENT_SET_SEARCH_INPUT_VALUE';

export const legalDashboardSetPage = payloadParamActionCreator(LEGAL_DASHBOARD_SET_PAGE);
const legalDashboardFetchBackendPage = payloadParamActionCreator(LEGAL_DASHBOARD_FETCH_BACKEND_PAGE);
const legalDashboardChangeSortField = payloadParamActionCreator(LEGAL_DASHBOARD_CHANGE_SORT_FIELD);
export const setComponentSearchInputValue = payloadParamActionCreator(LEGAL_DASHBOARD_COMPONENT_SET_SEARCH_INPUT_VALUE);

function loadResultsFulfilled(resultsType, results) {
  return {
    type: LEGAL_DASHBOARD_LOAD_RESULTS_FULFILLED,
    payload: { resultsType, results },
  };
}

function loadResultsFailed(resultsType, error) {
  return {
    type: LEGAL_DASHBOARD_LOAD_RESULTS_FAILED,
    payload: { resultsType, error },
  };
}

export function searchByComponentName() {
  return (dispatch, getState) => {
    dispatch({
      type: LEGAL_DASHBOARD_COMPONENT_SEARCH,
    });
    return dispatchResults(dispatch, 'components', getState);
  };
}

export function loadDashboardUI(resultsType) {
  return (dispatch, getState) => {
    const { applications, categories, organizations, stages } = getState().legalDashboardFilter;
    const areFiltersEmpty = organizations.length + applications.length + categories.length + stages.length === 0;
    if (areFiltersEmpty) {
      //In this case LEGAL_DASHBOARD_LOAD_RESULTS_REQUESTED must be dispatched only after all
      //the async operations executed in loadFilter() are completed otherwise, the filters UI pane will break
      return dispatch(loadFilter()).finally(() => dispatch(loadResults(resultsType)));
    }
    return dispatch(loadResults(resultsType));
  };
}

export function loadResults(resultsType) {
  return (dispatch, getState) => {
    dispatch({
      type: LEGAL_DASHBOARD_LOAD_RESULTS_REQUESTED,
      payload: resultsType,
    });
    return dispatchResults(dispatch, resultsType, getState);
  };
}

function dispatchResults(dispatch, resultsType, getState) {
  return fetchResults(resultsType, getState())
    .then((payload) => {
      dispatch(loadResultsFulfilled(resultsType, payload.data));
    })
    .catch((error) => {
      dispatch(loadResultsFailed(resultsType, error));
      return Promise.reject(error);
    });
}

function fetchResults(resultsType, state) {
  const { applications, organizations, stages, categories, progressOptions } = state.legalDashboardFilter.appliedFilter;
  const backendPage = state.legalDashboard[resultsType].backendPage || 1;

  const appliedFilter = {
    applicationIds: Array.from(applications),
    organizationIds: Array.from(organizations),
    stageTypeIds: Array.from(stages),
    tagIds: Array.from(categories),
    reviewStatus: Array.from(progressOptions),
    page: backendPage,
    pageSize: DASHBOARD[resultsType].itemsPerPage * DASHBOARD[resultsType].pagesToFill,
    order: state.legalDashboard[resultsType].sortField,
  };

  if (resultsType === 'components') {
    appliedFilter.componentName = state.legalDashboard[resultsType].componentNameToSearch;
  }

  const serviceMethod = getServiceMethod(resultsType);
  return axios.post(serviceMethod(), appliedFilter);
}

function getServiceMethod(resultsType) {
  switch (resultsType) {
    case 'applications':
      return getLegalDashboardApplicationsUrl;
    case 'components':
      return getLegalDashboardComponentsUrl;

    default:
      throw new Error('retrieving legal dashboard results is not supported for ' + resultsType);
  }
}

export function fetchBackendPage(resultsType, page) {
  return (dispatch) => {
    dispatch(legalDashboardFetchBackendPage({ resultsType, page }));
    return dispatch(loadResults(resultsType));
  };
}

export function changeSortField(resultsType, sortField) {
  return (dispatch) => {
    dispatch(legalDashboardChangeSortField({ resultsType, sortField }));
    return dispatch(fetchBackendPage(resultsType, 1));
  };
}
