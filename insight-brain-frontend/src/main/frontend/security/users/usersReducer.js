/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { always, any, compose, find, isEmpty, pick, test, propEq, clone, keys } from 'ramda';
import { nxTextInputStateHelpers } from '@sonatype/react-shared-components';
import { createReducerFromActionMap, propSetConst } from '../../util/reduxUtil';
import { pathSet, propSet } from '../../util/jsUtil';
import {
  combineValidators,
  validateForm,
  validateNonEmpty,
  validateEmailPatternMatch,
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
  USER_FORM_SUBMIT_MASK_TIMER_DONE,
  USER_FORM_DELETE_MASK_TIMER_DONE,
  USER_SET_FIRST_NAME,
  USER_SET_LAST_NAME,
  USER_SET_EMAIL,
  USER_SET_USERNAME,
  USER_SET_PASSWORD,
  USER_SET_MATCH_PASSWORD,
  USER_RESET_FORM,
  EDIT_USER_LOAD_REQUESTED,
  EDIT_USER_LOAD_FAILED,
  EDIT_USER_LOAD_FULFILLED,
  EDIT_USER_UPDATE_REQUESTED,
  EDIT_USER_UPDATE_FULFILLED,
  EDIT_USER_UPDATE_FAILED,
  DELETE_USER_REQUESTED,
  DELETE_USER_FULFILLED,
  DELETE_USER_FAILED,
  RESET_USER_PASSWORD_REQUESTED,
  RESET_USER_PASSWORD_FULFILLED,
  RESET_USER_PASSWORD_FAILED,
  RESET_USER_PASSWORD_RESET_VALUE,
  USER_LIST_LOAD_REQUESTED,
  USER_LIST_LOAD_FAILED,
  USER_LIST_LOAD_FULFILLED,
} from './usersActions';

const { initialState: initUserInput, userInput } = nxTextInputStateHelpers;

export const initialState = Object.freeze({
  loading: true,
  loadError: null,
  isDirty: false,
  validationError: null,
  submitMaskState: null,
  deleteMaskState: null,
  resetMaskState: null,
  saveError: null,
  deleteError: null,
  resetError: null,
  inputFields: {
    firstName: initUserInput(''),
    lastName: initUserInput(''),
    email: initUserInput(''),
    username: initUserInput(''),
    password: initUserInput(''),
    matchPassword: initUserInput(''),
  },
  users: [],
  currentUsername: null,
  selectedUserServerData: {},
  newPassword: null,
});

const clearedErrors = pick(['loadError', 'saveError', 'deleteError', 'resetError'], initialState);
const editFormFields = ['firstName', 'lastName', 'email'];
const inviteFormFields = pick(['firstName', 'lastName', 'email'], initialState.inputFields);

const updatedComputedProps = compose(computeIsDirty, computeValidationError);

function computeIsDirty(state) {
  const { inputFields, selectedUserServerData } = state;
  const isDirty = isEmpty(selectedUserServerData)
    ? any((prop) => !isEmpty(inputFields[prop].value), keys(inputFields))
    : any((prop) => inputFields[prop].trimmedValue !== selectedUserServerData[prop], editFormFields);

  return propSet('isDirty', isDirty, state);
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
  validateEmailPatternMatch('Use valid format: abc@xyz.com'),
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
    users: payload.users,
    currentUsername: payload.currentUsername,
    inputFields: payload.inviteMode ? inviteFormFields : initialState.inputFields,
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

function loadEditFulfilled(payload, state) {
  return {
    ...state,
    ...clearedErrors,
    loading: false,
    inputFields: {
      firstName: initUserInput(payload.firstName),
      lastName: initUserInput(payload.lastName),
      email: initUserInput(payload.email),
    },
    selectedUserServerData: payload,
  };
}

function loadEditFailed(payload, state) {
  return {
    ...state,
    loading: false,
    loadError: payload,
  };
}

function updateRequested(_, state) {
  return {
    ...state,
    submitMaskState: false,
    ...clearedErrors,
  };
}

function updateFulfilled(_, state) {
  return {
    ...state,
    submitMaskState: true,
    isDirty: false,
    ...clearedErrors,
  };
}

function updateFailed(payload, state) {
  return {
    ...state,
    saveError: payload,
    submitMaskState: null,
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

function deleteFailed(payload, state) {
  return {
    ...state,
    deleteMaskState: null,
    deleteError: payload,
  };
}

function resetFulfilled({ newPassword }, state) {
  return {
    ...state,
    resetMaskState: true,
    resetError: null,
    newPassword,
  };
}

function resetFailed(payload, state) {
  return {
    ...state,
    resetMaskState: null,
    resetError: payload,
  };
}

function removeNewPassword(_, state) {
  return {
    ...state,
    newPassword: null,
    resetMaskState: null,
    resetError: null,
  };
}

const reducerActionMap = {
  [USER_SET_FIRST_NAME]: setInput('firstName', nameValidator),
  [USER_SET_LAST_NAME]: setInput('lastName', nameValidator),
  [USER_SET_EMAIL]: setInput('email', emailValidator),
  [USER_SET_USERNAME]: setUsernameInput,
  [USER_SET_PASSWORD]: setPasswordInput,
  [USER_SET_MATCH_PASSWORD]: setPasswordMatchInput,
  [CREATE_USER_LOAD_REQUESTED]: always(initialState),
  [CREATE_USER_LOAD_FULFILLED]: loadFulfilled,
  [CREATE_USER_LOAD_FAILED]: loadFailed,
  [CREATE_USER_SAVE_REQUESTED]: saveRequested,
  [CREATE_USER_SAVE_FULFILLED]: saveFulfilled,
  [CREATE_USER_SAVE_FAILED]: saveFailed,
  [EDIT_USER_LOAD_REQUESTED]: always(initialState),
  [EDIT_USER_LOAD_FULFILLED]: loadEditFulfilled,
  [EDIT_USER_LOAD_FAILED]: loadEditFailed,
  [EDIT_USER_UPDATE_REQUESTED]: updateRequested,
  [EDIT_USER_UPDATE_FULFILLED]: updateFulfilled,
  [EDIT_USER_UPDATE_FAILED]: updateFailed,
  [DELETE_USER_REQUESTED]: propSetConst('deleteMaskState', false),
  [DELETE_USER_FULFILLED]: deleteFulfilled,
  [DELETE_USER_FAILED]: deleteFailed,
  [RESET_USER_PASSWORD_REQUESTED]: propSetConst('resetMaskState', false),
  [RESET_USER_PASSWORD_FULFILLED]: resetFulfilled,
  [RESET_USER_PASSWORD_FAILED]: resetFailed,
  [RESET_USER_PASSWORD_RESET_VALUE]: removeNewPassword,
  [USER_LIST_LOAD_REQUESTED]: always(initialState),
  [USER_LIST_LOAD_FULFILLED]: loadFulfilled,
  [USER_LIST_LOAD_FAILED]: loadFailed,
  [USER_FORM_SUBMIT_MASK_TIMER_DONE]: propSetConst('submitMaskState', null),
  [USER_FORM_DELETE_MASK_TIMER_DONE]: propSetConst('deleteMaskState', null),
  [USER_RESET_FORM]: always(initialState),
};

const reducer = createReducerFromActionMap(reducerActionMap, initialState);
export default reducer;
