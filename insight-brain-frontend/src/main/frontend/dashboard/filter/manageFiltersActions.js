/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { SUBMIT_MASK_SUCCESS_VISIBLE_TIME_MS } from '@sonatype/react-shared-components';
import { noPayloadActionCreator, payloadParamActionCreator } from '../../util/reduxUtil';
import { SET_DISPLAY_SAVE_FILTER_MODAL } from './dashboardFilterActions';

export const FETCH_SAVED_FILTERS_FULFILLED = 'FETCH_SAVED_FILTERS_FULFILLED';
export const FETCH_SAVED_FILTERS_FAILED = 'FETCH_SAVED_FILTERS_FAILED';
export const SAVE_FILTER_REQUESTED = 'SAVE_FILTER_REQUESTED';
export const SAVE_FILTER_FULFILLED = 'SAVE_FILTER_FULFILLED';
export const SAVE_FILTER_FAILED = 'SAVE_FILTER_FAILED';
export const DELETE_SPECIFIED_FILTERS_REQUESTED = 'DELETE_SPECIFIED_FILTERS_REQUESTED';
export const DELETE_SPECIFIED_FILTERS_FULFILLED = 'DELETE_SPECIFIED_FILTERS_FULFILLED';
export const DELETE_SPECIFIED_FILTERS_FAILED = 'DELETE_SPECIFIED_FILTERS_FAILED';
export const RESET_DELETE_FILTERS_STATUS = 'RESET_DELETE_FILTERS_STATUS';

function manageFiltersActions($http, CLMLocations, $q, $timeout, filterService) {

  function fetchSavedFilters() {
    return dispatch => {
      return $http.get(CLMLocations.getDashboardSavedFilters())
          .then(({data}) => dispatch(fetchSavedFiltersFulfilled(data)))
          .catch(error => {
            dispatch(fetchSavedFiltersFailed(error));
            return $q.reject(error);
          });
    };
  }

  const fetchSavedFiltersFulfilled = payloadParamActionCreator(FETCH_SAVED_FILTERS_FULFILLED);
  const fetchSavedFiltersFailed = payloadParamActionCreator(FETCH_SAVED_FILTERS_FAILED);

  function saveFilter(name) {
    return (dispatch, getState) => {
      const { dashboardFilter } = getState(),
          { appliedFilter } = dashboardFilter,
          namedFilter = { name: name, filter: filterService.filterToJson(appliedFilter) };

      dispatch({ type: SAVE_FILTER_REQUESTED });

      return $http.put(CLMLocations.getDashboardSavedFilters(), namedFilter).then(function({data}) {
        dispatch({ type: SAVE_FILTER_FULFILLED, payload: data });
        setTimeout(() => {
          dispatch({ type: SET_DISPLAY_SAVE_FILTER_MODAL, payload: false });
        }, SUBMIT_MASK_SUCCESS_VISIBLE_TIME_MS);

        return dispatch(fetchSavedFilters());
      }, function(error) {
        dispatch({ type: SAVE_FILTER_FAILED, payload: error });
        return $q.reject(error);
      });
    };
  }

  function deleteSpecifiedFilters(filtersToDelete) {
    return dispatch => {
      dispatch({ type: DELETE_SPECIFIED_FILTERS_REQUESTED });
      return filterService.deleteSavedFilters(filtersToDelete)
          .catch(function(error) {
            dispatch({ type: DELETE_SPECIFIED_FILTERS_FAILED, payload: error });
            return $q.reject(error);
          })
          .then(function() {
            dispatch({ type: DELETE_SPECIFIED_FILTERS_FULFILLED, payload: filtersToDelete });

            return dispatch(fetchSavedFilters());
          });
    };
  }

  const resetDeleteFiltersStatus = noPayloadActionCreator(RESET_DELETE_FILTERS_STATUS);

  return {
    fetchSavedFilters,
    deleteSpecifiedFilters,
    saveFilter,
    resetDeleteFiltersStatus
  };
}

manageFiltersActions.$inject = ['$http', 'CLMLocations', '$q', '$timeout', 'dashboardFilterService'];

export default manageFiltersActions;
