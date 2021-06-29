/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { always, any, compose, find, isEmpty, pick, test, propEq, clone } from 'ramda';
import { nxTextInputStateHelpers } from '@sonatype/react-shared-components';
import { createReducerFromActionMap, propSetConst } from '../../util/reduxUtil';
import { pathSet } from '../../util/jsUtil';
import {
  combineValidators,
  validateForm,
  validateNonEmpty,
  validatePatternMatch,
  validateNameCharacters,
  validateUsernameCharacters,
} from '../../util/validationUtil';
import {
  CREATE_USER_LOAD_REQUESTED,
  CREATE_USER_LOAD_FULFILLED,
  CREATE_USER_LOAD_FAILED,
  CREATE_USER_SAVE_REQUESTED,
  CREATE_USER_SAVE_FULFILLED,
  CREATE_USER_SAVE_FAILED,
  CREATE_USER_SAVE_SUBMIT_MASK_TIMER_DONE,
  CREATE_USER_SET_FIRST_NAME,
  CREATE_USER_SET_LAST_NAME,
  CREATE_USER_SET_EMAIL,
  CREATE_USER_SET_USERNAME,
  CREATE_USER_SET_PASSWORD,
  CREATE_USER_SET_MATCH_PASSWORD,
  CREATE_USER_RESET_FORM,
  fullTextFields,
} from './userFormActions';

const { initialState: initUserInput, userInput } = nxTextInputStateHelpers;

export const initialState = Object.freeze({
  loading: true,
  loadError: null,
  isDirty: false,
  validationError: null,
  submitMaskState: null,
  saveError: null,
  inputFields: {
    firstName: initUserInput(''),
    lastName: initUserInput(''),
    email: initUserInput(''),
    username: initUserInput(''),
    password: initUserInput(''),
    matchPassword: initUserInput(''),
  },
  users: [],
});

const clearedErrors = pick(['loadError', 'saveError'], initialState);

const updatedComputedProps = compose(computeIsDirty, computeValidationError);

function computeIsDirty(state) {
  const { inputFields } = state;
  const isDirty = any((prop) => !isEmpty(inputFields[prop].value), fullTextFields);

  return pathSet(['isDirty'], isDirty, state);
}

function computeValidationError(state) {
  const { inputFields } = state;
  const validationError = validateForm(clone(inputFields));

  return pathSet(['validationError'], validationError, state);
}

const validateDoubleWhitespace = (payload) =>
  test(/ {2,}|\t/, payload) ? 'No leading, trailing or double spaces or tabs' : null;

const nameValidator = combineValidators([validateNonEmpty, validateDoubleWhitespace, validateNameCharacters]);

const emailValidator = combineValidators([
  validateNonEmpty,
  validatePatternMatch(/[\w.]+@[\w.]+\.\w+/, 'Use valid format: abc@xyz.com'),
]);

const setInput = (fieldName, validator) => (payload, state) => {
  const textInput = userInput(validator, payload);
  const newState = pathSet(['inputFields', fieldName], textInput, state);

  return updatedComputedProps(newState);
};

const setUsernameInput = (payload, state) => {
  const validateDuplication = (value) => {
    const isDuplicate = propEq('usernameLowercase', value.toLowerCase());
    return find(isDuplicate, state.users) ? 'Username already taken' : null;
  };

  const validator = combineValidators([validateNonEmpty, validateUsernameCharacters, validateDuplication]);

  return setInput('username', validator)(payload, state);
};

const setPasswordInput = (payload, state) => {
  const input = userInput(validateNonEmpty, payload);
  const newState = pathSet(['inputFields', 'password'], input, state);

  return validateMatchPassword(newState);
};

const setPasswordMatchInput = (payload, state) => {
  const input = userInput(validateNonEmpty, payload);
  const newState = pathSet(['inputFields', 'matchPassword'], input, state);

  return validateMatchPassword(newState);
};

const validatePassword = ({ inputFields }) => {
  const { password, matchPassword } = inputFields;

  const notEqual = password.value !== matchPassword.value;
  const isPristine = matchPassword.isPristine;

  return !isEmpty(matchPassword.value) && notEqual && !isPristine ? 'Passwords must match!' : null;
};

const validateMatchPassword = (state) => {
  const error = validatePassword(state);
  const leftoverError = !error && state.inputFields.matchPassword.validationErrors === 'Passwords must match!';

  if (leftoverError) {
    return updatedComputedProps(pathSet(['inputFields', 'matchPassword', 'validationErrors'], null, state));
  }

  return error
    ? updatedComputedProps(pathSet(['inputFields', 'matchPassword', 'validationErrors'], error, state))
    : updatedComputedProps(state);
};

function loadFulfilled(payload, state) {
  return {
    ...state,
    loading: false,
    users: payload,
    ...clearedErrors,
  };
}

function loadFailed(payload, state) {
  return {
    ...state,
    loading: false,
    loadError: payload,
  };
}

function saveRequested(_, state) {
  return {
    ...state,
    submitMaskState: false,
    ...clearedErrors,
  };
}

function saveFulfilled(_, state) {
  return {
    ...state,
    submitMaskState: true,
    isDirty: false,
    inputFields: {
      ...initialState.inputFields,
    },
    ...clearedErrors,
  };
}

function saveFailed(payload, state) {
  return {
    ...state,
    saveError: payload,
    submitMaskState: null,
  };
}

const reducerActionMap = {
  [CREATE_USER_LOAD_REQUESTED]: always(initialState),
  [CREATE_USER_LOAD_FULFILLED]: loadFulfilled,
  [CREATE_USER_LOAD_FAILED]: loadFailed,
  [CREATE_USER_SET_FIRST_NAME]: setInput('firstName', nameValidator),
  [CREATE_USER_SET_LAST_NAME]: setInput('lastName', nameValidator),
  [CREATE_USER_SET_EMAIL]: setInput('email', emailValidator),
  [CREATE_USER_SET_USERNAME]: setUsernameInput,
  [CREATE_USER_SET_PASSWORD]: setPasswordInput,
  [CREATE_USER_SET_MATCH_PASSWORD]: setPasswordMatchInput,
  [CREATE_USER_SAVE_REQUESTED]: saveRequested,
  [CREATE_USER_SAVE_FULFILLED]: saveFulfilled,
  [CREATE_USER_SAVE_FAILED]: saveFailed,
  [CREATE_USER_SAVE_SUBMIT_MASK_TIMER_DONE]: propSetConst('submitMaskState', null),
  [CREATE_USER_RESET_FORM]: always(initialState),
};

const reducer = createReducerFromActionMap(reducerActionMap, initialState);
export default reducer;
