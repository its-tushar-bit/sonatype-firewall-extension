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
  getLegalDashboardFilters,
  getOrganizationsUrl,
} from '../../../util/CLMLocation';
import { Messages } from '../../../utilAngular/CommonServices';
import { loadResults } from '../legalDashboardActions';
import { fetchStageTypes } from '../../../stages/stagesActions';
import { actions as ownerSideNavActions } from 'MainRoot/OrgsAndPolicies/ownerSideNav/ownerSideNavSlice';
import { filterToJson } from './legalDashboardFilterService';
import { fetchSavedFilters } from './manageLegalFiltersActions';
import defaultFilter from './defaultFilter';

export const LEGAL_DASHBOARD_LOAD_FILTER_REQUESTED = 'LEGAL_DASHBOARD_LOAD_FILTER_REQUESTED';
export const LEGAL_DASHBOARD_LOAD_FILTER_FAILED = 'LEGAL_DASHBOARD_LOAD_FILTER_FAILED';
export const LEGAL_DASHBOARD_FETCH_AVAILABLE_FILTER_OPTIONS_FULFILLED =
  'LEGAL_DASHBOARD_FETCH_AVAILABLE_FILTER_OPTIONS_FULFILLED';
export const LEGAL_DASHBOARD_FETCH_CURRENT_FILTER_FULFILLED = 'LEGAL_DASHBOARD_FETCH_CURRENT_FILTER_FULFILLED';
export const LEGAL_DASHBOARD_APPLY_SAVED_FILTER_FAILED = 'LEGAL_DASHBOARD_APPLY_SAVED_FILTER_FAILED';
export const LEGAL_DASHBOARD_APPLY_FILTER_REQUESTED = 'LEGAL_DASHBOARD_APPLY_FILTER_REQUESTED';
export const LEGAL_DASHBOARD_APPLY_FILTER_FULFILLED = 'LEGAL_DASHBOARD_APPLY_FILTER_FULFILLED';
export const LEGAL_DASHBOARD_APPLY_FILTER_FAILED = 'LEGAL_DASHBOARD_APPLY_FILTER_FAILED';
export const LEGAL_DASHBOARD_APPLY_FILTER_CANCELLED = 'LEGAL_DASHBOARD_APPLY_FILTER_CANCELLED';
export const LEGAL_DASHBOARD_TOGGLE_FILTER = 'LEGAL_DASHBOARD_TOGGLE_FILTER';
export const LEGAL_DASHBOARD_TOGGLE_APPS_AND_ORGS = 'LEGAL_DASHBOARD_TOGGLE_APPS_AND_ORGS';
export const LEGAL_DASHBOARD_REVERT_FILTER = 'LEGAL_DASHBOARD_REVERT_FILTER';
export const LEGAL_DASHBOARD_SET_DISPLAY_SAVE_FILTER_MODAL = 'LEGAL_DASHBOARD_SET_DISPLAY_SAVE_FILTER_MODAL';
export const LEGAL_DASHBOARD_TOGGLE_FILTER_SIDEBAR = 'LEGAL_DASHBOARD_TOGGLE_FILTER_SIDEBAR';

export function loadFilter() {
  return (dispatch, getState) => {
    dispatch({ type: LEGAL_DASHBOARD_LOAD_FILTER_REQUESTED });

    const promises = [
      axios.get(getApplicationsUrl()),
      axios.get(getOrganizationsUrl()),
      axios.get(getApplicationTagsUrl()),
      axios.get(getLegalDashboardFilters()),
      dispatch(fetchStageTypes('dashboard')),
      dispatch(fetchSavedFilters()),
      dispatch(ownerSideNavActions.loadOwnerList()),
    ];

    return axios
      .all(promises)
      .then((data) => {
        const [applications, organizations, categoriesData, filterData] = data;
        // Get dashboard-stages from general state
        const { dashboard } = getState().stages;

        dispatch(
          fetchAvailableFilterOptionsFulfilled(
            applications.data,
            organizations.data,
            categoriesData.data,
            dashboard.stageTypes
          )
        );
        return dispatch(fetchCurrentFilterFulfilled(filterData.data));
      })
      .catch((error) => {
        dispatch(loadFilterFailed(error));
        return Promise.reject(error);
      });
  };
}

function fetchAvailableFilterOptionsFulfilled(applications, organizations, categories, stages) {
  return {
    type: LEGAL_DASHBOARD_FETCH_AVAILABLE_FILTER_OPTIONS_FULFILLED,
    payload: {
      applications,
      organizations,
      categories,
      stages,
    },
  };
}

const fetchCurrentFilterFulfilled = payloadParamActionCreator(LEGAL_DASHBOARD_FETCH_CURRENT_FILTER_FULFILLED);

const loadFilterFailed = payloadParamActionCreator(LEGAL_DASHBOARD_LOAD_FILTER_FAILED);

function persistAppliedFilter(filter, basedOnFilterName) {
  return (dispatch) => {
    dispatch({ type: LEGAL_DASHBOARD_APPLY_FILTER_REQUESTED });
    const applyFilterBody = {
      filter,
      basedOnFilterName,
      type: 'ADVANCED_LEGAL_PACK_DASHBOARD',
    };
    return axios.put(getLegalDashboardFilters(), applyFilterBody);
  };
}

export function applyFilter(filter, basedOnFilterName) {
  return (dispatch) =>
    dispatch(persistAppliedFilter(filter, basedOnFilterName))
      .catch((error) => {
        dispatch(applyFilterFailed(Messages.getHttpErrorMessage(error)));
        return Promise.reject(error);
      })
      .then(({ data }) => dispatch(applyFilterFulfilled(data, basedOnFilterName)));
}

export function applyDefaultFilter() {
  return (dispatch) =>
    dispatch(persistAppliedFilter(filterToJson(defaultFilter), null))
      .catch((error) => {
        dispatch(applySavedFilterFailed('Default filter'));
        return Promise.reject(error);
      })
      .then(({ data }) => dispatch(applyFilterFulfilled(data, null)));
}

export function applySavedFilter({ filter, name }) {
  return (dispatch) =>
    dispatch(persistAppliedFilter(filter, name))
      .catch((error) => {
        dispatch(applySavedFilterFailed(name));
        return Promise.reject(error);
      })
      .then(({ data }) => dispatch(applyFilterFulfilled(data, name)));
}

const applyFilterFailed = payloadParamActionCreator(LEGAL_DASHBOARD_APPLY_FILTER_FAILED);

export const applyFilterCancelled = noPayloadActionCreator(LEGAL_DASHBOARD_APPLY_FILTER_CANCELLED);

const applySavedFilterFailed = payloadParamActionCreator(LEGAL_DASHBOARD_APPLY_SAVED_FILTER_FAILED);

function applyFilterFulfilled(data, basedOnFilterName) {
  return (dispatch, getState) => {
    dispatch({
      type: LEGAL_DASHBOARD_APPLY_FILTER_FULFILLED,
      payload: {
        filter: data.filter,
        basedOnFilterName,
      },
    });
    return dispatch(loadResults(getState().router.currentState.data.activeTab));
  };
}

export function toggleFilter(filterName, selectedIds) {
  return {
    type: LEGAL_DASHBOARD_TOGGLE_FILTER,
    payload: { filterName, selectedIds },
  };
}
export const setDisplaySaveFilterModal = payloadParamActionCreator(LEGAL_DASHBOARD_SET_DISPLAY_SAVE_FILTER_MODAL);

export function toggleAppsAndOrgs(selectedOrganizations, selectedApplications) {
  return {
    type: LEGAL_DASHBOARD_TOGGLE_APPS_AND_ORGS,
    payload: { selectedOrganizations, selectedApplications },
  };
}

export const toggleFilterSidebar = payloadParamActionCreator(LEGAL_DASHBOARD_TOGGLE_FILTER_SIDEBAR);

export const revert = noPayloadActionCreator(LEGAL_DASHBOARD_REVERT_FILTER);
