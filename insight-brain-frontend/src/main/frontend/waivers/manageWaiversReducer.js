/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { always } from 'ramda';

import { createReducerFromActionMap, propSetConst } from '../util/reduxUtil';
import {
  WAIVERS_LOAD_MANAGE_WAIVERS_DATA_REQUESTED,
  WAIVERS_LOAD_MANAGE_WAIVERS_DATA_FULFILLED,
  WAIVERS_LOAD_MANAGE_WAIVERS_DATA_FAILED
} from './waiverActions';
import { UI_ROUTER_ON_FINISH } from '../reduxUiRouter/routerActions';

export const initState = Object.freeze({
  loading: false,
  loadError: null,
  hasPermissionForAppWaivers: null
});

const setLoadError = (payload, state) => ({
  ...state,
  loading: false,
  loadError: payload
});

const setData = (payload, state) => ({
  ...state,
  loading: false,
  loadError: null,
  hasPermissionForAppWaivers: payload
});

const reducerActionMap = {
  [WAIVERS_LOAD_MANAGE_WAIVERS_DATA_REQUESTED]: propSetConst('loading', true),
  [WAIVERS_LOAD_MANAGE_WAIVERS_DATA_FAILED]: setLoadError,
  [WAIVERS_LOAD_MANAGE_WAIVERS_DATA_FULFILLED]: setData,
  [UI_ROUTER_ON_FINISH]: always(initState)
};

const manageWaiversReducer = createReducerFromActionMap(reducerActionMap, initState);
export default manageWaiversReducer;
