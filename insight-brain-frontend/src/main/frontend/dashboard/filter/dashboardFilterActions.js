/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import axios from 'axios';

import { fetchStageTypes } from '../../stages/stagesActions';
import { fetchSavedFilters } from './manageFiltersActions';
import { loadResults } from '../results/dashboardResultsActions';
import { noPayloadActionCreator, payloadParamActionCreator } from '../../util/reduxUtil';
import {
  getApplicationsUrl,
  getOrganizationsUrl,
  getApplicationTagsUrl,
  getDashboardFilters
} from '../../util/CLMLocation';
import { filterToJson } from './dashboardFilterService';
import defaultFilter from './defaultFilter';
import { Messages } from '../../util/CommonServices';

export const LOAD_FILTER_REQUESTED = 'LOAD_FILTER_REQUESTED';
export const FETCH_AVAILABLE_FILTER_OPTIONS_FULFILLED = 'FETCH_AVAILABLE_FILTER_OPTIONS_FULFILLED';
export const FETCH_CURRENT_FILTER_FULFILLED = 'FETCH_CURRENT_FILTER_FULFILLED';
export const LOAD_FILTER_FAILED = 'LOAD_FILTER_FAILED';
export const APPLY_SAVED_FILTER_FAILED = 'APPLY_SAVED_FILTER_FAILED';
export const APPLY_FILTER_REQUESTED = 'APPLY_FILTER_REQUESTED';
export const APPLY_FILTER_FULFILLED = 'APPLY_FILTER_FULFILLED';
export const APPLY_FILTER_FAILED = 'APPLY_FILTER_FAILED';
export const REFRESH_VIOLATION_DETAILS = 'REFRESH_VIOLATION_DETAILS';
export const REFRESH_VIOLATION_DETAILS_FAILED = 'REFRESH_VIOLATION_DETAILS_FAILED';
export const TOGGLE_FILTER = 'TOGGLE_FILTER';
export const TOGGLE_APPS_AND_ORGS = 'TOGGLE_APPS_AND_ORGS';
export const SELECT_AGE = 'SELECT_AGE';
export const REVERT_FILTER = 'REVERT_FILTER';
export const SET_DISPLAY_SAVE_FILTER_MODAL = 'SET_DISPLAY_SAVE_FILTER_MODAL';

export function loadFilter(resultsType = null) {
  return (dispatch, getState) => {
    dispatch({ type: LOAD_FILTER_REQUESTED });

    const promises = [
      axios.get(getApplicationsUrl()),
      axios.get(getOrganizationsUrl()),
      axios.get(getApplicationTagsUrl()),
      axios.get(getDashboardFilters()),
      dispatch(fetchStageTypes('dashboard')),
      dispatch(fetchSavedFilters())
    ];

    return axios.all(promises)
        .then(data => {
          const [applications, organizations, categoriesData, filterData] = data;
          // Get dashboard-stages from general state
          const { dashboard } = getState().stages;

          dispatch(fetchAvailableFilterOptionsFulfilled(
              applications.data, organizations.data, categoriesData.data, dashboard.stageTypes));
          return dispatch(fetchCurrentFilterFulfilled(filterData.data, resultsType));
        })
        .catch(error => {
          dispatch(loadFilterFailed(error));
          return Promise.reject(error);
        });
  };
}

function fetchAvailableFilterOptionsFulfilled(applications, organizations, categories, stages) {
  return {
    type: FETCH_AVAILABLE_FILTER_OPTIONS_FULFILLED,
    payload: {
      applications,
      organizations,
      categories,
      stages
    }
  };
}

function fetchCurrentFilterFulfilled(filter, resultsType) {
  return (dispatch, getState) => {
    resultsType = resultsType || getState().dashboard.currentTab;
    dispatch({
      type: FETCH_CURRENT_FILTER_FULFILLED,
      payload: filter
    });
    if (!filter.needsAcknowledgement) {
      return dispatch(loadResults(resultsType));
    }
    return Promise.resolve();
  };
}

const loadFilterFailed = payloadParamActionCreator(LOAD_FILTER_FAILED);

function persistAppliedFilter(filter, basedOnFilterName) {
  return dispatch => {
    dispatch({ type: APPLY_FILTER_REQUESTED });
    return axios.put(getDashboardFilters(), { filter, basedOnFilterName });
  };
}

export function applyFilter(filter, basedOnFilterName) {
  return dispatch => dispatch(persistAppliedFilter(filter, basedOnFilterName))
      .catch(error => {
        dispatch(applyFilterFailed(Messages.getHttpErrorMessage(error)));
        return Promise.reject(error);
      })
      .then(({data}) => dispatch(applyFilterFulfilled(data, basedOnFilterName)));
}

export function applyDefaultFilter() {
  return dispatch => dispatch(persistAppliedFilter(filterToJson(defaultFilter), null))
      .catch(error => {
        dispatch(applySavedFilterFailed('Default filter'));
        return Promise.reject(error);
      })
      .then(({data}) => dispatch(applyFilterFulfilled(data, null)));
}

export function applySavedFilter({ filter, name }) {
  return dispatch => dispatch(persistAppliedFilter(filter, name))
      .catch(error => {
        dispatch(applySavedFilterFailed(name));
        return Promise.reject(error);
      })
      .then(({data}) => dispatch(applyFilterFulfilled(data, name)));
}

const applyFilterFailed = payloadParamActionCreator(APPLY_FILTER_FAILED);

const applySavedFilterFailed = payloadParamActionCreator(APPLY_SAVED_FILTER_FAILED);

function applyFilterFulfilled(filter, basedOnFilterName) {
  return (dispatch, getState) => {
    dispatch({
      type: APPLY_FILTER_FULFILLED,
      payload: { filter, basedOnFilterName }
    });
    return dispatch(loadResults(getState().dashboard.currentTab));
  };
}

export function toggleFilter(filterName, selectedIds) {
  return {
    type: TOGGLE_FILTER,
    payload: { filterName, selectedIds }
  };
}

export const selectAge = payloadParamActionCreator(SELECT_AGE);

export const setDisplaySaveFilterModal = payloadParamActionCreator(SET_DISPLAY_SAVE_FILTER_MODAL);

export function toggleAppsAndOrgs(selectedOrganizations, selectedApplications) {
  return {
    type: TOGGLE_APPS_AND_ORGS,
    payload: { selectedOrganizations, selectedApplications }
  };
}

export const revert = noPayloadActionCreator(REVERT_FILTER);
