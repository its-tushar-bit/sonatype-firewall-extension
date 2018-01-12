/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import dashboardServicesModule from '../services/module';
import dashboardResultsActionsModule from '../results/dashboardResultsActions';

export const UPDATE_FILTERS_DIRTINESS = 'UPDATE_FILTERS_DIRTINESS';
export const UPDATE_FILTERS_REQUESTED = 'UPDATE_FILTERS_REQUESTED';
export const UPDATE_FILTERS_FULFILLED = 'UPDATE_FILTERS_FULFILLED';
export const REFRESH_VIOLATION_DETAILS = 'REFRESH_VIOLATION_DETAILS';
export const REFRESH_VIOLATION_DETAILS_FAILED = 'REFRESH_VIOLATION_DETAILS_FAILED';

function dashboardFilterActions(dashboardResultsActions, $http, CLMLocations, $q) {

  function updateFilters() {
    return {
      type: UPDATE_FILTERS_REQUESTED
    };
  }

  function updateFiltersDirtiness(isDirty) {
    return {
      type: UPDATE_FILTERS_DIRTINESS,
      payload: isDirty
    };
  }

  function updateFiltersFulfilled(filters, needsAcknowledgement, appliedFilterName) {
    return (dispatch, getState) => {
      dispatch({
        type: UPDATE_FILTERS_FULFILLED,
        payload: {filters, needsAcknowledgement, appliedFilterName}
      });
      if (!needsAcknowledgement) {
        return dispatch(dashboardResultsActions.loadResults(getState().dashboard.currentTab));
      }
      return $q.resolve();
    };
  }

  function refreshViolationsDetails() {
    return dispatch => {
      return $http.get(CLMLocations.getDashboardFilters())
          .then(response => response.data)
          .then(({filter, needsAcknowledgement}) => dispatch(updateFiltersFulfilled(filter, needsAcknowledgement)))
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

  return {
    updateFilters,
    updateFiltersDirtiness,
    updateFiltersFulfilled,
    refreshViolationsDetails
  };
}

dashboardFilterActions.$inject = ['dashboardResultsActions', '$http', 'CLMLocations', '$q'];

export default angular.module('dashboardFilterActionsModule',
    [dashboardServicesModule.name, dashboardResultsActionsModule.name])
    .factory('dashboardFilterActions', dashboardFilterActions);
