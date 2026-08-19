/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

import { nxTextInputStateHelpers } from '@sonatype/react-shared-components';

import {
  ROLE_EDITOR_DELETE_FAILED,
  ROLE_EDITOR_DELETE_FULFILLED,
  ROLE_EDITOR_DELETE_MASK_TIMER_DONE,
  ROLE_EDITOR_DELETE_REQUESTED,
  ROLE_EDITOR_LOAD_FAILED,
  ROLE_EDITOR_LOAD_FULFILLED,
  ROLE_EDITOR_LOAD_REQUESTED,
  ROLE_EDITOR_SAVE_SUBMIT_MASK_TIMER_DONE,
  ROLE_EDITOR_SET_READONLY,
  ROLE_EDITOR_SET_ROLE_DESCRIPTION,
  ROLE_EDITOR_SET_ROLE_NAME,
  ROLE_EDITOR_TOGGLE_VALUE,
  ROLE_EDITOR_UPDATE_FAILED,
  ROLE_EDITOR_UPDATE_FULFILLED,
  ROLE_EDITOR_UPDATE_REQUESTED,
} from './roleEditorActions';
import { createReducerFromActionMap, propSetConst } from '../../util/reduxUtil';
import { propSet } from '../../util/jsUtil';
import { clone, equals, mapObjIndexed, prop, any } from 'ramda';
import { combineValidators, validateNonEmpty } from '../../util/validationUtil';

const { initialState: initUserInput, userInput } = nxTextInputStateHelpers;

export const initialState = Object.freeze({
  deleteError: null,
  updateError: null,
  readonly: false,
  isDirty: false,
  loading: true,
  updateMaskState: null,
  deleteMaskState: null,
  loadError: null,
  builtIn: false,
  formState: {
    id: null,
    name: initUserInput(''),
    description: initUserInput(''),
    permissionCategories: [],
  },
  formStateFromServer: {},
});

const setInput = (fieldName, validator) => (payload, state) => {
  const textInput = userInput(validator, payload.value);
  const newState = {
    ...state,
    formState: {
      ...state.formState,
      [fieldName]: textInput,
    },
  };
  return {
    ...newState,
    isDirty: !isEqualFormState(newState.formState, state.formStateFromServer),
  };
};

function setNameInput(payload, state) {
  const {
    formState: { id: itemId },
  } = state;
  const duplicationValidator = (value) => {
    const exits = any((item) => item.name.toLowerCase() === value.toLowerCase() && item.id !== itemId, payload.roles);
    return exits ? 'Name is already in use' : null;
  };
  const validator = combineValidators([validateNonEmpty, duplicationValidator]);
  return setInput('name', validator)(payload, state);
}

function toggleValue(payload, state) {
  const { category, id } = payload;
  const permissionCategories = [...state.formState.permissionCategories];
  const permissionCategory = permissionCategories.find((cat) => cat.displayName === category);
  const permissionSelected = permissionCategory.permissions.find((permission) => permission.id === id);
  permissionSelected.allowed = !permissionSelected.allowed;
  const newState = {
    ...state,
    formState: {
      ...state.formState,
      permissionCategories: [...permissionCategories],
    },
  };
  return {
    ...newState,
    isDirty: !isEqualFormState(newState.formState, state.formStateFromServer),
  };
}

function isEqualFormState(currentFormState, formStateFromServer) {
  const propsToTrim = {
    name: currentFormState.name,
    description: currentFormState.description,
  };
  const trimmedInputs = mapObjIndexed(prop('trimmedValue'), propsToTrim);
  const newStateMapped = {
    ...currentFormState,
    ...trimmedInputs,
  };
  return equals(newStateMapped, formStateFromServer);
}

function loadFulfilled(payload, state) {
  const { permissionCategories, name, description, id, builtIn } = clone(payload);
  return {
    ...state,
    loading: false,
    loadError: null,
    builtIn,
    formState: {
      id,
      name: initUserInput(name || '', validateNonEmpty),
      description: initUserInput(description || '', validateNonEmpty),
      permissionCategories,
    },
    formStateFromServer: {
      id,
      name: name || '',
      description: description || '',
      permissionCategories: clone(permissionCategories),
    },
    isDirty: false,
  };
}

function loadFailed(payload, state) {
  return {
    ...state,
    loadError: payload,
    loading: false,
  };
}

function updateFailed(payload, state) {
  return {
    ...state,
    updateError: payload,
    loading: false,
    updateMaskState: null,
  };
}

function deleteFailed(payload, state) {
  return {
    ...state,
    deleteError: payload,
    deleteMaskState: null,
  };
}

function deleteFulfilled(_, state) {
  return {
    ...state,
    deleteMaskState: true,
    isDirty: false,
  };
}

const reducerActionMap = {
  [ROLE_EDITOR_SET_ROLE_NAME]: setNameInput,
  [ROLE_EDITOR_SET_ROLE_DESCRIPTION]: setInput('description', validateNonEmpty),
  [ROLE_EDITOR_TOGGLE_VALUE]: toggleValue,
  [ROLE_EDITOR_UPDATE_REQUESTED]: propSetConst('updateMaskState', false),
  [ROLE_EDITOR_UPDATE_FULFILLED]: (_, state) => ({
    ...state,
    isDirty: false,
    updateMaskState: true,
  }),
  [ROLE_EDITOR_UPDATE_FAILED]: updateFailed,
  [ROLE_EDITOR_SAVE_SUBMIT_MASK_TIMER_DONE]: propSetConst('updateMaskState', null),
  [ROLE_EDITOR_LOAD_REQUESTED]: () => initialState,
  [ROLE_EDITOR_LOAD_FULFILLED]: loadFulfilled,
  [ROLE_EDITOR_SET_READONLY]: propSet('readonly'),
  [ROLE_EDITOR_LOAD_FAILED]: loadFailed,
  [ROLE_EDITOR_DELETE_MASK_TIMER_DONE]: propSetConst('deleteMaskState', null),
  [ROLE_EDITOR_DELETE_REQUESTED]: propSetConst('deleteMaskState', false),
  [ROLE_EDITOR_DELETE_FULFILLED]: deleteFulfilled,
  [ROLE_EDITOR_DELETE_FAILED]: deleteFailed,
};

const roleEditorReducer = createReducerFromActionMap(reducerActionMap, initialState);

export default roleEditorReducer;
