/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import axios from 'axios';
import { SUBMIT_MASK_SUCCESS_VISIBLE_TIME_MS } from '@sonatype/react-shared-components';

import { capitalize, getISODateFromDateInput } from '../util/jsUtil';
import { noPayloadActionCreator, payloadParamActionCreator } from '../util/reduxUtil';
import { Messages } from '../utilAngular/CommonServices';
import { stateGo } from '../reduxUiRouter/routerActions';
import { getPermissionContextTestUrl } from '../utilAngular/CLMContextLocation';
import {
  getApplicationSummaryUrl,
  getAddPolicyViolationWaiverUrl,
  getOwnerContextHierarchyUrl,
  deleteWaiverUrl,
  getSimilarWaiversUrl,
} from '../util/CLMLocation';
import {
  fetchCrossStageViolation,
  loadApplicableWaivers,
  fetchCrossStageViolationAddWaiver,
} from '../violation/violationActions';
import { getExpiryTime, originNamesForAddRequestPages } from '../util/waiverUtils';

import { actions as policyViolationsActions } from '../componentDetails/ViolationsTableTile/policyViolationsSlice';
import { actions as waiverActions } from 'MainRoot/waivers/waiverSlice';
import { loadTransitiveViolationWaivers } from '../violation/transitiveViolationsActions';
import {
  selectHash,
  selectPrevRepositoryPolicyId,
  selectRepositoryId,
  selectIsFirewallOrRepository,
  selectRouterCurrentParams,
  selectCurrentRouteName,
  selectIsStandaloneFirewall,
} from 'MainRoot/reduxUiRouter/routerSelectors';
import { gotoWaiver, setSidebarNavListData } from 'MainRoot/sidebarNav/sidebarNavListActions';
import { loadExistingWaiversData } from 'MainRoot/firewall/firewallActions';
import { selectComponentDetailsViolationsSlice } from 'MainRoot/componentDetails/ViolationsTableTile/PolicyViolationsSelectors';
import { compose, prop } from 'ramda';
import { FIREWALL_FIREWALLPAGE_WAIVERS, FIREWALL_WAIVER_DETAILS } from 'MainRoot/constants/states';

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
export const WAIVERS_ADD_WAIVER_SET_REASON = 'WAIVERS_ADD_WAIVER_SET_REASON';
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
export const WAIVERS_LOAD_SIMILAR_WAIVERS_REQUESTED = 'WAIVERS_LOAD_SIMILAR_WAIVERS_REQUESTED';
export const WAIVERS_LOAD_SIMILAR_WAIVERS_FULFILLED = 'WAIVERS_LOAD_SIMILAR_WAIVERS_FULFILLED';
export const WAIVERS_LOAD_SIMILAR_WAIVERS_FAILED = 'WAIVERS_LOAD_SIMILAR_WAIVERS_FAILED';

export const WAIVERS_SET_MANAGE_WAIVERS_BACK_BUTTON_STATE_NAME = 'WAIVERS_SET_MANAGE_WAIVERS_BACK_BUTTON_STATE_NAME';

const saveWaiverRequested = noPayloadActionCreator(WAIVERS_SAVE_WAIVER_REQUESTED);
const saveWaiverFulfilled = noPayloadActionCreator(WAIVERS_SAVE_WAIVER_FULFILLED);
const saveWaiverFailed = payloadParamActionCreator(WAIVERS_SAVE_WAIVER_FAILED);
const loadAddWaiverDataRequested = noPayloadActionCreator(WAIVERS_LOAD_ADD_WAIVER_DATA_REQUESTED);
const loadAddWaiverDataFailed = payloadParamActionCreator(WAIVERS_LOAD_ADD_WAIVER_DATA_FAILED);
const loadAddWaiverDataFulfilled = payloadParamActionCreator(WAIVERS_LOAD_ADD_WAIVER_DATA_FULFILLED);
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
function saveWaiver(
  policyViolationId,
  waiverScope,
  ownerId,
  comment,
  componentMatcherStrategy,
  expiration,
  dispatch,
  waiverReasonId,
  expireWhenRemediationAvailable
) {
  dispatch(saveWaiverRequested());
  const url = getAddPolicyViolationWaiverUrl(waiverScope, ownerId, policyViolationId),
    payload = {
      comment,
      matcherStrategy: componentMatcherStrategy,
      expiryTime: typeof expiration === 'string' ? getISODateFromDateInput(expiration) : getExpiryTime(expiration),
      waiverReasonId,
      expireWhenRemediationAvailable,
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
  expiration,
  waiverReasonId,
  expireWhenRemediationAvailable
) => (dispatch) =>
  saveWaiver(
    policyViolationId,
    waiverScope,
    ownerId,
    comment,
    componentMatcherStrategy,
    expiration,
    dispatch,
    waiverReasonId,
    expireWhenRemediationAvailable
  )
    .then(() => dispatch(returnToAddWaiverOriginPage()))
    .catch((err) => dispatch(saveWaiverFailed(err)));

export const saveWaiverAndLoadPolicyViolationData = (
  policyViolationId,
  waiverScope,
  ownerId,
  comment,
  componentMatcherStrategy,
  expiration,
  waiverReasonId,
  expireWhenRemediationAvailable
) => (dispatch) =>
  saveWaiver(
    policyViolationId,
    waiverScope,
    ownerId,
    comment,
    componentMatcherStrategy,
    expiration,
    dispatch,
    waiverReasonId,
    expireWhenRemediationAvailable
  )
    .then(() => dispatch(policyViolationsActions.load()))
    .then(() => dispatch(resetAddWaiverData()))
    .catch((err) => dispatch(saveWaiverFailed(err)));

/**
 * @param {string } violationId
 */
export function loadAddWaiverData(violationId) {
  return (dispatch, getState) => {
    const state = getState();
    const isFirewallOrRepositoryComponent = selectIsFirewallOrRepository(state);
    const isCurrentRouteName = isFirewallOrRepositoryComponent;
    const repositoryPolicyId = isFirewallOrRepositoryComponent
      ? selectRepositoryId(state)
      : selectPrevRepositoryPolicyId(state);
    const fetchCrossStage = isCurrentRouteName ? fetchCrossStageViolationAddWaiver : fetchCrossStageViolation;

    dispatch(loadAddWaiverDataRequested());
    return dispatch(fetchCrossStage(violationId))
      .then(() => {
        const ownerType = isCurrentRouteName ? 'repository' : 'application',
          { violation } = getState(),
          { violationDetails } = violation,
          { applicationPublicId, policyId } = violationDetails,
          isPublicId = isCurrentRouteName ? repositoryPolicyId : applicationPublicId;
        // ToDo verify that ownerType is always application
        const promises = [
          loadOwnerContextHierarchy(ownerType, isPublicId, policyId),
          dispatch(waiverActions.loadCachedWaiverReasons()),
        ];
        return Promise.all(promises);
      })
      .then((responses) => {
        dispatch(
          loadAddWaiverDataFulfilled({
            waiverTargets: responses[0],
            comments: extractPreloadedCommentFromUrl(getState()),
            reasonId: extractPreloadedReasonIdFromUrl(getState()),
          })
        );
      })
      .catch((err) => dispatch(loadAddWaiverDataFailed(err)));
  };
}

const extractPreloadedCommentFromUrl = (state) => {
  return selectCurrentRouteName(state) === 'addWaiver' ? selectRouterCurrentParams(state)?.comments : undefined;
};

const extractPreloadedReasonIdFromUrl = (state) => {
  return selectCurrentRouteName(state) === 'addWaiver' ? selectRouterCurrentParams(state)?.reasonId : undefined;
};

export function returnToAddWaiverOriginPage() {
  return (dispatch, getState) => {
    const { prevParams, prevState, currentParams } = getState().router;

    const prevStateName = prevState && prevState.name;

    // If user canceled waiver creation, return to previous view
    switch (prevStateName) {
      case originNamesForAddRequestPages.APP_REPORT_COMPONENT_DETAILS:
        return dispatch(stateGo(originNamesForAddRequestPages.APP_REPORT_COMPONENT_DETAILS, prevParams));

      case originNamesForAddRequestPages.APP_REPORT_COMPONENT_DETAILS_LEGAL:
        return dispatch(stateGo(originNamesForAddRequestPages.APP_REPORT_COMPONENT_DETAILS_LEGAL, prevParams));

      case originNamesForAddRequestPages.APP_REPORT_COMPONENT_DETAILS_SECURITY:
        return dispatch(stateGo(originNamesForAddRequestPages.APP_REPORT_COMPONENT_DETAILS_SECURITY, prevParams));

      case originNamesForAddRequestPages.DASHBOARD_VIOLATIONS_VIEW:
        return dispatch(stateGo(originNamesForAddRequestPages.DASHBOARD_VIOLATIONS_VIEW, prevParams));

      case originNamesForAddRequestPages.APP_REPORT_CIP:
        return dispatch(
          stateGo(prevState.name, {
            ...prevParams,
            policyViolationId: currentParams.violationId,
          })
        );

      case originNamesForAddRequestPages.FIREWALL_COMPONENT_DETAILS:
        return dispatch(stateGo(originNamesForAddRequestPages.FIREWALL_COMPONENT_DETAILS, prevParams));

      case originNamesForAddRequestPages.FIREWALL_COMPONENT_DETAILS_SECURITY:
        return dispatch(stateGo(originNamesForAddRequestPages.FIREWALL_COMPONENT_DETAILS_SECURITY, prevParams));

      case originNamesForAddRequestPages.FIREWALL_COMPONENT_DETAILS_LEGAL:
        return dispatch(stateGo(originNamesForAddRequestPages.FIREWALL_COMPONENT_DETAILS_LEGAL, prevParams));

      case originNamesForAddRequestPages.REPOSITORY_COMPONENT_DETAILS:
        return dispatch(stateGo(originNamesForAddRequestPages.REPOSITORY_COMPONENT_DETAILS, prevParams));

      case originNamesForAddRequestPages.REPOSITORY_COMPONENT_DETAILS_SECURITY:
        return dispatch(stateGo(originNamesForAddRequestPages.REPOSITORY_COMPONENT_DETAILS_SECURITY, prevParams));

      case originNamesForAddRequestPages.REPOSITORY_COMPONENT_DETAILS_LEGAL:
        return dispatch(stateGo(originNamesForAddRequestPages.REPOSITORY_COMPONENT_DETAILS_LEGAL, prevParams));

      case originNamesForAddRequestPages.FIREWALL_VIOLATION_WAIVERS:
        return dispatch(stateGo(originNamesForAddRequestPages.FIREWALL_VIOLATION_WAIVERS, prevParams));

      case originNamesForAddRequestPages.REPOSITORY_VIOLATION_WAIVERS:
        return dispatch(stateGo(originNamesForAddRequestPages.REPOSITORY_VIOLATION_WAIVERS, prevParams));

      //Priorities Page Origin -> Component Details -> Policy Violations Tab
      case originNamesForAddRequestPages.CDP_WITHIN_PRIORITIES_PAGE_FROM_DEVELOPER_DASHBOARD:
        return dispatch(
          stateGo(originNamesForAddRequestPages.CDP_WITHIN_PRIORITIES_PAGE_FROM_DEVELOPER_DASHBOARD, prevParams)
        );

      case originNamesForAddRequestPages.CDP_WITHIN_PRIORITIES_PAGE_FROM_REPORTS:
        return dispatch(stateGo(originNamesForAddRequestPages.CDP_WITHIN_PRIORITIES_PAGE_FROM_REPORTS, prevParams));

      //Priorities Page Origin -> Component Details -> Security Tab
      case originNamesForAddRequestPages.CDP_WITHIN_PRIORITIES_PAGE_FROM_DEVELOPER_DASHBOARD_SECURITY:
        return dispatch(
          stateGo(
            originNamesForAddRequestPages.CDP_WITHIN_PRIORITIES_PAGE_FROM_DEVELOPER_DASHBOARD_SECURITY,
            prevParams
          )
        );

      case originNamesForAddRequestPages.CDP_WITHIN_PRIORITIES_PAGE_FROM_REPORTS_SECURITY:
        return dispatch(
          stateGo(originNamesForAddRequestPages.CDP_WITHIN_PRIORITIES_PAGE_FROM_REPORTS_SECURITY, prevParams)
        );

      //Priorities Page Origin -> Component Details -> Legal Tab
      case originNamesForAddRequestPages.CDP_WITHIN_PRIORITIES_PAGE_FROM_DEVELOPER_DASHBOARD_LEGAL:
        return dispatch(
          stateGo(originNamesForAddRequestPages.CDP_WITHIN_PRIORITIES_PAGE_FROM_DEVELOPER_DASHBOARD_LEGAL, prevParams)
        );

      case originNamesForAddRequestPages.CDP_WITHIN_PRIORITIES_PAGE_FROM_REPORTS_LEGAL:
        return dispatch(
          stateGo(originNamesForAddRequestPages.CDP_WITHIN_PRIORITIES_PAGE_FROM_REPORTS_LEGAL, prevParams)
        );

      // Came from a direct link to the Add Waiver Page or some other origin
      default:
        return dispatch(
          stateGo(originNamesForAddRequestPages.DASHBOARD_VIOLATIONS_VIEW, {
            id: currentParams.violationId,
            type: prevParams.type,
            sidebarReference: prevParams.sidebarReference,
          })
        );
    }
  };
}

export const setWaiverComment = payloadParamActionCreator(WAIVERS_ADD_WAIVER_SET_WAIVER_COMMENT);

export const setWaiverScope = payloadParamActionCreator(WAIVERS_ADD_WAIVER_SET_WAIVER_SCOPE);

export const setComponentMatcherStrategy = payloadParamActionCreator(WAIVERS_ADD_WAIVER_SET_COMPONENT_MATCHER_STRATEGY);

export const setExpiryTime = payloadParamActionCreator(WAIVERS_ADD_WAIVER_SET_EXPIRY_TIME);

export const setWaiverReason = payloadParamActionCreator(WAIVERS_ADD_WAIVER_SET_REASON);

export const setCustomExpiryTime = payloadParamActionCreator(WAIVERS_ADD_WAIVER_SET_CUSTOM_EXPIRY_TIME);

function loadOwnerContextHierarchy(ownerType, ownerId, policyId) {
  return axios
    .get(getOwnerContextHierarchyUrl(ownerType, ownerId, policyId))
    .then(({ data }) => processOwnerHierarchy(data));
  // let the error be handled by calling code.
}

export function loadPermissionForAppWaivers(applicationPublicId) {
  return axios
    .get(getApplicationSummaryUrl(applicationPublicId))
    .then(({ data }) => getAddWaiverPermissionForApplicationPromiseBuilder(data.id))
    .then(({ data }) => data.length === 1);
}

export const getAddWaiverPermissionForApplicationPromiseBuilder = (internalApplicationId) =>
  axios.put(getPermissionContextTestUrl('application', internalApplicationId), ['WAIVE_POLICY_VIOLATIONS']);

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
const loadSimilarWaiversRequested = noPayloadActionCreator(WAIVERS_LOAD_SIMILAR_WAIVERS_REQUESTED);
const loadSimilarWaiversFulfilled = payloadParamActionCreator(WAIVERS_LOAD_SIMILAR_WAIVERS_FULFILLED);
const loadSimilarWaiversFailed = payloadParamActionCreator(WAIVERS_LOAD_SIMILAR_WAIVERS_FAILED);
const deleteWaiverMaskTimerDone = noPayloadActionCreator(WAIVERS_DELETE_MASK_TIMER_DONE);

export const filterDataByIdAndRedirectToNextWaiverOrDashboard = (waiverList, waiverId, isStandaloneFirewall) => {
  return (dispatch) => {
    let idIndex = -1;
    const newWaiverList = waiverList.filter(({ id }, index) => {
      const itemFound = id === waiverId;
      idIndex = itemFound ? index : idIndex;
      return !itemFound;
    });

    if (waiverList.length === 1 || idIndex === -1) {
      const stateToGo = isStandaloneFirewall ? FIREWALL_FIREWALLPAGE_WAIVERS : 'dashboard.overview.waivers';
      dispatch(stateGo(stateToGo));
    } else {
      const nextItem = idIndex + 1 === waiverList.length ? waiverList[0] : waiverList[idIndex + 1];
      dispatch(gotoWaiver(nextItem.ownerId, nextItem.ownerType, nextItem.id, isStandaloneFirewall));
    }
    dispatch(setSidebarNavListData(newWaiverList));
  };
};

export function deleteWaiver(ownerType, ownerId, waiverId) {
  return (dispatch, getState) => {
    dispatch(deleteWaiverRequested());

    const state = getState();
    const { sidebarNavList } = state;
    const { reloadComponentWaivers } = selectComponentDetailsViolationsSlice(state);

    const policyViolationId = selectIsFirewallOrRepository(state)
      ? state.componentDetailsPolicyViolations.selectedPolicyViolation?.policyViolationId
      : state.violation.violationDetails?.policyViolationId;

    const isStandaloneFirewall = selectIsStandaloneFirewall(state);

    const endpointUrl = deleteWaiverUrl(ownerType, ownerId, waiverId);

    return axios
      .delete(endpointUrl)
      .then(() => {
        dispatch(deleteWaiverFulfilled());
        const routerName = selectCurrentRouteName(state);
        setTimeout(() => {
          if (routerName === 'waiver.details' || routerName === FIREWALL_WAIVER_DETAILS) {
            dispatch(
              filterDataByIdAndRedirectToNextWaiverOrDashboard(sidebarNavList.data, waiverId, isStandaloneFirewall)
            );
          }
          dispatch(deleteWaiverMaskTimerDone());
        }, SUBMIT_MASK_SUCCESS_VISIBLE_TIME_MS);
        if (routerName === 'transitiveViolations') {
          const currentParams = selectRouterCurrentParams(state);
          const { ownerId = '', scanId = '', hash = '' } = currentParams;
          dispatch(loadTransitiveViolationWaivers(ownerId, scanId, hash));
        } else if (routerName.match(/componentDetailsPage\.(?:security|violations|legal)/)) {
          const hash = selectHash(state);
          const ownerId = selectRepositoryId(state);
          dispatch(loadExistingWaiversData('repository', ownerId, hash));
          dispatch(reloadComponentWaivers ? policyViolationsActions.load() : loadApplicableWaivers(policyViolationId));
        } else {
          dispatch(reloadComponentWaivers ? policyViolationsActions.load() : loadApplicableWaivers(policyViolationId));
        }
      })
      .catch((err) => {
        dispatch(deleteWaiverFailed(Messages.getHttpErrorMessage(err)));
      });
  };
}

export const setShowUnsavedChangesModal = (flag) => {
  return function (dispatch) {
    dispatch(payloadParamActionCreator(WAIVERS_ADD_WAIVER_SET_SHOW_UNSAVED_CHANGES_MODAL)(flag));
  };
};

export function loadSimilarWaivers(id) {
  return function (dispatch) {
    if (!id) {
      return dispatch(loadSimilarWaiversFulfilled({ similarWaivers: [] }));
    }
    dispatch(loadSimilarWaiversRequested());
    return axios
      .get(getSimilarWaiversUrl(id))
      .then(compose(dispatch, loadSimilarWaiversFulfilled, prop('data')))
      .catch(compose(dispatch, loadSimilarWaiversFailed, Messages.getHttpErrorMessage));
  };
}
