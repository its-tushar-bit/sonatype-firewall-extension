/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import axios from 'axios';
import { both, complement, compose, find, isNil, prop, propEq, propSatisfies } from 'ramda';

import { noPayloadActionCreator, payloadParamActionCreator } from '../util/reduxUtil';
import {
  getViolationDetailsUrl,
  getVulnerabilityJsonDetailUrl,
  getApplicableWaiversUrl,
  getRepositoryPolicyViolationUrl,
  getApplicationSummaryUrl,
  getApplicableAutoWaiverUrl,
} from '../util/CLMLocation';
import { extractSecurityVulnerabilityRefId, getMostRecentScanId } from './violationSelectors';
import { loadEvidence } from './ReachabilityEvidence/reachabilityEvidenceSlice';
import { isNilOrEmpty } from '../util/jsUtil';
import { convertToWaiverViolationFormat } from '../util/waiverUtils';
import { selectComponentViolations } from '../componentDetails/ViolationsTableTile/PolicyViolationsSelectors';
import { loadPermissionForAppWaivers } from 'MainRoot/waivers/waiverActions';
import {
  selectPrevRepositoryPolicyId,
  selectRepositoryId,
  selectRepositoryPolicyId,
} from 'MainRoot/reduxUiRouter/routerSelectors';
import {
  selectIsFirewallOrRepositoryAndNotProxyStage,
  selectSelectedComponent,
} from 'MainRoot/applicationReport/applicationReportSelectors';
import { checkPermissions } from 'MainRoot/util/authorizationUtil';

export const VIOLATION_RESET_VIOLATION_DETAILS_REQUESTED = 'VIOLATION_RESET_VIOLATION_DETAILS_REQUESTED';
export const VIOLATION_LOAD_VIOLATION_DETAILS_REQUESTED = 'VIOLATION_LOAD_VIOLATION_DETAILS_REQUESTED';
export const VIOLATION_LOAD_VIOLATION_DETAILS_FULFILLED = 'VIOLATION_LOAD_VIOLATION_DETAILS_FULFILLED';
export const VIOLATION_LOAD_VIOLATION_DETAILS_FAILED = 'VIOLATION_LOAD_VIOLATION_DETAILS_FAILED';

export const VIOLATION_FETCH_CROSS_STAGE_VIOLATION_FULFILLED = 'VIOLATION_FETCH_CROSS_STAGE_VIOLATION_FULFILLED';
export const VIOLATION_FETCH_APPLICABLE_WAIVERS_FULFILLED = 'VIOLATION_FETCH_APPLICABLE_WAIVERS_FULFILLED';
export const VIOLATION_FETCH_APPLICABLE_WAIVERS_REQUESTED = 'VIOLATION_FETCH_APPLICABLE_WAIVERS_REQUESTED';
export const VIOLATION_FETCH_APPLICABLE_WAIVERS_FAILED = 'VIOLATION_FETCH_APPLICABLE_WAIVERS_FAILED';

export const VIOLATION_FETCH_APPLICABLE_AUTO_WAIVER_REQUESTED = 'VIOLATION_FETCH_APPLICABLE_AUTO_WAIVER_REQUESTED';
export const VIOLATION_FETCH_APPLICABLE_AUTO_WAIVER_FULFILLED = 'VIOLATION_FETCH_APPLICABLE_AUTO_WAIVER_FULFILLED';
export const VIOLATION_FETCH_APPLICABLE_AUTO_WAIVER_FAILED = 'VIOLATION_FETCH_APPLICABLE_AUTO_WAIVER_FAILED';

export const VIOLATION_LOAD_VULNERABILITY_DETAILS_REQUESTED = 'VIOLATION_LOAD_VULNERABILITY_DETAILS_REQUESTED';
export const VIOLATION_LOAD_VULNERABILITY_DETAILS_FULFILLED = 'VIOLATION_LOAD_VULNERABILITY_DETAILS_FULFILLED';
export const VIOLATION_LOAD_VULNERABILITY_DETAILS_FAILED = 'VIOLATION_LOAD_VULNERABILITY_DETAILS_FAILED';

export const VIOLATION_SORT_SIMILAR_WAIVERS = 'VIOLATION_SORT_SIMILAR_WAIVERS';
export const VIOLATION_SET_FILTER_IDS_SIMILAR_WAIVERS = 'VIOLATION_SET_FILTER_IDS_SIMILAR_WAIVERS';

const loadApplicableWaiversRequested = payloadParamActionCreator(VIOLATION_FETCH_APPLICABLE_WAIVERS_REQUESTED);
const loadApplicableWaiversFulfilled = payloadParamActionCreator(VIOLATION_FETCH_APPLICABLE_WAIVERS_FULFILLED);
const loadApplicableWaiversFailed = payloadParamActionCreator(VIOLATION_FETCH_APPLICABLE_WAIVERS_FAILED);

const loadAutoWaiverRequested = payloadParamActionCreator(VIOLATION_FETCH_APPLICABLE_AUTO_WAIVER_REQUESTED);
const loadAutoWaiverFulfilled = payloadParamActionCreator(VIOLATION_FETCH_APPLICABLE_AUTO_WAIVER_FULFILLED);
const loadAutoWaiverFailed = payloadParamActionCreator(VIOLATION_FETCH_APPLICABLE_AUTO_WAIVER_FAILED);

export function loadViolation(id) {
  return function (dispatch, getState) {
    dispatch(loadViolationDetailsRequested());

    const parallelRequests = [
      dispatch(fetchCrossStageViolation(id)),
      dispatch(loadApplicableWaivers(id)),
      dispatch(loadApplicableAutoWaiver(id)),
    ];

    return Promise.all(parallelRequests)
      .then(() => {
        // HRC violations have no owning application, so there is no application-scoped
        // waiver permission to fetch; skip the /applications/{publicId}/summary call to
        // avoid a 404 that would otherwise mask the successful violation load.
        const { applicationPublicId } = getState().violation.violationDetails || {};
        return applicationPublicId ? loadPermissionForAppWaivers(applicationPublicId) : false;
      })
      .then(compose(dispatch, loadViolationDetailsFulfilled))
      .then(() => {
        dispatch(loadReachabilityEvidence());
        return dispatch(loadVulnerabilityDetails());
      })
      .catch(compose(dispatch, loadViolationDetailsFailed));
  };
}

export function loadApplicableWaivers(id) {
  return function (dispatch) {
    dispatch(loadApplicableWaiversRequested());
    return axios
      .get(getApplicableWaiversUrl(id))
      .then(compose(dispatch, loadApplicableWaiversFulfilled, prop('data')))
      .catch(compose(dispatch, loadApplicableWaiversFailed));
  };
}

export function loadApplicableAutoWaiver(id) {
  return function (dispatch) {
    dispatch(loadAutoWaiverRequested());
    return axios
      .get(getApplicableAutoWaiverUrl(id))
      .then(compose(dispatch, loadAutoWaiverFulfilled, prop('data')))
      .catch(compose(dispatch, loadAutoWaiverFailed));
  };
}
export function setFilterIdsSimilarWaivers(selectedIds) {
  return function (dispatch) {
    return dispatch(payloadParamActionCreator(VIOLATION_SET_FILTER_IDS_SIMILAR_WAIVERS)(selectedIds));
  };
}

/**
 * @param id violationId
 * @returns Promise resolving to violationDetails or rejecting to Error
 */
export function fetchCrossStageViolation(id) {
  return function (dispatch, getState) {
    const state = getState();
    const { showManageWaiverPage } = state.firewall.componentDetailsPage;
    if (!showManageWaiverPage) {
      const { selectedViolationId, violationDetails } = state.violation;
      if (selectedViolationId === id) {
        return Promise.resolve(violationDetails);
      }
    }

    const { violationDetails } = state.firewall.componentDetailsPage;
    const { policyViolationId } = violationDetails;
    if (policyViolationId === id) {
      return Promise.resolve(violationDetails);
    }

    const isFirewallOrRepositoryAndNotContainer = selectIsFirewallOrRepositoryAndNotProxyStage(state);

    const repositoryId = isFirewallOrRepositoryAndNotContainer
      ? selectRepositoryId(state)
      : selectRepositoryPolicyId(state);

    const getDataUrl = isFirewallOrRepositoryAndNotContainer
      ? getRepositoryPolicyViolationUrl(repositoryId, id)
      : getViolationDetailsUrl(id);

    return axios.get(getDataUrl).then(({ data }) => {
      const violationData = isFirewallOrRepositoryAndNotContainer ? convertToWaiverViolationFormat(data) : data;
      const violations = selectComponentViolations(state);
      const waived = violations
        ? prop('waived', find(propEq('policyViolationId', violationData.policyViolationId), violations))
        : true;
      return dispatch(
        payloadParamActionCreator(VIOLATION_FETCH_CROSS_STAGE_VIOLATION_FULFILLED)({
          violationDetails: { ...violationData, waived },
          selectedViolationId: id,
        })
      );
    });
  };
}

export function fetchCrossStageViolationAddWaiver(id) {
  return function (dispatch, getState) {
    const state = getState();

    const isFirewallOrRepositoryAndNotContainer = selectIsFirewallOrRepositoryAndNotProxyStage(state);

    const repositoryId = isFirewallOrRepositoryAndNotContainer
      ? selectRepositoryId(state)
      : selectPrevRepositoryPolicyId(state);

    return axios.get(getRepositoryPolicyViolationUrl(repositoryId, id)).then(({ data }) => {
      const violationData = convertToWaiverViolationFormat(data);
      const violations = selectComponentViolations(state);
      const waived = violations
        ? prop('waived', find(propEq('policyViolationId', violationData.policyViolationId), violations))
        : true;
      return dispatch(
        payloadParamActionCreator(VIOLATION_FETCH_CROSS_STAGE_VIOLATION_FULFILLED)({
          violationDetails: { ...violationData, waived },
          selectedViolationId: id,
        })
      );
    });
  };
}

export const resetViolationDetails = noPayloadActionCreator(VIOLATION_RESET_VIOLATION_DETAILS_REQUESTED);
const loadViolationDetailsRequested = noPayloadActionCreator(VIOLATION_LOAD_VIOLATION_DETAILS_REQUESTED);
const loadViolationDetailsFulfilled = payloadParamActionCreator(VIOLATION_LOAD_VIOLATION_DETAILS_FULFILLED);
const loadViolationDetailsFailed = payloadParamActionCreator(VIOLATION_LOAD_VIOLATION_DETAILS_FAILED);

const isNotNil = complement(isNil),
  isSecurityReference = both(isNotNil, propEq('type', 'SECURITY_VULNERABILITY_REFID')),
  hasSecurityReference = propSatisfies(isSecurityReference, 'reference');

export function loadVulnerabilityDetails() {
  return function (dispatch, getState) {
    const component = selectSelectedComponent(getState());
    const identificationSource = component?.identificationSource;
    const {
        violation: { violationDetails },
        router: {
          currentParams: { publicId, scanId },
        },
      } = getState(),
      { constraintViolations, componentIdentifier } = violationDetails;

    if (isNilOrEmpty(constraintViolations) || isNilOrEmpty(constraintViolations[0].reasons)) {
      return Promise.resolve();
    }

    const reasonWithRefId = find(hasSecurityReference, constraintViolations[0].reasons);

    if (reasonWithRefId) {
      dispatch(loadVulnerabilityDetailsRequested());
      const refId = reasonWithRefId.reference.value;

      const isFirewallOrRepositoryAndNotContainer = selectIsFirewallOrRepositoryAndNotProxyStage(getState());

      const extraQueryParameters = isFirewallOrRepositoryAndNotContainer
        ? null
        : { ownerType: 'application', ownerId: publicId, scanId: scanId, identificationSource: identificationSource };

      return axios
        .get(getVulnerabilityJsonDetailUrl(refId, componentIdentifier, extraQueryParameters))
        .then(({ data }) => {
          if (!isFirewallOrRepositoryAndNotContainer) {
            return checkEditIqPermission(publicId)
              .then(() => dispatch(loadVulnerabilityDetailsFulfilled({ ...data, hasEditIqPermission: true })))
              .catch(() => dispatch(loadVulnerabilityDetailsFulfilled(data)));
          }
          return dispatch(loadVulnerabilityDetailsFulfilled(data));
        })
        .catch((err) => dispatch(loadVulnerabilityDetailsFailed(err)));
    } else {
      return Promise.resolve();
    }
  };
}

export function loadFirewallPolicyVulnerabilityDetails(conditionTriggerReferenceValue, componentIdentifierParam) {
  return function (dispatch, getState) {
    const state = getState();
    const routerComponentIdentifier = state.router.currentParams.componentIdentifier;
    const componentIdentifier =
      componentIdentifierParam ?? (routerComponentIdentifier ? JSON.parse(routerComponentIdentifier) : null);
    if (!componentIdentifier) {
      return Promise.resolve();
    }
    return axios
      .get(getVulnerabilityJsonDetailUrl(conditionTriggerReferenceValue, componentIdentifier))
      .then(({ data }) => dispatch(loadVulnerabilityDetailsFulfilled(data)))
      .catch((err) => dispatch(loadVulnerabilityDetailsFailed(err)));
  };
}

function checkEditIqPermission(applicationPublicId) {
  return axios.get(getApplicationSummaryUrl(applicationPublicId)).then(({ data }) => {
    return checkPermissions(['WRITE'], 'application', data.id);
  });
}

const loadVulnerabilityDetailsRequested = noPayloadActionCreator(VIOLATION_LOAD_VULNERABILITY_DETAILS_REQUESTED);
const loadVulnerabilityDetailsFulfilled = payloadParamActionCreator(VIOLATION_LOAD_VULNERABILITY_DETAILS_FULFILLED);
const loadVulnerabilityDetailsFailed = payloadParamActionCreator(VIOLATION_LOAD_VULNERABILITY_DETAILS_FAILED);

/**
 * Loads reachability evidence if the current violation is REACHABLE and has a
 * security vulnerability reference. Dispatched as part of the violation page load.
 * Evidence failure is non-blocking — it does not affect the main page load.
 */
export function loadReachabilityEvidence() {
  return function (dispatch, getState) {
    const state = getState();
    const { violationDetails } = state.violation;

    if (!violationDetails || violationDetails.reachabilityStatus !== 'REACHABLE') {
      return;
    }

    // HRC v1: the reachability-evidence endpoint (getReachabilityEvidenceUrl) is app-scoped
    // and has no HRC counterpart yet — a native HRC endpoint is deferred to Epic 2. On the
    // HRC route applicationPublicId is null, so this used to silently fall through the
    // `!applicationPublicId` guard below with no user-facing signal. Log the skip once
    // (debug only, filtered in CI) so the deferral is traceable in DevTools.
    const { hrcId } = state.router.currentParams;
    if (hrcId) {
      // eslint-disable-next-line no-console
      console.debug(
        'Reachability evidence skipped for HRC violation (no HRC endpoint yet; deferred to Epic 2). hrcId=%s',
        hrcId
      );
      return;
    }

    const { applicationPublicId, constraintViolations, stageData } = violationDetails;
    const vulnerabilityId = extractSecurityVulnerabilityRefId(constraintViolations);
    if (!vulnerabilityId || !applicationPublicId) {
      return;
    }

    const { scanId } = state.router.currentParams;
    const reportId = scanId || getMostRecentScanId(stageData);
    if (!reportId) {
      return;
    }

    dispatch(loadEvidence({ applicationPublicId, reportId, vulnerabilityId }));
  };
}
