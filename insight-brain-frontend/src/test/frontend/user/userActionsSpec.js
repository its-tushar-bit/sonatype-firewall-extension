/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import userModule from '../../../main/frontend/user/userModule';
import * as userSession from 'MainRoot/user/userSessionUtils';
import mainBundlePendoService, { setUrlService } from 'MainRoot/pendo/mainBundlePendoService';
import { getGlobalPermissionTestUrl } from 'MainRoot/util/CLMContextLocation';
import axios from 'axios';
import MockAdapter from 'axios-mock-adapter';
import { getSessionLogoutUrl, getShouldDisplayDefaultPasswordWarning } from 'MainRoot/util/CLMLocation';
import * as telemetryUtils from 'MainRoot/util/telemetryUtils';

describe('userActions', function () {
  let userActions, initialState, currentState, $httpBackend, $rootScope, loginDeferred, pendoDeferred, axiosMock;

  beforeAll(function () {
    const mockUrlService = { match: () => null };
    setUrlService(mockUrlService);
  });

  beforeEach(angular.mock.module(userModule.name));

  beforeEach(
    angular.mock.module(function ($provide) {
      SpecUtil.mockNgRedux($provide);
    })
  );

  beforeEach(inject(($q, _$httpBackend_, _userActions_, _$rootScope_) => {
    $httpBackend = _$httpBackend_;
    userActions = _userActions_;
    $rootScope = _$rootScope_;

    loginDeferred = $q.defer();
    pendoDeferred = $q.defer();
    axiosMock = new MockAdapter(axios);

    spyOn(telemetryUtils, 'submitTelemetryData');
    spyOn(userSession, 'waitForLogin').and.returnValue(loginDeferred.promise);
    spyOn($rootScope, '$broadcast').and.callThrough();
    spyOn($rootScope, '$emit').and.callThrough();

    spyOn(mainBundlePendoService, 'flush');

    initialState = {
      user: {
        currentUser: null,
        isDefaultUser: false,
        shouldDisplayNotice: false,
        canChangePassword: false,
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
  }));

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

    it(
      'should broadcast recalculateContainerHeights if the password was changed from default' +
        'and the user is *the* default admin',
      () => {
        // ShouldDisplayNotice means that the default admin has the default passwd.
        initialState.user.shouldDisplayNotice = true;
        initialState.user.isDefaultUser = true;
        const store = SpecUtil.mockReduxStore(initialState);
        store.dispatch(userActions.passwordChanged());

        expect($rootScope.$broadcast).toHaveBeenCalledWith('recalculateContainerHeights');
      }
    );

    it(
      'should not broadcast recalculateContainerHeights if the password changed from default' +
        'and the user is NOT the default admin',
      () => {
        initialState.user.shouldDisplayNotice = true;
        initialState.user.isDefaultUser = false;

        const store = SpecUtil.mockReduxStore(initialState);
        store.dispatch(userActions.passwordChanged());

        expect($rootScope.$broadcast).not.toHaveBeenCalledWith('recalculateContainerHeights');
      }
    );

    it('should not broadcast recalculateContainerHeights if the password was not the default', () => {
      initialState.user.shouldDisplayNotice = false;
      initialState.user.isDefaultUser = true;
      let store = SpecUtil.mockReduxStore(initialState);
      store.dispatch(userActions.passwordChanged());

      //No action is dispatched.
      expect($rootScope.$broadcast).not.toHaveBeenCalledWith('recalculateContainerHeights');

      initialState.user.isDefaultUser = false;
      store = SpecUtil.mockReduxStore(initialState);
      store.dispatch(userActions.passwordChanged());

      //No action is dispatched.
      expect($rootScope.$broadcast).not.toHaveBeenCalledWith('recalculateContainerHeights');
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

    it(
      'should broadcast recalculateContainerHeights if the password was changed from default' +
        'and the user is *the* default admin',
      () => {
        // ShouldDisplayNotice means that the default admin has the default passwd.
        initialState.user.shouldDisplayNotice = true;
        const selectedUser = { username: 'admin' };
        const store = SpecUtil.mockReduxStore(initialState);
        store.dispatch(userActions.passwordChangedForUser(selectedUser));

        expect($rootScope.$broadcast).toHaveBeenCalledWith('recalculateContainerHeights');
      }
    );

    it(
      'should not broadcast recalculateContainerHeights if the password changed from default' +
        'and the user is NOT the default admin',
      () => {
        initialState.user.shouldDisplayNotice = true;
        const selectedUser = { username: 'foo' };

        const store = SpecUtil.mockReduxStore(initialState);
        store.dispatch(userActions.passwordChangedForUser(selectedUser));

        expect($rootScope.$broadcast).not.toHaveBeenCalledWith('recalculateContainerHeights');
      }
    );

    it('should not broadcast recalculateContainerHeights if the password was not the default', () => {
      initialState.user.shouldDisplayNotice = false;
      let selectedUser = { username: 'admin' };
      const store = SpecUtil.mockReduxStore(initialState);
      store.dispatch(userActions.passwordChangedForUser(selectedUser));

      //No action is dispatched.
      expect($rootScope.$broadcast).not.toHaveBeenCalledWith('recalculateContainerHeights');

      selectedUser = { username: 'foo' };
      store.dispatch(userActions.passwordChangedForUser(selectedUser));

      //No action is dispatched.
      expect($rootScope.$broadcast).not.toHaveBeenCalledWith('recalculateContainerHeights');
    });
  });

  // Fix and re-enable these tests as userActions is moved off of angular in CLM-34380
  xdescribe('loadUser', () => {
    afterEach(() => {
      $httpBackend.verifyNoOutstandingExpectation();
      $httpBackend.verifyNoOutstandingRequest();
      if (axiosMock) {
        axiosMock.reset();
      }
    });

    afterAll(() => {
      if (axiosMock) {
        axiosMock.restore();
      }
    });

    it(
      'waits for the current user to log in, queries their permissions,  and sets shouldDisplayWarning to false if ' +
        'they do not have the CONFIGURE_SYSTEM permission',
      function (done) {
        axiosMock.onPut(getGlobalPermissionTestUrl(), ['CONFIGURE_SYSTEM']).reply(200, []);

        const store = SpecUtil.mockReduxStore(initialState);

        store
          .dispatch(userActions.loadUser())
          .then(() => {
            try {
              expect(store.getActions().length).toBe(2);
              expect(store.getActions()[0]).toEqual({
                type: 'LOAD_USER_REQUESTED',
              });
              expect(store.getActions()[1]).toEqual({
                type: 'LOAD_USER_FULFILLED',
                payload: {
                  currentUser: {
                    username: 'admin',
                    internalUser: true,
                  },
                  shouldDisplayWarning: false,
                },
              });
              done();
            } catch (e) {
              done.fail(e);
            }
          })
          .catch(done.fail);

        loginDeferred.resolve({ username: 'admin', internalUser: true });
        $rootScope.$digest();

        // Give axios promises time to resolve
        setTimeout(() => {
          $rootScope.$digest();
        }, 0);
      }
    );

    it(
      'queries shouldDisplayDefaultPasswordWarning if the user has CONFIGURE_SYSTEM and sets shouldDisplayWarning' +
        ' accordingly',
      function () {
        axiosMock.onPut(getGlobalPermissionTestUrl(), ['CONFIGURE_SYSTEM']).reply(200, ['CONFIGURE_SYSTEM']);
        $httpBackend.expectGET(getShouldDisplayDefaultPasswordWarning()).respond('true');

        const store = SpecUtil.mockReduxStore(initialState);
        const successSpy = jasmine.createSpy('successSpy');
        store.dispatch(userActions.loadUser()).then(successSpy);

        loginDeferred.resolve({ username: 'admin', internalUser: true });
        $rootScope.$digest();
        $httpBackend.flush();

        expect(successSpy).toHaveBeenCalled();
        expect(store.getActions().length).toBe(2);
        expect(store.getActions()[0]).toEqual({
          type: 'LOAD_USER_REQUESTED',
        });
        expect(store.getActions()[1]).toEqual({
          type: 'LOAD_USER_FULFILLED',
          payload: {
            currentUser: {
              username: 'admin',
              internalUser: true,
            },
            shouldDisplayWarning: true,
          },
        });
      }
    );

    it('sets shouldDisplayWarning to false if the shouldDisplayDefaultPasswordWarning endpoint returns false', function () {
      axiosMock.onPut(getGlobalPermissionTestUrl(), ['CONFIGURE_SYSTEM']).reply(200, ['CONFIGURE_SYSTEM']);
      $httpBackend.expectGET(getShouldDisplayDefaultPasswordWarning()).respond('false');

      const store = SpecUtil.mockReduxStore(initialState);
      const successSpy = jasmine.createSpy('successSpy');
      store.dispatch(userActions.loadUser()).then(successSpy);

      loginDeferred.resolve({ username: 'admin', internalUser: true });
      $rootScope.$digest();
      $httpBackend.flush();

      expect(successSpy).toHaveBeenCalled();
      expect(store.getActions().length).toBe(2);
      expect(store.getActions()[0]).toEqual({
        type: 'LOAD_USER_REQUESTED',
      });
      expect(store.getActions()[1]).toEqual({
        type: 'LOAD_USER_FULFILLED',
        payload: {
          currentUser: {
            username: 'admin',
            internalUser: true,
          },
          shouldDisplayWarning: false,
        },
      });
    });

    it('should dispatch error if the call to get the current user does not resolve', () => {
      axiosMock.onPut(getGlobalPermissionTestUrl(), ['CONFIGURE_SYSTEM']).reply(200, ['CONFIGURE_SYSTEM']);

      $httpBackend.expectGET(getShouldDisplayDefaultPasswordWarning()).respond('true');

      const store = SpecUtil.mockReduxStore(initialState);
      const errorSpy = jasmine.createSpy('errorSpy');
      store.dispatch(userActions.loadUser()).catch(errorSpy);

      $httpBackend.flush();

      loginDeferred.reject({ message: 'Some server error message' });
      $rootScope.$digest();

      expect(errorSpy).toHaveBeenCalled();
      expect(store.getActions().length).toBe(2);
      expect(store.getActions()[0]).toEqual({
        type: 'LOAD_USER_REQUESTED',
      });
      expect(store.getActions()[1]).toEqual({
        type: 'LOAD_USER_FAILED',
        payload: { message: 'Some server error message' },
      });
    });

    it('should set the warning flag to false if the call to get permissions does not resolve', () => {
      loginDeferred.resolve({ username: 'admin', internalUser: true });
      $rootScope.$digest();

      axiosMock.onPut(getGlobalPermissionTestUrl(), ['CONFIGURE_SYSTEM']).reply(500, 'Some server error message');

      const store = SpecUtil.mockReduxStore(initialState);
      const errorSpy = jasmine.createSpy('errorSpy');
      store.dispatch(userActions.loadUser()).catch(errorSpy);

      $httpBackend.flush();
      expect(errorSpy).not.toHaveBeenCalled();
      expect(store.getActions().length).toBe(2);
      expect(store.getActions()[0]).toEqual({
        type: 'LOAD_USER_REQUESTED',
      });
      expect(store.getActions()[1]).toEqual({
        type: 'LOAD_USER_FULFILLED',
        payload: {
          currentUser: {
            username: 'admin',
            internalUser: true,
          },
          shouldDisplayWarning: false,
        },
      });
    });

    it('should set the warning flag to false if the call to get it does not resolve', () => {
      loginDeferred.resolve({ username: 'admin', internalUser: true });
      $rootScope.$digest();

      axiosMock.onPut(getGlobalPermissionTestUrl(), ['CONFIGURE_SYSTEM']).reply(200, ['CONFIGURE_SYSTEM']);

      $httpBackend.expectGET(getShouldDisplayDefaultPasswordWarning()).respond(500, 'Some server error message');

      const store = SpecUtil.mockReduxStore(initialState);
      const errorSpy = jasmine.createSpy('errorSpy');
      store.dispatch(userActions.loadUser()).catch(errorSpy);

      $httpBackend.flush();
      expect(errorSpy).not.toHaveBeenCalled();
      expect(store.getActions().length).toBe(2);
      expect(store.getActions()[0]).toEqual({
        type: 'LOAD_USER_REQUESTED',
      });
      expect(store.getActions()[1]).toEqual({
        type: 'LOAD_USER_FULFILLED',
        payload: {
          currentUser: {
            username: 'admin',
            internalUser: true,
          },
          shouldDisplayWarning: false,
        },
      });
    });

    it('should submit telemetry data when the display flag is shown', () => {
      axiosMock.onPut(getGlobalPermissionTestUrl(), ['CONFIGURE_SYSTEM']).reply(200, ['CONFIGURE_SYSTEM']);

      $httpBackend.expectGET(getShouldDisplayDefaultPasswordWarning()).respond('true');

      const store = SpecUtil.mockReduxStore(initialState);
      const successSpy = jasmine.createSpy('successSpy');
      store.dispatch(userActions.loadUser()).then(successSpy);

      $httpBackend.flush();

      expect(telemetryUtils.submitTelemetryData).not.toHaveBeenCalled();

      loginDeferred.resolve({ username: 'admin', internalUser: true });
      $rootScope.$digest();

      expect(successSpy).toHaveBeenCalled();
      expect(telemetryUtils.submitTelemetryData).toHaveBeenCalledWith('ADMIN_PASSWORD_CHANGE', {
        action: 'WARNING_SHOWN',
      });
    });

    it('should not submit telemetry data when the display flag is not shown', () => {
      axiosMock.onPut(getGlobalPermissionTestUrl(), ['CONFIGURE_SYSTEM']).reply(200, ['CONFIGURE_SYSTEM']);

      $httpBackend.expectGET(getShouldDisplayDefaultPasswordWarning()).respond(false);

      const store = SpecUtil.mockReduxStore(initialState);
      const successSpy = jasmine.createSpy('successSpy');
      store.dispatch(userActions.loadUser()).then(successSpy);

      $httpBackend.flush();
      loginDeferred.resolve({ username: 'admin', internalUser: true });
      $rootScope.$digest();

      expect(successSpy).toHaveBeenCalled();
      expect(telemetryUtils.submitTelemetryData).not.toHaveBeenCalled();
    });

    it('should not submit telemetry data when the permissions call fails', () => {
      axiosMock.onPut(getGlobalPermissionTestUrl(), ['CONFIGURE_SYSTEM']).reply(500);

      const store = SpecUtil.mockReduxStore(initialState);
      const successSpy = jasmine.createSpy('successSpy');
      store.dispatch(userActions.loadUser()).then(successSpy);

      $httpBackend.flush();
      loginDeferred.resolve({ username: 'admin', internalUser: true });
      $rootScope.$digest();

      expect(successSpy).toHaveBeenCalled();
      expect(telemetryUtils.submitTelemetryData).not.toHaveBeenCalled();
    });

    it('should not submit telemetry data when the flag call fails', () => {
      loginDeferred.resolve({ username: 'admin', internalUser: true });
      $rootScope.$digest();

      axiosMock.onPut(getGlobalPermissionTestUrl(), ['CONFIGURE_SYSTEM']).reply(200, ['CONFIGURE_SYSTEM']);

      $httpBackend.expectGET(getShouldDisplayDefaultPasswordWarning()).respond(500);

      const store = SpecUtil.mockReduxStore(initialState);
      const successSpy = jasmine.createSpy('successSpy');
      store.dispatch(userActions.loadUser()).then(successSpy);

      $httpBackend.flush();

      expect(successSpy).toHaveBeenCalled();
      expect(telemetryUtils.submitTelemetryData).not.toHaveBeenCalled();
    });
  });

  describe('logout()', function () {
    it('provides the ability to log out by hitting a logout api endpoint with a delete request', function () {
      const store = SpecUtil.mockReduxStore(currentState);
      mainBundlePendoService.flush.and.returnValue(pendoDeferred.promise);
      $httpBackend.expectDELETE(getSessionLogoutUrl()).respond(204);

      store.dispatch(userActions.logout());
      $rootScope.$digest();
      pendoDeferred.resolve();

      expect($httpBackend.flush).not.toThrow();
    });

    it('still logs out if the pendo promise is rejected', function () {
      const store = SpecUtil.mockReduxStore(currentState);
      mainBundlePendoService.flush.and.returnValue(pendoDeferred.promise);
      $httpBackend.expectDELETE(getSessionLogoutUrl()).respond(204);

      store.dispatch(userActions.logout());
      $rootScope.$digest();
      pendoDeferred.reject();

      expect($httpBackend.flush).not.toThrow();
    });

    it(`doesn't log out from the server before the pendo promise completes`, function () {
      const store = SpecUtil.mockReduxStore(currentState);
      mainBundlePendoService.flush.and.returnValue(pendoDeferred.promise);
      $httpBackend.expectDELETE(getSessionLogoutUrl()).respond(204);

      store.dispatch(userActions.logout());
      $rootScope.$digest();
      $httpBackend.verifyNoOutstandingRequest();

      pendoDeferred.resolve();
      expect($httpBackend.flush).not.toThrow();
    });

    it(`provides the ability to log out for reverse proxy`, function () {
      const store = SpecUtil.mockReduxStore(currentState);
      mainBundlePendoService.flush.and.returnValue(pendoDeferred.promise);
      var headers = { Location: 'http://localhost/logout' };
      $httpBackend.whenDELETE(getSessionLogoutUrl()).respond(204, '', headers);
      store.dispatch(userActions.logout());
      pendoDeferred.resolve();
      $rootScope.$digest();
      $httpBackend.flush();
      $rootScope.$digest();
      expect($rootScope.$emit).toHaveBeenCalledWith('logout', headers.Location);
    });
  });
});
