/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import axios from 'axios';
import { SUBMIT_MASK_SUCCESS_VISIBLE_TIME_MS } from '@sonatype/react-shared-components';

import { capitalize, getFutureDate } from '../util/jsUtil';
import { noPayloadActionCreator, payloadParamActionCreator } from '../util/reduxUtil';
import { getAddPolicyViolationWaiverUrl, getOwnerContextHierarchyUrl } from '../util/CLMLocation';
import { loadViolation } from '../violation/violationPageActions';
import { stateGo } from '../reduxUiRouter/routerActions';

export const WAIVERS_LOAD_SCOPE_DATA_REQUESTED = 'ADD_WAIVER_LOAD_DATA_REQUESTED';
export const WAIVERS_LOAD_SCOPE_DATA_FULFILLED = 'ADD_WAIVER_LOAD_DATA_FULFILLED';
export const WAIVERS_LOAD_SCOPE_DATA_FAILED = 'ADD_WAIVER_LOAD_DATA_FAILED';
export const WAIVERS_SAVE_WAIVER_REQUESTED = 'ADD_WAIVER_SAVE_REQUESTED';
export const WAIVERS_SAVE_WAIVER_FULFILLED = 'ADD_WAIVER_SAVE_FULFILLED';
export const WAIVERS_SAVE_WAIVER_FAILED = 'ADD_WAIVER_SAVE_FAILED';
export const WAIVERS_ADD_WAIVER_SUBMIT_MASK_TIMER_DONE = 'ADD_WAIVER_SUBMIT_MASK_TIMER_DONE';
export const WAIVERS_ADD_WAIVER_SET_WAIVER_COMMENT = 'ADD_WAIVER_SET_WAIVER_COMMENT';
export const WAIVERS_ADD_WAIVER_SET_WAIVER_SCOPE = 'ADD_WAIVER_SET_WAIVER_SCOPE';
export const WAIVERS_ADD_WAIVER_SET_APPLY_TO_ALL_COMPONENTS = 'ADD_WAIVER_SET_APPLY_TO_ALL_COMPONENTS';
export const WAIVERS_ADD_WAIVER_SET_EXPIRY_TIME = 'ADD_WAIVER_SET_EXPIRY_TIME';

const saveWaiverRequested = noPayloadActionCreator(WAIVERS_SAVE_WAIVER_REQUESTED);
const saveWaiverFulfilled = noPayloadActionCreator(WAIVERS_SAVE_WAIVER_FULFILLED);
const saveWaiverFailed = payloadParamActionCreator(WAIVERS_SAVE_WAIVER_FAILED);
const loadAddWaiverDataRequested = noPayloadActionCreator(WAIVERS_LOAD_SCOPE_DATA_REQUESTED);
const loadAddWaiverDataFailed = payloadParamActionCreator(WAIVERS_LOAD_SCOPE_DATA_FAILED);
const loadAddWaiverDataFulfilled = payloadParamActionCreator(WAIVERS_LOAD_SCOPE_DATA_FULFILLED);

function startSubmitMaskTimer(dispatch) {
  setTimeout(() => {
    dispatch({ type: WAIVERS_ADD_WAIVER_SUBMIT_MASK_TIMER_DONE });
  }, SUBMIT_MASK_SUCCESS_VISIBLE_TIME_MS);
}

function getExpiryTime(expiration) {
  if (!expiration) {
    return null;
  }

  return getFutureDate(expiration);
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
          expiryTime: getExpiryTime(expiration)
        };

    return axios.post(url, payload)
        .then(() => {
          startSubmitMaskTimer(dispatch);
          dispatch(saveWaiverFulfilled());
          return dispatch(returnToAddWaiverOriginPage());
        })
        .catch((err) => {
          dispatch(saveWaiverFailed(err));
          return Promise.reject(err);
        });
  };
}

/**
 * @param {string } violationId
*/
export function loadAddWaiverData(violationId) {
  return (dispatch, getState) => {

    dispatch(loadAddWaiverDataRequested());
    return dispatch(loadViolation(violationId))
        .then(() => {
          const ownerType = 'application',
              { violationPage } = getState(),
              { violationDetails } = violationPage,
              { applicationPublicId, policyId } = violationDetails;
          // ToDo verify that ownerType is always application
          return loadOwnerContextHierarchy(ownerType, applicationPublicId, policyId);
        })
        .then((waiverTargets) => dispatch(loadAddWaiverDataFulfilled(waiverTargets)))
        .catch((err) => dispatch(loadAddWaiverDataFailed(err)));
  };
}

export function returnToAddWaiverOriginPage() {
  return (dispatch, getState) => {
    const { prevParams, prevState, currentParams } = getState().router;
    const originNameForViolationDetails = 'sidebarView.violation';
    const originNameForCip = 'applicationReport.policy';

    const prevStateName = prevState && prevState.name;
    switch (prevStateName) {
      case originNameForViolationDetails:
        return dispatch(stateGo(originNameForViolationDetails, prevParams));

      case originNameForCip:
        return dispatch(stateGo(prevState.name, { ...prevParams, policyViolationId: currentParams.violationId }));

      default:
        return dispatch(stateGo(originNameForViolationDetails, {id: currentParams.violationId}));
    }
  };
}

export const setWaiverComment = payloadParamActionCreator(WAIVERS_ADD_WAIVER_SET_WAIVER_COMMENT);

export const setWaiverScope = payloadParamActionCreator(WAIVERS_ADD_WAIVER_SET_WAIVER_SCOPE);

export const setApplyToAllComponents = payloadParamActionCreator(WAIVERS_ADD_WAIVER_SET_APPLY_TO_ALL_COMPONENTS);

export const setExpiryTime = payloadParamActionCreator(WAIVERS_ADD_WAIVER_SET_EXPIRY_TIME);

function loadOwnerContextHierarchy(ownerType, ownerId, policyId) {
  return axios.get(getOwnerContextHierarchyUrl(ownerType, ownerId, policyId))
      .then(({ data }) => processOwnerHierarchy(data));
  // let the error be handled by calling code.
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
