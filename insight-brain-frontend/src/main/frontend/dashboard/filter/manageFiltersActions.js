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
import { Messages } from '../../util/CommonServices';

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
export const TOGGLE_FILTERS_DROPDOWN = 'TOGGLE_FILTERS_DROPDOWN';
export const DOCUMENT_CLICKED = 'DOCUMENT_CLICKED';

export function fetchSavedFilters() {
  return dispatch => {
    return axios.get(getDashboardSavedFilters())
        .then(({ data }) => dispatch(fetchSavedFiltersFulfilled(data)))
        .catch(error => {
          dispatch(fetchSavedFiltersFailed(error));
          return Promise.reject(error);
        });
  };
}

const fetchSavedFiltersFulfilled = payloadParamActionCreator(FETCH_SAVED_FILTERS_FULFILLED);

const fetchSavedFiltersFailed = payloadParamActionCreator(FETCH_SAVED_FILTERS_FAILED);

export function saveFilter(name) {
  return (dispatch, getState) => {
    const { dashboardFilter } = getState(),
        { appliedFilter } = dashboardFilter,
        filter = filterToJson(appliedFilter),
        namedFilter = { name, filter };

    dispatch({ type: SAVE_FILTER_REQUESTED });

    return axios.put(getDashboardSavedFilters(), namedFilter)
        .catch(error => {
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

export const selectFilterToDelete = payloadParamActionCreator(SELECT_FILTER_TO_DELETE);
export const hideDeleteFilterModal = noPayloadActionCreator(HIDE_DELETE_FILTER_MODAL);
export const toggleFiltersDropdown = payloadParamActionCreator(TOGGLE_FILTERS_DROPDOWN);
export const handleDocumentClick = noPayloadActionCreator(DOCUMENT_CLICKED);

export function deleteFilter(filterName) {
  return dispatch => {
    dispatch({ type: DELETE_FILTER_REQUESTED });
    return axios.post(getDashboardDeleteFilterUrl(filterName))
        .catch(error => {
          dispatch({ type: DELETE_FILTER_FAILED, payload: Messages.getHttpErrorMessage(error) });
          return Promise.reject(error);
        })
        .then(() => {
          dispatch({ type: DELETE_FILTER_FULFILLED, payload: filterName });
          setTimeout(() => {
            dispatch(toggleFiltersDropdown(false));
            dispatch(hideDeleteFilterModal());
          }, SUBMIT_MASK_SUCCESS_VISIBLE_TIME_MS);

          return dispatch(fetchSavedFilters());
        });
  };
}
