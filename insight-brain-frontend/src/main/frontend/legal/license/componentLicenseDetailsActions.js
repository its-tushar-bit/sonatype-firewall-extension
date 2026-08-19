/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { payloadParamActionCreator } from '../../util/reduxUtil';
import { loadAvailableScopes, loadComponent, loadComponentByComponentIdentifier } from '../advancedLegalActions';

export const LICENSE_DETAILS_REQUEST = 'LICENSE_DETAILS_REQUEST';
export const LICENSE_DETAILS_FULFILLED = 'LICENSE_DETAILS_FULFILLED';
export const LICENSE_DETAILS_FAILED = 'LICENSE_DETAILS_FAILED';

export const licenseDetailsRequest = payloadParamActionCreator(LICENSE_DETAILS_REQUEST);
export const licenseDetailsFulfilled = payloadParamActionCreator(LICENSE_DETAILS_FULFILLED);
export const licenseDetailsFailed = payloadParamActionCreator(LICENSE_DETAILS_FAILED);

export function loadComponentAndLicenseDetails(ownerType, ownerId, hash, licenseIndex, componentIdentifier) {
  return (dispatch, getState) => {
    const component = getState().advancedLegal.component.component;
    if (!component) {
      dispatch(loadAvailableScopes(ownerType, ownerId));
      const promise = hash
        ? loadComponent(ownerType, ownerId, hash)
        : loadComponentByComponentIdentifier(componentIdentifier, {
            orgOrApp: ownerType,
            ownerId: ownerId,
          });
      return dispatch(promise).then(() => requestLoadLicenseDetails(dispatch, licenseIndex));
    } else {
      return requestLoadLicenseDetails(dispatch, licenseIndex);
    }
  };
}

export function isMultiLicense(licenseLegalMetadata, licenseName) {
  const license = licenseLegalMetadata.find((licenseMetadata) => licenseMetadata.licenseId === licenseName);
  return !license || license.isMulti;
}

function requestLoadLicenseDetails(dispatch, licenseIndex) {
  dispatch(
    licenseDetailsRequest({
      licenseIndex,
    })
  );

  return dispatch(
    licenseDetailsFulfilled({
      licenseIndex: licenseIndex,
    })
  );
}
