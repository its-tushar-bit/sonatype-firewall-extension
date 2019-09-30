/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { always, propEq } from 'ramda';

export const DEFAULT_ADMIN_PASSWORD_CHANGED = 'DEFAULT_ADMIN_PASSWORD_CHANGED';
export const LOAD_USER_REQUESTED = 'LOAD_USER_REQUESTED';
export const LOAD_USER_FULFILLED = 'LOAD_USER_FULFILLED';
export const LOAD_USER_FAILED = 'LOAD_USER_FAILED';

function userActions($rootScope, $q, $http, CurrentUser, CLMLocations, telemetryService, PermissionService) {

  function fetchUser() {
    const warningPromiseUrl = CLMLocations.getShouldDisplayDefaultPasswordWarning(),
        shouldDisplayWarningPromise = PermissionService.isAuthorized(['CONFIGURE_SYSTEM'], true)
            .then(function(isAdmin) {
              if (isAdmin) {
                // user is admin, check if we need to display the password warning
                return $http.get(warningPromiseUrl).then(propEq('data', 'true'));
              }
              else {
                // user is not admin, don't display the password warning
                return false;
              }
            })
            // if this call fails, do not fail the overall user lookup
            .catch(always(false));

    return $q.all({
      currentUser: CurrentUser.waitForLogin(),
      shouldDisplayWarning: shouldDisplayWarningPromise
    });
  }

  function fetchUserFulfilled(response) {
    if (response && response.shouldDisplayWarning) {
      fireTelemetryEventWarningShown();
    }
    return {
      type: LOAD_USER_FULFILLED,
      payload: response // { shouldDisplayWarning, currentUser }
    };
  }

  function fetchUserFailed(err) {
    return {
      type: LOAD_USER_FAILED,
      payload: err
    };
  }

  function loadUser() {
    return dispatch => {
      dispatch({ type: LOAD_USER_REQUESTED });

      return fetchUser()
          .then(response => {
            dispatch(fetchUserFulfilled(response));
          })
          .catch(error => {
            dispatch(fetchUserFailed(error));
            return $q.reject(error);
          });
    };
  }

  function dispatchDefaultAdminPasswordChanged(dispatch) {
    fireTelemetryEventPasswordChanged();
    dispatch({ type: DEFAULT_ADMIN_PASSWORD_CHANGED });
    //Notify all interested scopes that a height recalculation is needed.
    $rootScope.$broadcast('recalculateContainerHeights');
  }

  function passwordChanged() {
    return (dispatch, getState) => {
      const {user} = getState();
      if (user.shouldDisplayNotice && user.isDefaultUser) {
        dispatchDefaultAdminPasswordChanged(dispatch);
      }
    };
  }

  function passwordChangedForUser(selectedUser) {
    return (dispatch, getState) => {
      const {user} = getState();
      const isSelectedDefaultUser = selectedUser.username === 'admin';
      //Only fire the events if the flag was shown AND passwd was changed FOR the default admin
      if (user.shouldDisplayNotice && isSelectedDefaultUser) {
        dispatchDefaultAdminPasswordChanged(dispatch);
      }
    };
  }

  function fireTelemetryEventWarningShown() {
    telemetryService.submitData('ADMIN_PASSWORD_CHANGE', {
      action: 'WARNING_SHOWN'
    });
  }

  function fireTelemetryEventPasswordChanged() {
    telemetryService.submitData('ADMIN_PASSWORD_CHANGE', {
      action: 'PASSWORD_CHANGED_FROM_DEFAULT'
    });
  }

  return {
    loadUser,
    passwordChanged,
    passwordChangedForUser
  };
}
userActions.$inject = ['$rootScope', '$q', '$http', 'CurrentUser', 'CLMLocations', 'telemetryService',
  'PermissionService'];
export default userActions;
