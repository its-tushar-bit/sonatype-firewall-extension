/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import {createReducerFromActionMap} from '../../util/reduxUtil';
import {
  SCM_ONBOARDING_LOAD_FAILED,
  SCM_ONBOARDING_LOAD_FULFILLED,
  SCM_ONBOARDING_LOAD_REQUESTED
} from './scmOnboardingActions';
import {Messages} from '../../util/CommonServices';

const initialState = {
  loading: false,
  isManifestScanFeatureEnabled: false
};

function loadRequested() {
  return {
    ...initialState,
    loading: true
  };
}

function loadFulfilled(payload, state) {
  return {
    ...state,
    isManifestScanFeatureEnabled: payload.manifestScanFeatureEnabled,
    loading: false
  };
}

function loadFailed(payload) {
  return {
    ...initialState,
    loading: false,
    error: payload.response && payload.response.status === 404 ? null : Messages.getHttpErrorMessage(payload)
  };
}

const reducerActionMap = {
  [SCM_ONBOARDING_LOAD_REQUESTED]: loadRequested,
  [SCM_ONBOARDING_LOAD_FULFILLED]: loadFulfilled,
  [SCM_ONBOARDING_LOAD_FAILED]: loadFailed
};

const reducer = createReducerFromActionMap(reducerActionMap, initialState);
export default reducer;
