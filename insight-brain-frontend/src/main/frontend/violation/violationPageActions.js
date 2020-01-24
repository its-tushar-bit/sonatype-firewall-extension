/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import axios from 'axios';

import { noPayloadActionCreator, payloadParamActionCreator } from '../util/reduxUtil';
import { getViolationDetailsUrl } from '../util/CLMLocation';

export const LOAD_VIOLATION_REQUESTED = 'LOAD_VIOLATION_REQUESTED';
export const LOAD_VIOLATION_FULFILLED = 'LOAD_VIOLATION_FULFILLED';
export const LOAD_VIOLATION_FAILED = 'LOAD_VIOLATION_FAILED';

export function loadViolation(id) {
  return function(dispatch) {
    dispatch(loadViolationRequested());

    return axios.get(getViolationDetailsUrl(id))
        .then(({ data }) => dispatch(loadViolationFulfilled(data)))
        .catch(err => dispatch(loadViolationFailed(err)));
  };
}

const loadViolationRequested = noPayloadActionCreator(LOAD_VIOLATION_REQUESTED);
const loadViolationFulfilled = payloadParamActionCreator(LOAD_VIOLATION_FULFILLED);
const loadViolationFailed = payloadParamActionCreator(LOAD_VIOLATION_FAILED);
