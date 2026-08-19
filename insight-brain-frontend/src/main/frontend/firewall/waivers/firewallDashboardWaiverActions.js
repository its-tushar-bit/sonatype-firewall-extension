/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import axios from 'axios';
import { Messages } from 'MainRoot/util/CommonServices';
import { deleteWaiverUrl } from 'MainRoot/util/CLMLocation';
import { noPayloadActionCreator, payloadParamActionCreator } from 'MainRoot/util/reduxUtil';
import { loadWaiverResults } from 'MainRoot/dashboard/results/dashboardResultsActions';
import { checkPermissions } from 'MainRoot/util/authorizationUtil';

export const FIREWALL_DASHBOARD_LOAD_WAIVE_PERMISSION_FULFILLED = 'FIREWALL_DASHBOARD_LOAD_WAIVE_PERMISSION_FULFILLED';
export const FIREWALL_DASHBOARD_SET_WAIVER_TO_DELETE = 'FIREWALL_DASHBOARD_SET_WAIVER_TO_DELETE';
export const FIREWALL_DASHBOARD_HIDE_DELETE_WAIVER_MODAL = 'FIREWALL_DASHBOARD_HIDE_DELETE_WAIVER_MODAL';
export const FIREWALL_DASHBOARD_DELETE_WAIVER_REQUESTED = 'FIREWALL_DASHBOARD_DELETE_WAIVER_REQUESTED';
export const FIREWALL_DASHBOARD_DELETE_WAIVER_FULFILLED = 'FIREWALL_DASHBOARD_DELETE_WAIVER_FULFILLED';
export const FIREWALL_DASHBOARD_DELETE_WAIVER_FAILED = 'FIREWALL_DASHBOARD_DELETE_WAIVER_FAILED';
export const FIREWALL_DASHBOARD_DELETE_MASK_TIMER_DONE = 'FIREWALL_DASHBOARD_DELETE_MASK_TIMER_DONE';

const SUBMIT_MASK_SUCCESS_VISIBLE_TIME_MS = 1500;

const loadWaivePermissionFulfilled = payloadParamActionCreator(FIREWALL_DASHBOARD_LOAD_WAIVE_PERMISSION_FULFILLED);
export const setFirewallWaiverToDelete = payloadParamActionCreator(FIREWALL_DASHBOARD_SET_WAIVER_TO_DELETE);

export function loadFirewallDashboardWaivePermission() {
  return (dispatch) => {
    return checkPermissions(['WAIVE_POLICY_VIOLATIONS'])
      .then(() => dispatch(loadWaivePermissionFulfilled(true)))
      .catch(() => dispatch(loadWaivePermissionFulfilled(false)));
  };
}
export const hideFirewallDeleteWaiverModal = noPayloadActionCreator(FIREWALL_DASHBOARD_HIDE_DELETE_WAIVER_MODAL);

const deleteWaiverRequested = noPayloadActionCreator(FIREWALL_DASHBOARD_DELETE_WAIVER_REQUESTED);
const deleteWaiverFulfilled = noPayloadActionCreator(FIREWALL_DASHBOARD_DELETE_WAIVER_FULFILLED);
const deleteWaiverMaskTimerDone = noPayloadActionCreator(FIREWALL_DASHBOARD_DELETE_MASK_TIMER_DONE);
const deleteWaiverFailed = payloadParamActionCreator(FIREWALL_DASHBOARD_DELETE_WAIVER_FAILED);

export function deleteFirewallWaiver(ownerType, ownerId, waiverId) {
  return (dispatch) => {
    dispatch(deleteWaiverRequested());
    return axios
      .delete(deleteWaiverUrl(ownerType, ownerId, waiverId))
      .then(() => {
        dispatch(deleteWaiverFulfilled());
        setTimeout(() => {
          dispatch(deleteWaiverMaskTimerDone());
          dispatch(loadWaiverResults());
        }, SUBMIT_MASK_SUCCESS_VISIBLE_TIME_MS);
      })
      .catch((err) => {
        dispatch(deleteWaiverFailed(Messages.getHttpErrorMessage(err)));
      });
  };
}
