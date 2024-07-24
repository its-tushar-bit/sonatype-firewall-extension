/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import axios from 'axios';
import { getPermissions } from 'MainRoot/util/authorizationUtil';
import { getLicenseDetailsUrl, getLicenseSummaryUrl } from 'MainRoot/util/CLMLocation';

export default function ProductLicense() {
  return {
    load() {
      return loadIfNotYetLoaded();
    },
  };
}

let licenseInfoPromise = null;
// if licenseLoadedWithoutLoginOrWithError is true re-fetch the license data on next loadIfNotYetLoaded call, the
// license data was loaded while logged out or an error occurred
let licenseLoadedWithoutLoginOrWithError = false;

export function loadIfNotYetLoaded() {
  if (licenseInfoPromise == null || licenseLoadedWithoutLoginOrWithError) {
    licenseInfoPromise = getLicense();
  }
  return licenseInfoPromise;
}

async function getLicense() {
  let hasConfigureSystemPermission = false;

  try {
    hasConfigureSystemPermission = (await getPermissions(['CONFIGURE_SYSTEM'], 'global', 'global', false)).length;
    licenseLoadedWithoutLoginOrWithError = false;
  } catch (error) {
    // Always reload the license on next try if this response is an error
    licenseLoadedWithoutLoginOrWithError = true;
    if (error?.status !== 401) {
      // If it is not a user logged out 401 status, throw the error and let the application handle it
      throw error;
    }
  }

  const licenseUrl = hasConfigureSystemPermission ? getLicenseDetailsUrl() : getLicenseSummaryUrl();
  try {
    return (await axios.get(licenseUrl)).data;
  } catch (error) {
    // Always reload the license on next try if this response is an error
    licenseLoadedWithoutLoginOrWithError = true;
    throw error;
  }
}

// Visible for testing - Clear the loaded (cached) product license promise
export function clearLoadedProductLicensePromise() {
  licenseInfoPromise = null;
}
