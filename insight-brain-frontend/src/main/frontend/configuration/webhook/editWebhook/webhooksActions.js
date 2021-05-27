/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import axios from 'axios';
import { compose, mapObjIndexed, prop } from 'ramda';
import { noPayloadActionCreator, payloadParamActionCreator } from '../../../util/reduxUtil';
import { getProductFeaturesUrl, getWebhookEventTypesUrl, getWebhooksUrl } from '../../../util/CLMLocation';
import { SUBMIT_MASK_SUCCESS_VISIBLE_TIME_MS } from '@sonatype/react-shared-components';
import { stateGo } from '../../../reduxUiRouter/routerActions';
import { Messages } from '../../../util/CommonServices';
import { checkPermissions } from '../../../util/authorizationUtil';

export const EDIT_WEBHOOK_LOAD_REQUESTED = 'EDIT_WEBHOOK_LOAD_REQUESTED';
export const EDIT_WEBHOOK_LOAD_FAILED = 'EDIT_WEBHOOK_LOAD_FAILED';
export const EDIT_WEBHOOK_LOAD_FULFILLED = 'EDIT_WEBHOOK_LOAD_FULFILLED';

export const EDIT_WEBHOOK_TOGGLE_EVENT_TYPE = 'EDIT_WEBHOOK_TOGGLE_EVENT_TYPE';
export const EDIT_WEBHOOK_SET_URL = 'EDIT_WEBHOOK_SET_URL';
export const EDIT_WEBHOOK_SET_DESCRIPTION = 'EDIT_WEBHOOK_SET_DESCRIPTION';
export const EDIT_WEBHOOK_SET_SECRET_KEY = 'EDIT_WEBHOOK_SET_SECRET_KEY';

export const EDIT_WEBHOOK_SAVE_REQUESTED = 'EDIT_WEBHOOK_SAVE_REQUESTED';
export const EDIT_WEBHOOK_SAVE_FULFILLED = 'EDIT_WEBHOOK_SAVE_FULFILLED';
export const EDIT_WEBHOOK_SAVE_FAILED = 'EDIT_WEBHOOK_SAVE_FAILED';
export const EDIT_WEBHOOK_SAVE_SUBMIT_MASK_TIMER_DONE = 'EDIT_WEBHOOK_SAVE_SUBMIT_MASK_TIMER_DONE';

// LOAD
const loadRequested = noPayloadActionCreator(EDIT_WEBHOOK_LOAD_REQUESTED);
const loadFailed = payloadParamActionCreator(EDIT_WEBHOOK_LOAD_FAILED);
const loadFulfilled = payloadParamActionCreator(EDIT_WEBHOOK_LOAD_FULFILLED);

export function loadWebhookData() {
  return (dispatch) => {
    dispatch(loadRequested());

    return checkPermissions(['CONFIGURE_SYSTEM'])
      .then(() => {
        const eventTypesPromise = axios.get(getWebhookEventTypesUrl());
        const productFeaturesPromise = axios.get(getProductFeaturesUrl());
        return Promise.all([eventTypesPromise, productFeaturesPromise]);
      })
      .then(([eventTypes, productFeatures]) => {
        return dispatch(
          loadFulfilled({
            eventTypes: eventTypes.data,
            productFeatures: productFeatures.data,
          })
        );
      })
      .catch(compose(dispatch, loadFailed, Messages.getHttpErrorMessage));
  };
}

// EDIT FORM
export const setUrl = payloadParamActionCreator(EDIT_WEBHOOK_SET_URL);
export const setDescription = payloadParamActionCreator(EDIT_WEBHOOK_SET_DESCRIPTION);
export const setSecretKey = payloadParamActionCreator(EDIT_WEBHOOK_SET_SECRET_KEY);
export const toggleEventType = payloadParamActionCreator(EDIT_WEBHOOK_TOGGLE_EVENT_TYPE);

// SAVE
const saveRequested = noPayloadActionCreator(EDIT_WEBHOOK_SAVE_REQUESTED);
const saveFulfilled = noPayloadActionCreator(EDIT_WEBHOOK_SAVE_FULFILLED);
const saveFailed = payloadParamActionCreator(EDIT_WEBHOOK_SAVE_FAILED);
const submitMaskTimerDone = noPayloadActionCreator(EDIT_WEBHOOK_SAVE_SUBMIT_MASK_TIMER_DONE);

function startSubmitMaskSuccessTimer(dispatch) {
  setTimeout(() => {
    dispatch(submitMaskTimerDone());
    dispatch(stateGo('webhooks.list'));
  }, SUBMIT_MASK_SUCCESS_VISIBLE_TIME_MS);
}

export function saveWebhook() {
  return (dispatch, getState) => {
    dispatch(saveRequested());

    const { inputFields, selectedEventTypes } = getState().editWebhook;
    const trimmedInputs = mapObjIndexed(prop('trimmedValue'), inputFields);

    return axios
      .post(getWebhooksUrl(), {
        id: null,
        eventTypes: selectedEventTypes,
        ...trimmedInputs,
      })
      .then(() => {
        dispatch(saveFulfilled());
        startSubmitMaskSuccessTimer(dispatch);
      })
      .catch(compose(dispatch, saveFailed, Messages.getHttpErrorMessage));
  };
}
