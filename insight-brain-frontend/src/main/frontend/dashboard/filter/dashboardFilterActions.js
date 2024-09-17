/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import axios from 'axios';

import { fetchStageTypes } from '../../stages/stagesActions';
import { fetchSavedFilters } from './manageFiltersActions';
import { loadResults, RESET_ALL_TABS, setPage } from '../results/dashboardResultsActions';
import { actions as ownerSideNavActions } from 'MainRoot/OrgsAndPolicies/ownerSideNav/ownerSideNavSlice';
import { noPayloadActionCreator, payloadParamActionCreator } from '../../util/reduxUtil';
import {
  getApplicationsUrl,
  getOrganizationsUrl,
  getApplicationTagsUrl,
  getDashboardFilters,
  getRepositoriesUrl,
} from '../../util/CLMLocation';
import { filterToJson } from './dashboardFilterService';
import defaultFilter from './defaultFilter';
import { Messages } from '../../utilAngular/CommonServices';
import { actions as waiverActions } from 'MainRoot/waivers/waiverSlice';

export const LOAD_FILTER_REQUESTED = 'LOAD_FILTER_REQUESTED';
export const FETCH_AVAILABLE_FILTER_OPTIONS_FULFILLED = 'FETCH_AVAILABLE_FILTER_OPTIONS_FULFILLED';
export const FETCH_CURRENT_FILTER_FULFILLED = 'FETCH_CURRENT_FILTER_FULFILLED';
export const LOAD_FILTER_FAILED = 'LOAD_FILTER_FAILED';
export const APPLY_SAVED_FILTER_FAILED = 'APPLY_SAVED_FILTER_FAILED';
export const APPLY_FILTER_REQUESTED = 'APPLY_FILTER_REQUESTED';
export const APPLY_FILTER_FULFILLED = 'APPLY_FILTER_FULFILLED';
export const APPLY_FILTER_FAILED = 'APPLY_FILTER_FAILED';
export const APPLY_FILTER_CANCELLED = 'APPLY_FILTER_CANCELLED';
export const REFRESH_VIOLATION_DETAILS = 'REFRESH_VIOLATION_DETAILS';
export const REFRESH_VIOLATION_DETAILS_FAILED = 'REFRESH_VIOLATION_DETAILS_FAILED';
export const TOGGLE_FILTER = 'TOGGLE_FILTER';
export const TOGGLE_APPS_AND_ORGS = 'TOGGLE_APPS_AND_ORGS';
export const TOGGLE_REPOSITORIES = 'TOGGLE_REPOSITORIES';
export const SELECT_AGE = 'SELECT_AGE';
export const SELECT_EXPIRATION_DATE = 'SELECT_EXPIRATION_DATE';
export const REVERT_FILTER = 'REVERT_FILTER';
export const SET_DISPLAY_SAVE_FILTER_MODAL = 'SET_DISPLAY_SAVE_FILTER_MODAL';
export const TOGGLE_FILTER_SIDEBAR = 'TOGGLE_FILTER_SIDEBAR';

export function loadFilter(resultsType = null, isLoadResults = false) {
  return (dispatch, getState) => {
    dispatch({ type: LOAD_FILTER_REQUESTED });
    return Promise.all([
      axios.get(getApplicationsUrl()),
      axios.get(getOrganizationsUrl()),
      axios.get(getApplicationTagsUrl()),
      axios.get(getDashboardFilters()),
      axios.get(getRepositoriesUrl()),
      dispatch(waiverActions.loadCachedWaiverReasons()),
      dispatch(fetchStageTypes('dashboard')),
      dispatch(fetchSavedFilters()),
      dispatch(ownerSideNavActions.loadOwnerList()),
    ])
      .then((data) => {
        const [applications, organizations, categoriesData, filterData, repositories] = data;
        // Get dashboard-stages from general state
        const { dashboard } = getState().stages;

        dispatch(
          fetchAvailableFilterOptionsFulfilled(
            applications.data,
            organizations.data,
            categoriesData.data,
            dashboard.stageTypes,
            repositories.data.repositories
          )
        );
        return dispatch(fetchCurrentFilterFulfilled(filterData.data, resultsType, isLoadResults));
      })
      .catch((error) => {
        dispatch(loadFilterFailed(Messages.getHttpErrorMessage(error)));
      });
  };
}

function fetchAvailableFilterOptionsFulfilled(applications, organizations, categories, stages, repositories) {
  return {
    type: FETCH_AVAILABLE_FILTER_OPTIONS_FULFILLED,
    payload: {
      applications,
      organizations,
      categories,
      stages,
      repositories,
    },
  };
}

function fetchCurrentFilterFulfilled(filter, resultsType, isLoadResults) {
  return (dispatch, getState) => {
    resultsType = resultsType || getState().dashboard.currentTab;
    dispatch({
      type: FETCH_CURRENT_FILTER_FULFILLED,
      payload: filter,
    });
    if (isLoadResults && !filter.needsAcknowledgement) {
      return dispatch(loadResults(resultsType));
    }
    return Promise.resolve();
  };
}

const loadFilterFailed = payloadParamActionCreator(LOAD_FILTER_FAILED);

function persistAppliedFilter(filter, basedOnFilterName) {
  return (dispatch) => {
    dispatch({ type: APPLY_FILTER_REQUESTED });
    return axios.put(getDashboardFilters(), { filter, basedOnFilterName });
  };
}

export function applyFilter(filter, basedOnFilterName) {
  return (dispatch) =>
    dispatch(persistAppliedFilter(filter, basedOnFilterName))
      .then(({ data }) => dispatch(applyFilterFulfilled(data, basedOnFilterName)))
      .catch((error) => {
        dispatch(applyFilterFailed(Messages.getHttpErrorMessage(error)));
      });
}

export function applyDefaultFilter() {
  return (dispatch) =>
    dispatch(persistAppliedFilter(filterToJson(defaultFilter), null))
      .then(({ data }) => dispatch(applyFilterFulfilled(data, null)))
      .catch((error) => {
        dispatch(applySavedFilterFailed(`Default Filter Error: ${Messages.getHttpErrorMessage(error)}`));
      });
}

export function applySavedFilter({ filter, name }) {
  return (dispatch) =>
    dispatch(persistAppliedFilter(filter, name))
      .then(({ data }) => dispatch(applyFilterFulfilled(data, name)))
      .catch((error) => {
        dispatch(applySavedFilterFailed(`${name} Error: ${Messages.getHttpErrorMessage(error)}`));
      });
}

const applyFilterFailed = payloadParamActionCreator(APPLY_FILTER_FAILED);

export const applyFilterCancelled = noPayloadActionCreator(APPLY_FILTER_CANCELLED);

const applySavedFilterFailed = payloadParamActionCreator(APPLY_SAVED_FILTER_FAILED);

function applyFilterFulfilled(filter, basedOnFilterName) {
  return (dispatch, getState) => {
    dispatch({
      type: RESET_ALL_TABS,
    });
    dispatch({
      type: APPLY_FILTER_FULFILLED,
      payload: { filter, basedOnFilterName },
    });
    return dispatch(setPage(getState().dashboard.currentTab, 0));
  };
}

export function toggleFilter(filterName, selectedIds) {
  return {
    type: TOGGLE_FILTER,
    payload: { filterName, selectedIds },
  };
}

export const selectAge = payloadParamActionCreator(SELECT_AGE);

export const selectExpirationDate = payloadParamActionCreator(SELECT_EXPIRATION_DATE);

export const setDisplaySaveFilterModal = payloadParamActionCreator(SET_DISPLAY_SAVE_FILTER_MODAL);

export function toggleAppsAndOrgs(selectedOrganizations, selectedApplications) {
  return {
    type: TOGGLE_APPS_AND_ORGS,
    payload: { selectedOrganizations, selectedApplications },
  };
}

export const toggleRepositories = payloadParamActionCreator(TOGGLE_REPOSITORIES);

export const toggleFilterSidebar = payloadParamActionCreator(TOGGLE_FILTER_SIDEBAR);

export const revert = noPayloadActionCreator(REVERT_FILTER);
