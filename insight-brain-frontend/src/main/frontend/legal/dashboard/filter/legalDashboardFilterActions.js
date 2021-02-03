/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import axios from 'axios';

import { noPayloadActionCreator, payloadParamActionCreator } from '../../../util/reduxUtil';
import {
  getApplicationsUrl,
  getApplicationTagsUrl,
  getDashboardFilters,
  getOrganizationsUrl
} from '../../../util/CLMLocation';
import { Messages } from '../../../util/CommonServices';
import { loadResults } from '../legalDashboardActions';
import { fetchStageTypes } from '../../../stages/stagesActions';

export const LOAD_LEGAL_FILTER_REQUESTED = 'LOAD_LEGAL_FILTER_REQUESTED';
export const LOAD_LEGAL_FILTER_FAILED = 'LOAD_LEGAL_FILTER_FAILED';
export const FETCH_LEGAL_AVAILABLE_FILTER_OPTIONS_FULFILLED = 'FETCH_LEGAL_AVAILABLE_FILTER_OPTIONS_FULFILLED';
export const FETCH_LEGAL_CURRENT_FILTER_FULFILLED = 'FETCH_LEGAL_CURRENT_FILTER_FULFILLED';
export const APPLY_SAVED_FILTER_FAILED = 'APPLY_SAVED_FILTER_FAILED';
export const APPLY_LEGAL_FILTER_REQUESTED = 'APPLY_LEGAL_FILTER_REQUESTED';
export const APPLY_LEGAL_FILTER_FULFILLED = 'APPLY_LEGAL_FILTER_FULFILLED';
export const APPLY_LEGAL_FILTER_FAILED = 'APPLY_LEGAL_FILTER_FAILED';
export const TOGGLE_LEGAL_FILTER = 'TOGGLE_LEGAL_FILTER';
export const TOGGLE_LEGAL_APPS_AND_ORGS = 'TOGGLE_LEGAL_APPS_AND_ORGS';
export const REVERT_LEGAL_FILTER = 'REVERT_LEGAL_FILTER';
export const SET_DISPLAY_LEGAL_SAVE_FILTER_MODAL = 'SET_DISPLAY_LEGAL_SAVE_FILTER_MODAL';

export function loadFilter(resultsType = null) {
  return (dispatch, getState) => {
    dispatch({ type: LOAD_LEGAL_FILTER_REQUESTED });

    const promises = [
      axios.get(getApplicationsUrl()),
      axios.get(getOrganizationsUrl()),
      axios.get(getApplicationTagsUrl()),
      axios.get(getDashboardFilters()),
      dispatch(fetchStageTypes('dashboard'))
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
    type: FETCH_LEGAL_AVAILABLE_FILTER_OPTIONS_FULFILLED,
    payload: {
      applications,
      organizations,
      categories,
      stages
    }
  };
}

function fetchCurrentFilterFulfilled(filter, resultsType) {
  return (dispatch) => {
    resultsType = resultsType || 'applications';
    dispatch({
      type: FETCH_LEGAL_CURRENT_FILTER_FULFILLED,
      payload: filter
    });
    if (!filter.needsAcknowledgement) {
      return dispatch(loadResults(resultsType));
    }
    return Promise.resolve();
  };
}

const loadFilterFailed = payloadParamActionCreator(LOAD_LEGAL_FILTER_FAILED);

function persistAppliedFilter(filter, basedOnFilterName) {
  return dispatch => {
    dispatch({ type: APPLY_LEGAL_FILTER_REQUESTED });
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

const applyFilterFailed = payloadParamActionCreator(APPLY_LEGAL_FILTER_FAILED);

function applyFilterFulfilled(filter, basedOnFilterName) {
  return (dispatch) => {
    dispatch({
      type: APPLY_LEGAL_FILTER_FULFILLED,
      payload: { filter, basedOnFilterName }
    });
    return dispatch(loadResults('applications'));
  };
}

export function toggleFilter(filterName, selectedIds) {
  return {
    type: TOGGLE_LEGAL_FILTER,
    payload: { filterName, selectedIds }
  };
}
export const setDisplaySaveFilterModal = payloadParamActionCreator(SET_DISPLAY_LEGAL_SAVE_FILTER_MODAL);

export function toggleAppsAndOrgs(selectedOrganizations, selectedApplications) {
  return {
    type: TOGGLE_LEGAL_APPS_AND_ORGS,
    payload: { selectedOrganizations, selectedApplications }
  };
}

export const revert = noPayloadActionCreator(REVERT_LEGAL_FILTER);
