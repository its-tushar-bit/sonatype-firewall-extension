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
  TOGGLE_FILTERS_DROPDOWN,
  SELECT_FILTER_TO_DELETE,
  HIDE_DELETE_FILTER_MODAL,
  DOCUMENT_CLICKED
} from './manageFiltersActions';

import {
  APPLY_FILTER_FULFILLED,
  APPLY_FILTER_REQUESTED,
  FETCH_CURRENT_FILTER_FULFILLED,
  SET_DISPLAY_SAVE_FILTER_MODAL
} from './dashboardFilterActions';

const initState = {
  savedFilters: null,
  savedFilterListError: null,
  saveFilterError: null,
  saveFilterSaving: false,
  saveFilterSuccess: false,
  appliedFilter: null,
  appliedFilterName: null,
  showDirtyAsterisk: false,
  filtersDropdownOpen: false,
  filterToDelete: null,
  deleteFilterError: null,
  deleteFilterSaving: false,
  deleteFilterSuccess: false
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
  [DELETE_FILTER_REQUESTED]: propSetConst('deleteFilterSaving', true),
  [DELETE_FILTER_FULFILLED]: deleteFilterFulfilled,
  [DELETE_FILTER_FAILED]: deleteFilterFailed,
  [SET_DISPLAY_SAVE_FILTER_MODAL]: resetProps(['saveFilterSaving', 'saveFilterError', 'saveFilterSuccess']),
  [TOGGLE_FILTERS_DROPDOWN]: propSet('filtersDropdownOpen'),
  [SELECT_FILTER_TO_DELETE]: selectFilterToDelete,
  [HIDE_DELETE_FILTER_MODAL]: resetProps(['filterToDelete']),
  [DOCUMENT_CLICKED]: closeFiltersMenuIfNeeded,
  [APPLY_FILTER_REQUESTED]: closeFiltersMenuIfNeeded
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
const manageFiltersReducer = createReducerFromActionMap(reducerActionMap, initState);
export default manageFiltersReducer;
