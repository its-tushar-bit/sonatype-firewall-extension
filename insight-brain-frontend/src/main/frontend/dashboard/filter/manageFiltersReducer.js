/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { append, equals, compose, curry, merge, pick, find, propEq } from 'ramda';
import { propSet } from '../../util/jsUtil';
import { createReducerFromActionMap, propSetConst } from '../../util/reduxUtil';
import defaultFilter from './defaultFilter';
import { filterToJson } from './dashboardFilterService';
import {
  FETCH_SAVED_FILTERS_FULFILLED,
  FETCH_SAVED_FILTERS_FAILED,
  SAVE_FILTER_REQUESTED,
  SAVE_FILTER_FULFILLED,
  SAVE_FILTER_FAILED,
  DELETE_FILTER_REQUESTED,
  DELETE_FILTER_FULFILLED,
  DELETE_FILTER_FAILED,
  SELECT_FILTER_TO_DELETE,
  HIDE_DELETE_FILTER_MODAL,
  SAVE_FILTER_OVERWRITE_REQUESTED,
  SAVE_DUPLICATE_FILTER_REQUESTED,
  SAVE_CONFIRM_CANCELLED,
} from './manageFiltersActions';

import {
  APPLY_FILTER_FULFILLED,
  FETCH_CURRENT_FILTER_FULFILLED,
  SET_DISPLAY_SAVE_FILTER_MODAL,
} from './dashboardFilterActions';

const initState = {
  savedFilters: null,
  savedFilterListError: null,
  saveFilterError: null,
  saveFilterWarning: null,
  saveFilterMaskState: null,
  appliedFilter: null,
  appliedFilterName: null,
  existingDuplicateFilterName: null,
  showDirtyAsterisk: false,
  filterToDelete: null,
  deleteFilterError: null,
  deleteFilterMaskState: null,
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
  [FETCH_CURRENT_FILTER_FULFILLED]: updateAppliedFilterName,
  [APPLY_FILTER_FULFILLED]: updateAppliedFilterName,
  [FETCH_SAVED_FILTERS_FULFILLED]: fetchSavedFiltersFulfilled,
  [FETCH_SAVED_FILTERS_FAILED]: propSet('savedFilterListError'),
  [SAVE_FILTER_REQUESTED]: propSetConst('saveFilterMaskState', false),
  [SAVE_FILTER_FULFILLED]: saveFilterFulfilled,
  [SAVE_FILTER_FAILED]: saveFilterFailed,
  [DELETE_FILTER_REQUESTED]: propSetConst('deleteFilterMaskState', false),
  [DELETE_FILTER_FULFILLED]: deleteFilterFulfilled,
  [DELETE_FILTER_FAILED]: deleteFilterFailed,
  [SET_DISPLAY_SAVE_FILTER_MODAL]: resetProps(['saveFilterMaskState', 'saveFilterError']),
  [SELECT_FILTER_TO_DELETE]: selectFilterToDelete,
  [HIDE_DELETE_FILTER_MODAL]: resetProps(['filterToDelete']),
  [SAVE_FILTER_OVERWRITE_REQUESTED]: saveFilterOverwriteRequested,
  [SAVE_DUPLICATE_FILTER_REQUESTED]: saveDuplicateFilterRequested,
  [SAVE_CONFIRM_CANCELLED]: saveConfirmCancelled,
};

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
    saveFilterMaskState: true,
    showDirtyAsterisk: false,
    saveFilterWarning: null,
  };
}

function saveFilterFailed(payload, state) {
  return {
    ...state,
    saveFilterError: payload,
    saveFilterMaskState: null,
  };
}

function saveFilterOverwriteRequested(payload, state) {
  return {
    ...state,
    existingDuplicateFilterName: null,
    saveFilterError: null,
    saveFilterWarning: WARNING_OVERWRITE,
  };
}

function saveDuplicateFilterRequested(payload, state) {
  return {
    ...state,
    existingDuplicateFilterName: payload,
    saveFilterError: null,
    saveFilterWarning: WARNING_NAME_IN_USE,
  };
}

function saveConfirmCancelled(payload, state) {
  return {
    ...state,
    existingDuplicateFilterName: null,
    saveFilterError: null,
    saveFilterWarning: null,
  };
}

function selectFilterToDelete(payload, state) {
  return compose(
    resetProps(['deleteFilterError', 'deleteFilterMaskState'], null),
    propSet('filterToDelete', payload)
  )(state);
}

/**
 * @param payload deleted filter name
 */
function deleteFilterFulfilled(payload, state) {
  const activeFilterWasDeleted = state.appliedFilterName === payload,
    stateWithDeleteFilterMaskState = { ...state, deleteFilterMaskState: true };

  if (activeFilterWasDeleted) {
    return compose(setShowDirtyAsterisk(), resetProps(['appliedFilterName'], null))(stateWithDeleteFilterMaskState);
  }

  return stateWithDeleteFilterMaskState;
}

function deleteFilterFailed(payload, state) {
  return resetProps(['deleteFilterMaskState'], payload, {
    ...state,
    deleteFilterError: payload,
  });
}

const setShowDirtyAsterisk = () => (state) => {
  const { appliedFilter, appliedFilterName, savedFilters } = state,
    cleanFilter = appliedFilterName
      ? find(propEq('name', appliedFilterName), savedFilters).filter
      : filterToJson(defaultFilter),
    showDirtyAsterisk = !equals(appliedFilter, cleanFilter);

  return { ...state, showDirtyAsterisk };
};

/**
 * The main reducer function for this file.  Works by looking up the action type in the reducerAction map
 * and then executing the found function
 */
const manageFiltersReducer = createReducerFromActionMap(reducerActionMap, initState);
export default manageFiltersReducer;
