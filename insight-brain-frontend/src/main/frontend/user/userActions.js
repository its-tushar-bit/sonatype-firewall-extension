/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import axios from 'axios';
import { always, path, propEq } from 'ramda';
import { SUBMIT_MASK_SUCCESS_VISIBLE_TIME_MS } from '@sonatype/react-shared-components';
import { actions as unsavedChangesModalActions } from 'MainRoot/modals/unsavedChangesModal/unsavedChangesModalSlice';
import { Messages } from 'MainRoot/util/CommonServices';
import { isAuthorized } from 'MainRoot/util/permissionService';
import {
  getShouldDisplayDefaultPasswordWarning,
  getSessionLogoutUrl,
  getChangeMyPasswordUrl,
} from 'MainRoot/util/CLMLocation';
import pendoService from '../pendo/mainBundlePendoService';
import { submitTelemetryData } from 'MainRoot/util/telemetryUtils';
import { ensureUserLoggedIn } from 'MainRoot/user/userSessionSlice';
import { logoutRedirection } from 'MainRoot/util/urlUtil';
import {
  LOAD_USER_REQUESTED,
  LOAD_USER_FULFILLED,
  LOAD_USER_FAILED,
  USER_LOGGED_OUT,
  CHANGE_PASSWORD_REQUESTED,
  CHANGE_PASSWORD_FULFILLED,
  CHANGE_PASSWORD_FAILED,
  CHANGE_PASSWORD_STATUS_RESET,
  DEFAULT_ADMIN_PASSWORD_CHANGED,
} from './userActionTypes';

export {
  LOAD_USER_REQUESTED,
  LOAD_USER_FULFILLED,
  LOAD_USER_FAILED,
  USER_LOGGED_OUT,
  CHANGE_PASSWORD_REQUESTED,
  CHANGE_PASSWORD_FULFILLED,
  CHANGE_PASSWORD_FAILED,
  CHANGE_PASSWORD_STATUS_RESET,
  DEFAULT_ADMIN_PASSWORD_CHANGED,
};

function fetchUserDisplayPasswordWarningConfig() {
  const warningPromiseUrl = getShouldDisplayDefaultPasswordWarning();
  return (
    isAuthorized(['CONFIGURE_SYSTEM'])
      .then(function (isAdmin) {
        if (isAdmin) {
          // user is admin, check if we need to display the password warning
          return axios.get(warningPromiseUrl).then((response) => propEq('data', true)(response));
        } else {
          // user is not admin, don't display the password warning
          return false;
        }
      })
      // if this call fails, do not fail the overall user lookup
      .catch(always(false))
  );
}

function fetchUserFulfilled(response) {
  if (response && response.shouldDisplayWarning) {
    fireTelemetryEventWarningShown();
  }
  return {
    type: LOAD_USER_FULFILLED,
    payload: response, // { shouldDisplayWarning, currentUser }
  };
}

function fetchUserFailed(err) {
  return {
    type: LOAD_USER_FAILED,
    payload: err,
  };
}

function loadUser() {
  return async (dispatch) => {
    dispatch({ type: LOAD_USER_REQUESTED });

    const userDataRecovered = {
      currentUser: null,
      shouldDisplayWarning: false,
    };

    const resultUserLoggedIn = await dispatch(ensureUserLoggedIn());
    if (resultUserLoggedIn && resultUserLoggedIn.payload) {
      if (resultUserLoggedIn.payload.error) {
        dispatch(fetchUserFailed(resultUserLoggedIn.payload.error));
        return;
      }

      // We need to wait this action to be completed before returning with the whole response object
      userDataRecovered.shouldDisplayWarning = await fetchUserDisplayPasswordWarningConfig();
      userDataRecovered.currentUser = resultUserLoggedIn.payload;
      dispatch(fetchUserFulfilled(userDataRecovered));
    } else {
      dispatch(fetchUserFailed(resultUserLoggedIn?.error));
    }
  };
}

function onLogoutConfirmation() {
  return async (dispatch) => {
    dispatch({ type: USER_LOGGED_OUT });
    const serverLogoutRequest = () => axios.delete(getSessionLogoutUrl());
    await pendoService.flush();
    const resultServerLogout = await serverLogoutRequest();
    // Clear any onbeforeunload handlers to avoid being prevented to leave the current page by using redirection.
    // Otherwise a browser blocking alert dialog will appear
    // IMPORTANT: this handler needs to be cleared otherwise it will make fail firewallOnboarding functional tests
    window.onbeforeunload = null;

    let toLocation;
    if (resultServerLogout && resultServerLogout.error) {
      toLocation = {
        headers: {},
        error: resultServerLogout.error?.message,
      };
    } else {
      toLocation = resultServerLogout.headers['Location'] || resultServerLogout.headers['location'];
    }
    logoutRedirection(toLocation);
  };
}

function logout() {
  return async (dispatch, getState) => {
    const state = getState();
    const currentState = state.router.currentState;
    const isDirtyPath = currentState.data && currentState.data.isDirty;
    const isCurrentRouteDirty = isDirtyPath ? path(isDirtyPath, state) : false;

    if (isCurrentRouteDirty) {
      return dispatch(unsavedChangesModalActions.open()).then(
        () => dispatch(onLogoutConfirmation()),
        () => {}
      );
    } else {
      return dispatch(onLogoutConfirmation());
    }
  };
}

function changePassword({ oldPassword, newPassword }) {
  return (dispatch) => {
    dispatch({ type: CHANGE_PASSWORD_REQUESTED });
    axios
      .put(getChangeMyPasswordUrl(), { oldPassword, newPassword })
      .then(() => {
        dispatch({ type: CHANGE_PASSWORD_FULFILLED });
        dispatch(passwordChanged());
      })
      .catch((err) => {
        dispatch({
          type: CHANGE_PASSWORD_FAILED,
          payload: {
            message: Messages.getHttpErrorMessage(err),
          },
        });
      });
  };
}

function resetChangedPasswordStatus() {
  return (dispatch) => dispatch({ type: CHANGE_PASSWORD_STATUS_RESET });
}

function dispatchDefaultAdminPasswordChanged(dispatch) {
  fireTelemetryEventPasswordChanged();
  dispatch({ type: DEFAULT_ADMIN_PASSWORD_CHANGED });
}

function passwordChanged() {
  return (dispatch, getState) => {
    setTimeout(() => {
      dispatch(resetChangedPasswordStatus());
    }, SUBMIT_MASK_SUCCESS_VISIBLE_TIME_MS);
    const { user } = getState();
    if (user.shouldDisplayNotice && user.isDefaultUser) {
      dispatchDefaultAdminPasswordChanged(dispatch);
    }
  };
}

function passwordChangedForUser(selectedUser) {
  return (dispatch, getState) => {
    const { user } = getState();
    const isSelectedDefaultUser = selectedUser.username === 'admin';
    //Only fire the events if the flag was shown AND passwd was changed FOR the default admin
    if (user.shouldDisplayNotice && isSelectedDefaultUser) {
      dispatchDefaultAdminPasswordChanged(dispatch);
    }
  };
}

function fireTelemetryEventWarningShown() {
  submitTelemetryData('ADMIN_PASSWORD_CHANGE', {
    action: 'WARNING_SHOWN',
  });
}

function fireTelemetryEventPasswordChanged() {
  submitTelemetryData('ADMIN_PASSWORD_CHANGE', {
    action: 'PASSWORD_CHANGED_FROM_DEFAULT',
  });
}

export default {
  loadUser,
  logout,
  changePassword,
  passwordChanged,
  passwordChangedForUser,
  resetChangedPasswordStatus,
};
