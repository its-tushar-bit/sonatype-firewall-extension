/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

import { createReducerFromActionMap, propSetConst } from '../../util/reduxUtil';
import {
  PRODUCT_LICENSE_CLEAR_UPDATE_LICENSE_ERROR,
  PRODUCT_LICENSE_INVALID,
  PRODUCT_LICENSE_LOAD_FAILED,
  PRODUCT_LICENSE_LOAD_FULFILLED,
  PRODUCT_LICENSE_LOAD_REQUESTED,
  PRODUCT_LICENSE_SUBMIT_MASK_TIMER_DONE,
  PRODUCT_LICENSE_UPDATE_LICENSE_FAILED,
  PRODUCT_LICENSE_UPDATE_LICENSE_FULFILLED,
  PRODUCT_LICENSE_UPDATE_LICENSE_REQUESTED,
  PRODUCT_LICENSE_UNINSTALL_LICENSE_FAIL,
  PRODUCT_LICENSE_UNINSTALL_LICENSE_FULFILLED,
  PRODUCT_LICENSE_UNINSTALL_LICENSE_REQUESTED,
  PRODUCT_LICENSE_UNINSTALL_MASK_TIMER_DONE,
} from './productLicenseActions';

export const initialState = {
  license: null,
  loading: true,
  loadError: null,
  submitMaskState: null,
  updateLicenseError: null,
  uninstallMaskState: null,
  uninstallError: null,
  installed: false,
};

function loadFulfilled(payload, state) {
  return {
    ...state,
    license: { ...payload },
    loading: false,
    loadError: null,
    installed: true,
  };
}

function loadFailed(payload, state) {
  return {
    ...state,
    loading: false,
    loadError: payload,
    license: null,
  };
}

function invalidLicense(_, state) {
  return {
    ...state,
    loading: false,
    loadError: null,
    license: null,
    installed: false,
  };
}

function updateLicenseFulfilled(_, state) {
  return {
    ...state,
    updateLicenseError: null,
    submitMaskState: true,
    installed: true,
  };
}

function updateLicenseFailed(payload, state) {
  return {
    ...state,
    updateLicenseError: payload,
    submitMaskState: null,
  };
}

function uninstallFulfilled(_, state) {
  return {
    ...state,
    uninstallMaskState: true,
    uninstallError: null,
    installed: false,
  };
}

function uninstallFail(payload, state) {
  return {
    ...state,
    uninstallMaskState: null,
    uninstallError: payload,
  };
}

function uninstallMaskTimerDone(_, state) {
  return {
    ...state,
    uninstallMaskState: null,
    loading: true,
  };
}

const reducerActionMap = {
  [PRODUCT_LICENSE_LOAD_REQUESTED]: () => initialState,
  [PRODUCT_LICENSE_LOAD_FULFILLED]: loadFulfilled,
  [PRODUCT_LICENSE_LOAD_FAILED]: loadFailed,
  [PRODUCT_LICENSE_INVALID]: invalidLicense,
  [PRODUCT_LICENSE_UPDATE_LICENSE_REQUESTED]: propSetConst('submitMaskState', false),
  [PRODUCT_LICENSE_UPDATE_LICENSE_FULFILLED]: updateLicenseFulfilled,
  [PRODUCT_LICENSE_SUBMIT_MASK_TIMER_DONE]: propSetConst('submitMaskState', null),
  [PRODUCT_LICENSE_UPDATE_LICENSE_FAILED]: updateLicenseFailed,
  [PRODUCT_LICENSE_CLEAR_UPDATE_LICENSE_ERROR]: propSetConst('updateLicenseError', null),
  [PRODUCT_LICENSE_UNINSTALL_LICENSE_REQUESTED]: propSetConst('uninstallMaskState', false),
  [PRODUCT_LICENSE_UNINSTALL_LICENSE_FULFILLED]: uninstallFulfilled,
  [PRODUCT_LICENSE_UNINSTALL_LICENSE_FAIL]: uninstallFail,
  [PRODUCT_LICENSE_UNINSTALL_MASK_TIMER_DONE]: uninstallMaskTimerDone,
};

const productLicenseDetailReducer = createReducerFromActionMap(reducerActionMap, initialState);

export default productLicenseDetailReducer;
