/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import axios from 'axios';
import { both, complement, find, isNil, propEq, propSatisfies } from 'ramda';

import { noPayloadActionCreator, payloadParamActionCreator } from '../util/reduxUtil';
import {
  getViolationDetailsUrl,
  getVulnerabilityJsonDetailUrl,
  getApplicableWaiversUrl
} from '../util/CLMLocation';
import { isNilOrEmpty } from '../util/jsUtil';

export const LOAD_VIOLATION_REQUESTED = 'LOAD_VIOLATION_REQUESTED';
export const LOAD_VIOLATION_FULFILLED = 'LOAD_VIOLATION_FULFILLED';
export const LOAD_VIOLATION_FAILED = 'LOAD_VIOLATION_FAILED';

export const LOAD_VULNERABILITY_DETAILS_REQUESTED = 'LOAD_VULNERABILITY_DETAILS_REQUESTED';
export const LOAD_VULNERABILITY_DETAILS_FULFILLED = 'LOAD_VULNERABILITY_DETAILS_FULFILLED';
export const LOAD_VULNERABILITY_DETAILS_FAILED = 'LOAD_VULNERABILITY_DETAILS_FAILED';

export const VIOLATION_LOAD_APPLICABLE_WAIVERS_REQUESTED = 'VIOLATION_LOAD_APPLICABLE_WAIVERS_REQUESTED';
export const VIOLATION_LOAD_APPLICABLE_WAIVERS_FULFILLED = 'VIOLATION_LOAD_APPLICABLE_WAIVERS_FULFILLED';
export const VIOLATION_LOAD_APPLICABLE_WAIVERS_FAILED = 'VIOLATION_LOAD_APPLICABLE_WAIVERS_FAILED';

export function loadViolation(id) {
  return function(dispatch, getState) {

    const { violationPage } = getState(),
        { violationDetails, selectedViolationId } = violationPage;

    // avoid requesting an already loaded violation but request waivers every time as they may have changed
    const violationDetailsRequest = id === selectedViolationId
      ? Promise.resolve({ data: violationDetails })
      : axios.get(getViolationDetailsUrl(id));

    dispatch(loadViolationRequested());

    const parallelRequests = [
      violationDetailsRequest,
      axios.get(getApplicableWaiversUrl(id))
    ];

    return axios.all(parallelRequests)
        .then(([violationDetailsResponse, applicableWaiversResponse]) => {
          return dispatch(loadViolationFulfilled({
            violationDetails: violationDetailsResponse.data,
            applicableWaivers: applicableWaiversResponse.data,
            selectedViolationId: id
          }));
        })
        .then(() => dispatch(loadVulnerabilityDetails()))
        .catch(err => {
          dispatch(loadViolationFailed(err));
          return Promise.reject(err);
        });
  };
}

const loadViolationRequested = noPayloadActionCreator(LOAD_VIOLATION_REQUESTED);
const loadViolationFulfilled = payloadParamActionCreator(LOAD_VIOLATION_FULFILLED);
const loadViolationFailed = payloadParamActionCreator(LOAD_VIOLATION_FAILED);

const isNotNil = complement(isNil),
    isSecurityReference = both(isNotNil, propEq('type', 'SECURITY_VULNERABILITY_REFID')),
    hasSecurityReference = propSatisfies(isSecurityReference, 'reference');

export function loadVulnerabilityDetails() {
  return function(dispatch, getState) {
    const { violationPage: { violationDetails } } = getState(),
        { constraintViolations, componentIdentifier } = violationDetails;

    if (isNilOrEmpty(constraintViolations) || isNilOrEmpty(constraintViolations[0].reasons)) {
      return Promise.resolve();
    }

    const reasonWithRefId = find(hasSecurityReference, constraintViolations[0].reasons);

    if (reasonWithRefId) {
      dispatch(loadVulnerabilityDetailsRequested());
      const refId = reasonWithRefId.reference.value;
      return axios.get(getVulnerabilityJsonDetailUrl(refId, componentIdentifier))
          .then(({ data }) => dispatch(loadVulnerabilityDetailsFulfilled(data)))
          .catch(err => dispatch(loadVulnerabilityDetailsFailed(err)));
    }
    else {
      return Promise.resolve();
    }
  };
}

const loadVulnerabilityDetailsRequested = noPayloadActionCreator(LOAD_VULNERABILITY_DETAILS_REQUESTED);
const loadVulnerabilityDetailsFulfilled = payloadParamActionCreator(LOAD_VULNERABILITY_DETAILS_FULFILLED);
const loadVulnerabilityDetailsFailed = payloadParamActionCreator(LOAD_VULNERABILITY_DETAILS_FAILED);

const loadApplicableWaiversRequested = noPayloadActionCreator(VIOLATION_LOAD_APPLICABLE_WAIVERS_REQUESTED);
const loadApplicableWaiversFulfilled = payloadParamActionCreator(VIOLATION_LOAD_APPLICABLE_WAIVERS_FULFILLED);
const loadApplicableWaiversFailed = payloadParamActionCreator(VIOLATION_LOAD_APPLICABLE_WAIVERS_FAILED);

export function loadApplicableWaivers(policyViolationId) {
  return function(dispatch) {
    dispatch(loadApplicableWaiversRequested());
    return axios.get(getApplicableWaiversUrl(policyViolationId))
        .then(({ data }) => {
          return dispatch(loadApplicableWaiversFulfilled(data));
        })
        .catch((err) => {
          dispatch(loadApplicableWaiversFailed(err));
        });
  };
}
