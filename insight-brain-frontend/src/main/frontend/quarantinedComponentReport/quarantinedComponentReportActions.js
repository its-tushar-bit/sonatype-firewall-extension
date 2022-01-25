/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import axios from 'axios';

import { noPayloadActionCreator, payloadParamActionCreator } from '../util/reduxUtil';

import { getQuarantinedComponentOverviewUrl, getQuarantinedComponentPolicyViolationsUrl } from '../util/CLMLocation';

export const QUARANTINED_REPORT_LOAD_QUARANTINE_COMPONENT_OVERVIEW_REQUESTED =
  'QUARANTINED_REPORT_LOAD_QUARANTINE_COMPONENT_OVERVIEW_REQUESTED';
export const QUARANTINED_REPORT_LOAD_QUARANTINE_COMPONENT_OVERVIEW_FULFILLED =
  'QUARANTINED_REPORT_LOAD_QUARANTINE_COMPONENT_OVERVIEW_FULFILLED';
export const QUARANTINED_REPORT_LOAD_QUARANTINE_COMPONENT_OVERVIEW_FAILED =
  'QUARANTINED_REPORT_LOAD_QUARANTINE_COMPONENT_OVERVIEW_FAILED';

const loadQuarantineComponentOverviewRequested = noPayloadActionCreator(
  QUARANTINED_REPORT_LOAD_QUARANTINE_COMPONENT_OVERVIEW_REQUESTED
);
const loadQuarantineComponentOverviewFulfilled = payloadParamActionCreator(
  QUARANTINED_REPORT_LOAD_QUARANTINE_COMPONENT_OVERVIEW_FULFILLED
);
const loadQuarantineComponentOverviewFailed = payloadParamActionCreator(
  QUARANTINED_REPORT_LOAD_QUARANTINE_COMPONENT_OVERVIEW_FAILED
);

export const LOAD_POLICY_VIOLATIONS_REQUESTED = 'LOAD_POLICY_VIOLATIONS_REQUESTED';
export const LOAD_POLICY_VIOLATIONS_FULFILLED = 'LOAD_POLICY_VIOLATIONS_FULFILLED';
export const LOAD_POLICY_VIOLATIONS_FAILED = 'LOAD_POLICY_VIOLATIONS_FAILED';

const loadPolicyViolationsRequested = noPayloadActionCreator(LOAD_POLICY_VIOLATIONS_REQUESTED);
const loadPolicyViolationsFulfilled = payloadParamActionCreator(LOAD_POLICY_VIOLATIONS_FULFILLED);
const loadPolicyViolationsFailed = payloadParamActionCreator(LOAD_POLICY_VIOLATIONS_FAILED);

export function loadQuarantineReportData(token) {
  return (dispatch) =>
    Promise.all([dispatch(loadQuarantineComponentOverview(token)), dispatch(loadPolicyViolations(token))]);
}

export function loadQuarantineComponentOverview(token) {
  return function (dispatch) {
    dispatch(loadQuarantineComponentOverviewRequested());
    return axios
      .get(getQuarantinedComponentOverviewUrl(token))
      .then(({ data }) => {
        dispatch(loadQuarantineComponentOverviewFulfilled(data));
      })
      .catch((error) => {
        dispatch(loadQuarantineComponentOverviewFailed(error));
      });
  };
}

export function loadPolicyViolations(token) {
  return function (dispatch) {
    dispatch(loadPolicyViolationsRequested());
    return axios
      .get(getQuarantinedComponentPolicyViolationsUrl(token))
      .then(({ data }) => {
        dispatch(loadPolicyViolationsFulfilled(data));
      })
      .catch((error) => {
        dispatch(loadPolicyViolationsFailed(error));
      });
  };
}
