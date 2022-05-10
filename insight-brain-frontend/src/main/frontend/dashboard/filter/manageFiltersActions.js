/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import axios from 'axios';
import { SUBMIT_MASK_SUCCESS_VISIBLE_TIME_MS } from '@sonatype/react-shared-components';

import { filterToJson } from './dashboardFilterService';
import { SET_DISPLAY_SAVE_FILTER_MODAL } from './dashboardFilterActions';
import { noPayloadActionCreator, payloadParamActionCreator } from '../../util/reduxUtil';
import { getDashboardDeleteFilterUrl, getDashboardSavedFilters } from '../../util/CLMLocation';
import { Messages } from '../../utilAngular/CommonServices';

export const FETCH_SAVED_FILTERS_FULFILLED = 'FETCH_SAVED_FILTERS_FULFILLED';
export const FETCH_SAVED_FILTERS_FAILED = 'FETCH_SAVED_FILTERS_FAILED';
export const SAVE_FILTER_REQUESTED = 'SAVE_FILTER_REQUESTED';
export const SAVE_FILTER_FULFILLED = 'SAVE_FILTER_FULFILLED';
export const SAVE_FILTER_FAILED = 'SAVE_FILTER_FAILED';
export const SELECT_FILTER_TO_DELETE = 'SELECT_FILTER_TO_DELETE';
export const HIDE_DELETE_FILTER_MODAL = 'HIDE_DELETE_FILTER_MODAL';
export const DELETE_FILTER_REQUESTED = 'DELETE_FILTER_REQUESTED';
export const DELETE_FILTER_FULFILLED = 'DELETE_FILTER_FULFILLED';
export const DELETE_FILTER_FAILED = 'DELETE_FILTER_FAILED';
export const SAVE_FILTER_OVERWRITE_REQUESTED = 'SAVE_FILTER_OVERWRITE_REQUESTED';
export const SAVE_DUPLICATE_FILTER_REQUESTED = 'SAVE_DUPLICATE_FILTER_REQUESTED';
export const SAVE_CONFIRM_CANCELLED = 'SAVE_CONFIRM_CANCELLED';

export function fetchSavedFilters() {
  return (dispatch) => {
    return axios
      .get(getDashboardSavedFilters())
      .then(({ data }) => dispatch(fetchSavedFiltersFulfilled(data)))
      .catch((error) => {
        dispatch(fetchSavedFiltersFailed(error));
        return Promise.reject(error);
      });
  };
}

const fetchSavedFiltersFulfilled = payloadParamActionCreator(FETCH_SAVED_FILTERS_FULFILLED);

const fetchSavedFiltersFailed = payloadParamActionCreator(FETCH_SAVED_FILTERS_FAILED);

export function saveFilter({ name, isOverwriting }) {
  return (dispatch, getState) => {
    const { dashboardFilter, manageFilters } = getState(),
      { appliedFilter } = dashboardFilter,
      filter = filterToJson(appliedFilter),
      namedFilter = { name, filter },
      { saveFilterWarning, savedFilters } = manageFilters;

    if (!saveFilterWarning) {
      if (isOverwriting) {
        return dispatch({ type: SAVE_FILTER_OVERWRITE_REQUESTED });
      }

      const normalizedName = normalize(name);
      const existingDuplicateFilter = savedFilters.find((filterName) => normalizedName === normalize(filterName.name));

      if (existingDuplicateFilter) {
        return dispatch({
          type: SAVE_DUPLICATE_FILTER_REQUESTED,
          payload: existingDuplicateFilter.name,
        });
      }
    }

    dispatch({ type: SAVE_FILTER_REQUESTED });

    return axios
      .put(getDashboardSavedFilters(), namedFilter)
      .catch((error) => {
        dispatch({ type: SAVE_FILTER_FAILED, payload: error });
        return Promise.reject(error);
      })
      .then(({ data }) => {
        dispatch({ type: SAVE_FILTER_FULFILLED, payload: data });
        setTimeout(() => {
          dispatch({ type: SET_DISPLAY_SAVE_FILTER_MODAL, payload: false });
        }, SUBMIT_MASK_SUCCESS_VISIBLE_TIME_MS);

        return dispatch(fetchSavedFilters());
      });
  };
}

function normalize(name) {
  return name.toLowerCase().replace(/\s/g, '');
}

export function cancelSaveFilter() {
  return (dispatch, getState) => {
    const { manageFilters } = getState(),
      { saveFilterWarning } = manageFilters;
    if (saveFilterWarning == null) {
      dispatch({ type: SET_DISPLAY_SAVE_FILTER_MODAL, payload: false });
    } else {
      dispatch({ type: SAVE_CONFIRM_CANCELLED });
    }
  };
}

export const selectFilterToDelete = payloadParamActionCreator(SELECT_FILTER_TO_DELETE);
export const hideDeleteFilterModal = noPayloadActionCreator(HIDE_DELETE_FILTER_MODAL);

export function deleteFilter(filterName) {
  return (dispatch) => {
    dispatch({ type: DELETE_FILTER_REQUESTED });
    return axios
      .post(getDashboardDeleteFilterUrl(filterName))
      .catch((error) => {
        dispatch({
          type: DELETE_FILTER_FAILED,
          payload: Messages.getHttpErrorMessage(error),
        });
        return Promise.reject(error);
      })
      .then(() => {
        dispatch({ type: DELETE_FILTER_FULFILLED, payload: filterName });
        setTimeout(() => {
          dispatch(hideDeleteFilterModal());
        }, SUBMIT_MASK_SUCCESS_VISIBLE_TIME_MS);

        return dispatch(fetchSavedFilters());
      });
  };
}
