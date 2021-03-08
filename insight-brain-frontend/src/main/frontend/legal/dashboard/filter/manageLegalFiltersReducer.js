/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { append, equals, compose, curry, merge, pick, find, propEq } from 'ramda';
import { propSet } from '../../../util/jsUtil';
import { createReducerFromActionMap, propSetConst } from '../../../util/reduxUtil';
import {
  LEGAL_DASHBOARD_FETCH_SAVE_FILTERS_FULFILLED,
  LEGAL_DASHBOARD_FETCH_SAVED_FILTERS_FAILED,
  LEGAL_DASHBOARD_SAVE_FILTER_REQUESTED,
  LEGAL_DASHBOARD_SAVE_FILTER_FULFILLED,
  LEGAL_DASHBOARD_SAVE_FILTER_FAILED,
  LEGAL_DASHBOARD_DELETE_FILTER_REQUESTED,
  LEGAL_DASHBOARD_DELETE_FILTER_FULFILLED,
  LEGAL_DASHBOARD_DELETE_FILTER_FAILED,
  LEGAL_DASHBOARD_TOGGLE_FILTERS_DROPDOWN,
  LEGAL_DASHBOARD_SELECT_FILTER_TO_DELETE,
  LEGAL_DASHBOARD_HIDE_DELETE_FILTER_MODAL,
  LEGAL_DASHBOARD_DOCUMENT_CLICKED,
  LEGAL_DASHBOARD_SAVE_FILTER_OVERWRITE_REQUESTED,
  LEGAL_DASHBOARD_SAVE_DUPLICATE_FILTER_REQUESTED,
  LEGAL_DASHBOARD_SAVE_CONFIRM_CANCELLED
} from './manageLegalFiltersActions';

import {
  LEGAL_DASHBOARD_APPLY_FILTER_FULFILLED,
  LEGAL_DASHBOARD_APPLY_FILTER_REQUESTED,
  LEGAL_DASHBOARD_FETCH_CURRENT_FILTER_FULFILLED,
  LEGAL_DASHBOARD_SET_DISPLAY_SAVE_FILTER_MODAL
} from './legalDashboardFilterActions';
import { filterToJson } from './legalDashboardFilterService';
import defaultFilter from './defaultFilter';

const initState = {
  savedFilters: null,
  savedFilterListError: null,
  saveFilterError: null,
  saveFilterWarning: null,
  saveFilterSaving: false,
  saveFilterSuccess: false,
  appliedFilter: null,
  appliedFilterName: null,
  existingDuplicateFilterName: null,
  showDirtyAsterisk: false,
  filtersDropdownOpen: false,
  filterToDelete: null,
  deleteFilterError: null,
  deleteFilterSaving: false,
  deleteFilterSuccess: false
};

export const WARNING_NAME_IN_USE = 'nameInUseWarning';
export const WARNING_OVERWRITE = 'overwriteWarning';

/*
 * Create a function for reducerActionMap which resets the specified properties back to their values from initState.
 * the payload parameter is ignored
 */
const resetProps = curry((propNames, payload, state) => merge(state, pick(propNames, initState)));

/*
 * A map from action name to reducer function.  The reducer functions must all take two parameters: the payload and
 * the state
 */

const reducerActionMap = {
  [LEGAL_DASHBOARD_FETCH_CURRENT_FILTER_FULFILLED]: updateAppliedFilterName,
  [LEGAL_DASHBOARD_APPLY_FILTER_FULFILLED]: updateAppliedFilterName,
  [LEGAL_DASHBOARD_FETCH_SAVE_FILTERS_FULFILLED]: fetchSavedFiltersFulfilled,
  [LEGAL_DASHBOARD_FETCH_SAVED_FILTERS_FAILED]: propSet('savedFilterListError'),
  [LEGAL_DASHBOARD_SAVE_FILTER_REQUESTED]: propSetConst('saveFilterSaving', true),
  [LEGAL_DASHBOARD_SAVE_FILTER_FULFILLED]: saveFilterFulfilled,
  [LEGAL_DASHBOARD_SAVE_FILTER_FAILED]: saveFilterFailed,
  [LEGAL_DASHBOARD_DELETE_FILTER_REQUESTED]: propSetConst('deleteFilterSaving', true),
  [LEGAL_DASHBOARD_DELETE_FILTER_FULFILLED]: deleteFilterFulfilled,
  [LEGAL_DASHBOARD_DELETE_FILTER_FAILED]: deleteFilterFailed,
  [LEGAL_DASHBOARD_SET_DISPLAY_SAVE_FILTER_MODAL]:
      resetProps(['saveFilterSaving', 'saveFilterError', 'saveFilterSuccess']),
  [LEGAL_DASHBOARD_TOGGLE_FILTERS_DROPDOWN]: propSet('filtersDropdownOpen'),
  [LEGAL_DASHBOARD_SELECT_FILTER_TO_DELETE]: selectFilterToDelete,
  [LEGAL_DASHBOARD_HIDE_DELETE_FILTER_MODAL]: resetProps(['filterToDelete']),
  [LEGAL_DASHBOARD_DOCUMENT_CLICKED]: closeFiltersMenuIfNeeded,
  [LEGAL_DASHBOARD_APPLY_FILTER_REQUESTED]: closeFiltersMenuIfNeeded,
  [LEGAL_DASHBOARD_SAVE_FILTER_OVERWRITE_REQUESTED]: saveFilterOverwriteRequested,
  [LEGAL_DASHBOARD_SAVE_DUPLICATE_FILTER_REQUESTED]: saveDuplicateFilterRequested,
  [LEGAL_DASHBOARD_SAVE_CONFIRM_CANCELLED]: saveConfirmCancelled
};

function closeFiltersMenuIfNeeded(payload, state) {
  // don't close Filters Menu while Delete Filter modal is open
  if (state.filterToDelete) {
    return state;
  }

  return {...state, filtersDropdownOpen: false};
}

function fetchSavedFiltersFulfilled(payload, state) {
  return compose(propSet('savedFilters', payload), resetProps(['savedFilterListError'], payload))(state);
}

function updateAppliedFilterName(payload, state) {
  return compose(
      setShowDirtyAsterisk(),
      propSetConst('appliedFilter', payload.filter, null),
      propSetConst('appliedFilterName', payload.basedOnFilterName, null)
  )(state);
}

function saveFilterFulfilled(payload, state) {
  return {
    ...state,
    savedFilters: append(payload, state.savedFilters),
    appliedFilterName: payload.name,
    existingDuplicateFilterName: null,
    saveFilterSuccess: true,
    showDirtyAsterisk: false,
    saveFilterWarning: null
  };
}

function saveFilterFailed(payload, state) {
  return {
    ...state,
    saveFilterError: payload,
    saveFilterSaving: false,
    saveFilterSuccess: false
  };
}

function saveFilterOverwriteRequested(payload, state) {
  return {
    ...state,
    existingDuplicateFilterName: null,
    saveFilterError: null,
    saveFilterWarning: WARNING_OVERWRITE
  };
}

function saveDuplicateFilterRequested(payload, state) {
  return {
    ...state,
    existingDuplicateFilterName: payload,
    saveFilterError: null,
    saveFilterWarning: WARNING_NAME_IN_USE
  };
}

function saveConfirmCancelled(payload, state) {
  return {
    ...state,
    existingDuplicateFilterName: null,
    saveFilterError: null,
    saveFilterWarning: null
  };
}

function selectFilterToDelete(payload, state) {
  return compose(
      resetProps(['deleteFilterSaving', 'deleteFilterError', 'deleteFilterSuccess'], null),
      propSet('filterToDelete', payload)
  )(state);
}

/**
 * @param payload deleted filter name
 */
function deleteFilterFulfilled(payload, state) {
  const activeFilterWasDeleted = state.appliedFilterName === payload,
      stateWithDeleteFilterSuccess = { ...state, deleteFilterSuccess: true };

  if (activeFilterWasDeleted) {
    return compose(
        setShowDirtyAsterisk(),
        resetProps(['appliedFilterName'], null)
    )(stateWithDeleteFilterSuccess);
  }

  return stateWithDeleteFilterSuccess;
}

function deleteFilterFailed(payload, state) {
  return resetProps(['deleteFilterSaving', 'deleteFilterSuccess'], payload,
      { ...state, deleteFilterError: payload });
}

const setShowDirtyAsterisk = () => state => {
  const {appliedFilter, appliedFilterName, savedFilters} = state,
      cleanFilter = appliedFilterName
        ? find(propEq('name', appliedFilterName), savedFilters).filter
        : filterToJson(defaultFilter),
      showDirtyAsterisk = !equals(appliedFilter, cleanFilter);

  return {...state, showDirtyAsterisk};
};

/**
 * The main reducer function for this file.  Works by looking up the action type in the reducerAction map
 * and then executing the found function
 */
const manageLegalFiltersReducer = createReducerFromActionMap(reducerActionMap, initState);
export default manageLegalFiltersReducer;
