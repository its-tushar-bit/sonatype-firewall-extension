/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { createReducerFromActionMap } from '../../util/reduxUtil';
import {
  LICENSE_DETAILS_FAILED,
  LICENSE_DETAILS_FULFILLED,
  LICENSE_DETAILS_REQUEST,
} from './componentLicenseDetailsActions';

const initialState = Object.freeze({
  selectedLicense: null,
  licenseIndex: 0,
});

function requestLicenseDetails(payload, state) {
  return {
    ...state,
    licenseIndex: parseInt(payload.licenseIndex),
  };
}

function updateLicenseDetails(payload, state) {
  return {
    ...state,
    licenseIndex: parseInt(payload.licenseIndex),
  };
}

function failureLicenseDetails(payload, state) {
  return {
    ...state,
    error: payload.value,
  };
}

const reducerActionMap = {
  [LICENSE_DETAILS_REQUEST]: requestLicenseDetails,
  [LICENSE_DETAILS_FULFILLED]: updateLicenseDetails,
  [LICENSE_DETAILS_FAILED]: failureLicenseDetails,
};

const componentLicenseDetailsReducer = createReducerFromActionMap(reducerActionMap, initialState);
export default componentLicenseDetailsReducer;
