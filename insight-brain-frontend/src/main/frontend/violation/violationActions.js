/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import axios from 'axios';
import { both, complement, compose, find, isNil, prop, propEq, propSatisfies } from 'ramda';

import { noPayloadActionCreator, payloadParamActionCreator } from '../util/reduxUtil';
import { getViolationDetailsUrl, getVulnerabilityJsonDetailUrl, getApplicableWaiversUrl } from '../util/CLMLocation';
import { isNilOrEmpty } from '../util/jsUtil';
import { selectComponentViolations } from '../componentDetails/ViolationsTableTile/PolicyViolationsSelectors';

export const VIOLATION_LOAD_VIOLATION_DETAILS_REQUESTED = 'VIOLATION_LOAD_VIOLATION_DETAILS_REQUESTED';
export const VIOLATION_LOAD_VIOLATION_DETAILS_FULFILLED = 'VIOLATION_LOAD_VIOLATION_DETAILS_FULFILLED';
export const VIOLATION_LOAD_VIOLATION_DETAILS_FAILED = 'VIOLATION_LOAD_VIOLATION_DETAILS_FAILED';

export const VIOLATION_FETCH_CROSS_STAGE_VIOLATION_FULFILLED = 'VIOLATION_FETCH_CROSS_STAGE_VIOLATION_FULFILLED';
export const VIOLATION_FETCH_APPLICABLE_WAIVERS_FULFILLED = 'VIOLATION_FETCH_APPLICABLE_WAIVERS_FULFILLED';

export const VIOLATION_LOAD_VULNERABILITY_DETAILS_REQUESTED = 'VIOLATION_LOAD_VULNERABILITY_DETAILS_REQUESTED';
export const VIOLATION_LOAD_VULNERABILITY_DETAILS_FULFILLED = 'VIOLATION_LOAD_VULNERABILITY_DETAILS_FULFILLED';
export const VIOLATION_LOAD_VULNERABILITY_DETAILS_FAILED = 'VIOLATION_LOAD_VULNERABILITY_DETAILS_FAILED';

export function loadViolation(id) {
  return function (dispatch) {
    dispatch(loadViolationDetailsRequested());

    const parallelRequests = [dispatch(fetchCrossStageViolation(id)), dispatch(fetchApplicableWaivers(id))];

    return Promise.all(parallelRequests)
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
    const { violationDetails, selectedViolationId } = state.violation;

    if (selectedViolationId === id) {
      return Promise.resolve(violationDetails);
    }

    return axios.get(getViolationDetailsUrl(id)).then(({ data }) => {
      const violations = selectComponentViolations(state);
      const waived = violations
        ? prop('waived', find(propEq('policyViolationId', data.policyViolationId), violations))
        : true;
      return dispatch(
        payloadParamActionCreator(VIOLATION_FETCH_CROSS_STAGE_VIOLATION_FULFILLED)({
          violationDetails: { ...data, waived },
          selectedViolationId: id,
        })
      );
    });
  };
}

const loadViolationDetailsRequested = noPayloadActionCreator(VIOLATION_LOAD_VIOLATION_DETAILS_REQUESTED);
const loadViolationDetailsFulfilled = noPayloadActionCreator(VIOLATION_LOAD_VIOLATION_DETAILS_FULFILLED);
const loadViolationDetailsFailed = payloadParamActionCreator(VIOLATION_LOAD_VIOLATION_DETAILS_FAILED);

const isNotNil = complement(isNil),
  isSecurityReference = both(isNotNil, propEq('type', 'SECURITY_VULNERABILITY_REFID')),
  hasSecurityReference = propSatisfies(isSecurityReference, 'reference');

export function loadVulnerabilityDetails() {
  return function (dispatch, getState) {
    const {
        violation: { violationDetails },
      } = getState(),
      { constraintViolations, componentIdentifier } = violationDetails;

    if (isNilOrEmpty(constraintViolations) || isNilOrEmpty(constraintViolations[0].reasons)) {
      return Promise.resolve();
    }

    const reasonWithRefId = find(hasSecurityReference, constraintViolations[0].reasons);

    if (reasonWithRefId) {
      dispatch(loadVulnerabilityDetailsRequested());
      const refId = reasonWithRefId.reference.value;
      return axios
        .get(getVulnerabilityJsonDetailUrl(refId, componentIdentifier))
        .then(({ data }) => dispatch(loadVulnerabilityDetailsFulfilled(data)))
        .catch((err) => dispatch(loadVulnerabilityDetailsFailed(err)));
    } else {
      return Promise.resolve();
    }
  };
}

const loadVulnerabilityDetailsRequested = noPayloadActionCreator(VIOLATION_LOAD_VULNERABILITY_DETAILS_REQUESTED);
const loadVulnerabilityDetailsFulfilled = payloadParamActionCreator(VIOLATION_LOAD_VULNERABILITY_DETAILS_FULFILLED);
const loadVulnerabilityDetailsFailed = payloadParamActionCreator(VIOLATION_LOAD_VULNERABILITY_DETAILS_FAILED);
