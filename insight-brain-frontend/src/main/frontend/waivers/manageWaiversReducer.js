/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { omit } from 'ramda';

import { createReducerFromActionMap } from '../util/reduxUtil';
import {
  WAIVERS_LOAD_APPLICABLE_WAIVERS_REQUESTED,
  WAIVERS_LOAD_APPLICABLE_WAIVERS_FULFILLED,
  WAIVERS_LOAD_APPLICABLE_WAIVERS_FAILED,
  WAIVERS_SET_MANAGE_WAIVERS_BACK_BUTTON_STATE_NAME,
} from './waiverActions';
import { UI_ROUTER_ON_FINISH } from '../reduxUiRouter/routerActions';

export const initState = Object.freeze({
  loadingApplicableWaivers: false,
  loadApplicableWaiversError: null,
  hasPermissionForAppWaivers: null,
  previousRouterStateNameForComponentDetails: null,
});

const loadApplicableWaiversRequested = (payload, state) => ({
  ...state,
  loadingApplicableWaivers: true,
  loadApplicableWaiversError: null,
});

const loadApplicableWaiversFulfilled = (payload, state) => ({
  ...state,
  loadingApplicableWaivers: false,
  loadApplicableWaiversError: null,
});

const loadApplicableWaiversFailed = (payload, state) => ({
  ...state,
  loadingApplicableWaivers: false,
  loadApplicableWaiversError: payload,
});

const setPreviousRouterStateNameForComponentDetails = (payload, state) => ({
  ...state,
  previousRouterStateNameForComponentDetails: payload,
});

const resetState = (_, { previousRouterStateNameForComponentDetails }) => ({
  ...omit(['previousRouterStateNameForComponentDetails'], initState),
  previousRouterStateNameForComponentDetails,
});

const reducerActionMap = {
  [WAIVERS_LOAD_APPLICABLE_WAIVERS_REQUESTED]: loadApplicableWaiversRequested,
  [WAIVERS_LOAD_APPLICABLE_WAIVERS_FULFILLED]: loadApplicableWaiversFulfilled,
  [WAIVERS_LOAD_APPLICABLE_WAIVERS_FAILED]: loadApplicableWaiversFailed,
  [WAIVERS_SET_MANAGE_WAIVERS_BACK_BUTTON_STATE_NAME]: setPreviousRouterStateNameForComponentDetails,
  [UI_ROUTER_ON_FINISH]: resetState,
};

const manageWaiversReducer = createReducerFromActionMap(reducerActionMap, initState);
export default manageWaiversReducer;
