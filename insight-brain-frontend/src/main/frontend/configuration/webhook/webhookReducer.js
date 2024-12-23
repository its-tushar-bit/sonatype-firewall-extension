/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { remove, slice, startsWith, any, isEmpty } from 'ramda';
import { nxTextInputStateHelpers } from '@sonatype/react-shared-components';
import { createReducerFromActionMap, propSetConst } from '../../util/reduxUtil';
import {
  EDIT_WEBHOOK_LOAD_REQUESTED,
  EDIT_WEBHOOK_LOAD_FULFILLED,
  EDIT_WEBHOOK_LOAD_EDIT_FULFILLED,
  EDIT_WEBHOOK_LOAD_FAILED,
  EDIT_WEBHOOK_TOGGLE_EVENT_TYPE,
  EDIT_WEBHOOK_SET_URL,
  EDIT_WEBHOOK_SET_DESCRIPTION,
  EDIT_WEBHOOK_SET_SECRET_KEY,
  EDIT_WEBHOOK_SAVE_REQUESTED,
  EDIT_WEBHOOK_SUBMIT_MASK_TIMER_DONE,
  EDIT_WEBHOOK_SAVE_FULFILLED,
  EDIT_WEBHOOK_SAVE_FAILED,
  EDIT_WEBHOOK_DELETE_REQUESTED,
  EDIT_WEBHOOK_DELETE_FULFILLED,
  EDIT_WEBHOOK_DELETE_FAILED,
  EDIT_WEBHOOK_FETCH_PRODUCT_FEATURES_FULFILLED,
  EDIT_WEBHOOK_FETCH_EVENT_TYPES_FULFILLED,
  EDIT_WEBHOOK_FETCH_WEBHOOKS_FULFILLED,
  EDIT_WEBHOOK_SET_IS_URL_HTTP,
  validateUrlIsHttp,
} from './webhookActions';
import { eqValues, isNilOrEmpty, pathSet, propSet } from '../../util/jsUtil';
import { combineValidators, validateNonEmpty } from '../../util/validationUtil';

const { initialState: initUserInput, userInput } = nxTextInputStateHelpers;

const invalidUrlPrefixError = 'Webhook URL must start with http:// or https://';

export const initialState = Object.freeze({
  isLoading: true,
  loadError: null,
  saveError: null,
  deleteError: null,
  formValidationMessage: null,
  updateMaskState: null,
  deleteMaskState: null,
  availableEventTypes: [],
  selectedEventTypes: [],
  isAppWebhooksSupported: false,
  isDirty: false,
  inputFields: {
    url: initUserInput(''),
    description: initUserInput(''),
    secretKey: initUserInput(''),
  },
  webhooks: [],
  serverData: {},
  isUrlHTTP: false,
});

const textFields = ['url', 'description', 'secretKey'];

const computeIsDirty = (state) => {
  const { inputFields, serverData, selectedEventTypes } = state;
  let isDirtyTextProps, isDirtyEventTypes;

  if (!isEmpty(serverData)) {
    isDirtyTextProps = any((prop) => inputFields[prop].trimmedValue !== serverData[prop], textFields);
    isDirtyEventTypes = !eqValues(selectedEventTypes, serverData.eventTypes);
  } else {
    isDirtyTextProps = any((prop) => !isNilOrEmpty(inputFields[prop].trimmedValue), textFields);
    isDirtyEventTypes = !isNilOrEmpty(selectedEventTypes);
  }
  const isDirty = isDirtyTextProps || isDirtyEventTypes;
  return { ...state, isDirty };
};

const setTextInput = (fieldName, validator) => (payload, state) => {
  const textInput = userInput(validator, payload);
  const isUrlHTTP = validateUrlIsHttp(textInput.trimmedValue);
  return { ...computeIsDirty(pathSet(['inputFields', fieldName], textInput, state)), isUrlHTTP };
};

function validateWebhookUrl(value) {
  return startsWith(slice(0, value.length, 'http://'), value) || startsWith(slice(0, value.length, 'https://'), value)
    ? null
    : invalidUrlPrefixError;
}

const urlValidator = combineValidators([validateNonEmpty, validateWebhookUrl]);

function loadFulfilled(_, state) {
  return {
    ...state,
    isLoading: false,
    loadError: null,
  };
}

function loadEditFulfilled(payload, state) {
  return {
    ...state,
    isLoading: false,
    loadError: null,
    selectedEventTypes: payload.eventTypes,
    inputFields: {
      description: initUserInput(payload.description),
      url: initUserInput(payload.url),
      secretKey: initUserInput(payload.secretKey),
    },
    serverData: payload,
  };
}

function loadFailed(loadError, state) {
  return { ...state, isLoading: false, loadError };
}

function toggleEventType(eventType, state) {
  const { selectedEventTypes } = state,
    index = selectedEventTypes.indexOf(eventType);
  if (index === -1) {
    return computeIsDirty({ ...state, selectedEventTypes: [...selectedEventTypes, eventType] });
  } else {
    return computeIsDirty({ ...state, selectedEventTypes: remove(index, 1, selectedEventTypes) });
  }
}

function saveFulfilled(_, state) {
  return {
    ...state,
    updateMaskState: true,
    saveError: null,
    isDirty: false,
  };
}

function saveFailed(saveError, state) {
  return {
    ...state,
    updateMaskState: null,
    saveError,
  };
}
function deleteFulfilled(_, state) {
  return {
    ...state,
    isDirty: false,
    deleteMaskState: true,
    deleteError: null,
  };
}

function deleteFailed(deleteError, state) {
  return {
    ...state,
    deleteMaskState: null,
    deleteError,
  };
}

function resetSubmitMaskState(_, state) {
  return {
    ...state,
    deleteMaskState: null,
    updateMaskState: null,
  };
}

function saveRequested(_, state) {
  return { ...state, updateMaskState: false };
}

function updateIfUrlHttpInState(payload, state) {
  return { ...state, isUrlHTTP: payload };
}

const reducerActionMap = {
  [EDIT_WEBHOOK_LOAD_REQUESTED]: () => initialState,
  [EDIT_WEBHOOK_FETCH_PRODUCT_FEATURES_FULFILLED]: propSet('isAppWebhooksSupported'),
  [EDIT_WEBHOOK_FETCH_EVENT_TYPES_FULFILLED]: propSet('availableEventTypes'),
  [EDIT_WEBHOOK_FETCH_WEBHOOKS_FULFILLED]: propSet('webhooks'),
  [EDIT_WEBHOOK_LOAD_FULFILLED]: loadFulfilled,
  [EDIT_WEBHOOK_LOAD_EDIT_FULFILLED]: loadEditFulfilled,
  [EDIT_WEBHOOK_LOAD_FAILED]: loadFailed,
  [EDIT_WEBHOOK_TOGGLE_EVENT_TYPE]: toggleEventType,
  [EDIT_WEBHOOK_SET_URL]: setTextInput('url', urlValidator),
  [EDIT_WEBHOOK_SET_DESCRIPTION]: setTextInput('description'),
  [EDIT_WEBHOOK_SET_SECRET_KEY]: setTextInput('secretKey'),
  [EDIT_WEBHOOK_SAVE_REQUESTED]: saveRequested,
  [EDIT_WEBHOOK_SAVE_FULFILLED]: saveFulfilled,
  [EDIT_WEBHOOK_SAVE_FAILED]: saveFailed,
  [EDIT_WEBHOOK_SUBMIT_MASK_TIMER_DONE]: resetSubmitMaskState,
  [EDIT_WEBHOOK_DELETE_REQUESTED]: propSetConst('deleteMaskState', false),
  [EDIT_WEBHOOK_DELETE_FULFILLED]: deleteFulfilled,
  [EDIT_WEBHOOK_DELETE_FAILED]: deleteFailed,
  [EDIT_WEBHOOK_SET_IS_URL_HTTP]: updateIfUrlHttpInState,
};

const webhookReducer = createReducerFromActionMap(reducerActionMap, initialState);
export default webhookReducer;
