/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { always } from 'ramda';

import { UI_ROUTER_ON_FINISH } from '../../reduxUiRouter/routerActions';
import { createReducerFromActionMap, propSetConst } from '../../util/reduxUtil';
import {
  WAIVERS_DELETE_WAIVER_REQUESTED,
  WAIVERS_DELETE_WAIVER_FULFILLED,
  WAIVERS_DELETE_WAIVER_FAILED,
  WAIVERS_DELETE_MASK_TIMER_DONE,
  WAIVERS_SET_WAIVER_TO_DELETE,
  WAIVERS_HIDE_DELETE_WAIVER_MODAL,
} from '../waiverActions';

const initState = Object.freeze({
  waiverToDelete: null,
  deleteWaiverSaving: null,
  deleteWaiverError: null,
});

const setWaiverToDelete = (payload) => ({
  ...initState,
  waiverToDelete: payload,
});

const deleteWaiverRequested = (payload, state) => ({
  ...state,
  deleteWaiverSaving: false,
  deleteWaiverError: null,
});

const deleteWaiverFailed = (payload, state) => ({
  ...state,
  deleteWaiverSaving: null,
  deleteWaiverError: payload,
});

const reducerActionMap = {
  [UI_ROUTER_ON_FINISH]: always(initState),
  [WAIVERS_SET_WAIVER_TO_DELETE]: setWaiverToDelete,
  [WAIVERS_HIDE_DELETE_WAIVER_MODAL]: always(initState),
  [WAIVERS_DELETE_WAIVER_REQUESTED]: deleteWaiverRequested,
  [WAIVERS_DELETE_WAIVER_FAILED]: deleteWaiverFailed,
  [WAIVERS_DELETE_WAIVER_FULFILLED]: propSetConst('deleteWaiverSaving', true),
  [WAIVERS_DELETE_MASK_TIMER_DONE]: always(initState),
};

const deleteWaiverReducer = createReducerFromActionMap(reducerActionMap, initState);
export default deleteWaiverReducer;
