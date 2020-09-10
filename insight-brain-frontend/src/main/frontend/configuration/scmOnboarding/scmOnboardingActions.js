/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

import {noPayloadActionCreator, payloadParamActionCreator} from '../../util/reduxUtil';
import axios from 'axios';
import {getManifestScanConfigUrl} from '../../util/CLMLocation';

export const SCM_ONBOARDING_LOAD_REQUESTED = 'SCM_ONBOARDING_LOAD_REQUESTED';
export const SCM_ONBOARDING_LOAD_FULFILLED = 'SCM_ONBOARDING_LOAD_FULFILLED';
export const SCM_ONBOARDING_LOAD_FAILED = 'SCM_ONBOARDING_LOAD_FAILED';

export function load() {
  return function(dispatch) {
    dispatch(loadRequested());

    return axios.get(getManifestScanConfigUrl())
        .then(({ data }) => { dispatch(loadFulfilled(data)); })
        .catch(error => { dispatch(loadFailed(error)); });
  };
}

const loadRequested = noPayloadActionCreator(SCM_ONBOARDING_LOAD_REQUESTED);
const loadFulfilled = payloadParamActionCreator(SCM_ONBOARDING_LOAD_FULFILLED);
const loadFailed = payloadParamActionCreator(SCM_ONBOARDING_LOAD_FAILED);
