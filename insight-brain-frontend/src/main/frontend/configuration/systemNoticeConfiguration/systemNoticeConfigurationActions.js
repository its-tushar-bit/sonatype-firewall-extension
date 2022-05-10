/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import axios from 'axios';
import { compose } from 'ramda';
import { SUBMIT_MASK_SUCCESS_VISIBLE_TIME_MS } from '@sonatype/react-shared-components';

import { noPayloadActionCreator, payloadParamActionCreator } from '../../util/reduxUtil';
import { checkPermissions } from '../../util/authorizationUtil';
import { getSystemNoticeFetchUrl, getSystemNoticeUrl } from '../../util/CLMLocation';
import { Messages } from '../../utilAngular/CommonServices';

export const SYSTEM_NOTICE_CONFIGURATION_LOAD_REQUESTED = 'SYSTEM_NOTICE_CONFIGURATION_LOAD_REQUESTED';
export const SYSTEM_NOTICE_CONFIGURATION_LOAD_FULFILLED = 'SYSTEM_NOTICE_CONFIGURATION_LOAD_FULFILLED';
export const SYSTEM_NOTICE_CONFIGURATION_SYSTEM_NOTICE_LOAD_FAILED =
  'SYSTEM_NOTICE_CONFIGURATION_SYSTEM_NOTICE_LOAD_FAILED';
export const SYSTEM_NOTICE_CONFIGURATION_LOAD_PAGE_FAILED = 'SYSTEM_NOTICE_CONFIGURATION_LOAD_PAGE_FAILED';

const loadRequested = noPayloadActionCreator(SYSTEM_NOTICE_CONFIGURATION_LOAD_REQUESTED);
const loadFulfilled = payloadParamActionCreator(SYSTEM_NOTICE_CONFIGURATION_LOAD_FULFILLED);
const loadSystemNoticeFailed = payloadParamActionCreator(SYSTEM_NOTICE_CONFIGURATION_SYSTEM_NOTICE_LOAD_FAILED);
const loadPageFailed = payloadParamActionCreator(SYSTEM_NOTICE_CONFIGURATION_LOAD_PAGE_FAILED);

export function load() {
  return function (dispatch) {
    dispatch(loadRequested());

    return checkPermissions(['CONFIGURE_SYSTEM'])
      .then(() => dispatch(loadSystemNotice()))
      .catch(compose(dispatch, loadPageFailed, Messages.getHttpErrorMessage));
  };
}

export function loadSystemNotice() {
  return function (dispatch) {
    dispatch(loadRequested());
    return axios
      .get(getSystemNoticeFetchUrl())
      .then(({ data }) => {
        dispatch(loadFulfilled(data));
      })
      .catch((error) => {
        dispatch(loadSystemNoticeFailed(Messages.getHttpErrorMessage(error)));
      });
  };
}

export const SYSTEM_NOTICE_CONFIGURATION_UPDATE_REQUESTED = 'SYSTEM_NOTICE_CONFIGURATION_UPDATE_REQUESTED';
export const SYSTEM_NOTICE_CONFIGURATION_UPDATE_FULFILLED = 'SYSTEM_NOTICE_CONFIGURATION_UPDATE_FULFILLED';
export const SYSTEM_NOTICE_CONFIGURATION_UPDATE_FAILED = 'SYSTEM_NOTICE_CONFIGURATION_UPDATE_FAILED';

export const SYSTEM_NOTICE_CONFIGURATION_UPDATE_SUBMIT_MASK_TIMER_DONE =
  'SYSTEM_NOTICE_CONFIGURATION_UPDATE_SUBMIT_MASK_TIMER_DONE';

const updateRequested = noPayloadActionCreator(SYSTEM_NOTICE_CONFIGURATION_UPDATE_REQUESTED);
const updateFulfilled = noPayloadActionCreator(SYSTEM_NOTICE_CONFIGURATION_UPDATE_FULFILLED);
const updateFailed = payloadParamActionCreator(SYSTEM_NOTICE_CONFIGURATION_UPDATE_FAILED);

function startSubmitMaskSuccessTimer(dispatch) {
  setTimeout(() => {
    dispatch({ type: SYSTEM_NOTICE_CONFIGURATION_UPDATE_SUBMIT_MASK_TIMER_DONE });
  }, SUBMIT_MASK_SUCCESS_VISIBLE_TIME_MS);
}

export function toServerData({ enabled, message }) {
  return {
    enabled,
    message: message.trimmedValue,
  };
}

export function update() {
  return function (dispatch, getState) {
    dispatch(updateRequested());
    const formState = getState().systemNoticeConfiguration.formState;
    return axios
      .put(getSystemNoticeUrl(), toServerData(formState))
      .then(() => {
        dispatch(updateFulfilled());
        startSubmitMaskSuccessTimer(dispatch);
      })
      .catch((error) => {
        dispatch(updateFailed(Messages.getHttpErrorMessage(error)));
      });
  };
}

export const SYSTEM_NOTICE_CONFIGURATION_RESET_FORM = 'SYSTEM_NOTICE_CONFIGURATION_RESET_FORM';
export const resetForm = noPayloadActionCreator(SYSTEM_NOTICE_CONFIGURATION_RESET_FORM);

export const SYSTEM_NOTICE_CONFIGURATION_TOGGLE_ENABLED = 'SYSTEM_NOTICE_CONFIGURATION_TOGGLE_ENABLED';
export const toggleIsEnabled = payloadParamActionCreator(SYSTEM_NOTICE_CONFIGURATION_TOGGLE_ENABLED);

export const SYSTEM_NOTICE_CONFIGURATION_SET_MESSAGE = 'SYSTEM_NOTICE_CONFIGURATION_SET_MESSAGE';
export const setMessage = payloadParamActionCreator(SYSTEM_NOTICE_CONFIGURATION_SET_MESSAGE);
