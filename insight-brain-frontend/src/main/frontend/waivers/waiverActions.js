/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import axios from 'axios';
import { compose, path } from 'ramda';
import { SUBMIT_MASK_SUCCESS_VISIBLE_TIME_MS } from '@sonatype/react-shared-components';

import { capitalize } from '../util/jsUtil';
import { noPayloadActionCreator, payloadParamActionCreator } from '../util/reduxUtil';
import { Messages } from '../util/CommonServices';
import { getAddPolicyViolationWaiverUrl, getOwnerContextHierarchyUrl, deleteWaiverUrl } from '../util/CLMLocation';

import { stateGo } from '../reduxUiRouter/routerActions';
import { getPermissionContextTestUrl } from '../util/CLMContextLocation';
import { getApplicationSummaryUrl } from '../util/CLMLocation';
import { fetchCrossStageViolation, fetchApplicableWaivers } from '../violation/violationActions';
import { getExpiryTime } from '../util/waiverUtils';

import { actions as policyViolationsActions } from '../componentDetails/violations/PolicyViolationsRedux';

export const WAIVERS_LOAD_ADD_WAIVER_DATA_REQUESTED = 'WAIVERS_LOAD_ADD_WAIVER_DATA_REQUESTED';
export const WAIVERS_LOAD_ADD_WAIVER_DATA_FULFILLED = 'WAIVERS_LOAD_ADD_WAIVER_DATA_FULFILLED';
export const WAIVERS_LOAD_ADD_WAIVER_DATA_FAILED = 'WAIVERS_LOAD_ADD_WAIVER_DATA_FAILED';
export const WAIVERS_SAVE_WAIVER_REQUESTED = 'WAIVERS_SAVE_WAIVER_REQUESTED';
export const WAIVERS_SAVE_WAIVER_FULFILLED = 'WAIVERS_SAVE_WAIVER_FULFILLED';
export const WAIVERS_SAVE_WAIVER_FAILED = 'WAIVERS_SAVE_WAIVER_FAILED';
export const WAIVERS_ADD_WAIVER_SUBMIT_MASK_TIMER_DONE = 'WAIVERS_ADD_WAIVER_SUBMIT_MASK_TIMER_DONE';
export const WAIVERS_ADD_WAIVER_SET_WAIVER_COMMENT = 'WAIVERS_ADD_WAIVER_SET_WAIVER_COMMENT';
export const WAIVERS_ADD_WAIVER_SET_WAIVER_SCOPE = 'WAIVERS_ADD_WAIVER_SET_WAIVER_SCOPE';
export const WAIVERS_ADD_WAIVER_SET_APPLY_TO_ALL_COMPONENTS = 'WAIVERS_ADD_WAIVER_SET_APPLY_TO_ALL_COMPONENTS';
export const WAIVERS_ADD_WAIVER_SET_EXPIRY_TIME = 'WAIVERS_ADD_WAIVER_SET_EXPIRY_TIME';
export const WAIVERS_LOAD_MANAGE_WAIVERS_DATA_REQUESTED = 'WAIVERS_LOAD_MANAGE_WAIVERS_DATA_REQUESTED';
export const WAIVERS_LOAD_MANAGE_WAIVERS_DATA_FULFILLED = 'WAIVERS_LOAD_MANAGE_WAIVERS_DATA_FULFILLED';
export const WAIVERS_LOAD_MANAGE_WAIVERS_DATA_FAILED = 'WAIVERS_LOAD_MANAGE_WAIVERS_DATA_FAILED';
export const WAIVERS_SET_IS_REQUEST_WAIVER_POPOVER_SHOWN = 'WAIVERS_SET_IS_REQUEST_WAIVER_POPOVER_SHOWN';
export const WAIVERS_SET_WAIVER_TO_DELETE = 'WAIVERS_SET_WAIVER_TO_DELETE';
export const WAIVERS_HIDE_DELETE_WAIVER_MODAL = 'WAIVERS_HIDE_DELETE_WAIVER_MODAL';
export const WAIVERS_DELETE_WAIVER_REQUESTED = 'WAIVERS_DELETE_WAIVER_REQUESTED';
export const WAIVERS_DELETE_WAIVER_FULFILLED = 'WAIVERS_DELETE_WAIVER_FULFILLED';
export const WAIVERS_DELETE_WAIVER_FAILED = 'WAIVERS_DELETE_WAIVER_FAILED';
export const WAIVERS_DELETE_MASK_TIMER_DONE = 'WAIVERS_DELETE_MASK_TIMER_DONE';

export const WAIVERS_LOAD_APPLICABLE_WAIVERS_REQUESTED = 'WAIVERS_LOAD_APPLICABLE_WAIVERS_REQUESTED';
export const WAIVERS_LOAD_APPLICABLE_WAIVERS_FULFILLED = 'WAIVERS_LOAD_APPLICABLE_WAIVERS_FULFILLED';
export const WAIVERS_LOAD_APPLICABLE_WAIVERS_FAILED = 'WAIVERS_LOAD_APPLICABLE_WAIVERS_FAILED';

const saveWaiverRequested = noPayloadActionCreator(WAIVERS_SAVE_WAIVER_REQUESTED);
const saveWaiverFulfilled = noPayloadActionCreator(WAIVERS_SAVE_WAIVER_FULFILLED);
const saveWaiverFailed = payloadParamActionCreator(WAIVERS_SAVE_WAIVER_FAILED);
const loadAddWaiverDataRequested = noPayloadActionCreator(WAIVERS_LOAD_ADD_WAIVER_DATA_REQUESTED);
const loadAddWaiverDataFailed = payloadParamActionCreator(WAIVERS_LOAD_ADD_WAIVER_DATA_FAILED);
const loadAddWaiverDataFulfilled = payloadParamActionCreator(WAIVERS_LOAD_ADD_WAIVER_DATA_FULFILLED);
const loadManageWaiversDataRequested = noPayloadActionCreator(WAIVERS_LOAD_MANAGE_WAIVERS_DATA_REQUESTED);
const loadManageWaiversDataFulfilled = payloadParamActionCreator(WAIVERS_LOAD_MANAGE_WAIVERS_DATA_FULFILLED);
const loadManageWaiversDataFailed = payloadParamActionCreator(WAIVERS_LOAD_MANAGE_WAIVERS_DATA_FAILED);

function startSubmitMaskTimer(dispatch) {
  setTimeout(() => {
    dispatch({ type: WAIVERS_ADD_WAIVER_SUBMIT_MASK_TIMER_DONE });
  }, SUBMIT_MASK_SUCCESS_VISIBLE_TIME_MS);
}

/**
 * @param policyViolationId { string }
 * @param waiverScope { string } application | organization | root_organization
 * @param ownerId { string }
 * @param comment { string }
 * @param applyToAllComponents { boolean }
 * @param expiration { string }
 */
export function saveWaiver(policyViolationId, waiverScope, ownerId, comment, applyToAllComponents, expiration) {
  return (dispatch) => {
    dispatch(saveWaiverRequested());

    const url = getAddPolicyViolationWaiverUrl(waiverScope, ownerId, policyViolationId),
      payload = {
        comment,
        applyToAllComponents,
        expiryTime: getExpiryTime(expiration),
      };

    return axios
      .post(url, payload)
      .then(() => {
        startSubmitMaskTimer(dispatch);
        dispatch(saveWaiverFulfilled());
        return dispatch(returnToAddWaiverOriginPage());
      })
      .catch((err) => {
        return dispatch(saveWaiverFailed(err));
      });
  };
}

/**
 * @param {string } violationId
 */
export function loadAddWaiverData(violationId) {
  return (dispatch, getState) => {
    dispatch(loadAddWaiverDataRequested());
    return dispatch(fetchCrossStageViolation(violationId))
      .then(() => {
        const ownerType = 'application',
          { violation } = getState(),
          { violationDetails } = violation,
          { applicationPublicId, policyId } = violationDetails;
        // ToDo verify that ownerType is always application
        return loadOwnerContextHierarchy(ownerType, applicationPublicId, policyId);
      })
      .then((waiverTargets) => dispatch(loadAddWaiverDataFulfilled(waiverTargets)))
      .catch((err) => dispatch(loadAddWaiverDataFailed(err)));
  };
}

/**
 * @param {string } violationId
 */
export function loadManageWaiversData(violationId) {
  return (dispatch, getState) => {
    dispatch(loadManageWaiversDataRequested());
    dispatch(loadApplicableWaivers(violationId));

    return dispatch(fetchCrossStageViolation(violationId))
      .then(() => loadPermissionForAppWaivers(getState().violation.violationDetails.applicationPublicId))
      .then(compose(dispatch, loadManageWaiversDataFulfilled))
      .catch(compose(dispatch, loadManageWaiversDataFailed));
  };
}

export function returnToAddWaiverOriginPage() {
  return (dispatch, getState) => {
    const { prevParams, prevState, currentParams } = getState().router;
    const originNameForViolationDetails = 'listWaivers';
    const originNameForCip = 'applicationReport.policy';

    const prevStateName = prevState && prevState.name;
    switch (prevStateName) {
      case originNameForViolationDetails:
        return dispatch(stateGo(originNameForViolationDetails, prevParams));

      case originNameForCip:
        return dispatch(
          stateGo(prevState.name, {
            ...prevParams,
            policyViolationId: currentParams.violationId,
          })
        );

      default:
        return dispatch(
          stateGo(originNameForViolationDetails, {
            violationId: currentParams.violationId,
          })
        );
    }
  };
}

export const setWaiverComment = payloadParamActionCreator(WAIVERS_ADD_WAIVER_SET_WAIVER_COMMENT);

export const setWaiverScope = payloadParamActionCreator(WAIVERS_ADD_WAIVER_SET_WAIVER_SCOPE);

export const setApplyToAllComponents = payloadParamActionCreator(WAIVERS_ADD_WAIVER_SET_APPLY_TO_ALL_COMPONENTS);

export const setExpiryTime = payloadParamActionCreator(WAIVERS_ADD_WAIVER_SET_EXPIRY_TIME);

function loadOwnerContextHierarchy(ownerType, ownerId, policyId) {
  return axios
    .get(getOwnerContextHierarchyUrl(ownerType, ownerId, policyId))
    .then(({ data }) => processOwnerHierarchy(data));
  // let the error be handled by calling code.
}

function loadPermissionForAppWaivers(applicationPublicId) {
  return axios
    .get(getApplicationSummaryUrl(applicationPublicId))
    .then(({ data }) => axios.put(getPermissionContextTestUrl('application', data.id), ['WAIVE_POLICY_VIOLATIONS']))
    .then(({ data }) => data.length === 1);
}

/**
 * Flattens the Org/Apps hierarchy
 */
function processOwnerHierarchy(context) {
  // note that since the context data only includes the ancestors of the waiver, `children` should
  // never have more than one element
  const processedChildren = context.children ? processOwnerHierarchy(context.children[0]) : [],
    { type, id, name } = context,
    label = capitalize(type);

  return processedChildren.concat({ type, id, name, label });
}

export const setWaiverToDelete = payloadParamActionCreator(WAIVERS_SET_WAIVER_TO_DELETE);
export const hideDeleteWaiverModal = noPayloadActionCreator(WAIVERS_HIDE_DELETE_WAIVER_MODAL);
const deleteWaiverRequested = noPayloadActionCreator(WAIVERS_DELETE_WAIVER_REQUESTED);
const deleteWaiverFulfilled = noPayloadActionCreator(WAIVERS_DELETE_WAIVER_FULFILLED);
const deleteWaiverFailed = payloadParamActionCreator(WAIVERS_DELETE_WAIVER_FAILED);
const deleteWaiverMaskTimerDone = noPayloadActionCreator(WAIVERS_DELETE_MASK_TIMER_DONE);

export function deleteWaiver(ownerType, ownerId, waiverId) {
  return (dispatch, getState) => {
    dispatch(deleteWaiverRequested());

    const { violation, componentDetailsPolicyViolations } = getState();
    const { reloadComponentWaivers } = componentDetailsPolicyViolations;
    const policyViolationId = path(['violationDetails', 'policyViolationId'], violation);
    const endpointUrl = deleteWaiverUrl(ownerType, ownerId, waiverId);

    return axios
      .delete(endpointUrl)
      .then(() => {
        dispatch(deleteWaiverFulfilled());
        if (!reloadComponentWaivers) {
          dispatch(loadApplicableWaivers(policyViolationId));
        } else {
          dispatch(policyViolationsActions.load());
        }
        setTimeout(() => {
          dispatch(deleteWaiverMaskTimerDone());
        }, SUBMIT_MASK_SUCCESS_VISIBLE_TIME_MS);
      })
      .catch((err) => {
        dispatch(deleteWaiverFailed(Messages.getHttpErrorMessage(err)));
      });
  };
}

const loadApplicableWaiversRequested = noPayloadActionCreator(WAIVERS_LOAD_APPLICABLE_WAIVERS_REQUESTED);
const loadApplicableWaiversFulfilled = noPayloadActionCreator(WAIVERS_LOAD_APPLICABLE_WAIVERS_FULFILLED);
const loadApplicableWaiversFailed = payloadParamActionCreator(WAIVERS_LOAD_APPLICABLE_WAIVERS_FAILED);

export function loadApplicableWaivers(policyViolationId) {
  return function (dispatch) {
    dispatch(loadApplicableWaiversRequested());
    return dispatch(fetchApplicableWaivers(policyViolationId))
      .then(compose(dispatch, loadApplicableWaiversFulfilled))
      .catch(compose(dispatch, loadApplicableWaiversFailed));
  };
}

export function setIsRequestWaiverPopoverShown(flag) {
  return function (dispatch) {
    dispatch(payloadParamActionCreator(WAIVERS_SET_IS_REQUEST_WAIVER_POPOVER_SHOWN)(flag));
  };
}
