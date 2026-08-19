/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { createReducerFromActionMap } from '../../../util/reduxUtil';
import { LICENSE_DETAILS_SELECTED_LICENSE_FILE } from './componentLicenseFilesDetailsActions';

const initialState = Object.freeze({
  licenseIndex: null,
  selectedLicense: null,
  loadingLicenseDetails: true,
});

function selectedLicenseDetail(payload, state) {
  return {
    ...state,
    licenseIndex: parseInt(payload.licenseIndex),
    selectedLicense: payload.license,
    loadingLicenseDetails: false,
  };
}

const reducerActionMap = {
  [LICENSE_DETAILS_SELECTED_LICENSE_FILE]: selectedLicenseDetail,
};

const componentLicenseFilesDetailsReducer = createReducerFromActionMap(reducerActionMap, initialState);
export default componentLicenseFilesDetailsReducer;
