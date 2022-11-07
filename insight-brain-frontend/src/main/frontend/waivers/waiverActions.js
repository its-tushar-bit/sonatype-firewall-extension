/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import axios from 'axios';
import { compose, path } from 'ramda';
import { SUBMIT_MASK_SUCCESS_VISIBLE_TIME_MS } from '@sonatype/react-shared-components';

import { capitalize, getISODateFromDateInput } from '../util/jsUtil';
import { noPayloadActionCreator, payloadParamActionCreator } from '../util/reduxUtil';
import { Messages } from '../utilAngular/CommonServices';
import { getAddPolicyViolationWaiverUrl, getOwnerContextHierarchyUrl, deleteWaiverUrl } from '../util/CLMLocation';

import { stateGo } from '../reduxUiRouter/routerActions';
import { getPermissionContextTestUrl } from '../utilAngular/CLMContextLocation';
import { getApplicationSummaryUrl } from '../util/CLMLocation';
import { fetchCrossStageViolation, fetchApplicableWaivers } from '../violation/violationActions';
import { getExpiryTime, originNamesForAddRequestPages } from '../util/waiverUtils';

import { actions as policyViolationsActions } from '../componentDetails/ViolationsTableTile/policyViolationsSlice';
import { loadTransitiveViolationWaivers } from '../violation/transitiveViolationsActions';
import { selectPreviousRouteName, selectIsFirewall } from 'MainRoot/reduxUiRouter/routerSelectors';
import { gotoWaiver, setSidebarNavListData } from 'MainRoot/sidebarNav/sidebarNavListActions';

export const WAIVERS_LOAD_ADD_WAIVER_DATA_REQUESTED = 'WAIVERS_LOAD_ADD_WAIVER_DATA_REQUESTED';
export const WAIVERS_LOAD_ADD_WAIVER_DATA_FULFILLED = 'WAIVERS_LOAD_ADD_WAIVER_DATA_FULFILLED';
export const WAIVERS_LOAD_ADD_WAIVER_DATA_FAILED = 'WAIVERS_LOAD_ADD_WAIVER_DATA_FAILED';
export const WAIVERS_SAVE_WAIVER_REQUESTED = 'WAIVERS_SAVE_WAIVER_REQUESTED';
export const WAIVERS_SAVE_WAIVER_FULFILLED = 'WAIVERS_SAVE_WAIVER_FULFILLED';
export const WAIVERS_SAVE_WAIVER_FAILED = 'WAIVERS_SAVE_WAIVER_FAILED';
export const WAIVERS_ADD_WAIVER_SUBMIT_MASK_TIMER_DONE = 'WAIVERS_ADD_WAIVER_SUBMIT_MASK_TIMER_DONE';
export const WAIVERS_ADD_WAIVER_SET_WAIVER_COMMENT = 'WAIVERS_ADD_WAIVER_SET_WAIVER_COMMENT';
export const WAIVERS_ADD_WAIVER_SET_WAIVER_SCOPE = 'WAIVERS_ADD_WAIVER_SET_WAIVER_SCOPE';
export const WAIVERS_ADD_WAIVER_SET_COMPONENT_MATCHER_STRATEGY = 'WAIVERS_ADD_WAIVER_SET_COMPONENT_MATCHER_STRATEGY';
export const WAIVERS_ADD_WAIVER_SET_EXPIRY_TIME = 'WAIVERS_ADD_WAIVER_SET_EXPIRY_TIME';
export const WAIVERS_ADD_WAIVER_SET_CUSTOM_EXPIRY_TIME = 'WAIVERS_ADD_WAIVER_SET_CUSTOM_EXPIRY_TIME';
export const WAIVERS_LOAD_MANAGE_WAIVERS_DATA_REQUESTED = 'WAIVERS_LOAD_MANAGE_WAIVERS_DATA_REQUESTED';
export const WAIVERS_LOAD_MANAGE_WAIVERS_DATA_FULFILLED = 'WAIVERS_LOAD_MANAGE_WAIVERS_DATA_FULFILLED';
export const WAIVERS_LOAD_MANAGE_WAIVERS_DATA_FAILED = 'WAIVERS_LOAD_MANAGE_WAIVERS_DATA_FAILED';
export const WAIVERS_SET_WAIVER_TO_DELETE = 'WAIVERS_SET_WAIVER_TO_DELETE';
export const WAIVERS_ADD_WAIVER_SET_SHOW_UNSAVED_CHANGES_MODAL = 'WAIVERS_ADD_WAIVER_SET_SHOW_UNSAVED_CHANGES_MODAL';
export const WAIVERS_HIDE_DELETE_WAIVER_MODAL = 'WAIVERS_HIDE_DELETE_WAIVER_MODAL';
export const WAIVERS_DELETE_WAIVER_REQUESTED = 'WAIVERS_DELETE_WAIVER_REQUESTED';
export const WAIVERS_DELETE_WAIVER_FULFILLED = 'WAIVERS_DELETE_WAIVER_FULFILLED';
export const WAIVERS_DELETE_WAIVER_FAILED = 'WAIVERS_DELETE_WAIVER_FAILED';
export const WAIVERS_DELETE_MASK_TIMER_DONE = 'WAIVERS_DELETE_MASK_TIMER_DONE';
export const WAIVERS_RESET_ADD_WAIVER_DATA = 'WAIVERS_RESET_ADD_WAIVER_DATA';

export const WAIVERS_LOAD_APPLICABLE_WAIVERS_REQUESTED = 'WAIVERS_LOAD_APPLICABLE_WAIVERS_REQUESTED';
export const WAIVERS_LOAD_APPLICABLE_WAIVERS_FULFILLED = 'WAIVERS_LOAD_APPLICABLE_WAIVERS_FULFILLED';
export const WAIVERS_LOAD_APPLICABLE_WAIVERS_FAILED = 'WAIVERS_LOAD_APPLICABLE_WAIVERS_FAILED';

export const WAIVERS_SET_MANAGE_WAIVERS_BACK_BUTTON_STATE_NAME = 'WAIVERS_SET_MANAGE_WAIVERS_BACK_BUTTON_STATE_NAME';

const saveWaiverRequested = noPayloadActionCreator(WAIVERS_SAVE_WAIVER_REQUESTED);
const saveWaiverFulfilled = noPayloadActionCreator(WAIVERS_SAVE_WAIVER_FULFILLED);
const saveWaiverFailed = payloadParamActionCreator(WAIVERS_SAVE_WAIVER_FAILED);
const loadAddWaiverDataRequested = noPayloadActionCreator(WAIVERS_LOAD_ADD_WAIVER_DATA_REQUESTED);
const loadAddWaiverDataFailed = payloadParamActionCreator(WAIVERS_LOAD_ADD_WAIVER_DATA_FAILED);
const loadAddWaiverDataFulfilled = payloadParamActionCreator(WAIVERS_LOAD_ADD_WAIVER_DATA_FULFILLED);
const loadManageWaiversDataRequested = noPayloadActionCreator(WAIVERS_LOAD_MANAGE_WAIVERS_DATA_REQUESTED);
const loadManageWaiversDataFulfilled = payloadParamActionCreator(WAIVERS_LOAD_MANAGE_WAIVERS_DATA_FULFILLED);
const loadManageWaiversDataFailed = payloadParamActionCreator(WAIVERS_LOAD_MANAGE_WAIVERS_DATA_FAILED);
const setManageWaiversBackButtonStateName = payloadParamActionCreator(
  WAIVERS_SET_MANAGE_WAIVERS_BACK_BUTTON_STATE_NAME
);
export const resetAddWaiverData = noPayloadActionCreator(WAIVERS_RESET_ADD_WAIVER_DATA);

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
 * @param componentMatcherStrategy { string } EXACT_COMPONENT | ALL_COMPONENTS | ALL_VERSIONS
 * @param expiration { string }
 */
function saveWaiver(policyViolationId, waiverScope, ownerId, comment, componentMatcherStrategy, expiration, dispatch) {
  dispatch(saveWaiverRequested());
  const url = getAddPolicyViolationWaiverUrl(waiverScope, ownerId, policyViolationId),
    payload = {
      comment,
      matcherStrategy: componentMatcherStrategy,
      expiryTime: typeof expiration === 'string' ? getISODateFromDateInput(expiration) : getExpiryTime(expiration),
    };
  return axios.post(url, payload).then(() => {
    startSubmitMaskTimer(dispatch);
    return dispatch(saveWaiverFulfilled());
  });
}

export const saveWaiverAndRedirect = (
  policyViolationId,
  waiverScope,
  ownerId,
  comment,
  componentMatcherStrategy,
  expiration
) => (dispatch) =>
  saveWaiver(policyViolationId, waiverScope, ownerId, comment, componentMatcherStrategy, expiration, dispatch)
    .then(() => dispatch(returnToAddWaiverOriginPage()))
    .catch((err) => dispatch(saveWaiverFailed(err)));

export const saveWaiverAndLoadPolicyViolationData = (
  policyViolationId,
  waiverScope,
  ownerId,
  comment,
  componentMatcherStrategy,
  expiration
) => (dispatch) =>
  saveWaiver(policyViolationId, waiverScope, ownerId, comment, componentMatcherStrategy, expiration, dispatch)
    .then(() => dispatch(policyViolationsActions.load()))
    .then(() => dispatch(resetAddWaiverData()))
    .catch((err) => dispatch(saveWaiverFailed(err)));

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
 * @param { string } violationId
 */
export function loadManageWaiversData(violationId) {
  return (dispatch, getState) => {
    dispatch(loadManageWaiversDataRequested());
    dispatch(loadApplicableWaivers(violationId));

    const routerPreviousStateName = selectPreviousRouteName(getState());
    if (routerPreviousStateName?.includes('componentDetails')) {
      dispatch(setManageWaiversBackButtonStateName(routerPreviousStateName));
    }

    return dispatch(fetchCrossStageViolation(violationId))
      .then(() =>
        selectIsFirewall(getState())
          ? getAddWaiverPermissionForRepository(getState().router.currentParams.repositoryPolicyId)
          : loadPermissionForAppWaivers(getState().violation.violationDetails.applicationPublicId)
      )
      .then(compose(dispatch, loadManageWaiversDataFulfilled))
      .catch(compose(dispatch, loadManageWaiversDataFailed));
  };
}

export function returnToAddWaiverOriginPage() {
  return (dispatch, getState) => {
    const { prevParams, prevState, currentParams } = getState().router;

    const prevStateName = prevState && prevState.name;

    // If user canceled waiver creation, return to previous view
    switch (prevStateName) {
      case originNamesForAddRequestPages.APP_REPORT_VIOLATION_WAIVERS:
        return dispatch(stateGo(originNamesForAddRequestPages.APP_REPORT_VIOLATION_WAIVERS, prevParams));

      case originNamesForAddRequestPages.APP_REPORT_COMPONENT_DETAILS:
        return dispatch(stateGo(originNamesForAddRequestPages.APP_REPORT_COMPONENT_DETAILS, prevParams));

      case originNamesForAddRequestPages.WAIVERS_FOR_VIOLATION:
        return dispatch(stateGo(originNamesForAddRequestPages.WAIVERS_FOR_VIOLATION, prevParams));

      case originNamesForAddRequestPages.DASHBOARD_VIOLATIONS_VIEW:
        return dispatch(stateGo(originNamesForAddRequestPages.DASHBOARD_VIOLATIONS_VIEW, prevParams));

      case originNamesForAddRequestPages.APP_REPORT_CIP:
        return dispatch(
          stateGo(prevState.name, {
            ...prevParams,
            policyViolationId: currentParams.violationId,
          })
        );
      // Came from a direct link to the Add Waiver Page or some other origin
      default:
        return dispatch(
          stateGo(originNamesForAddRequestPages.WAIVERS_FOR_VIOLATION, {
            violationId: currentParams.violationId,
          })
        );
    }
  };
}

export const setWaiverComment = payloadParamActionCreator(WAIVERS_ADD_WAIVER_SET_WAIVER_COMMENT);

export const setWaiverScope = payloadParamActionCreator(WAIVERS_ADD_WAIVER_SET_WAIVER_SCOPE);

export const setComponentMatcherStrategy = payloadParamActionCreator(WAIVERS_ADD_WAIVER_SET_COMPONENT_MATCHER_STRATEGY);

export const setExpiryTime = payloadParamActionCreator(WAIVERS_ADD_WAIVER_SET_EXPIRY_TIME);

export const setCustomExpiryTime = payloadParamActionCreator(WAIVERS_ADD_WAIVER_SET_CUSTOM_EXPIRY_TIME);

function loadOwnerContextHierarchy(ownerType, ownerId, policyId) {
  return axios
    .get(getOwnerContextHierarchyUrl(ownerType, ownerId, policyId))
    .then(({ data }) => processOwnerHierarchy(data));
  // let the error be handled by calling code.
}

function loadPermissionForAppWaivers(applicationPublicId) {
  return axios
    .get(getApplicationSummaryUrl(applicationPublicId))
    .then(({ data }) => getAddWaiverPermissionForApplicationPromiseBuilder(data.id))
    .then(({ data }) => data.length === 1);
}

export const getAddWaiverPermissionForApplicationPromiseBuilder = (internalApplicationId) =>
  axios.put(getPermissionContextTestUrl('application', internalApplicationId), ['WAIVE_POLICY_VIOLATIONS']);

const getAddWaiverPermissionForRepository = (repositoryId) =>
  axios
    .put(getPermissionContextTestUrl('repository', repositoryId), ['WAIVE_POLICY_VIOLATIONS'])
    .then(({ data }) => data.length === 1);

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

export const filterDataByIdAndRedirectToNextWaiverOrDashboard = (waiverList, waiverId) => {
  return (dispatch) => {
    let idIndex = -1;
    const newWaiverList = waiverList.filter(({ id }, index) => {
      const itemFound = id === waiverId;
      idIndex = itemFound ? index : idIndex;
      return !itemFound;
    });

    if (waiverList.length === 1 || idIndex === -1) {
      dispatch(stateGo('dashboard.overview.waivers'));
    } else {
      const nextItem = idIndex + 1 === waiverList.length ? waiverList[0] : waiverList[idIndex + 1];
      dispatch(gotoWaiver(nextItem.ownerId, nextItem.ownerType, nextItem.id));
    }
    dispatch(setSidebarNavListData(newWaiverList));
  };
};

export function deleteWaiver(ownerType, ownerId, waiverId) {
  return (dispatch, getState) => {
    dispatch(deleteWaiverRequested());

    const { violation, componentDetailsPolicyViolations, router, sidebarNavList } = getState();
    const { reloadComponentWaivers } = componentDetailsPolicyViolations;
    const policyViolationId = path(['violationDetails', 'policyViolationId'], violation);
    const endpointUrl = deleteWaiverUrl(ownerType, ownerId, waiverId);

    return axios
      .delete(endpointUrl)
      .then(() => {
        dispatch(deleteWaiverFulfilled());
        const currentState = router.currentState;
        if (currentState.name === 'waiver.details') {
          dispatch(filterDataByIdAndRedirectToNextWaiverOrDashboard(sidebarNavList.data, waiverId));
        } else if (currentState.name === 'transitiveViolations') {
          const ownerId = router.currentParams.ownerId;
          const scanId = router.currentParams.scanId;
          const hash = router.currentParams.hash;
          dispatch(loadTransitiveViolationWaivers(ownerId, scanId, hash));
        } else {
          if (!reloadComponentWaivers) {
            dispatch(loadApplicableWaivers(policyViolationId));
          } else {
            dispatch(policyViolationsActions.load());
          }
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

export const setShowUnsavedChangesModal = (flag) => {
  return function (dispatch) {
    dispatch(payloadParamActionCreator(WAIVERS_ADD_WAIVER_SET_SHOW_UNSAVED_CHANGES_MODAL)(flag));
  };
};
