/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { nxDateInputStateHelpers } from '@sonatype/react-shared-components';
import { initialState, userInput } from '@sonatype/react-shared-components/components/NxTextInput/stateHelpers';
import { always, equals } from 'ramda';

import { UI_ROUTER_ON_FINISH } from '../reduxUiRouter/routerActions';

import { createReducerFromActionMap, propSetConst } from '../util/reduxUtil';
import { waiverMatcherStrategy } from '../util/waiverUtils';
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
  WAIVERS_ADD_WAIVER_SET_COMPONENT_MATCHER_STRATEGY,
  WAIVERS_ADD_WAIVER_SET_EXPIRY_TIME,
  WAIVERS_ADD_WAIVER_SET_CUSTOM_EXPIRY_TIME,
  WAIVERS_ADD_WAIVER_SET_SHOW_UNSAVED_CHANGES_MODAL,
  WAIVERS_RESET_ADD_WAIVER_DATA,
  WAIVERS_ADD_WAIVER_SET_REASON,
} from './waiverActions';

import { isCustomExpiryTimeValid } from './AddWaiverForm';

const initState = Object.freeze({
  isDirty: false,
  loading: false,
  loadError: null,
  submitMaskState: null,
  submitError: null,
  showUnsavedChangesModal: false,
  // data
  waiverComments: Object.freeze(initialState('')),
  availableWaiverScopes: null,
  selectedWaiverScope: null,
  componentMatcherStrategy: waiverMatcherStrategy.EXACT_COMPONENT,
  expiryTime: null,
  waiverReasonId: null,
  customExpiryTime: nxDateInputStateHelpers.initialState(''),
  fieldsPristineState: null,
});

/**
 * Checks if a form is dirty by comparing its current values with the pristine fields
 * @param {State} state the state to check if it's dirty
 */
const isFormDirty = (state) => {
  const {
    selectedWaiverScope,
    componentMatcherStrategy,
    expiryTime,
    waiverReasonId,
    waiverComments,
    fieldsPristineState,
  } = state;

  const currentFields = {
    selectedWaiverScope,
    componentMatcherStrategy,
    expiryTime,
    waiverReasonId,
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

const setLoadedData = ({ waiverTargets, comments, reasonId }, state) => ({
  ...state,
  loading: false,
  loadError: null,
  submitError: null,
  availableWaiverScopes: waiverTargets,
  selectedWaiverScope: waiverTargets[0], // automatically set selectedWaiverScope with the owner
  waiverComments: initialState(comments ? decodeURIComponent(comments) : ''),
  waiverReasonId: reasonId ? reasonId : null,
  fieldsPristineState: {
    // save a snapshot of what pristine fields are like
    selectedWaiverScope: waiverTargets[0],
    componentMatcherStrategy: waiverMatcherStrategy.EXACT_COMPONENT,
    expiryTime: null,
    waiverReasonId: reasonId ? reasonId : null,
    waiverComments: comments ? decodeURIComponent(comments) : '',
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

const setComponentMatcherStrategy = (payload, state) =>
  setIsDirtyFlag({
    ...state,
    componentMatcherStrategy: payload,
  });

const setExpiryTime = (payload, state) =>
  setIsDirtyFlag({
    ...state,
    expiryTime: payload,
    customExpiryTime: nxDateInputStateHelpers.initialState(''),
  });

const setWaiverReason = (payload, state) =>
  setIsDirtyFlag({
    ...state,
    waiverReasonId: payload || null,
  });

const customDateValidator = (value) => (isCustomExpiryTimeValid(value) ? null : 'Date must be in the future');

const setCustomExpiryTime = (payload, state) =>
  setIsDirtyFlag({
    ...state,
    customExpiryTime: nxDateInputStateHelpers.userInput(customDateValidator, payload),
  });

const saveWaiverFulfilled = (payload, state) => ({
  ...state,
  submitMaskState: true,
  isDirty: false,
});

const setShowUnsavedChangesModal = (payload, state) => ({
  ...state,
  showUnsavedChangesModal: payload,
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
  [WAIVERS_ADD_WAIVER_SET_COMPONENT_MATCHER_STRATEGY]: setComponentMatcherStrategy,
  [WAIVERS_ADD_WAIVER_SET_EXPIRY_TIME]: setExpiryTime,
  [WAIVERS_ADD_WAIVER_SET_REASON]: setWaiverReason,
  [WAIVERS_ADD_WAIVER_SET_CUSTOM_EXPIRY_TIME]: setCustomExpiryTime,
  [WAIVERS_ADD_WAIVER_SET_SHOW_UNSAVED_CHANGES_MODAL]: setShowUnsavedChangesModal,
  [WAIVERS_RESET_ADD_WAIVER_DATA]: always(initState),
  [UI_ROUTER_ON_FINISH]: always(initState),
};

const addWaiverReducer = createReducerFromActionMap(reducerActionMap, initState);
export default addWaiverReducer;
