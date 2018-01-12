/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { set, lensProp, compose, append, contains, curry, merge, pick } from 'ramda';

import {
  FETCH_SAVED_FILTERS_FULFILLED,
  FETCH_SAVED_FILTERS_FAILED,
  APPLY_SAVED_FILTER,
  SAVE_FILTER_REQUESTED,
  SAVE_FILTER_FULFILLED,
  SAVE_FILTER_FAILED,
  DELETE_SPECIFIED_FILTERS_REQUESTED,
  DELETE_SPECIFIED_FILTERS_FULFILLED,
  DELETE_SPECIFIED_FILTERS_FAILED,
  RESET_SAVE_FILTER_STATUS,
  RESET_DELETE_FILTERS_STATUS
} from './manageFiltersActions';

import { UPDATE_FILTERS_FULFILLED } from './dashboardFilterActions';

const initState = {
  savedFilters: null,
  savedFilterListError: null,
  saveFilterError: null,
  saveFilterSaving: false,
  saveFilterSuccess: false,
  appliedFilterName: null,
  deleteFiltersError: null,
  deleteFiltersSaving: false,
  deleteFiltersSuccess: false
};

/**
 * The main reducer function for this file.  Works by looking up the action type in the reducerAction map
 * and then executing the found function
 */
export default function manageFiltersReducer(state = initState, action) {
  const type = action && action.type,
      payload = action && action.payload,
      reducer = type && reducerActionMap[type];

  return reducer ? reducer(payload, state) : state;
}

/*
 * set the specified property.  When partially applied in the first arg, results in a function fit for
 * use in the reducerActionMap below
 */
const propSet = curry((propName, payload, state) => set(lensProp(propName), payload, state));

/*
 * like propSet but is meant to be partially applied in 2 args.  The payload is ignored and is only an argument
 * to conform to the interface needed by reducerActionMap
 */
const propSetConst = curry((propName, constValue, payload, state) => set(lensProp(propName), constValue, state));

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
  [UPDATE_FILTERS_FULFILLED]: updateFiltersFulfilled,
  [FETCH_SAVED_FILTERS_FULFILLED]: fetchSavedFiltersFulfilled,
  [FETCH_SAVED_FILTERS_FAILED]: propSet('savedFilterListError'),
  [APPLY_SAVED_FILTER]: applySavedFilter,
  [SAVE_FILTER_REQUESTED]: propSetConst('saveFilterSaving', true),
  [SAVE_FILTER_FULFILLED]: saveFilterFulfilled,
  [SAVE_FILTER_FAILED]: saveFilterFailed,
  [DELETE_SPECIFIED_FILTERS_REQUESTED]: propSetConst('deleteFiltersSaving', true),
  [DELETE_SPECIFIED_FILTERS_FULFILLED]: deleteSpecifiedFiltersFulfilled,
  [DELETE_SPECIFIED_FILTERS_FAILED]: deleteFiltersFailed,
  [RESET_SAVE_FILTER_STATUS]: resetProps(['saveFilterSaving', 'saveFilterError', 'saveFilterSuccess']),
  [RESET_DELETE_FILTERS_STATUS]: resetProps(['deleteFiltersSaving', 'deleteFiltersError', 'deleteFiltersSuccess'])
};

function fetchSavedFiltersFulfilled(payload, state) {
  return compose(propSet('savedFilters', payload), resetProps(['savedFilterListError'], payload))(state);
}

function updateFiltersFulfilled(payload, state) {
  return { ...state, appliedFilterName: payload.appliedFilterName };
}

function applySavedFilter(payload, state) {
  return { ...state, appliedFilterName: payload.name };
}

function saveFilterFulfilled(payload, state) {
  return {
    ...state,
    savedFilters: append(payload, state.savedFilters),
    appliedFilterName: payload.name,
    saveFilterSuccess: true
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

/**
 * @param payload the filters that were deleted
 */
function deleteSpecifiedFiltersFulfilled(payload, state) {
  const activeFilterWasDeleted = contains(state.appliedFilterName, payload),
      stateWithDeleteFiltersSuccess = { ...state, deleteFiltersSuccess: true };

  return activeFilterWasDeleted ? resetProps(['appliedFilterName'], payload, stateWithDeleteFiltersSuccess) :
    stateWithDeleteFiltersSuccess;
}

function deleteFiltersFailed(payload, state) {
  return resetProps(['deleteFiltersSaving', 'deleteFiltersSuccess'], payload,
      { ...state, deleteFiltersError: payload });
}
