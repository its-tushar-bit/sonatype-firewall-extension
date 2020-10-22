/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { initialState, userInput } from '@sonatype/react-shared-components/components/NxTextInput/stateHelpers';
import { always } from 'ramda';

import { UI_ROUTER_ON_FINISH } from '../reduxUiRouter/routerActions';

import { createReducerFromActionMap, propSetConst } from '../util/reduxUtil';
import {
  WAIVERS_LOAD_SCOPE_DATA_REQUESTED,
  WAIVERS_LOAD_SCOPE_DATA_FULFILLED,
  WAIVERS_LOAD_SCOPE_DATA_FAILED,
  WAIVERS_SAVE_WAIVER_REQUESTED,
  WAIVERS_SAVE_WAIVER_FULFILLED,
  WAIVERS_SAVE_WAIVER_FAILED,
  WAIVERS_ADD_WAIVER_SUBMIT_MASK_TIMER_DONE,
  WAIVERS_ADD_WAIVER_SET_WAIVER_COMMENT,
  WAIVERS_ADD_WAIVER_SET_WAIVER_SCOPE,
  WAIVERS_ADD_WAIVER_SET_APPLY_TO_ALL_COMPONENTS,
  WAIVERS_ADD_WAIVER_SET_EXPIRY_TIME
} from './waiverActions';
import { propSet } from '../util/jsUtil';

const initState = Object.freeze({
  loading: false,
  loadError: null,
  submitMaskState: null,
  submitError: null,
  // data
  waiverComments: Object.freeze(initialState('')),
  availableWaiverScopes: null,
  selectedWaiverScope: null,
  applyToAllComponents: false,
  expiryTime: null
});

const loadDataFailed = (payload, state) => ({
  ...state,
  loading: false,
  loadError: payload
});

const setWaiverData = (payload, state) => ({
  ...state,
  loading: false,
  loadError: null,
  submitError: null,
  availableWaiverScopes: payload,
  selectedWaiverScope: payload[0] // automatically set selectedWaiverScope with the owner
});

const saveWaiverRequested = (payload, state) => ({
  ...state,
  submitMaskState: false,
  submitError: null
});

const saveWaiverFailed = (payload, state) => ({
  ...state,
  submitMaskState: null,
  submitError: payload
});

const setWaiverComment = (payload, state) => ({
  ...state,
  waiverComments: userInput(null, payload)
});

const reducerActionMap = {
  [WAIVERS_LOAD_SCOPE_DATA_REQUESTED]: propSetConst('loading', true),
  [WAIVERS_LOAD_SCOPE_DATA_FULFILLED]: setWaiverData,
  [WAIVERS_LOAD_SCOPE_DATA_FAILED]: loadDataFailed,
  [WAIVERS_SAVE_WAIVER_REQUESTED]: saveWaiverRequested,
  [WAIVERS_SAVE_WAIVER_FULFILLED]: propSetConst('submitMaskState', true),
  [WAIVERS_SAVE_WAIVER_FAILED]: saveWaiverFailed,
  [WAIVERS_ADD_WAIVER_SUBMIT_MASK_TIMER_DONE]: propSetConst('submitMaskState', null),
  [WAIVERS_ADD_WAIVER_SET_WAIVER_COMMENT]: setWaiverComment,
  [WAIVERS_ADD_WAIVER_SET_WAIVER_SCOPE]: propSet('selectedWaiverScope'),
  [WAIVERS_ADD_WAIVER_SET_APPLY_TO_ALL_COMPONENTS]: propSet('applyToAllComponents'),
  [WAIVERS_ADD_WAIVER_SET_EXPIRY_TIME]: propSet('expiryTime'),
  [UI_ROUTER_ON_FINISH]: always(initState)
};

const addWaiverReducer = createReducerFromActionMap(reducerActionMap, initState);
export default addWaiverReducer;
