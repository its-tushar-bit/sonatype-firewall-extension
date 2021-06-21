/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import axios from 'axios';
import { compose, mapObjIndexed, prop, pick } from 'ramda';
import { SUBMIT_MASK_SUCCESS_VISIBLE_TIME_MS } from '@sonatype/react-shared-components';
import { Messages } from '../../util/CommonServices';
import { noPayloadActionCreator, payloadParamActionCreator } from '../../util/reduxUtil';
import { checkPermissions } from '../../util/authorizationUtil';
import { getUserUrl } from '../../util/CLMLocation';
import { stateGo } from '../../reduxUiRouter/routerActions';

export const CREATE_USER_SET_FIRST_NAME = 'CREATE_USER_SET_FIRST_NAME';
export const CREATE_USER_SET_LAST_NAME = 'CREATE_USER_SET_LAST_NAME';
export const CREATE_USER_SET_EMAIL = 'CREATE_USER_SET_EMAIL';
export const CREATE_USER_SET_USERNAME = 'CREATE_USER_SET_USERNAME';
export const CREATE_USER_SET_PASSWORD = 'CREATE_USER_SET_PASSWORD';
export const CREATE_USER_SET_MATCH_PASSWORD = 'CREATE_USER_SET_MATCH_PASSWORD';

export const setFirstName = payloadParamActionCreator(CREATE_USER_SET_FIRST_NAME);
export const setLastName = payloadParamActionCreator(CREATE_USER_SET_LAST_NAME);
export const setEmail = payloadParamActionCreator(CREATE_USER_SET_EMAIL);
export const setUserName = payloadParamActionCreator(CREATE_USER_SET_USERNAME);
export const setPassword = payloadParamActionCreator(CREATE_USER_SET_PASSWORD);
export const setMatchPassword = payloadParamActionCreator(CREATE_USER_SET_MATCH_PASSWORD);

export const CREATE_USER_RESET_FORM = 'CREATE_USER_RESET_FORM';
export const resetForm = noPayloadActionCreator(CREATE_USER_RESET_FORM);

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

export const CREATE_USER_SAVE_SUBMIT_MASK_TIMER_DONE = 'CREATE_USER_SAVE_SUBMIT_MASK_TIMER_DONE';

const saveRequested = noPayloadActionCreator(CREATE_USER_SAVE_REQUESTED);
const saveFulfilled = noPayloadActionCreator(CREATE_USER_SAVE_FULFILLED);
const saveFailed = payloadParamActionCreator(CREATE_USER_SAVE_FAILED);

function startSubmitMaskSuccessTimer(dispatch) {
  setTimeout(() => {
    dispatch({ type: CREATE_USER_SAVE_SUBMIT_MASK_TIMER_DONE });
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
