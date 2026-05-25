/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { always } from 'ramda';
import { UI_ROUTER_ON_FINISH } from 'MainRoot/reduxUiRouter/routerActions';
import { createReducerFromActionMap, propSetConst } from 'MainRoot/util/reduxUtil';
import {
  FIREWALL_DASHBOARD_LOAD_WAIVE_PERMISSION_FULFILLED,
  FIREWALL_DASHBOARD_SET_WAIVER_TO_DELETE,
  FIREWALL_DASHBOARD_HIDE_DELETE_WAIVER_MODAL,
  FIREWALL_DASHBOARD_DELETE_WAIVER_REQUESTED,
  FIREWALL_DASHBOARD_DELETE_WAIVER_FULFILLED,
  FIREWALL_DASHBOARD_DELETE_WAIVER_FAILED,
  FIREWALL_DASHBOARD_DELETE_MASK_TIMER_DONE,
} from './firewallDashboardWaiverActions';

const initState = Object.freeze({
  hasWaivePermission: false,
  waiverToDelete: null,
  deleteWaiverSaving: null,
  deleteWaiverError: null,
});

const setWaiverToDelete = (payload, state) => ({
  ...state,
  waiverToDelete: payload,
  deleteWaiverSaving: null,
  deleteWaiverError: null,
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

const loadWaivePermissionFulfilled = (payload, state) => ({ ...state, hasWaivePermission: payload });

const reducerActionMap = {
  [UI_ROUTER_ON_FINISH]: always(initState),
  [FIREWALL_DASHBOARD_LOAD_WAIVE_PERMISSION_FULFILLED]: loadWaivePermissionFulfilled,
  [FIREWALL_DASHBOARD_SET_WAIVER_TO_DELETE]: setWaiverToDelete,
  [FIREWALL_DASHBOARD_HIDE_DELETE_WAIVER_MODAL]: (payload, state) => ({ ...initState, hasWaivePermission: state.hasWaivePermission }),
  [FIREWALL_DASHBOARD_DELETE_WAIVER_REQUESTED]: deleteWaiverRequested,
  [FIREWALL_DASHBOARD_DELETE_WAIVER_FAILED]: deleteWaiverFailed,
  [FIREWALL_DASHBOARD_DELETE_WAIVER_FULFILLED]: propSetConst('deleteWaiverSaving', true),
  [FIREWALL_DASHBOARD_DELETE_MASK_TIMER_DONE]: (payload, state) => ({ ...initState, hasWaivePermission: state.hasWaivePermission }),
};

const firewallDashboardWaiverReducer = createReducerFromActionMap(reducerActionMap, initState);
export default firewallDashboardWaiverReducer;
