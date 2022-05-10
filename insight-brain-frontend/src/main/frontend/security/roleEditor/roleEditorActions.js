/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import axios from 'axios';
import { SUBMIT_MASK_SUCCESS_VISIBLE_TIME_MS } from '@sonatype/react-shared-components';

import { noPayloadActionCreator, payloadParamActionCreator } from '../../util/reduxUtil';
import { getPermissions, authErrorMessage } from '../../util/authorizationUtil';
import { getRoleByIdUrl, getRoleForNewUrl, getRoleListUrl } from '../../util/CLMLocation';
import { stateGo } from '../../reduxUiRouter/routerActions';
import { Messages } from '../../utilAngular/CommonServices';
import { mapObjIndexed, prop } from 'ramda';

export const ROLE_EDITOR_SET_READONLY = 'ROLE_EDITOR_SET_READONLY';
const setReadOnly = payloadParamActionCreator(ROLE_EDITOR_SET_READONLY);

export const ROLE_EDITOR_LOAD_REQUESTED = 'ROLE_EDITOR_LOAD_REQUESTED';
export const ROLE_EDITOR_LOAD_FULFILLED = 'ROLE_EDITOR_LOAD_FULFILLED';
export const ROLE_EDITOR_LOAD_FAILED = 'ROLE_EDITOR_LOAD_FAILED';

const loadRequested = noPayloadActionCreator(ROLE_EDITOR_LOAD_REQUESTED);
const loadFulfilled = payloadParamActionCreator(ROLE_EDITOR_LOAD_FULFILLED);
const loadFailed = payloadParamActionCreator(ROLE_EDITOR_LOAD_FAILED);

export const ROLE_EDITOR_SET_ROLE_NAME = 'ROLE_EDITOR_SET_ROLE_NAME';
export const ROLE_EDITOR_SET_ROLE_DESCRIPTION = 'ROLE_EDITOR_SET_ROLE_DESCRIPTION';
export const ROLE_EDITOR_TOGGLE_VALUE = 'ROLE_EDITOR_TOGGLE_VALUE';

export const setRoleName = payloadParamActionCreator(ROLE_EDITOR_SET_ROLE_NAME);
export const setRoleDescription = payloadParamActionCreator(ROLE_EDITOR_SET_ROLE_DESCRIPTION);
export const toggleValue = payloadParamActionCreator(ROLE_EDITOR_TOGGLE_VALUE);

export const ROLE_EDITOR_UPDATE_REQUESTED = 'ROLE_EDITOR_UPDATE_REQUESTED';
export const ROLE_EDITOR_UPDATE_FULFILLED = 'ROLE_EDITOR_UPDATE_FULFILLED';
export const ROLE_EDITOR_UPDATE_FAILED = 'ROLE_EDITOR_UPDATE_FAILED';

const updateRequested = noPayloadActionCreator(ROLE_EDITOR_UPDATE_REQUESTED);
const updateFulfilled = noPayloadActionCreator(ROLE_EDITOR_UPDATE_FULFILLED);
const updateFailed = payloadParamActionCreator(ROLE_EDITOR_UPDATE_FAILED);

export const ROLE_EDITOR_SAVE_SUBMIT_MASK_TIMER_DONE = 'ROLE_EDITOR_SAVE_SUBMIT_MASK_TIMER_DONE';
export const ROLE_EDITOR_DELETE_MASK_TIMER_DONE = 'ROLE_EDITOR_DELETE_MASK_TIMER_DONE';

const updateMaskTimerDone = noPayloadActionCreator(ROLE_EDITOR_SAVE_SUBMIT_MASK_TIMER_DONE);
const deleteMaskTimerDone = noPayloadActionCreator(ROLE_EDITOR_DELETE_MASK_TIMER_DONE);

export const permissions = ['VIEW_ROLES', 'EDIT_ROLES'];

export const ROLE_EDITOR_DELETE_REQUESTED = 'ROLE_EDITOR_DELETE_REQUESTED';
export const ROLE_EDITOR_DELETE_FULFILLED = 'ROLE_EDITOR_DELETE_FULFILLED';
export const ROLE_EDITOR_DELETE_FAILED = 'ROLE_EDITOR_DELETE_FAILED';

const deleteRequested = noPayloadActionCreator(ROLE_EDITOR_DELETE_REQUESTED);
const deleteFulfilled = noPayloadActionCreator(ROLE_EDITOR_DELETE_FULFILLED);
const deleteFailed = payloadParamActionCreator(ROLE_EDITOR_DELETE_FAILED);

function startSubmitMaskSuccessTimer(dispatch, timerDoneFunc) {
  setTimeout(() => {
    dispatch(timerDoneFunc());
    dispatch(stateGo('rolesList'));
  }, SUBMIT_MASK_SUCCESS_VISIBLE_TIME_MS);
}

export function save() {
  return (dispatch, getState) => {
    dispatch(updateRequested());
    const {
      roleEditor: { formState },
    } = getState();
    const { name, description, id } = formState;
    const propsToTrim = {
      name,
      description,
    };
    const trimmedInputs = mapObjIndexed(prop('trimmedValue'), propsToTrim);
    const request = axios[id ? 'put' : 'post'](getRoleListUrl(), { ...formState, ...trimmedInputs });
    return request
      .then(() => {
        dispatch(updateFulfilled());
        startSubmitMaskSuccessTimer(dispatch, updateMaskTimerDone);
      })
      .catch((error) => {
        dispatch(updateFailed(Messages.getHttpErrorMessage(error)));
      });
  };
}

export function load(roleId) {
  return (dispatch) => {
    dispatch(loadRequested());
    let allowedToEdit;
    return getPermissions(permissions)
      .then((data) => {
        allowedToEdit = data.includes('EDIT_ROLES');
        if (!data.includes('VIEW_ROLES') || (!allowedToEdit && !roleId)) {
          throw authErrorMessage;
        }
      })
      .then(() => {
        const url = roleId ? getRoleByIdUrl(roleId) : getRoleForNewUrl();
        return axios.get(url);
      })
      .then(({ data }) => {
        dispatch(setReadOnly(!allowedToEdit || data.builtIn));
        dispatch(loadFulfilled(data));
      })
      .catch((error) => dispatch(loadFailed(Messages.getHttpErrorMessage(error))));
  };
}

export function deleteRole(roleId) {
  return (dispatch) => {
    dispatch(deleteRequested());
    return axios
      .delete(getRoleByIdUrl(roleId))
      .then(() => {
        dispatch(deleteFulfilled());
        startSubmitMaskSuccessTimer(dispatch, deleteMaskTimerDone);
      })
      .catch((error) => {
        dispatch(deleteFailed(Messages.getHttpErrorMessage(error)));
      });
  };
}
