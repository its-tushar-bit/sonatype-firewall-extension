/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import axios from 'axios';
import { compose, mapObjIndexed, prop, pick, find } from 'ramda';
import { SUBMIT_MASK_SUCCESS_VISIBLE_TIME_MS } from '@sonatype/react-shared-components';
import { Messages } from '../../util/CommonServices';
import { noPayloadActionCreator, payloadParamActionCreator } from '../../util/reduxUtil';
import { checkPermissions } from '../../util/authorizationUtil';
import { getUserUrl } from '../../util/CLMLocation';
import { stateGo } from '../../reduxUiRouter/routerActions';

export const USER_SET_FIRST_NAME = 'USER_SET_FIRST_NAME';
export const USER_SET_LAST_NAME = 'USER_SET_LAST_NAME';
export const USER_SET_EMAIL = 'USER_SET_EMAIL';
export const USER_SET_USERNAME = 'USER_SET_USERNAME';
export const USER_SET_PASSWORD = 'USER_SET_PASSWORD';
export const USER_SET_MATCH_PASSWORD = 'USER_SET_MATCH_PASSWORD';

export const setFirstName = payloadParamActionCreator(USER_SET_FIRST_NAME);
export const setLastName = payloadParamActionCreator(USER_SET_LAST_NAME);
export const setEmail = payloadParamActionCreator(USER_SET_EMAIL);
export const setUserName = payloadParamActionCreator(USER_SET_USERNAME);
export const setPassword = payloadParamActionCreator(USER_SET_PASSWORD);
export const setMatchPassword = payloadParamActionCreator(USER_SET_MATCH_PASSWORD);

export const USER_RESET_FORM = 'USER_RESET_FORM';
export const resetForm = noPayloadActionCreator(USER_RESET_FORM);

export const CREATE_USER_LOAD_REQUESTED = 'CREATE_USER_LOAD_REQUESTED';
export const CREATE_USER_LOAD_FAILED = 'CREATE_USER_LOAD_FAILED';
export const CREATE_USER_LOAD_FULFILLED = 'CREATE_USER_LOAD_FULFILLED';

const loadRequested = noPayloadActionCreator(CREATE_USER_LOAD_REQUESTED);
const loadFailed = payloadParamActionCreator(CREATE_USER_LOAD_FAILED);
const loadFulfilled = payloadParamActionCreator(CREATE_USER_LOAD_FULFILLED);

export function loadCreateUserPage() {
  return (dispatch) => {
    dispatch(loadRequested());

    return checkPermissions(['CONFIGURE_SYSTEM'])
      .then(() => {
        return axios.get(getUserUrl()).then(({ data }) => {
          dispatch(loadFulfilled(data));
        });
      })
      .catch(compose(dispatch, loadFailed, Messages.getHttpErrorMessage));
  };
}

export const CREATE_USER_SAVE_REQUESTED = 'CREATE_USER_SAVE_REQUESTED';
export const CREATE_USER_SAVE_FULFILLED = 'CREATE_USER_SAVE_FULFILLED';
export const CREATE_USER_SAVE_FAILED = 'CREATE_USER_SAVE_FAILED';

export const USER_FORM_SUBMIT_MASK_TIMER_DONE = 'USER_FORM_SUBMIT_MASK_TIMER_DONE';

const saveRequested = noPayloadActionCreator(CREATE_USER_SAVE_REQUESTED);
const saveFulfilled = noPayloadActionCreator(CREATE_USER_SAVE_FULFILLED);
const saveFailed = payloadParamActionCreator(CREATE_USER_SAVE_FAILED);

function startSubmitMaskSuccessTimer(dispatch) {
  setTimeout(() => {
    dispatch({ type: USER_FORM_SUBMIT_MASK_TIMER_DONE });
    dispatch(stateGo('users'));
  }, SUBMIT_MASK_SUCCESS_VISIBLE_TIME_MS);
}

const textFields = ['firstName', 'lastName', 'email', 'username'];
const textPasswordFields = ['password', 'matchPassword'];

export const fullTextFields = [...textFields, ...textPasswordFields];

export function save() {
  return function (dispatch, getState) {
    dispatch(saveRequested());

    const textState = pick(textFields, getState().userForm.inputFields);
    const passwordState = pick(['password'], getState().userForm.inputFields);

    const textInputs = mapObjIndexed(prop('trimmedValue'), textState);
    const passwordInputs = mapObjIndexed(prop('value'), passwordState);

    return axios
      .post(getUserUrl(), { ...textInputs, ...passwordInputs })
      .then(() => {
        dispatch(saveFulfilled());
        startSubmitMaskSuccessTimer(dispatch);
      })
      .catch(compose(dispatch, saveFailed, Messages.getHttpErrorMessage));
  };
}

export const EDIT_USER_LOAD_REQUESTED = 'EDIT_USER_LOAD_REQUESTED';
export const EDIT_USER_LOAD_FAILED = 'EDIT_USER_LOAD_FAILED';
export const EDIT_USER_LOAD_FULFILLED = 'EDIT_USER_LOAD_FULFILLED';

const loadEditRequested = noPayloadActionCreator(EDIT_USER_LOAD_REQUESTED);
const loadEditFulfilled = payloadParamActionCreator(EDIT_USER_LOAD_FULFILLED);
const loadEditFailed = payloadParamActionCreator(EDIT_USER_LOAD_FAILED);

export function loadUserById(userId) {
  return (dispatch) => {
    dispatch(loadEditRequested());

    return checkPermissions(['CONFIGURE_SYSTEM'])
      .then(() => {
        return axios.get(getUserUrl(userId)).then(({ data }) => {
          const user = find((user) => user.id === userId, data);
          if (!user) {
            throw 'Unable to locate user';
          }

          dispatch(loadEditFulfilled(user));
        });
      })
      .catch(compose(dispatch, loadEditFailed, Messages.getHttpErrorMessage));
  };
}

export const EDIT_USER_UPDATE_REQUESTED = 'EDIT_USER_UPDATE_REQUESTED';
export const EDIT_USER_UPDATE_FULFILLED = 'EDIT_USER_UPDATE_FULFILLED';
export const EDIT_USER_UPDATE_FAILED = 'EDIT_USER_UPDATE_FAILED';

const updateRequested = noPayloadActionCreator(EDIT_USER_UPDATE_REQUESTED);
const updateFulfilled = noPayloadActionCreator(EDIT_USER_UPDATE_FULFILLED);
const updateFailed = payloadParamActionCreator(EDIT_USER_UPDATE_FAILED);

export function update() {
  return function (dispatch, getState) {
    dispatch(updateRequested());

    const { selectedUserServerData, inputFields } = getState().userForm;

    const textInputs = mapObjIndexed(prop('trimmedValue'), inputFields);

    return axios
      .put(getUserUrl(), { ...selectedUserServerData, ...textInputs })
      .then(() => {
        dispatch(updateFulfilled());
        startSubmitMaskSuccessTimer(dispatch);
      })
      .catch(compose(dispatch, updateFailed, Messages.getHttpErrorMessage));
  };
}
