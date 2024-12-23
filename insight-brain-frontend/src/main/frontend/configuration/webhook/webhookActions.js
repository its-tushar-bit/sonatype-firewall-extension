/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import axios from 'axios';
import { compose, mapObjIndexed, prop, find, startsWith } from 'ramda';
import { noPayloadActionCreator, payloadParamActionCreator } from '../../util/reduxUtil';
import {
  deleteWebhooksUrl,
  getProductFeaturesUrl,
  getWebhookEventTypesUrl,
  getWebhooksUrl,
} from '../../util/CLMLocation';
import { SUBMIT_MASK_SUCCESS_VISIBLE_TIME_MS } from '@sonatype/react-shared-components';
import { stateGo } from '../../reduxUiRouter/routerActions';
import { Messages } from '../../utilAngular/CommonServices';
import { checkPermissions } from '../../util/authorizationUtil';

export const EDIT_WEBHOOK_LOAD_REQUESTED = 'EDIT_WEBHOOK_LOAD_REQUESTED';
export const EDIT_WEBHOOK_LOAD_FAILED = 'EDIT_WEBHOOK_LOAD_FAILED';
export const EDIT_WEBHOOK_LOAD_FULFILLED = 'EDIT_WEBHOOK_LOAD_FULFILLED';
export const EDIT_WEBHOOK_LOAD_EDIT_FULFILLED = 'EDIT_WEBHOOK_LOAD_EDIT_FULFILLED';
export const EDIT_WEBHOOK_FETCH_PRODUCT_FEATURES_FULFILLED = 'EDIT_WEBHOOK_FETCH_PRODUCT_FEATURES_FULFILLED';
export const EDIT_WEBHOOK_FETCH_EVENT_TYPES_FULFILLED = 'EDIT_WEBHOOK_FETCH_EVENT_TYPES_FULFILLED';
export const EDIT_WEBHOOK_FETCH_WEBHOOKS_FULFILLED = 'EDIT_WEBHOOK_FETCH_WEBHOOKS_FULFILLED';

export const EDIT_WEBHOOK_TOGGLE_EVENT_TYPE = 'EDIT_WEBHOOK_TOGGLE_EVENT_TYPE';
export const EDIT_WEBHOOK_SET_URL = 'EDIT_WEBHOOK_SET_URL';
export const EDIT_WEBHOOK_SET_DESCRIPTION = 'EDIT_WEBHOOK_SET_DESCRIPTION';
export const EDIT_WEBHOOK_SET_SECRET_KEY = 'EDIT_WEBHOOK_SET_SECRET_KEY';
export const EDIT_WEBHOOK_SET_IS_URL_HTTP = 'EDIT_WEBHOOK_SET_IS_URL_HTTP';

export const EDIT_WEBHOOK_SAVE_REQUESTED = 'EDIT_WEBHOOK_SAVE_REQUESTED';
export const EDIT_WEBHOOK_SAVE_FULFILLED = 'EDIT_WEBHOOK_SAVE_FULFILLED';
export const EDIT_WEBHOOK_SAVE_FAILED = 'EDIT_WEBHOOK_SAVE_FAILED';

export const EDIT_WEBHOOK_DELETE_REQUESTED = 'EDIT_WEBHOOK_DELETE_REQUESTED';
export const EDIT_WEBHOOK_DELETE_FULFILLED = 'EDIT_WEBHOOK_DELETE_FULFILLED';
export const EDIT_WEBHOOK_DELETE_FAILED = 'EDIT_WEBHOOK_DELETE_FAILED';

export const EDIT_WEBHOOK_SUBMIT_MASK_TIMER_DONE = 'EDIT_WEBHOOK_SUBMIT_MASK_TIMER_DONE';

// LOAD
const loadRequested = noPayloadActionCreator(EDIT_WEBHOOK_LOAD_REQUESTED);
const loadFailed = payloadParamActionCreator(EDIT_WEBHOOK_LOAD_FAILED);
const loadFulfilled = noPayloadActionCreator(EDIT_WEBHOOK_LOAD_FULFILLED);
const loadEditFulfilled = payloadParamActionCreator(EDIT_WEBHOOK_LOAD_EDIT_FULFILLED);

const fetchProductFeaturesFulfilled = payloadParamActionCreator(EDIT_WEBHOOK_FETCH_PRODUCT_FEATURES_FULFILLED);
const fetchEventTypesFulfilled = payloadParamActionCreator(EDIT_WEBHOOK_FETCH_EVENT_TYPES_FULFILLED);
const fetchWebhooksFulfilled = payloadParamActionCreator(EDIT_WEBHOOK_FETCH_WEBHOOKS_FULFILLED);

function fetchProductFeatures() {
  return (dispatch) => {
    return axios
      .get(getProductFeaturesUrl())
      .then(prop('data'))
      .then((productFeatures) => {
        const isAppWebhooksSupported = productFeatures.includes('webhooks-for-applications');
        const isRepoWebhooksSupported = productFeatures.includes('webhooks-for-repositories');
        if (!isAppWebhooksSupported && !isRepoWebhooksSupported) {
          throw 'Webhooks feature is not supported by your license.';
        }
        return isAppWebhooksSupported;
      })
      .then(compose(dispatch, fetchProductFeaturesFulfilled));
  };
}

function fetchEventTypes() {
  return (dispatch) => {
    return axios.get(getWebhookEventTypesUrl()).then(prop('data')).then(compose(dispatch, fetchEventTypesFulfilled));
  };
}

function fetchWebhooks() {
  return (dispatch) => {
    return axios.get(getWebhooksUrl()).then(prop('data')).then(compose(dispatch, fetchWebhooksFulfilled));
  };
}

export function loadWebhookPage(webhookId) {
  return (dispatch) => {
    if (webhookId) {
      return dispatch(loadEditWebhookPage(webhookId));
    }

    return dispatch(loadAddWebhookPage());
  };
}

export function loadAddWebhookPage() {
  return (dispatch) => {
    dispatch(loadRequested());

    return checkPermissions(['CONFIGURE_SYSTEM'])
      .then(() => {
        return Promise.all([dispatch(fetchProductFeatures()), dispatch(fetchEventTypes())]);
      })
      .then(compose(dispatch, loadFulfilled))
      .catch(compose(dispatch, loadFailed, Messages.getHttpErrorMessage));
  };
}

export function loadEditWebhookPage(webhookId) {
  return (dispatch) => {
    dispatch(loadRequested());

    return checkPermissions(['CONFIGURE_SYSTEM'])
      .then(() => {
        return Promise.all([dispatch(fetchProductFeatures()), dispatch(fetchEventTypes()), dispatch(fetchWebhooks())]);
      })
      .then((data) => {
        const webhook = find((webhook) => webhook.id === webhookId, data[2].payload);
        if (!webhook) {
          throw 'Unable to locate webhook';
        }

        // Update state after evaluating if the url is http or not
        dispatch(setIsUrlHttp(validateUrlIsHttp(webhook.url)));
        dispatch(loadEditFulfilled(webhook));
      })
      .catch(compose(dispatch, loadFailed, Messages.getHttpErrorMessage));
  };
}

export function loadWebhookListPage() {
  return (dispatch) => {
    dispatch(loadRequested());

    return checkPermissions(['CONFIGURE_SYSTEM'])
      .then(() => {
        return Promise.all([dispatch(fetchProductFeatures()), dispatch(fetchWebhooks())]);
      })
      .then(compose(dispatch, loadFulfilled))
      .catch(compose(dispatch, loadFailed, Messages.getHttpErrorMessage));
  };
}

// EDIT FORM
export const setUrl = payloadParamActionCreator(EDIT_WEBHOOK_SET_URL);
export const setDescription = payloadParamActionCreator(EDIT_WEBHOOK_SET_DESCRIPTION);
export const setSecretKey = payloadParamActionCreator(EDIT_WEBHOOK_SET_SECRET_KEY);
export const toggleEventType = payloadParamActionCreator(EDIT_WEBHOOK_TOGGLE_EVENT_TYPE);
export const setIsUrlHttp = payloadParamActionCreator(EDIT_WEBHOOK_SET_IS_URL_HTTP);

// SAVE
const saveRequested = noPayloadActionCreator(EDIT_WEBHOOK_SAVE_REQUESTED);
const saveFulfilled = noPayloadActionCreator(EDIT_WEBHOOK_SAVE_FULFILLED);
const saveFailed = payloadParamActionCreator(EDIT_WEBHOOK_SAVE_FAILED);

// DELETE
const deleteRequested = noPayloadActionCreator(EDIT_WEBHOOK_DELETE_REQUESTED);
const deleteFulfilled = noPayloadActionCreator(EDIT_WEBHOOK_DELETE_FULFILLED);
const deleteFailed = payloadParamActionCreator(EDIT_WEBHOOK_DELETE_FAILED);

const submitMaskTimerDone = noPayloadActionCreator(EDIT_WEBHOOK_SUBMIT_MASK_TIMER_DONE);

function startSubmitMaskSuccessTimer(dispatch) {
  setTimeout(() => {
    dispatch(submitMaskTimerDone());
    dispatch(stateGo('listWebhooks'));
  }, SUBMIT_MASK_SUCCESS_VISIBLE_TIME_MS);
}

export function saveWebhook() {
  return (dispatch, getState) => {
    dispatch(saveRequested());

    const { inputFields, selectedEventTypes, serverData } = getState().webhooks;
    const trimmedInputs = mapObjIndexed(prop('trimmedValue'), inputFields);

    const request = axios[serverData.id ? 'put' : 'post'](getWebhooksUrl(), {
      id: serverData.id,
      eventTypes: selectedEventTypes,
      ...trimmedInputs,
    });

    return request
      .then(() => {
        dispatch(saveFulfilled());
        startSubmitMaskSuccessTimer(dispatch);
      })
      .catch(compose(dispatch, saveFailed, Messages.getHttpErrorMessage));
  };
}

export function deleteWebhook(webhookId) {
  return (dispatch) => {
    dispatch(deleteRequested());
    return axios
      .delete(deleteWebhooksUrl(webhookId))
      .then(() => {
        dispatch(deleteFulfilled());
        startSubmitMaskSuccessTimer(dispatch);
      })
      .catch(compose(dispatch, deleteFailed, Messages.getHttpErrorMessage));
  };
}

export function validateUrlIsHttp(url) {
  const httpStr = 'http://';
  return url && url.length >= httpStr.length && startsWith(httpStr, url);
}
