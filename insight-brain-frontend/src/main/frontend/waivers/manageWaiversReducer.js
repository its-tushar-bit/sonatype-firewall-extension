/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { omit } from 'ramda';

import { createReducerFromActionMap } from '../util/reduxUtil';
import { WAIVERS_SET_MANAGE_WAIVERS_BACK_BUTTON_STATE_NAME } from './waiverActions';
import { UI_ROUTER_ON_FINISH } from '../reduxUiRouter/routerActions';

export const initState = Object.freeze({
  hasPermissionForAppWaivers: null,
  previousRouterStateNameForComponentDetails: null,
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
  [WAIVERS_SET_MANAGE_WAIVERS_BACK_BUTTON_STATE_NAME]: setPreviousRouterStateNameForComponentDetails,
  [UI_ROUTER_ON_FINISH]: resetState,
};

const manageWaiversReducer = createReducerFromActionMap(reducerActionMap, initState);
export default manageWaiversReducer;
