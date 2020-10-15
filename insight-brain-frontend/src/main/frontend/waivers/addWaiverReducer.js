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
  ADD_WAIVER_LOAD_DATA_REQUESTED,
  ADD_WAIVER_LOAD_DATA_FULFILLED,
  ADD_WAIVER_LOAD_DATA_FAILED,
  ADD_WAIVER_SAVE_REQUESTED,
  ADD_WAIVER_SAVE_FULFILLED,
  ADD_WAIVER_SAVE_FAILED,
  ADD_WAIVER_SUBMIT_MASK_TIMER_DONE,
  ADD_WAIVER_SET_WAIVER_COMMENT,
  ADD_WAIVER_SET_WAIVER_SCOPE,
  ADD_WAIVER_SET_APPLY_TO_ALL_COMPONENTS,
  ADD_WAIVER_SET_EXPIRY_TIME
} from './addWaiverActions';
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
  [ADD_WAIVER_LOAD_DATA_REQUESTED]: propSetConst('loading', true),
  [ADD_WAIVER_LOAD_DATA_FULFILLED]: setWaiverData,
  [ADD_WAIVER_LOAD_DATA_FAILED]: loadDataFailed,
  [ADD_WAIVER_SAVE_REQUESTED]: saveWaiverRequested,
  [ADD_WAIVER_SAVE_FULFILLED]: propSetConst('submitMaskState', true),
  [ADD_WAIVER_SAVE_FAILED]: saveWaiverFailed,
  [ADD_WAIVER_SUBMIT_MASK_TIMER_DONE]: propSetConst('submitMaskState', null),
  [ADD_WAIVER_SET_WAIVER_COMMENT]: setWaiverComment,
  [ADD_WAIVER_SET_WAIVER_SCOPE]: propSet('selectedWaiverScope'),
  [ADD_WAIVER_SET_APPLY_TO_ALL_COMPONENTS]: propSet('applyToAllComponents'),
  [ADD_WAIVER_SET_EXPIRY_TIME]: propSet('expiryTime'),
  [UI_ROUTER_ON_FINISH]: always(initState)
};

const addWaiverReducer = createReducerFromActionMap(reducerActionMap, initState);
export default addWaiverReducer;
