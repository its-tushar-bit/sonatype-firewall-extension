/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { remove, slice, startsWith } from 'ramda';
import { nxTextInputStateHelpers } from '@sonatype/react-shared-components';
import { createReducerFromActionMap, propSetConst } from '../../../util/reduxUtil';
import {
  EDIT_WEBHOOK_LOAD_REQUESTED,
  EDIT_WEBHOOK_LOAD_FULFILLED,
  EDIT_WEBHOOK_LOAD_FAILED,
  EDIT_WEBHOOK_TOGGLE_EVENT_TYPE,
  EDIT_WEBHOOK_SET_URL,
  EDIT_WEBHOOK_SET_DESCRIPTION,
  EDIT_WEBHOOK_SET_SECRET_KEY,
  EDIT_WEBHOOK_SAVE_REQUESTED,
  EDIT_WEBHOOK_SAVE_SUBMIT_MASK_TIMER_DONE,
  EDIT_WEBHOOK_SAVE_FULFILLED,
  EDIT_WEBHOOK_SAVE_FAILED,
} from './webhooksActions';
import { pathSet } from '../../../util/jsUtil';
import { combineValidators, validateNonEmpty } from '../../../util/validationUtil';

const { initialState: initUserInput, userInput } = nxTextInputStateHelpers;

const invalidUrlPrefixError = 'Webhook URL must start with http:// or https://';
const featureNotSupportedError = 'Webhooks feature is not supported by your license.';

export const initialState = Object.freeze({
  isLoading: true,
  loadError: null,
  saveError: null,
  formValidationMessage: null,
  submitMaskState: null,
  availableEventTypes: [],
  selectedEventTypes: [],
  isAppWebhooksSupported: false,
  inputFields: {
    url: initUserInput(''),
    description: initUserInput(''),
    secretKey: initUserInput(''),
  },
});

const setTextInput = (fieldName, validator) => (payload, state) => {
  const textInput = userInput(validator, payload);
  return pathSet(['inputFields', fieldName], textInput, state);
};

function validateWebhookUrl(value) {
  return startsWith(slice(0, value.length, 'http://'), value) || startsWith(slice(0, value.length, 'https://'), value)
    ? null
    : invalidUrlPrefixError;
}

const urlValidator = combineValidators([validateNonEmpty, validateWebhookUrl]);

function loadFulfilled({ eventTypes, productFeatures }, state) {
  const isAppWebhooksSupported = productFeatures.includes('webhooks-for-applications');
  const isRepoWebhooksSupported = productFeatures.includes('webhooks-for-repositories');

  if (!isAppWebhooksSupported && !isRepoWebhooksSupported) {
    return {
      ...state,
      isLoading: false,
      loadError: featureNotSupportedError,
    };
  }

  return {
    ...state,
    isLoading: false,
    loadError: null,
    availableEventTypes: eventTypes,
    isAppWebhooksSupported,
  };
}

function loadFailed(loadError, state) {
  return { ...state, isLoading: false, loadError };
}

function toggleEventType(eventType, state) {
  const { selectedEventTypes } = state,
    index = selectedEventTypes.indexOf(eventType);
  if (index === -1) {
    return { ...state, selectedEventTypes: [...selectedEventTypes, eventType] };
  } else {
    return { ...state, selectedEventTypes: remove(index, 1, selectedEventTypes) };
  }
}

function saveFulfilled(_, state) {
  return {
    ...state,
    submitMaskState: true,
    saveError: null,
  };
}

function saveFailed(saveError, state) {
  return {
    ...state,
    submitMaskState: null,
    saveError,
  };
}

const reducerActionMap = {
  [EDIT_WEBHOOK_LOAD_REQUESTED]: () => initialState,
  [EDIT_WEBHOOK_LOAD_FULFILLED]: loadFulfilled,
  [EDIT_WEBHOOK_LOAD_FAILED]: loadFailed,
  [EDIT_WEBHOOK_TOGGLE_EVENT_TYPE]: toggleEventType,
  [EDIT_WEBHOOK_SET_URL]: setTextInput('url', urlValidator),
  [EDIT_WEBHOOK_SET_DESCRIPTION]: setTextInput('description'),
  [EDIT_WEBHOOK_SET_SECRET_KEY]: setTextInput('secretKey'),
  [EDIT_WEBHOOK_SAVE_REQUESTED]: propSetConst('submitMaskState', false),
  [EDIT_WEBHOOK_SAVE_FULFILLED]: saveFulfilled,
  [EDIT_WEBHOOK_SAVE_FAILED]: saveFailed,
  [EDIT_WEBHOOK_SAVE_SUBMIT_MASK_TIMER_DONE]: propSetConst('submitMaskState', null),
};

const editWebhookReducer = createReducerFromActionMap(reducerActionMap, initialState);
export default editWebhookReducer;
