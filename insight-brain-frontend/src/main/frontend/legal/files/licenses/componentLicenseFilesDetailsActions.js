/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

import { payloadParamActionCreator } from '../../../util/reduxUtil';
import { loadAvailableScopes, loadComponent, loadComponentByComponentIdentifier } from '../../advancedLegalActions';

export const LICENSE_DETAILS_SELECTED_LICENSE_FILE = 'LICENSE_DETAILS_SELECTED_LICENSE_FILE';

export const selectedLicenseDetail = payloadParamActionCreator(LICENSE_DETAILS_SELECTED_LICENSE_FILE);

export function refreshLicenseFilesDetails() {
  return (dispatch, getState) => {
    const currentParams = getState().router && getState().router.currentParams;
    return (
      currentParams &&
      currentParams.licenseIndex &&
      dispatchSelectedLicense(dispatch, getState(), currentParams.licenseIndex)
    );
  };
}

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
      return dispatch(promise).then(() => dispatchSelectedLicense(dispatch, getState(), licenseIndex));
    } else {
      return dispatchSelectedLicense(dispatch, getState(), licenseIndex);
    }
  };
}

function dispatchSelectedLicense(dispatch, state, licenseIndex) {
  const { license } = extractRoutingParameters(state, licenseIndex);
  dispatch(
    selectedLicenseDetail({
      licenseIndex: licenseIndex,
      license,
    })
  );
}

function extractRoutingParameters(state, requestedLicenseIndex) {
  const advancedLegalState = state.advancedLegal;
  const routerParams = state.router.currentParams;
  const licenseIndex = requestedLicenseIndex || state.componentLicenseDetails.licenseIndex;
  const component = advancedLegalState.component.component;
  const license = component?.licenseLegalData.licenseFiles[licenseIndex];

  const ownerType = routerParams.ownerType;
  const ownerPublicId = routerParams.ownerId;
  return { license, component, ownerType, ownerPublicId };
}
