/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

import mainBundlePendoService, { setUrlService } from 'MainRoot/pendo/mainBundlePendoService';
import { getGlobalPermissionTestUrl } from 'MainRoot/util/CLMContextLocation';
import { getSessionLogoutUrl, getSessionUrl, getShouldDisplayDefaultPasswordWarning } from 'MainRoot/util/CLMLocation';
import * as telemetryUtils from 'MainRoot/util/telemetryUtils';
import userActions, { LOAD_USER_FAILED, LOAD_USER_FULFILLED } from '../../../main/frontend/user/userActions';
import { axiosMockAdapter } from 'TestRoot/SpecUtil';
import * as urlUtil from 'MainRoot/util/urlUtil';

describe('userActions', function () {
  let initialState, currentState, pendoDeferred, axiosMock;

  const testNonAdminUserInfo = {
    currentUser: 'test-user',
    isDefaultUser: false,
    shouldDisplayNotice: false,
    canChangePassword: false,
  };

  beforeAll(function () {
    const mockUrlService = { match: () => null };
    setUrlService(mockUrlService);
  });

  beforeEach(() => {
    axiosMock = axiosMockAdapter();
    pendoDeferred = new Promise((resolve) => resolve(true));
    spyOn(telemetryUtils, 'submitTelemetryData');
    spyOn(mainBundlePendoService, 'flush');
    spyOn(urlUtil, 'logoutRedirection');

    initialState = {
      user: {
        currentUser: null,
        isDefaultUser: false,
        shouldDisplayNotice: false,
        canChangePassword: false,
      },
      // required by async thunk that checks if user is logged in. Initially set to null to force login calls
      userSession: {
        data: null,
      },
    };

    currentState = {
      router: {
        currentState: {
          name: 'firewallOnboarding.firewallOnboardingPage',
          url: '/firewallOnboarding',
          data: {
            title: 'Firewall Onboarding',
            isDirty: ['firewallOnboarding', 'isConfiguring'],
          },
        },
      },
    };
  });

  describe('passwordChanged', () => {
    it(
      'should dispatch action if the password was changed from default' + 'and the user is *the* default admin',
      () => {
        // ShouldDisplayNotice means that the default admin has the default passwd.
        initialState.user.shouldDisplayNotice = true;
        // isDefaultUser means that the user is *THE* default admin
        initialState.user.isDefaultUser = true;

        const store = SpecUtil.mockReduxStore(initialState);
        store.dispatch(userActions.passwordChanged());

        const defaultAdminPasswordChangedAction = store
          .getActions()
          .find((action) => action.type === 'DEFAULT_ADMIN_PASSWORD_CHANGED');
        expect(defaultAdminPasswordChangedAction).toEqual({ type: 'DEFAULT_ADMIN_PASSWORD_CHANGED' });
      }
    );

    it(
      'should not dispatch action if the password changed from default' + 'and the user is NOT the default admin',
      () => {
        initialState.user.shouldDisplayNotice = true;
        initialState.user.isDefaultUser = false;

        const store = SpecUtil.mockReduxStore(initialState);
        store.dispatch(userActions.passwordChanged());

        const defaultAdminPasswordChangedAction = store
          .getActions()
          .find((action) => action.type === 'DEFAULT_ADMIN_PASSWORD_CHANGED');

        expect(defaultAdminPasswordChangedAction).toBeUndefined();
      }
    );

    it('should NOT dispatch the action if the password was not the default', () => {
      initialState.user.shouldDisplayNotice = false;
      initialState.user.isDefaultUser = true;

      const store = SpecUtil.mockReduxStore(initialState);
      store.dispatch(userActions.passwordChanged());

      const defaultAdminPasswordChangedAction = store
        .getActions()
        .find((action) => action.type === 'DEFAULT_ADMIN_PASSWORD_CHANGED');
      //No action is dispatched.
      expect(defaultAdminPasswordChangedAction).toBeUndefined();
    });

    it('should fire telemetry if the password was changed from default' + 'and the user is THE default admin', () => {
      initialState.user.shouldDisplayNotice = true;
      initialState.user.isDefaultUser = true;

      const store = SpecUtil.mockReduxStore(initialState);
      store.dispatch(userActions.passwordChanged());

      expect(telemetryUtils.submitTelemetryData).toHaveBeenCalledWith('ADMIN_PASSWORD_CHANGE', {
        action: 'PASSWORD_CHANGED_FROM_DEFAULT',
      });
    });

    it(
      'should not fire telemetry if the password was changed from default' + 'and the user is not THE default admin',
      () => {
        initialState.user.shouldDisplayNotice = true;
        initialState.user.isDefaultUser = false;

        const store = SpecUtil.mockReduxStore(initialState);
        store.dispatch(userActions.passwordChanged());

        expect(telemetryUtils.submitTelemetryData).not.toHaveBeenCalled();
      }
    );

    it('should not fire telemetry if the password was not changed from default', () => {
      initialState.user.shouldDisplayNotice = false;
      const store = SpecUtil.mockReduxStore(initialState);
      store.dispatch(userActions.passwordChanged());

      expect(telemetryUtils.submitTelemetryData).not.toHaveBeenCalled();
    });
  });

  describe('passwordChangedForUser', () => {
    it(
      'should dispatch action if the password was changed from default' + 'and the user is *the* default admin',
      () => {
        // ShouldDisplayNotice means that the default admin has the default passwd.
        initialState.user.shouldDisplayNotice = true;
        const selectedUser = { username: 'admin' };
        const store = SpecUtil.mockReduxStore(initialState);
        store.dispatch(userActions.passwordChangedForUser(selectedUser));

        expect(store.getActions().length).toBe(1);
        expect(store.getActions()[0]).toEqual({
          type: 'DEFAULT_ADMIN_PASSWORD_CHANGED',
        });
      }
    );

    it(
      'should not dispatch action if the password changed from default' + 'and the user is NOT the default admin',
      () => {
        initialState.user.shouldDisplayNotice = true;
        const selectedUser = { username: 'foo' };

        const store = SpecUtil.mockReduxStore(initialState);
        store.dispatch(userActions.passwordChangedForUser(selectedUser));

        expect(store.getActions().length).toBe(0);
      }
    );

    it('should NOT dispatch the action if the password was not the default', () => {
      initialState.user.shouldDisplayNotice = false;
      let selectedUser = { username: 'admin' };
      const store = SpecUtil.mockReduxStore(initialState);
      store.dispatch(userActions.passwordChangedForUser(selectedUser));

      //No action is dispatched.
      expect(store.getActions().length).toBe(0);

      selectedUser = { username: 'foo' };
      store.dispatch(userActions.passwordChangedForUser(selectedUser));

      //No action is dispatched.
      expect(store.getActions().length).toBe(0);
    });

    it('should fire telemetry if the password was changed from default' + 'and the user is THE default admin', () => {
      initialState.user.shouldDisplayNotice = true;
      const selectedUser = { username: 'admin' };

      const store = SpecUtil.mockReduxStore(initialState);
      store.dispatch(userActions.passwordChangedForUser(selectedUser));

      expect(telemetryUtils.submitTelemetryData).toHaveBeenCalledWith('ADMIN_PASSWORD_CHANGE', {
        action: 'PASSWORD_CHANGED_FROM_DEFAULT',
      });
    });

    it(
      'should not fire telemetry if the password was changed from default' + 'and the user is not THE default admin',
      () => {
        initialState.user.shouldDisplayNotice = true;
        const selectedUser = { username: 'foo' };

        const store = SpecUtil.mockReduxStore(initialState);
        store.dispatch(userActions.passwordChangedForUser(selectedUser));

        expect(telemetryUtils.submitTelemetryData).not.toHaveBeenCalled();
      }
    );

    it('should not fire telemetry if the password was not changed from default', () => {
      initialState.user.shouldDisplayNotice = false;
      let selectedUser = { username: 'foo' };
      const store = SpecUtil.mockReduxStore(initialState);
      store.dispatch(userActions.passwordChangedForUser(selectedUser));

      expect(telemetryUtils.submitTelemetryData).not.toHaveBeenCalled();

      selectedUser = { username: 'admin' };
      store.dispatch(userActions.passwordChangedForUser(selectedUser));
      expect(telemetryUtils.submitTelemetryData).not.toHaveBeenCalled();
    });
  });

  describe('loadUser', () => {
    const assertActionsForSuccesfulLoad = function (actionsPerformed, expectedShouldDisplayWarning) {
      expect(actionsPerformed[0]).toEqual({
        type: 'LOAD_USER_REQUESTED',
      });
      expect(actionsPerformed[1].type).toEqual('userSession/ensureUserLoggedIn/pending');
      expect(actionsPerformed[1].payload).toEqual(undefined);
      expect(actionsPerformed[2].type).toEqual('userSession/fetchUserSession/pending');
      expect(actionsPerformed[2].payload).toEqual(undefined);
      expect(actionsPerformed[3].type).toEqual(LOAD_USER_FULFILLED);
      expect(actionsPerformed[3].payload).toEqual({
        currentUser: testNonAdminUserInfo,
        shouldDisplayWarning: testNonAdminUserInfo.shouldDisplayNotice,
      });
      expect(actionsPerformed[4].type).toEqual('userSession/fetchUserSession/fulfilled');
      expect(actionsPerformed[4].payload).toEqual(testNonAdminUserInfo);
      expect(actionsPerformed[5].type).toEqual('userSession/ensureUserLoggedIn/fulfilled');
      expect(actionsPerformed[5].payload).toEqual(testNonAdminUserInfo);
      expect(actionsPerformed[6].payload).toEqual({
        currentUser: testNonAdminUserInfo,
        shouldDisplayWarning: expectedShouldDisplayWarning,
      });
      expect(actionsPerformed[6].type).toEqual(LOAD_USER_FULFILLED);
    };

    it(
      'waits for the current user to log in, queries their permissions,  and sets shouldDisplayWarning to false' +
        ' if they do not have the CONFIGURE_SYSTEM permission',
      function (done) {
        //Mock axios calls with no CONFIGURE_SYSTEM permission on the mocked response
        axiosMock.onPut(getGlobalPermissionTestUrl(), ['CONFIGURE_SYSTEM']).reply(200, []);
        //Mock axios call to get the current user session info
        axiosMock.onGet(getSessionUrl()).reply(200, testNonAdminUserInfo);
        axiosMock.onGet(getShouldDisplayDefaultPasswordWarning()).reply(200, true);

        const store = SpecUtil.mockReduxStore(initialState);
        store.dispatch(userActions.loadUser()).then(() => {
          // All checks on the redux store after the loadUser action is complete
          const actionsPerformed = store.getActions();
          expect(actionsPerformed.length).toBe(7);
          assertActionsForSuccesfulLoad(actionsPerformed, false);
          done();
        });
      }
    );

    it(
      'queries shouldDisplayDefaultPasswordWarning if the user has CONFIGURE_SYSTEM ' +
        'and sets shouldDisplayWarning accordingly',
      function (done) {
        //Mock axios calls with no CONFIGURE_SYSTEM permission on the mocked response
        axiosMock.onPut(getGlobalPermissionTestUrl(), ['CONFIGURE_SYSTEM']).reply(200, ['CONFIGURE_SYSTEM']);
        //Mock axios call to get the current user session info
        axiosMock.onGet(getSessionUrl()).reply(200, testNonAdminUserInfo);
        axiosMock.onGet(getShouldDisplayDefaultPasswordWarning()).reply(200, 'true');

        const store = SpecUtil.mockReduxStore(initialState);
        store.dispatch(userActions.loadUser()).then(() => {
          // All checks on the redux store after the loadUser action is complete
          const actionsPerformed = store.getActions();
          expect(actionsPerformed.length).toBe(7);
          assertActionsForSuccesfulLoad(actionsPerformed, true);
          done();
        });
      }
    );

    it('sets shouldDisplayWarning to false if the shouldDisplayDefaultPasswordWarning endpoint returns false', function (done) {
      //Mock axios calls with no CONFIGURE_SYSTEM permission on the mocked response
      axiosMock.onPut(getGlobalPermissionTestUrl(), ['CONFIGURE_SYSTEM']).reply(200, ['CONFIGURE_SYSTEM']);
      //Mock axios call to get the current user session info
      axiosMock.onGet(getSessionUrl()).reply(200, testNonAdminUserInfo);
      axiosMock.onGet(getShouldDisplayDefaultPasswordWarning()).reply(200, 'false');

      const store = SpecUtil.mockReduxStore(initialState);
      store.dispatch(userActions.loadUser()).then(() => {
        // All checks on the redux store after the loadUser action is complete
        const actionsPerformed = store.getActions();
        expect(actionsPerformed.length).toBe(7);
        assertActionsForSuccesfulLoad(actionsPerformed, false);
        done();
      });
    });

    it('should dispatch error if the call to get the current user does not resolve', (done) => {
      //Mock axios calls with no CONFIGURE_SYSTEM permission on the mocked response
      axiosMock.onPut(getGlobalPermissionTestUrl(), ['CONFIGURE_SYSTEM']).reply(200, ['CONFIGURE_SYSTEM']);
      //Force mock axios call to get the current user session info to fail
      axiosMock.onGet(getSessionUrl()).reply(500, { message: 'Some server error message' });
      axiosMock.onGet(getShouldDisplayDefaultPasswordWarning()).reply(200, 'true');

      const store = SpecUtil.mockReduxStore(initialState);
      store.dispatch(userActions.loadUser()).then(() => {
        // All checks on the redux store after the loadUser action is complete
        const actionsPerformed = store.getActions();
        expect(actionsPerformed.length).toBe(6);
        expect(actionsPerformed[0]).toEqual({
          type: 'LOAD_USER_REQUESTED',
        });
        expect(actionsPerformed[5].payload).toEqual({ name: 'RejectWithValue', message: 'Rejected' });
        expect(actionsPerformed[5].type).toEqual(LOAD_USER_FAILED);
        done();
      });
    });

    it('should set the warning flag to false if the call to get permissions does not resolve', (done) => {
      //Force mock axios getGlobalPermissionTestUrl call to fail
      axiosMock
        .onPut(getGlobalPermissionTestUrl(), ['CONFIGURE_SYSTEM'])
        .reply(500, { message: 'some server error message' });

      const store = SpecUtil.mockReduxStore(initialState);
      store.dispatch(userActions.loadUser()).then(() => {
        // All checks on the redux store after the loadUser action is complete
        const actionsPerformed = store.getActions();
        expect(actionsPerformed.length).toBe(6);
        expect(actionsPerformed[0]).toEqual({
          type: 'LOAD_USER_REQUESTED',
        });
        expect(actionsPerformed[5].payload).toEqual({ name: 'RejectWithValue', message: 'Rejected' });
        expect(actionsPerformed[5].type).toEqual(LOAD_USER_FAILED);
        done();
      });
    });

    it('should set the warning flag to false if the call to get it does not resolve', (done) => {
      //Mock axios calls with no CONFIGURE_SYSTEM permission on the mocked response
      axiosMock.onPut(getGlobalPermissionTestUrl(), ['CONFIGURE_SYSTEM']).reply(200, ['CONFIGURE_SYSTEM']);
      //Mock axios call to get the current user session info
      axiosMock.onGet(getSessionUrl()).reply(200, testNonAdminUserInfo);
      axiosMock.onGet(getShouldDisplayDefaultPasswordWarning()).reply(500, 'Some server error message');

      const store = SpecUtil.mockReduxStore(initialState);
      store.dispatch(userActions.loadUser()).then(() => {
        // All checks on the redux store after the loadUser action is complete
        const actionsPerformed = store.getActions();
        expect(actionsPerformed.length).toBe(7);
        assertActionsForSuccesfulLoad(actionsPerformed, false);
        done();
      });
    });

    it('should submit telemetry data when the display flag is shown', (done) => {
      //Mock axios calls with no CONFIGURE_SYSTEM permission on the mocked response
      axiosMock.onPut(getGlobalPermissionTestUrl(), ['CONFIGURE_SYSTEM']).reply(200, ['CONFIGURE_SYSTEM']);
      //Mock axios call to get the current user session info
      axiosMock.onGet(getSessionUrl()).reply(200, testNonAdminUserInfo);
      axiosMock.onGet(getShouldDisplayDefaultPasswordWarning()).reply(200, true);

      const store = SpecUtil.mockReduxStore(initialState);
      store.dispatch(userActions.loadUser()).then(() => {
        // All checks on the redux store after the loadUser action is complete
        const actionsPerformed = store.getActions();
        expect(actionsPerformed.length).toBe(7);
        expect(actionsPerformed[0]).toEqual({
          type: 'LOAD_USER_REQUESTED',
        });
        expect(actionsPerformed[6].payload).toEqual({
          currentUser: testNonAdminUserInfo,
          shouldDisplayWarning: true,
        });
        expect(actionsPerformed[6].type).toEqual(LOAD_USER_FULFILLED);
        expect(telemetryUtils.submitTelemetryData).toHaveBeenCalledWith('ADMIN_PASSWORD_CHANGE', {
          action: 'WARNING_SHOWN',
        });
        done();
      });
    });

    it('should not submit telemetry data when the display flag is not shown', (done) => {
      //Mock axios calls with no CONFIGURE_SYSTEM permission on the mocked response
      axiosMock.onPut(getGlobalPermissionTestUrl(), ['CONFIGURE_SYSTEM']).reply(200, ['CONFIGURE_SYSTEM']);
      axiosMock.onGet(getSessionUrl()).reply(200, testNonAdminUserInfo);
      axiosMock.onGet(getShouldDisplayDefaultPasswordWarning()).reply(200, false);

      const store = SpecUtil.mockReduxStore(initialState);
      store.dispatch(userActions.loadUser()).then(() => {
        // All checks on the redux store after the loadUser action is complete
        const actionsPerformed = store.getActions();
        expect(actionsPerformed.length).toBe(7);
        assertActionsForSuccesfulLoad(actionsPerformed, false);
        expect(telemetryUtils.submitTelemetryData).not.toHaveBeenCalled();
        done();
      });
    });

    it('should not submit telemetry data when the permissions call fails', (done) => {
      //Mock permissions call to fail
      axiosMock.onPut(getGlobalPermissionTestUrl(), ['CONFIGURE_SYSTEM']).reply(500);

      const store = SpecUtil.mockReduxStore(initialState);
      store.dispatch(userActions.loadUser()).then(() => {
        // All checks on the redux store after the loadUser action is complete
        const actionsPerformed = store.getActions();
        expect(actionsPerformed.length).toBe(6);
        expect(actionsPerformed[0]).toEqual({
          type: 'LOAD_USER_REQUESTED',
        });
        expect(actionsPerformed[5].type).toEqual(LOAD_USER_FAILED);
        expect(telemetryUtils.submitTelemetryData).not.toHaveBeenCalled();
        done();
      });
    });

    it('should not submit telemetry data when the flag call fails', (done) => {
      axiosMock.onPut(getGlobalPermissionTestUrl(), ['CONFIGURE_SYSTEM']).reply(200, ['CONFIGURE_SYSTEM']);
      axiosMock.onGet(getSessionUrl()).reply(200, testNonAdminUserInfo);
      axiosMock.onGet(getShouldDisplayDefaultPasswordWarning()).reply(500);

      const store = SpecUtil.mockReduxStore(initialState);
      store.dispatch(userActions.loadUser()).then(() => {
        // All checks on the redux store after the loadUser action is complete
        const actionsPerformed = store.getActions();
        expect(actionsPerformed.length).toBe(7);
        assertActionsForSuccesfulLoad(actionsPerformed, false);
        expect(telemetryUtils.submitTelemetryData).not.toHaveBeenCalled();
        done();
      });
    });
  });

  describe('logout()', function () {
    it('provides the ability to log out by hitting a logout api endpoint with a delete request', function () {
      axiosMock.onDelete(getSessionLogoutUrl()).reply(204);

      const store = SpecUtil.mockReduxStore(currentState);
      mainBundlePendoService.flush.and.returnValue(pendoDeferred);

      store
        .dispatch(userActions.logout())
        .then(() => {
          expect(mainBundlePendoService.flush).toHaveBeenCalled();
          expect(urlUtil.logoutRedirection).toHaveBeenCalled();
          pendoDeferred.then((p) => {
            expect(p).toBe(true);
          });
        })
        .catch(() => {});
    });

    it('still logs out if the pendo promise is rejected', function (done) {
      // Mocked the logout redirection function to prevent unwanted redirection during test using the window object
      urlUtil.logoutRedirection.and.callFake(() => null);

      axiosMock.onDelete(getSessionLogoutUrl()).reply(204);
      const rejectedPromise = Promise.reject('test rejection');
      const store = SpecUtil.mockReduxStore(currentState);
      mainBundlePendoService.flush.and.callFake(() => rejectedPromise);
      store.dispatch(userActions.logout()).catch(() => {
        const actions = store.getActions();
        expect(actions[0].type).toBe('USER_LOGGED_OUT');
        done();
      });
    });

    it('provides the ability to log out for reverse proxy', function () {
      const afterLogoutRedirectionUrl = 'http://localhost/logout';
      axiosMock.onDelete(getSessionLogoutUrl()).reply(204, '', { Location: afterLogoutRedirectionUrl });

      const store = SpecUtil.mockReduxStore(currentState);
      mainBundlePendoService.flush.and.returnValue(pendoDeferred);

      store
        .dispatch(userActions.logout())
        .then(() => {
          expect(mainBundlePendoService.flush).toHaveBeenCalled();
          expect(urlUtil.logoutRedirection).toHaveBeenCalledWith(afterLogoutRedirectionUrl);
          pendoDeferred.then((p) => {
            expect(p).toBe(true);
          });
        })
        .catch(() => {});
    });
  });
});
