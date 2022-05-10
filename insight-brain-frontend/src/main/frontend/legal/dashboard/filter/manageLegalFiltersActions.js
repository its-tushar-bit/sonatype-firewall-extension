/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import axios from 'axios';
import { SUBMIT_MASK_SUCCESS_VISIBLE_TIME_MS } from '@sonatype/react-shared-components';

import { noPayloadActionCreator, payloadParamActionCreator } from '../../../util/reduxUtil';
import { getLegalDashboardDeleteFilterUrl, getLegalDashboardSavedFilters } from '../../../util/CLMLocation';
import { Messages } from '../../../utilAngular/CommonServices';
import { LEGAL_DASHBOARD_SET_DISPLAY_SAVE_FILTER_MODAL } from './legalDashboardFilterActions';
import { filterToJson } from './legalDashboardFilterService';

export const LEGAL_DASHBOARD_FETCH_SAVE_FILTERS_FULFILLED = 'LEGAL_DASHBOARD_FETCH_SAVE_FILTERS_FULFILLED';
export const LEGAL_DASHBOARD_FETCH_SAVED_FILTERS_FAILED = 'LEGAL_DASHBOARD_FETCH_SAVED_FILTERS_FAILED';
export const LEGAL_DASHBOARD_SAVE_FILTER_REQUESTED = 'LEGAL_DASHBOARD_SAVE_FILTER_REQUESTED';
export const LEGAL_DASHBOARD_SAVE_FILTER_FULFILLED = 'LEGAL_DASHBOARD_SAVE_FILTER_FULFILLED';
export const LEGAL_DASHBOARD_SAVE_FILTER_FAILED = 'LEGAL_DASHBOARD_SAVE_FILTER_FAILED';
export const LEGAL_DASHBOARD_SELECT_FILTER_TO_DELETE = 'LEGAL_DASHBOARD_SELECT_FILTER_TO_DELETE';
export const LEGAL_DASHBOARD_HIDE_DELETE_FILTER_MODAL = 'LEGAL_DASHBOARD_HIDE_DELETE_FILTER_MODAL';
export const LEGAL_DASHBOARD_DELETE_FILTER_REQUESTED = 'LEGAL_DASHBOARD_DELETE_FILTER_REQUESTED';
export const LEGAL_DASHBOARD_DELETE_FILTER_FULFILLED = 'LEGAL_DASHBOARD_DELETE_FILTER_FULFILLED';
export const LEGAL_DASHBOARD_DELETE_FILTER_FAILED = 'LEGAL_DASHBOARD_DELETE_FILTER_FAILED';
export const LEGAL_DASHBOARD_SAVE_FILTER_OVERWRITE_REQUESTED = 'LEGAL_DASHBOARD_SAVE_FILTER_OVERWRITE_REQUESTED';
export const LEGAL_DASHBOARD_SAVE_DUPLICATE_FILTER_REQUESTED = 'LEGAL_DASHBOARD_SAVE_DUPLICATE_FILTER_REQUESTED';
export const LEGAL_DASHBOARD_SAVE_CONFIRM_CANCELLED = 'LEGAL_DASHBOARD_SAVE_CONFIRM_CANCELLED';

export function fetchSavedFilters() {
  return (dispatch) => {
    return axios
      .get(getLegalDashboardSavedFilters())
      .then(({ data }) => dispatch(fetchSavedFiltersFulfilled(data)))
      .catch((error) => {
        dispatch(fetchSavedFiltersFailed(error));
        return Promise.reject(error);
      });
  };
}

const fetchSavedFiltersFulfilled = payloadParamActionCreator(LEGAL_DASHBOARD_FETCH_SAVE_FILTERS_FULFILLED);

const fetchSavedFiltersFailed = payloadParamActionCreator(LEGAL_DASHBOARD_FETCH_SAVED_FILTERS_FAILED);

export function saveFilter({ name, isOverwriting }) {
  return (dispatch, getState) => {
    const { legalDashboardFilter, manageLegalFilters } = getState(),
      { appliedFilter } = legalDashboardFilter,
      filter = filterToJson(appliedFilter),
      namedFilter = { name, filter, type: 'ADVANCED_LEGAL_PACK_DASHBOARD' },
      { saveFilterWarning, savedFilters } = manageLegalFilters;

    if (!saveFilterWarning) {
      if (isOverwriting) {
        return dispatch({
          type: LEGAL_DASHBOARD_SAVE_FILTER_OVERWRITE_REQUESTED,
        });
      }

      const normalizedName = normalize(name);
      const existingDuplicateFilter = savedFilters.find((filterName) => normalizedName === normalize(filterName.name));

      if (existingDuplicateFilter) {
        return dispatch({
          type: LEGAL_DASHBOARD_SAVE_DUPLICATE_FILTER_REQUESTED,
          payload: existingDuplicateFilter.name,
        });
      }
    }

    dispatch({ type: LEGAL_DASHBOARD_SAVE_FILTER_REQUESTED });

    return axios
      .put(getLegalDashboardSavedFilters(), namedFilter)
      .catch((error) => {
        dispatch({ type: LEGAL_DASHBOARD_SAVE_FILTER_FAILED, payload: error });
        return Promise.reject(error);
      })
      .then(({ data }) => {
        dispatch({
          type: LEGAL_DASHBOARD_SAVE_FILTER_FULFILLED,
          payload: data,
        });
        setTimeout(() => {
          dispatch({
            type: LEGAL_DASHBOARD_SET_DISPLAY_SAVE_FILTER_MODAL,
            payload: false,
          });
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
    const { manageLegalFilters } = getState(),
      { saveFilterWarning } = manageLegalFilters;
    if (saveFilterWarning == null) {
      dispatch({
        type: LEGAL_DASHBOARD_SET_DISPLAY_SAVE_FILTER_MODAL,
        payload: false,
      });
    } else {
      dispatch({ type: LEGAL_DASHBOARD_SAVE_CONFIRM_CANCELLED });
    }
  };
}

export const selectFilterToDelete = payloadParamActionCreator(LEGAL_DASHBOARD_SELECT_FILTER_TO_DELETE);
export const hideDeleteFilterModal = noPayloadActionCreator(LEGAL_DASHBOARD_HIDE_DELETE_FILTER_MODAL);

export function deleteFilter(filterName) {
  return (dispatch) => {
    dispatch({ type: LEGAL_DASHBOARD_DELETE_FILTER_REQUESTED });
    return axios
      .delete(getLegalDashboardDeleteFilterUrl(filterName))
      .catch((error) => {
        dispatch({
          type: LEGAL_DASHBOARD_DELETE_FILTER_FAILED,
          payload: Messages.getHttpErrorMessage(error),
        });
        return Promise.reject(error);
      })
      .then(() => {
        dispatch({
          type: LEGAL_DASHBOARD_DELETE_FILTER_FULFILLED,
          payload: filterName,
        });
        setTimeout(() => {
          dispatch(hideDeleteFilterModal());
        }, SUBMIT_MASK_SUCCESS_VISIBLE_TIME_MS);

        return dispatch(fetchSavedFilters());
      });
  };
}
