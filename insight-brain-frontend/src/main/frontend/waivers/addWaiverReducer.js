/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { initialState, userInput } from '@sonatype/react-shared-components/components/NxTextInput/stateHelpers';
import { always, equals } from 'ramda';

import { UI_ROUTER_ON_FINISH } from '../reduxUiRouter/routerActions';

import { createReducerFromActionMap, propSetConst } from '../util/reduxUtil';
import {
  WAIVERS_LOAD_ADD_WAIVER_DATA_REQUESTED,
  WAIVERS_LOAD_ADD_WAIVER_DATA_FULFILLED,
  WAIVERS_LOAD_ADD_WAIVER_DATA_FAILED,
  WAIVERS_SAVE_WAIVER_REQUESTED,
  WAIVERS_SAVE_WAIVER_FULFILLED,
  WAIVERS_SAVE_WAIVER_FAILED,
  WAIVERS_ADD_WAIVER_SUBMIT_MASK_TIMER_DONE,
  WAIVERS_ADD_WAIVER_SET_WAIVER_COMMENT,
  WAIVERS_ADD_WAIVER_SET_WAIVER_SCOPE,
  WAIVERS_ADD_WAIVER_SET_APPLY_TO_ALL_COMPONENTS,
  WAIVERS_ADD_WAIVER_SET_EXPIRY_TIME,
} from './waiverActions';

const initState = Object.freeze({
  isDirty: false,
  loading: false,
  loadError: null,
  submitMaskState: null,
  submitError: null,
  // data
  waiverComments: Object.freeze(initialState('')),
  availableWaiverScopes: null,
  selectedWaiverScope: null,
  applyToAllComponents: false,
  expiryTime: null,
  fieldsPristineState: null,
});

/**
 * Checks if a form is dirty by comparing its current values with the pristine fields
 * @param {State} state the state to check if it's dirty
 */
const isFormDirty = (state) => {
  const { selectedWaiverScope, applyToAllComponents, expiryTime, waiverComments, fieldsPristineState } = state;

  const currentFields = {
    selectedWaiverScope,
    applyToAllComponents,
    expiryTime,
    waiverComments: waiverComments.value,
  };
  return !equals(fieldsPristineState, currentFields);
};

/**
 * Populates the `isDirty` property for a given newState
 * @param {State} partialNewState the state updated with new values
 */
const setIsDirtyFlag = (partialNewState) => ({
  ...partialNewState,
  isDirty: isFormDirty(partialNewState),
});

const loadDataFailed = (payload, state) => ({
  ...state,
  loading: false,
  loadError: payload,
});

const setLoadedData = (payload, state) => ({
  ...state,
  loading: false,
  loadError: null,
  submitError: null,
  availableWaiverScopes: payload,
  selectedWaiverScope: payload[0], // automatically set selectedWaiverScope with the owner
  fieldsPristineState: {
    // save a snapshot of what pristine fields are like
    selectedWaiverScope: payload[0],
    applyToAllComponents: false,
    expiryTime: null,
    waiverComments: '',
  },
});

const saveWaiverRequested = (payload, state) => ({
  ...state,
  submitMaskState: false,
  submitError: null,
});

const saveWaiverFailed = (payload, state) => ({
  ...state,
  submitMaskState: null,
  submitError: payload,
});

const setWaiverComment = (payload, state) =>
  setIsDirtyFlag({
    ...state,
    waiverComments: userInput(null, payload),
  });

const setSelectedWaiverScope = (payload, state) =>
  setIsDirtyFlag({
    ...state,
    selectedWaiverScope: payload,
  });

const setApplyToAllComponents = (payload, state) =>
  setIsDirtyFlag({
    ...state,
    applyToAllComponents: payload,
  });

const setExpiryTime = (payload, state) =>
  setIsDirtyFlag({
    ...state,
    expiryTime: payload,
  });

const saveWaiverFulfilled = (payload, state) => ({
  ...state,
  submitMaskState: true,
  isDirty: false,
});

const reducerActionMap = {
  [WAIVERS_LOAD_ADD_WAIVER_DATA_REQUESTED]: propSetConst('loading', true),
  [WAIVERS_LOAD_ADD_WAIVER_DATA_FULFILLED]: setLoadedData,
  [WAIVERS_LOAD_ADD_WAIVER_DATA_FAILED]: loadDataFailed,
  [WAIVERS_SAVE_WAIVER_REQUESTED]: saveWaiverRequested,
  [WAIVERS_SAVE_WAIVER_FULFILLED]: saveWaiverFulfilled,
  [WAIVERS_SAVE_WAIVER_FAILED]: saveWaiverFailed,
  [WAIVERS_ADD_WAIVER_SUBMIT_MASK_TIMER_DONE]: propSetConst('submitMaskState', null),
  [WAIVERS_ADD_WAIVER_SET_WAIVER_COMMENT]: setWaiverComment,
  [WAIVERS_ADD_WAIVER_SET_WAIVER_SCOPE]: setSelectedWaiverScope,
  [WAIVERS_ADD_WAIVER_SET_APPLY_TO_ALL_COMPONENTS]: setApplyToAllComponents,
  [WAIVERS_ADD_WAIVER_SET_EXPIRY_TIME]: setExpiryTime,
  [UI_ROUTER_ON_FINISH]: always(initState),
};

const addWaiverReducer = createReducerFromActionMap(reducerActionMap, initState);
export default addWaiverReducer;
