/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { noPayloadActionCreator, payloadParamActionCreator } from '../../util/reduxUtil';

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
export const CLEAR_FILTER = 'CLEAR_FILTER';
export const REVERT_FILTER = 'REVERT_FILTER';

export default function dashboardFilterActions(dashboardResultsActions, manageFiltersActions, $http, CLMLocations, $q,
                                               ApplicationStore, StageTypeStore, OrganizationStore) {

  function loadFilter() {
    return dispatch => {
      dispatch({
        type: LOAD_FILTER_REQUESTED
      });

      const promises = [
        ApplicationStore.get(), StageTypeStore.getDashboardStages(), OrganizationStore.get(),
        $http.get(CLMLocations.getApplicationTagsUrl()), $http.get(CLMLocations.getDashboardFilters()),
        // make sure that saved filters are loaded before we load dashboard filter so we can handle showDirtyAsterisk
        dispatch(manageFiltersActions.fetchSavedFilters())
      ];

      return $q.all(promises)
          .then(data => {
            const [applications, stages, organizations, categoriesData, filterData] = data;
            dispatch(fetchAvailableFilterOptionsFulfilled(applications, stages, organizations, categoriesData.data));
            return dispatch(fetchCurrentFilterFulfilled(filterData.data));
          })
          .catch(error => {
            dispatch(loadFilterFailed(error));
            return $q.reject(error);
          });
    };
  }

  function fetchAvailableFilterOptionsFulfilled(applications, stages, organizations, categories) {
    return {
      type: FETCH_AVAILABLE_FILTER_OPTIONS_FULFILLED,
      payload: {
        applications,
        stages,
        organizations,
        categories
      }
    };
  }

  function fetchCurrentFilterFulfilled(filter) {
    return (dispatch, getState) => {
      dispatch({
        type: FETCH_CURRENT_FILTER_FULFILLED,
        payload: filter
      });
      if (!filter.needsAcknowledgement) {
        return dispatch(dashboardResultsActions.loadResults(getState().dashboard.currentTab));
      }
      return $q.resolve();
    };
  }

  const loadFilterFailed = payloadParamActionCreator(LOAD_FILTER_FAILED);

  function applyFilter(filter, basedOnFilterName) {
    return dispatch => {
      dispatch({
        type: APPLY_FILTER_REQUESTED
      });

      return $http.put(CLMLocations.getDashboardFilters(), {filter, basedOnFilterName})
          .catch(error => {
            dispatch(applyFilterFailed(error));
            return $q.reject(error);
          })
          .then(({data}) => dispatch(updateFiltersFulfilled(data, basedOnFilterName)));
    };
  }

  function applySavedFilter(savedFilter) {
    const {filter, name} = savedFilter;
    return dispatch => {
      dispatch({
        type: APPLY_FILTER_REQUESTED
      });

      return $http.put(CLMLocations.getDashboardFilters(), {filter, basedOnFilterName: name})
          .catch(error => {
            dispatch(applySavedFilterFailed(name));
            return $q.reject(error);
          })
          .then(({data}) => dispatch(updateFiltersFulfilled(data, name)));
    };
  }

  const applyFilterFailed = payloadParamActionCreator(APPLY_FILTER_FAILED);
  const applySavedFilterFailed = payloadParamActionCreator(APPLY_SAVED_FILTER_FAILED);

  function updateFiltersFulfilled(filter, basedOnFilterName) {
    return (dispatch, getState) => {
      dispatch({
        type: APPLY_FILTER_FULFILLED,
        payload: {filter, basedOnFilterName}
      });
      return dispatch(dashboardResultsActions.loadResults(getState().dashboard.currentTab));
    };
  }

  function refreshViolationsDetails() {
    return dispatch => {
      return $http.get(CLMLocations.getDashboardFilters())
          .then(response => response.data)
          .then(({filter, basedOnFilterName}) => dispatch(updateFiltersFulfilled(filter, basedOnFilterName)))
          .then(() => dispatch({type: REFRESH_VIOLATION_DETAILS}))
          .catch(error => {
            dispatch(refreshViolationDetailsFailed(error));
            return $q.reject(error);
          });
    };
  }

  function refreshViolationDetailsFailed(error) {
    return {
      type: REFRESH_VIOLATION_DETAILS_FAILED,
      payload: error
    };
  }

  function toggleFilter(filterName, selectedIds) {
    return {
      type: TOGGLE_FILTER,
      payload: {filterName, selectedIds}
    };
  }

  const selectAge = payloadParamActionCreator(SELECT_AGE);

  function toggleAppsAndOrgs(selectedOrganizations, selectedApplications) {
    return {
      type: TOGGLE_APPS_AND_ORGS,
      payload: {selectedOrganizations, selectedApplications}
    };
  }

  const clear = noPayloadActionCreator(CLEAR_FILTER);
  const revert = noPayloadActionCreator(REVERT_FILTER);

  return {
    clear,
    revert,
    selectAge,
    toggleAppsAndOrgs,
    toggleFilter,
    loadFilter,
    applyFilter,
    applySavedFilter,
    refreshViolationsDetails
  };
}

dashboardFilterActions.$inject = [
  'dashboardResultsActions', 'manageFiltersActions', '$http', 'CLMLocations', '$q', 'ApplicationStore',
  'StageTypeStore', 'OrganizationStore'
];
