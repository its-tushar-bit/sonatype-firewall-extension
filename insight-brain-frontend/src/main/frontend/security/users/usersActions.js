/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import axios from 'axios';
import { compose, mapObjIndexed, prop, pick, find, map } from 'ramda';
import { SUBMIT_MASK_SUCCESS_VISIBLE_TIME_MS } from '@sonatype/react-shared-components';
import { Messages } from '../../utilAngular/CommonServices';
import { noPayloadActionCreator, payloadParamActionCreator } from '../../util/reduxUtil';
import { checkPermissions } from '../../util/authorizationUtil';
import {
  getUserUrl,
  getUserByIdUrl,
  getMultiTenantUserUrl,
  getMultiTenantUserByIdUrl,
  getUserResetPasswordByIdUrl,
  getSessionUrl,
} from '../../util/CLMLocation';
import { stateGo } from '../../reduxUiRouter/routerActions';
import userActions from '../../user/userActions';
import { actions as productFeaturesActions } from '../../productFeatures/productFeaturesSlice';
import { selectTenantMode } from '../../productFeatures/productFeaturesSelectors';

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
  return async (dispatch, getState) => {
    dispatch(loadRequested());

    try {
      await checkPermissions(['CONFIGURE_SYSTEM']);
      await dispatch(productFeaturesActions.fetchProductFeaturesIfNeeded());

      const tenantMode = selectTenantMode(getState()),
        inviteMode = tenantMode === 'multi-tenant',
        usersUrl = inviteMode ? getMultiTenantUserUrl() : getUserUrl();

      await axios.get(usersUrl).then(({ data }) => {
        dispatch(loadFulfilled({ users: data, currentUsername: null, inviteMode }));
      });
    } catch (e) {
      dispatch(loadFailed(Messages.getHttpErrorMessage(e)));
    }
  };
}

export const CREATE_USER_SAVE_REQUESTED = 'CREATE_USER_SAVE_REQUESTED';
export const CREATE_USER_SAVE_FULFILLED = 'CREATE_USER_SAVE_FULFILLED';
export const CREATE_USER_SAVE_FAILED = 'CREATE_USER_SAVE_FAILED';

export const USER_FORM_SUBMIT_MASK_TIMER_DONE = 'USER_FORM_SUBMIT_MASK_TIMER_DONE';
export const USER_FORM_DELETE_MASK_TIMER_DONE = 'USER_FORM_DELETE_MASK_TIMER_DONE';

const saveRequested = noPayloadActionCreator(CREATE_USER_SAVE_REQUESTED);
const saveFulfilled = noPayloadActionCreator(CREATE_USER_SAVE_FULFILLED);
const saveFailed = payloadParamActionCreator(CREATE_USER_SAVE_FAILED);

function startSubmitMaskSuccessTimer(dispatch) {
  setTimeout(() => {
    dispatch({ type: USER_FORM_SUBMIT_MASK_TIMER_DONE });
    dispatch(stateGo('users'));
  }, SUBMIT_MASK_SUCCESS_VISIBLE_TIME_MS);
}

function startDeleteMaskSuccessTimer(dispatch) {
  setTimeout(() => {
    dispatch({ type: USER_FORM_DELETE_MASK_TIMER_DONE });
    dispatch(stateGo('users', undefined, { reload: true }));
  }, SUBMIT_MASK_SUCCESS_VISIBLE_TIME_MS);
}

function startResetPasswordMaskSuccessTimer(dispatch) {
  setTimeout(() => {
    dispatch({ type: USER_FORM_SUBMIT_MASK_TIMER_DONE });
  }, SUBMIT_MASK_SUCCESS_VISIBLE_TIME_MS);
}

const textFields = ['firstName', 'lastName', 'email', 'username'];
const textPasswordFields = ['password', 'matchPassword'];

export const fullTextFields = [...textFields, ...textPasswordFields];

export function save() {
  return function (dispatch, getState) {
    dispatch(saveRequested());

    const textState = pick(textFields, getState().userConfiguration.inputFields);
    const passwordState = pick(['password'], getState().userConfiguration.inputFields);

    const textInputs = mapObjIndexed(prop('trimmedValue'), textState);
    const passwordInputs = mapObjIndexed(prop('value'), passwordState);

    const tenantMode = selectTenantMode(getState()),
      usersUrl = tenantMode === 'multi-tenant' ? getMultiTenantUserUrl() : getUserUrl(),
      multitenantUsername = tenantMode === 'multi-tenant' ? { username: textInputs.email } : null;

    return axios
      .post(usersUrl, { ...textInputs, ...passwordInputs, ...multitenantUsername })
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

    const { selectedUserServerData, inputFields } = getState().userConfiguration;

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

export const DELETE_USER_REQUESTED = 'DELETE_USER_REQUESTED';
export const DELETE_USER_FULFILLED = 'DELETE_USER_FULFILLED';
export const DELETE_USER_FAILED = 'DELETE_USER_FAILED';

const deleteRequested = noPayloadActionCreator(DELETE_USER_REQUESTED);
const deleteFulfilled = noPayloadActionCreator(DELETE_USER_FULFILLED);
const deleteFailed = payloadParamActionCreator(DELETE_USER_FAILED);

export function deleteUser(userId) {
  return async (dispatch, getState) => {
    dispatch(deleteRequested());

    try {
      await dispatch(productFeaturesActions.fetchProductFeaturesIfNeeded());

      const tenantMode = selectTenantMode(getState()),
        urlFn = tenantMode === 'multi-tenant' ? getMultiTenantUserByIdUrl : getUserByIdUrl;

      await axios.delete(urlFn(userId));

      dispatch(deleteFulfilled());
      startDeleteMaskSuccessTimer(dispatch);
    } catch (e) {
      dispatch(deleteFailed(Messages.getHttpErrorMessage(e)));
    }
  };
}

export const RESET_USER_PASSWORD_REQUESTED = 'RESET_USER_PASSWORD_REQUESTED';
export const RESET_USER_PASSWORD_FULFILLED = 'RESET_USER_PASSWORD_FULFILLED';
export const RESET_USER_PASSWORD_FAILED = 'RESET_USER_PASSWORD_FAILED';

const resetRequested = noPayloadActionCreator(RESET_USER_PASSWORD_REQUESTED);
const resetFulfilled = payloadParamActionCreator(RESET_USER_PASSWORD_FULFILLED);
const resetFailed = payloadParamActionCreator(RESET_USER_PASSWORD_FAILED);

export function resetPassword(userId, username) {
  return (dispatch) => {
    dispatch(resetRequested());

    return axios
      .put(getUserResetPasswordByIdUrl(userId))
      .then(({ data }) => {
        dispatch(resetFulfilled(data));
        dispatch(userActions().passwordChangedForUser({ username }));

        startResetPasswordMaskSuccessTimer(dispatch);
      })
      .catch(compose(dispatch, resetFailed, Messages.getHttpErrorMessage));
  };
}

export const RESET_USER_PASSWORD_RESET_VALUE = 'RESET_USER_PASSWORD_RESET_VALUE';
export const resetInitialNewPasswordValue = noPayloadActionCreator(RESET_USER_PASSWORD_RESET_VALUE);

export const USER_LIST_LOAD_REQUESTED = 'USER_LIST_LOAD_REQUESTED';
export const USER_LIST_LOAD_FAILED = 'USER_LIST_LOAD_FAILED';
export const USER_LIST_LOAD_FULFILLED = 'USER_LIST_LOAD_FULFILLED';

const loadListRequested = noPayloadActionCreator(USER_LIST_LOAD_REQUESTED);
const loadListFailed = payloadParamActionCreator(USER_LIST_LOAD_FAILED);
const loadListFulfilled = payloadParamActionCreator(USER_LIST_LOAD_FULFILLED);

export function loadListPage() {
  return async (dispatch, getState) => {
    dispatch(loadListRequested());

    try {
      await checkPermissions(['CONFIGURE_SYSTEM']);

      await dispatch(productFeaturesActions.fetchProductFeaturesIfNeeded());

      const tenantMode = selectTenantMode(getState()),
        usersPromise = tenantMode === 'multi-tenant' ? fetchMtiqUsers() : fetchOnPremUsers();

      const [users, { data: session }] = await Promise.all([usersPromise, axios.get(getSessionUrl())]);

      dispatch(loadListFulfilled({ users, currentUsername: session.username }));
    } catch (e) {
      dispatch(loadListFailed(Messages.getHttpErrorMessage(e)));
    }
  };
}

async function fetchOnPremUsers() {
  const response = await axios.get(getUserUrl());
  return response.data;
}

async function fetchMtiqUsers() {
  const response = await axios.get(getMultiTenantUserUrl());
  return map((user) => ({ ...user, id: user.username }), response.data);
}
