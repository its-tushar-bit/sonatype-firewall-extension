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
} from '../util/CLMLocation';
import { isNilOrEmpty } from '../util/jsUtil';
import { convertToWaiverViolationFormat } from '../util/waiverUtils';
import { selectComponentViolations } from '../componentDetails/ViolationsTableTile/PolicyViolationsSelectors';
import { loadPermissionForAppWaivers } from 'MainRoot/waivers/waiverActions';
import {
  selectIsFirewallOrRepository,
  selectPrevRepositoryPolicyId,
  selectRepositoryId,
  selectRepositoryPolicyId,
} from 'MainRoot/reduxUiRouter/routerSelectors';

export const VIOLATION_RESET_VIOLATION_DETAILS_REQUESTED = 'VIOLATION_RESET_VIOLATION_DETAILS_REQUESTED';
export const VIOLATION_LOAD_VIOLATION_DETAILS_REQUESTED = 'VIOLATION_LOAD_VIOLATION_DETAILS_REQUESTED';
export const VIOLATION_LOAD_VIOLATION_DETAILS_FULFILLED = 'VIOLATION_LOAD_VIOLATION_DETAILS_FULFILLED';
export const VIOLATION_LOAD_VIOLATION_DETAILS_FAILED = 'VIOLATION_LOAD_VIOLATION_DETAILS_FAILED';

export const VIOLATION_FETCH_CROSS_STAGE_VIOLATION_FULFILLED = 'VIOLATION_FETCH_CROSS_STAGE_VIOLATION_FULFILLED';
export const VIOLATION_FETCH_APPLICABLE_WAIVERS_FULFILLED = 'VIOLATION_FETCH_APPLICABLE_WAIVERS_FULFILLED';

export const VIOLATION_LOAD_VULNERABILITY_DETAILS_REQUESTED = 'VIOLATION_LOAD_VULNERABILITY_DETAILS_REQUESTED';
export const VIOLATION_LOAD_VULNERABILITY_DETAILS_FULFILLED = 'VIOLATION_LOAD_VULNERABILITY_DETAILS_FULFILLED';
export const VIOLATION_LOAD_VULNERABILITY_DETAILS_FAILED = 'VIOLATION_LOAD_VULNERABILITY_DETAILS_FAILED';

export function loadViolation(id) {
  return function (dispatch, getState) {
    dispatch(loadViolationDetailsRequested());

    const parallelRequests = [dispatch(fetchCrossStageViolation(id)), dispatch(fetchApplicableWaivers(id))];

    return Promise.all(parallelRequests)
      .then(() => loadPermissionForAppWaivers(getState().violation.violationDetails.applicationPublicId))
      .then(compose(dispatch, loadViolationDetailsFulfilled))
      .then(compose(dispatch, loadVulnerabilityDetails))
      .catch(compose(dispatch, loadViolationDetailsFailed));
  };
}

export function fetchApplicableWaivers(id) {
  return function (dispatch) {
    return axios
      .get(getApplicableWaiversUrl(id))
      .then(compose(dispatch, payloadParamActionCreator(VIOLATION_FETCH_APPLICABLE_WAIVERS_FULFILLED), prop('data')));
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

    const repositoryId = selectIsFirewallOrRepository(state)
      ? selectRepositoryId(state)
      : selectRepositoryPolicyId(state);

    const getDataUrl = selectIsFirewallOrRepository(state)
      ? getRepositoryPolicyViolationUrl(repositoryId, id)
      : getViolationDetailsUrl(id);

    return axios.get(getDataUrl).then(({ data }) => {
      const violationData = selectIsFirewallOrRepository(state) ? convertToWaiverViolationFormat(data) : data;
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

    const repositoryId = selectIsFirewallOrRepository(state)
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
    const {
        violation: { violationDetails },
        router: {
          currentParams: { publicId },
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
      const extraQueryParameters = {
        ownerType: 'application',
        ownerId: publicId,
      };

      return axios
        .get(getVulnerabilityJsonDetailUrl(refId, componentIdentifier, extraQueryParameters))
        .then(({ data }) => dispatch(loadVulnerabilityDetailsFulfilled(data)))
        .catch((err) => dispatch(loadVulnerabilityDetailsFailed(err)));
    } else {
      return Promise.resolve();
    }
  };
}

export function loadFirewallPolicyVulnerabilityDetails(conditionTriggerReferenceValue) {
  return function (dispatch, getState) {
    const state = getState();
    const componentIdentifier = JSON.parse(state.router.currentParams.componentIdentifier);
    return axios
      .get(getVulnerabilityJsonDetailUrl(conditionTriggerReferenceValue, componentIdentifier))
      .then(({ data }) => dispatch(loadVulnerabilityDetailsFulfilled(data)))
      .catch((err) => dispatch(loadVulnerabilityDetailsFailed(err)));
  };
}

const loadVulnerabilityDetailsRequested = noPayloadActionCreator(VIOLATION_LOAD_VULNERABILITY_DETAILS_REQUESTED);
const loadVulnerabilityDetailsFulfilled = payloadParamActionCreator(VIOLATION_LOAD_VULNERABILITY_DETAILS_FULFILLED);
const loadVulnerabilityDetailsFailed = payloadParamActionCreator(VIOLATION_LOAD_VULNERABILITY_DETAILS_FAILED);
