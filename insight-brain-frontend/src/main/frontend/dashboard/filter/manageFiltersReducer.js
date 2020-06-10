/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { append, equals, compose, contains, curry, merge, pick, find, propEq } from 'ramda';
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
  DELETE_SPECIFIED_FILTERS_REQUESTED,
  DELETE_SPECIFIED_FILTERS_FULFILLED,
  DELETE_SPECIFIED_FILTERS_FAILED,
  RESET_DELETE_FILTERS_STATUS,
  TOGGLE_FILTERS_DROPDOWN
} from './manageFiltersActions';

import {
  APPLY_FILTER_FULFILLED,
  FETCH_CURRENT_FILTER_FULFILLED,
  SET_DISPLAY_SAVE_FILTER_MODAL
} from './dashboardFilterActions';

const initState = {
  savedFilters: null,
  savedFilterListError: null,
  saveFilterError: null,
  saveFilterSaving: false,
  saveFilterSuccess: false,
  appliedFilterName: null,
  showDirtyAsterisk: false,
  filtersDropdownOpen: false,
  deleteFiltersError: null,
  deleteFiltersSaving: false,
  deleteFiltersSuccess: false
};

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
  [SAVE_FILTER_REQUESTED]: propSetConst('saveFilterSaving', true),
  [SAVE_FILTER_FULFILLED]: saveFilterFulfilled,
  [SAVE_FILTER_FAILED]: saveFilterFailed,
  [DELETE_SPECIFIED_FILTERS_REQUESTED]: propSetConst('deleteFiltersSaving', true),
  [DELETE_SPECIFIED_FILTERS_FULFILLED]: deleteSpecifiedFiltersFulfilled,
  [DELETE_SPECIFIED_FILTERS_FAILED]: deleteFiltersFailed,
  [SET_DISPLAY_SAVE_FILTER_MODAL]: resetProps(['saveFilterSaving', 'saveFilterError', 'saveFilterSuccess']),
  [RESET_DELETE_FILTERS_STATUS]: resetProps(['deleteFiltersSaving', 'deleteFiltersError', 'deleteFiltersSuccess']),
  [TOGGLE_FILTERS_DROPDOWN]: propSet('filtersDropdownOpen')
};

function fetchSavedFiltersFulfilled(payload, state) {
  return compose(propSet('savedFilters', payload), resetProps(['savedFilterListError'], payload))(state);
}

function updateAppliedFilterName(payload, state) {
  return compose(
      setShowDirtyAsterisk(payload),
      propSetConst('appliedFilterName', payload.basedOnFilterName, payload)
  )(state);
}

function saveFilterFulfilled(payload, state) {
  return {
    ...state,
    savedFilters: append(payload, state.savedFilters),
    appliedFilterName: payload.name,
    saveFilterSuccess: true,
    showDirtyAsterisk: false
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

const setShowDirtyAsterisk = payload => state => {
  const {basedOnFilterName, filter} = payload;
  const cleanFilter = basedOnFilterName
    ? find(propEq('name', basedOnFilterName), state.savedFilters).filter
    : filterToJson(defaultFilter);

  const showDirtyAsterisk = !equals(filter, cleanFilter);
  return {...state, showDirtyAsterisk};
};

/**
 * The main reducer function for this file.  Works by looking up the action type in the reducerAction map
 * and then executing the found function
 */
const manageFiltersReducer = createReducerFromActionMap(reducerActionMap, initState);
export default manageFiltersReducer;
