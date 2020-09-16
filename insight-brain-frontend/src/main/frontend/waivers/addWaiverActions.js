/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import axios from 'axios';
import { SUBMIT_MASK_SUCCESS_VISIBLE_TIME_MS } from '@sonatype/react-shared-components';

import { capitalize } from '../util/jsUtil';
import { noPayloadActionCreator, payloadParamActionCreator } from '../util/reduxUtil';
import { getAddPolicyViolationWaiverUrl, getOwnerContextHierarchyUrl } from '../util/CLMLocation';
import { loadViolation } from '../violation/violationPageActions';
import { stateGo } from '../reduxUiRouter/routerActions';

export const ADD_WAIVER_LOAD_DATA_REQUESTED = 'ADD_WAIVER_LOAD_DATA_REQUESTED';
export const ADD_WAIVER_LOAD_DATA_FULFILLED = 'ADD_WAIVER_LOAD_DATA_FULFILLED';
export const ADD_WAIVER_LOAD_DATA_FAILED = 'ADD_WAIVER_LOAD_DATA_FAILED';
export const ADD_WAIVER_SAVE_REQUESTED = 'ADD_WAIVER_SAVE_REQUESTED';
export const ADD_WAIVER_SAVE_FULFILLED = 'ADD_WAIVER_SAVE_FULFILLED';
export const ADD_WAIVER_SAVE_FAILED = 'ADD_WAIVER_SAVE_FAILED';
export const ADD_WAIVER_SUBMIT_MASK_TIMER_DONE = 'ADD_WAIVER_SUBMIT_MASK_TIMER_DONE';
export const ADD_WAIVER_SET_WAIVER_COMMENT = 'ADD_WAIVER_SET_WAIVER_COMMENT';
export const ADD_WAIVER_SET_WAIVER_SCOPE = 'ADD_WAIVER_SET_WAIVER_SCOPE';
export const ADD_WAIVER_SET_APPLY_TO_ALL_COMPONENTS = 'ADD_WAIVER_SET_APPLY_TO_ALL_COMPONENTS';

const saveWaiverRequested = noPayloadActionCreator(ADD_WAIVER_SAVE_REQUESTED);
const saveWaiverFulfilled = noPayloadActionCreator(ADD_WAIVER_SAVE_FULFILLED);
const saveWaiverFailed = payloadParamActionCreator(ADD_WAIVER_SAVE_FAILED);
const loadAddWaiverDataRequested = noPayloadActionCreator(ADD_WAIVER_LOAD_DATA_REQUESTED);
const loadAddWaiverDataFailed = payloadParamActionCreator(ADD_WAIVER_LOAD_DATA_FAILED);
const loadAddWaiverDataFulfilled = payloadParamActionCreator(ADD_WAIVER_LOAD_DATA_FULFILLED);

function startSubmitMaskTimer(dispatch) {
  setTimeout(() => {
    dispatch({ type: ADD_WAIVER_SUBMIT_MASK_TIMER_DONE });
  }, SUBMIT_MASK_SUCCESS_VISIBLE_TIME_MS);
}

/**
 * @param policyViolationId { string }
 * @param waiverScope { string } application | organization | root_organization
 * @param ownerId { string }
 * @param comment { string }
 * @param applyToAllComponents { boolean }
 */
export function saveWaiver(policyViolationId, waiverScope, ownerId, comment, applyToAllComponents) {
  return (dispatch) => {
    dispatch(saveWaiverRequested());

    const url = getAddPolicyViolationWaiverUrl(waiverScope, ownerId, policyViolationId),
        payload = {
          comment,
          applyToAllComponents
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

export const setWaiverComment = payloadParamActionCreator(ADD_WAIVER_SET_WAIVER_COMMENT);

export const setWaiverScope = payloadParamActionCreator(ADD_WAIVER_SET_WAIVER_SCOPE);

export const setApplyToAllComponents = payloadParamActionCreator(ADD_WAIVER_SET_APPLY_TO_ALL_COMPONENTS);

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
