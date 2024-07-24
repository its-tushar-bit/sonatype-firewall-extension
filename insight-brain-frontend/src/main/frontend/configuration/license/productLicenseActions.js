/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

import { SUBMIT_MASK_SUCCESS_VISIBLE_TIME_MS } from '@sonatype/react-shared-components';
import axios from 'axios';
import { compose } from 'ramda';
import { getLicenseUploadUrl } from '../../util/CLMLocation';
import { Messages } from '../../utilAngular/CommonServices';
import { getDaysFromNow } from '../../util/jsUtil';

import { noPayloadActionCreator, payloadParamActionCreator } from '../../util/reduxUtil';
import { loadIfNotYetLoaded } from 'MainRoot/utility/services/ProductLicense';

export const PRODUCT_LICENSE_LOAD_REQUESTED = 'PRODUCT_LICENSE_LOAD_REQUESTED';
export const PRODUCT_LICENSE_LOAD_FULFILLED = 'PRODUCT_LICENSE_LOAD_FULFILLED';
export const PRODUCT_LICENSE_LOAD_FAILED = 'PRODUCT_LICENSE_LOAD_FAILED';

const loadRequested = noPayloadActionCreator(PRODUCT_LICENSE_LOAD_REQUESTED);
const loadFulfilled = payloadParamActionCreator(PRODUCT_LICENSE_LOAD_FULFILLED);
const loadFailed = payloadParamActionCreator(PRODUCT_LICENSE_LOAD_FAILED);

export const PRODUCT_LICENSE_INVALID = 'PRODUCT_LICENSE_INVALID';

const invalidLicense = noPayloadActionCreator(PRODUCT_LICENSE_INVALID);

export const PRODUCT_LICENSE_UPDATE_LICENSE_REQUESTED = 'PRODUCT_LICENSE_UPDATE_LICENSE_REQUESTED';
export const PRODUCT_LICENSE_UPDATE_LICENSE_FULFILLED = 'PRODUCT_LICENSE_UPDATE_LICENSE_FULFILLED';
export const PRODUCT_LICENSE_UPDATE_LICENSE_FAILED = 'PRODUCT_LICENSE_UPDATE_LICENSE_FAILED';

const updateLicenseRequested = noPayloadActionCreator(PRODUCT_LICENSE_UPDATE_LICENSE_REQUESTED);
const updateLicenseFulfilled = noPayloadActionCreator(PRODUCT_LICENSE_UPDATE_LICENSE_FULFILLED);
const updateLicenseFailed = payloadParamActionCreator(PRODUCT_LICENSE_UPDATE_LICENSE_FAILED);

export const PRODUCT_LICENSE_SUBMIT_MASK_TIMER_DONE = 'PRODUCT_LICENSE_SUBMIT_MASK_TIMER_DONE';
const submitMaskTimerDone = noPayloadActionCreator(PRODUCT_LICENSE_SUBMIT_MASK_TIMER_DONE);

export const PRODUCT_LICENSE_CLEAR_UPDATE_LICENSE_ERROR = 'PRODUCT_LICENSE_CLEAR_UPDATE_LICENSE_ERROR';
export const clearUpdateLicenseError = noPayloadActionCreator(PRODUCT_LICENSE_CLEAR_UPDATE_LICENSE_ERROR);

function startSubmitMaskSuccessTimer(dispatch) {
  return new Promise((resolve) => {
    setTimeout(() => {
      dispatch(submitMaskTimerDone());
      resolve(true);
    }, SUBMIT_MASK_SUCCESS_VISIBLE_TIME_MS);
  });
}

export function load() {
  return async (dispatch) => {
    dispatch(loadRequested());

    let licenseInfo;
    try {
      licenseInfo = await loadIfNotYetLoaded();
    } catch (error) {
      const licenseIsInvalid = error?.response?.status === 402;
      if (licenseIsInvalid) {
        dispatch(invalidLicense());
      } else {
        dispatch(loadFailed(Messages.getHttpErrorMessage(error)));
      }
      return;
    }

    return dispatch(
      loadFulfilled({
        ...licenseInfo,
        ...(licenseInfo.expiryTimestamp && { daysToExpiration: getDaysFromNow(licenseInfo.expiryTimestamp) }),
      })
    );
  };
}

export function updateLicense(license) {
  return (dispatch) => {
    dispatch(updateLicenseRequested());
    const form = new FormData();
    form.append('file', license);
    return axios
      .post(getLicenseUploadUrl(), form)
      .then(() => dispatch(updateLicenseFulfilled()))
      .then(() => startSubmitMaskSuccessTimer(dispatch))
      .catch(compose(dispatch, updateLicenseFailed, Messages.getHttpErrorMessage));
  };
}

export const PRODUCT_LICENSE_UNINSTALL_LICENSE_REQUESTED = 'PRODUCT_LICENSE_UNINSTALL_LICENSE_REQUESTED';
export const PRODUCT_LICENSE_UNINSTALL_LICENSE_FULFILLED = 'PRODUCT_LICENSE_UNINSTALL_LICENSE_FULFILLED';
export const PRODUCT_LICENSE_UNINSTALL_LICENSE_FAIL = 'PRODUCT_LICENSE_UNINSTALL_LICENSE_FAIL';

const uninstallLicenseRequested = noPayloadActionCreator(PRODUCT_LICENSE_UNINSTALL_LICENSE_REQUESTED);
const uninstallLicenseFulfilled = noPayloadActionCreator(PRODUCT_LICENSE_UNINSTALL_LICENSE_FULFILLED);
const uninstallLicenseFail = payloadParamActionCreator(PRODUCT_LICENSE_UNINSTALL_LICENSE_FAIL);

export const PRODUCT_LICENSE_UNINSTALL_MASK_TIMER_DONE = 'PRODUCT_LICENSE_UNINSTALL_MASK_TIMER_DONE';
const uninstallMaskTimerDone = noPayloadActionCreator(PRODUCT_LICENSE_UNINSTALL_MASK_TIMER_DONE);

function startUninstallSubmitMaskSuccessTimer(dispatch) {
  return new Promise((resolve) => {
    setTimeout(() => {
      resolve(dispatch(uninstallMaskTimerDone()));
    }, SUBMIT_MASK_SUCCESS_VISIBLE_TIME_MS);
  });
}

export function uninstallLicense() {
  return (dispatch) => {
    dispatch(uninstallLicenseRequested());
    return axios
      .delete(getLicenseUploadUrl())
      .then(compose(dispatch, uninstallLicenseFulfilled))
      .then(() => startUninstallSubmitMaskSuccessTimer(dispatch))
      .catch(compose(dispatch, uninstallLicenseFail, Messages.getHttpErrorMessage));
  };
}
