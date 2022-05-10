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
import { Messages } from '../../utilAngular/CommonServices';
import { getSuccessMetricsConfigUrl } from '../../util/CLMLocation';

export const SUCCESS_METRICS_CONFIGURATION_LOAD_REQUESTED = 'SUCCESS_METRICS_CONFIGURATION_LOAD_REQUESTED';
export const SUCCESS_METRICS_CONFIGURATION_LOAD_FULFILLED = 'SUCCESS_METRICS_CONFIGURATION_LOAD_FULFILLED';
export const SUCCESS_METRICS_CONFIGURATION_LOAD_FAILED = 'SUCCESS_METRICS_CONFIGURATION_LOAD_FAILED';

const loadRequested = noPayloadActionCreator(SUCCESS_METRICS_CONFIGURATION_LOAD_REQUESTED);
const loadFulfilled = payloadParamActionCreator(SUCCESS_METRICS_CONFIGURATION_LOAD_FULFILLED);
const loadFailed = payloadParamActionCreator(SUCCESS_METRICS_CONFIGURATION_LOAD_FAILED);

export function load() {
  return function (dispatch) {
    dispatch(loadRequested());

    return checkPermissions(['CONFIGURE_SYSTEM'])
      .then(() => dispatch(loadConfiguration()))
      .catch(compose(dispatch, loadFailed, Messages.getHttpErrorMessage));
  };
}

export function loadConfiguration() {
  return function (dispatch) {
    dispatch(loadRequested());
    return axios
      .get(getSuccessMetricsConfigUrl())
      .then(({ data }) => {
        dispatch(loadFulfilled(data));
      })
      .catch((error) => {
        dispatch(loadFailed(Messages.getHttpErrorMessage(error)));
      });
  };
}

export const SUCCESS_METRICS_CONFIGURATION_UPDATE_REQUESTED = 'SUCCESS_METRICS_CONFIGURATION_UPDATE_REQUESTED';
export const SUCCESS_METRICS_CONFIGURATION_UPDATE_FULFILLED = 'SUCCESS_METRICS_CONFIGURATION_UPDATE_FULFILLED';
export const SUCCESS_METRICS_CONFIGURATION_UPDATE_FAILED = 'SUCCESS_METRICS_CONFIGURATION_UPDATE_FAILED';

export const SUCCESS_METRICS_CONFIGURATION_UPDATE_SUBMIT_MASK_TIMER_DONE =
  'SUCCESS_METRICS_CONFIGURATION_UPDATE_SUBMIT_MASK_TIMER_DONE';

const updateRequested = noPayloadActionCreator(SUCCESS_METRICS_CONFIGURATION_UPDATE_REQUESTED);
const updateFulfilled = noPayloadActionCreator(SUCCESS_METRICS_CONFIGURATION_UPDATE_FULFILLED);
const updateFailed = payloadParamActionCreator(SUCCESS_METRICS_CONFIGURATION_UPDATE_FAILED);

function startSubmitMaskSuccessTimer(dispatch) {
  setTimeout(() => {
    dispatch({ type: SUCCESS_METRICS_CONFIGURATION_UPDATE_SUBMIT_MASK_TIMER_DONE });
  }, SUBMIT_MASK_SUCCESS_VISIBLE_TIME_MS);
}

export function update() {
  return function (dispatch, getState) {
    dispatch(updateRequested());
    return axios
      .put(getSuccessMetricsConfigUrl(), getState().successMetricsConfiguration.formState)
      .then(() => {
        dispatch(updateFulfilled());
        startSubmitMaskSuccessTimer(dispatch);
      })
      .catch((error) => {
        dispatch(updateFailed(Messages.getHttpErrorMessage(error)));
      });
  };
}

export const SUCCESS_METRICS_CONFIGURATION_RESET_FORM = 'SUCCESS_METRICS_CONFIGURATION_RESET_FORM';
export const resetForm = noPayloadActionCreator(SUCCESS_METRICS_CONFIGURATION_RESET_FORM);

export const SUCCESS_METRICS_CONFIGURATION_TOGGLE_ENABLED = 'SUCCESS_METRICS_CONFIGURATION_TOGGLE_ENABLED';
export const toggleIsEnabled = payloadParamActionCreator(SUCCESS_METRICS_CONFIGURATION_TOGGLE_ENABLED);
