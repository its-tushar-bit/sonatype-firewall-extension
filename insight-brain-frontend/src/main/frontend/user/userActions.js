/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { always, path, propEq } from 'ramda';
import { SUBMIT_MASK_SUCCESS_VISIBLE_TIME_MS } from '@sonatype/react-shared-components';

export const LOAD_USER_REQUESTED = 'LOAD_USER_REQUESTED';
export const LOAD_USER_FULFILLED = 'LOAD_USER_FULFILLED';
export const LOAD_USER_FAILED = 'LOAD_USER_FAILED';

export const USER_LOGGED_OUT = 'USER_LOGGED_OUT';

export const CHANGE_PASSWORD_REQUESTED = 'CHANGE_PASSWORD_REQUESTED';
export const CHANGE_PASSWORD_FULFILLED = 'CHANGE_PASSWORD_FULFILLED';
export const CHANGE_PASSWORD_FAILED = 'CHANGE_PASSWORD_FAILED';
export const CHANGE_PASSWORD_STATUS_RESET = 'CHANGE_PASSWORD_STATUS_RESET';
export const DEFAULT_ADMIN_PASSWORD_CHANGED = 'DEFAULT_ADMIN_PASSWORD_CHANGED';

function userActions(
  $rootScope,
  $q,
  $http,
  CurrentUser,
  CLMLocations,
  telemetryService,
  PermissionService,
  messages,
  pendoService,
  unsavedChangesModalService,
  $window
) {
  function fetchUser() {
    const warningPromiseUrl = CLMLocations.getShouldDisplayDefaultPasswordWarning(),
      shouldDisplayWarningPromise = PermissionService.isAuthorized(['CONFIGURE_SYSTEM'], true)
        .then(function (isAdmin) {
          if (isAdmin) {
            // user is admin, check if we need to display the password warning
            return $http.get(warningPromiseUrl).then(propEq('data', 'true'));
          } else {
            // user is not admin, don't display the password warning
            return false;
          }
        })
        // if this call fails, do not fail the overall user lookup
        .catch(always(false));

    return $q.all({
      currentUser: CurrentUser.waitForLogin(),
      shouldDisplayWarning: shouldDisplayWarningPromise,
    });
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
    return (dispatch) => {
      dispatch({ type: LOAD_USER_REQUESTED });

      return fetchUser()
        .then((response) => {
          dispatch(fetchUserFulfilled(response));
        })
        .catch((error) => {
          dispatch(fetchUserFailed(error));
          return $q.reject(error);
        });
    };
  }

  function logout() {
    return (dispatch, getState) => {
      const state = getState();
      const currentState = state.router.currentState;
      const isDirtyPath = currentState.data && currentState.data.isDirty;
      const unsavedChangesModal = currentState?.data?.unsavedChangesModal;
      const isCurrentRouteDirty = isDirtyPath ? path(isDirtyPath, state) : false;

      function onLogoutConfirmation() {
        return (dispatch) => {
          dispatch({ type: USER_LOGGED_OUT });
          const serverLogout = () => $http.delete(CLMLocations.getSessionLogoutUrl());
          pendoService
            .flush()
            // continue the logout whether the pendo flush succeeds or fails
            .then(serverLogout, serverLogout)
            .then(function (response) {
              $($window).unbind('beforeunload');
              $rootScope.$emit('logout', response.headers('Location'));
            });
        };
      }

      if (isCurrentRouteDirty) {
        unsavedChangesModalService.open(unsavedChangesModal).then(() => dispatch(onLogoutConfirmation()));
      } else {
        dispatch(onLogoutConfirmation());
      }
    };
  }

  function changePassword({ oldPassword, newPassword }) {
    return (dispatch) => {
      dispatch({ type: CHANGE_PASSWORD_REQUESTED });
      $http
        .put(CLMLocations.getChangeMyPasswordUrl(), { oldPassword, newPassword })
        .then(() => {
          dispatch({ type: CHANGE_PASSWORD_FULFILLED });
          dispatch(passwordChanged());
        })
        .catch((err) => {
          dispatch({
            type: CHANGE_PASSWORD_FAILED,
            payload: {
              message: messages.getHttpErrorMessage(err),
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
    //Notify all interested scopes that a height recalculation is needed.
    $rootScope.$broadcast('recalculateContainerHeights');
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
    telemetryService.submitData('ADMIN_PASSWORD_CHANGE', {
      action: 'WARNING_SHOWN',
    });
  }

  function fireTelemetryEventPasswordChanged() {
    telemetryService.submitData('ADMIN_PASSWORD_CHANGE', {
      action: 'PASSWORD_CHANGED_FROM_DEFAULT',
    });
  }

  return {
    loadUser,
    logout,
    changePassword,
    passwordChanged,
    passwordChangedForUser,
    resetChangedPasswordStatus,
  };
}
userActions.$inject = [
  '$rootScope',
  '$q',
  '$http',
  'CurrentUser',
  'CLMLocations',
  'telemetryService',
  'PermissionService',
  'Messages',
  'pendoService',
  'unsavedChangesModalService',
  '$window',
];
export default userActions;
