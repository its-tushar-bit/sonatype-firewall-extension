/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import axios from 'axios';
import { both, complement, find, isNil, propEq, propSatisfies } from 'ramda';

import { noPayloadActionCreator, payloadParamActionCreator } from '../util/reduxUtil';
import { getViolationDetailsUrl, getVulnerabilityJsonDetailUrl } from '../util/CLMLocation';
import { isNilOrEmpty } from '../util/jsUtil';

export const LOAD_VIOLATION_REQUESTED = 'LOAD_VIOLATION_REQUESTED';
export const LOAD_VIOLATION_FULFILLED = 'LOAD_VIOLATION_FULFILLED';
export const LOAD_VIOLATION_FAILED = 'LOAD_VIOLATION_FAILED';

export const LOAD_VULNERABILITY_DETAILS_REQUESTED = 'LOAD_VULNERABILITY_DETAILS_REQUESTED';
export const LOAD_VULNERABILITY_DETAILS_FULFILLED = 'LOAD_VULNERABILITY_DETAILS_FULFILLED';
export const LOAD_VULNERABILITY_DETAILS_FAILED = 'LOAD_VULNERABILITY_DETAILS_FAILED';

export function loadViolation(id) {
  return function(dispatch) {
    dispatch(loadViolationRequested());

    return axios.get(getViolationDetailsUrl(id))
        .then(({ data }) => dispatch(loadViolationFulfilled(data)))
        .then(({ payload }) => dispatch(loadVulnerabilityDetails(payload)))
        .catch(err => dispatch(loadViolationFailed(err)));
  };
}

const loadViolationRequested = noPayloadActionCreator(LOAD_VIOLATION_REQUESTED);
const loadViolationFulfilled = payloadParamActionCreator(LOAD_VIOLATION_FULFILLED);
const loadViolationFailed = payloadParamActionCreator(LOAD_VIOLATION_FAILED);

const isNotNil = complement(isNil),
    isSecurityReference = both(isNotNil, propEq('type', 'SECURITY_VULNERABILITY_REFID')),
    hasSecurityReference = propSatisfies(isSecurityReference, 'reference');

function loadVulnerabilityDetails(violationDetails) {
  return function(dispatch) {
    const { constraintViolations, componentIdentifier } = violationDetails;

    if (isNilOrEmpty(constraintViolations) || isNilOrEmpty(constraintViolations[0].reasons)) {
      return;
    }

    const reasonWithRefId = find(hasSecurityReference, constraintViolations[0].reasons);

    if (reasonWithRefId) {
      dispatch(loadVulnerabilityDetailsRequested());
      const refId = reasonWithRefId.reference.value;
      return axios.get(getVulnerabilityJsonDetailUrl(refId, componentIdentifier))
          .then(({ data }) => dispatch(loadVulnerabilityDetailsFulfilled(data)))
          .catch(err => dispatch(loadVulnerabilityDetailsFailed(err)));
    }
  };
}

const loadVulnerabilityDetailsRequested = noPayloadActionCreator(LOAD_VULNERABILITY_DETAILS_REQUESTED);
const loadVulnerabilityDetailsFulfilled = payloadParamActionCreator(LOAD_VULNERABILITY_DETAILS_FULFILLED);
const loadVulnerabilityDetailsFailed = payloadParamActionCreator(LOAD_VULNERABILITY_DETAILS_FAILED);
