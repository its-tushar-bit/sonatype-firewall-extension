import changeDefaultAdminPasswordNoticeModule from '../../../main/frontend/changeDefaultAdminPasswordNotice/module';

describe('userActions', function() {
  let userActions,
      initialState,
      CLMLocations,
      telemetryService,
      CurrentUser,
      $httpBackend,
      $rootScope,
      loginDeferred;

  beforeEach(angular.mock.module(changeDefaultAdminPasswordNoticeModule.name));

  beforeEach(
      inject(($q, _$httpBackend_, _CLMLocations_, _userActions_, _telemetryService_, _$rootScope_, _CurrentUser_) => {
        $httpBackend = _$httpBackend_;
        CLMLocations = _CLMLocations_;
        userActions = _userActions_;
        telemetryService = _telemetryService_;
        $rootScope = _$rootScope_;
        CurrentUser = _CurrentUser_;

        loginDeferred = $q.defer();

        spyOn(telemetryService, 'submitData');
        spyOn(CurrentUser, 'waitForLogin').and.returnValue(loginDeferred.promise);
        spyOn($rootScope, '$broadcast').and.callThrough();

        initialState = {
          user: {
            currentUser: null,
            isDefaultUser: false,
            shouldDisplayNotice: false,
            canChangePassword: false
          }
        };
      })
  );

  describe('passwordChanged', () => {
    it('should dispatch action if the password was changed from default' +
    'and the user is *the* default admin', () => {
      // ShouldDisplayNotice means that the default admin has the default passwd.
      initialState.user.shouldDisplayNotice = true;
      // isDefaultUser means that the user is *THE* default admin
      initialState.user.isDefaultUser = true;

      const store = SpecUtil.mockReduxStore(initialState);
      store.dispatch(userActions.passwordChanged());

      expect(store.getActions().length).toBe(1);
      expect(store.getActions()[0]).toEqual({ type: 'DEFAULT_ADMIN_PASSWORD_CHANGED' });
    });

    it('should not dispatch action if the password changed from default' +
    'and the user is NOT the default admin', () => {
      initialState.user.shouldDisplayNotice = true;
      initialState.user.isDefaultUser = false;

      const store = SpecUtil.mockReduxStore(initialState);
      store.dispatch(userActions.passwordChanged());

      expect(store.getActions().length).toBe(0);
    });

    it('should NOT dispatch the action if the password was not the default', () => {
      initialState.user.shouldDisplayNotice = false;
      const store = SpecUtil.mockReduxStore(initialState);
      store.dispatch(userActions.passwordChanged());

      //No action is dispatched.
      expect(store.getActions().length).toBe(0);
    });

    it('should fire telemetry if the password was changed from default' +
    'and the user is THE default admin', () => {
      initialState.user.shouldDisplayNotice = true;
      initialState.user.isDefaultUser = true;

      const store = SpecUtil.mockReduxStore(initialState);
      store.dispatch(userActions.passwordChanged());

      expect(telemetryService.submitData).toHaveBeenCalledWith('ADMIN_PASSWORD_CHANGE', {
        action: 'PASSWORD_CHANGED_FROM_DEFAULT'
      });
    });

    it('should not fire telemetry if the password was changed from default' +
    'and the user is not THE default admin', () => {
      initialState.user.shouldDisplayNotice = true;
      initialState.user.isDefaultUser = false;

      const store = SpecUtil.mockReduxStore(initialState);
      store.dispatch(userActions.passwordChanged());

      expect(telemetryService.submitData).not.toHaveBeenCalled();
    });

    it('should not fire telemetry if the password was not changed from default', () => {
      initialState.user.shouldDisplayNotice = false;
      const store = SpecUtil.mockReduxStore(initialState);
      store.dispatch(userActions.passwordChanged());

      expect(telemetryService.submitData).not.toHaveBeenCalled();
    });

    it('should broadcast recalculateContainerHeights if the password was changed from default' +
    'and the user is *the* default admin', () => {
      // ShouldDisplayNotice means that the default admin has the default passwd.
      initialState.user.shouldDisplayNotice = true;
      initialState.user.isDefaultUser = true;
      const store = SpecUtil.mockReduxStore(initialState);
      store.dispatch(userActions.passwordChanged());

      expect($rootScope.$broadcast).toHaveBeenCalledWith('recalculateContainerHeights');
    });

    it('should not broadcast recalculateContainerHeights if the password changed from default' +
    'and the user is NOT the default admin', () => {
      initialState.user.shouldDisplayNotice = true;
      initialState.user.isDefaultUser = false;

      const store = SpecUtil.mockReduxStore(initialState);
      store.dispatch(userActions.passwordChanged());

      expect($rootScope.$broadcast).not.toHaveBeenCalledWith('recalculateContainerHeights');
    });

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
    it('should dispatch action if the password was changed from default' +
    'and the user is *the* default admin', () => {
      // ShouldDisplayNotice means that the default admin has the default passwd.
      initialState.user.shouldDisplayNotice = true;
      const selectedUser = { username: 'admin' };
      const store = SpecUtil.mockReduxStore(initialState);
      store.dispatch(userActions.passwordChangedForUser(selectedUser));

      expect(store.getActions().length).toBe(1);
      expect(store.getActions()[0]).toEqual({ type: 'DEFAULT_ADMIN_PASSWORD_CHANGED' });
    });

    it('should not dispatch action if the password changed from default' +
    'and the user is NOT the default admin', () => {
      initialState.user.shouldDisplayNotice = true;
      const selectedUser = { username: 'foo' };

      const store = SpecUtil.mockReduxStore(initialState);
      store.dispatch(userActions.passwordChangedForUser(selectedUser));

      expect(store.getActions().length).toBe(0);
    });

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

    it('should fire telemetry if the password was changed from default' +
    'and the user is THE default admin', () => {
      initialState.user.shouldDisplayNotice = true;
      const selectedUser = { username: 'admin' };

      const store = SpecUtil.mockReduxStore(initialState);
      store.dispatch(userActions.passwordChangedForUser(selectedUser));

      expect(telemetryService.submitData).toHaveBeenCalledWith('ADMIN_PASSWORD_CHANGE', {
        action: 'PASSWORD_CHANGED_FROM_DEFAULT'
      });
    });

    it('should not fire telemetry if the password was changed from default' +
    'and the user is not THE default admin', () => {
      initialState.user.shouldDisplayNotice = true;
      const selectedUser = { username: 'foo' };

      const store = SpecUtil.mockReduxStore(initialState);
      store.dispatch(userActions.passwordChangedForUser(selectedUser));

      expect(telemetryService.submitData).not.toHaveBeenCalled();
    });

    it('should not fire telemetry if the password was not changed from default', () => {
      initialState.user.shouldDisplayNotice = false;
      let selectedUser = { username: 'foo' };
      const store = SpecUtil.mockReduxStore(initialState);
      store.dispatch(userActions.passwordChangedForUser(selectedUser));

      expect(telemetryService.submitData).not.toHaveBeenCalled();

      selectedUser = { username: 'admin' };
      store.dispatch(userActions.passwordChangedForUser(selectedUser));

      expect(telemetryService.submitData).not.toHaveBeenCalled();
    });

    it('should broadcast recalculateContainerHeights if the password was changed from default' +
    'and the user is *the* default admin', () => {
      // ShouldDisplayNotice means that the default admin has the default passwd.
      initialState.user.shouldDisplayNotice = true;
      const selectedUser = { username: 'admin' };
      const store = SpecUtil.mockReduxStore(initialState);
      store.dispatch(userActions.passwordChangedForUser(selectedUser));

      expect($rootScope.$broadcast).toHaveBeenCalledWith('recalculateContainerHeights');
    });

    it('should not broadcast recalculateContainerHeights if the password changed from default' +
    'and the user is NOT the default admin', () => {
      initialState.user.shouldDisplayNotice = true;
      const selectedUser = { username: 'foo' };

      const store = SpecUtil.mockReduxStore(initialState);
      store.dispatch(userActions.passwordChangedForUser(selectedUser));

      expect($rootScope.$broadcast).not.toHaveBeenCalledWith('recalculateContainerHeights');
    });

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

  describe('loadUser', () => {
    let CLMContextLocations;

    beforeEach(inject(function(_CLMContextLocations_) {
      CLMContextLocations = _CLMContextLocations_;
    }));

    afterEach(() => {
      $httpBackend.verifyNoOutstandingExpectation();
      $httpBackend.verifyNoOutstandingRequest();
    });

    it('waits for the current user to log in, queries their permissions,  and sets shouldDisplayWarning to false if ' +
        'they do not have the CONFIGURE_SYSTEM permission', function() {
      $httpBackend.expectPUT(CLMContextLocations.getPermissionTestUrl(true), ['CONFIGURE_SYSTEM']).respond([]);

      const store = SpecUtil.mockReduxStore(initialState);
      const successSpy = jasmine.createSpy('successSpy');
      store.dispatch(userActions.loadUser())
          .then(successSpy);

      $httpBackend.flush();

      expect(successSpy).not.toHaveBeenCalled();

      loginDeferred.resolve({ username: 'admin', clmUser: true });
      $rootScope.$digest();

      expect(successSpy).toHaveBeenCalled();
      expect(store.getActions().length).toBe(2);
      expect(store.getActions()[0]).toEqual({
        type: 'LOAD_USER_REQUESTED'
      });
      expect(store.getActions()[1]).toEqual({
        type: 'LOAD_USER_FULFILLED',
        payload: {
          currentUser: {
            username: 'admin',
            clmUser: true
          },
          shouldDisplayWarning: false
        }
      });
    });

    it('queries shouldDisplayDefaultPasswordWarning if the user has CONFIGURE_SYSTEM and sets shouldDisplayWarning' +
        ' accordingly', function() {
      $httpBackend.expectPUT(CLMContextLocations.getPermissionTestUrl(true), ['CONFIGURE_SYSTEM'])
          .respond(['CONFIGURE_SYSTEM']);
      $httpBackend.expectGET(CLMLocations.getShouldDisplayDefaultPasswordWarning())
          .respond('true');

      const store = SpecUtil.mockReduxStore(initialState);
      const successSpy = jasmine.createSpy('successSpy');
      store.dispatch(userActions.loadUser())
          .then(successSpy);

      $httpBackend.flush();

      expect(successSpy).not.toHaveBeenCalled();

      loginDeferred.resolve({ username: 'admin', clmUser: true });
      $rootScope.$digest();

      expect(successSpy).toHaveBeenCalled();
      expect(store.getActions().length).toBe(2);
      expect(store.getActions()[0]).toEqual({
        type: 'LOAD_USER_REQUESTED'
      });
      expect(store.getActions()[1]).toEqual({
        type: 'LOAD_USER_FULFILLED',
        payload: {
          currentUser: {
            username: 'admin',
            clmUser: true
          },
          shouldDisplayWarning: true
        }
      });
    });

    it('sets shouldDisplayWarning to false if the shouldDisplayDefaultPasswordWarning endpoint returns false',
        function() {
          $httpBackend.expectPUT(CLMContextLocations.getPermissionTestUrl(true), ['CONFIGURE_SYSTEM'])
              .respond(['CONFIGURE_SYSTEM']);
          $httpBackend.expectGET(CLMLocations.getShouldDisplayDefaultPasswordWarning())
              .respond('false');

          const store = SpecUtil.mockReduxStore(initialState);
          const successSpy = jasmine.createSpy('successSpy');
          store.dispatch(userActions.loadUser())
              .then(successSpy);

          $httpBackend.flush();

          expect(successSpy).not.toHaveBeenCalled();

          loginDeferred.resolve({ username: 'admin', clmUser: true });
          $rootScope.$digest();

          expect(successSpy).toHaveBeenCalled();
          expect(store.getActions().length).toBe(2);
          expect(store.getActions()[0]).toEqual({
            type: 'LOAD_USER_REQUESTED'
          });
          expect(store.getActions()[1]).toEqual({
            type: 'LOAD_USER_FULFILLED',
            payload: {
              currentUser: {
                username: 'admin',
                clmUser: true
              },
              shouldDisplayWarning: false
            }
          });
        }
    );

    it('should dispatch error if the call to get the current user does not resolve', () => {
      $httpBackend.expectPUT(CLMContextLocations.getPermissionTestUrl(true), ['CONFIGURE_SYSTEM'])
          .respond(['CONFIGURE_SYSTEM']);

      $httpBackend.expectGET(CLMLocations.getShouldDisplayDefaultPasswordWarning())
          .respond('true');

      const store = SpecUtil.mockReduxStore(initialState);
      const errorSpy = jasmine.createSpy('errorSpy');
      store.dispatch(userActions.loadUser())
          .catch(errorSpy);

      $httpBackend.flush();

      loginDeferred.reject({ message: 'Some server error message' });
      $rootScope.$digest();

      expect(errorSpy).toHaveBeenCalled();
      expect(store.getActions().length).toBe(2);
      expect(store.getActions()[0]).toEqual({
        type: 'LOAD_USER_REQUESTED'
      });
      expect(store.getActions()[1]).toEqual({
        type: 'LOAD_USER_FAILED',
        payload: { message: 'Some server error message' }
      });
    });

    it('should set the warning flag to false if the call to get permissions does not resolve', () => {
      loginDeferred.resolve({ username: 'admin', clmUser: true });
      $rootScope.$digest();

      $httpBackend.expectPUT(CLMContextLocations.getPermissionTestUrl(true), ['CONFIGURE_SYSTEM'])
          .respond(500, 'Some server error message');

      const store = SpecUtil.mockReduxStore(initialState);
      const errorSpy = jasmine.createSpy('errorSpy');
      store.dispatch(userActions.loadUser())
          .catch(errorSpy);

      $httpBackend.flush();
      expect(errorSpy).not.toHaveBeenCalled();
      expect(store.getActions().length).toBe(2);
      expect(store.getActions()[0]).toEqual({
        type: 'LOAD_USER_REQUESTED'
      });
      expect(store.getActions()[1]).toEqual({
        type: 'LOAD_USER_FULFILLED',
        payload: {
          currentUser: {
            username: 'admin',
            clmUser: true
          },
          shouldDisplayWarning: false
        }
      });
    });

    it('should set the warning flag to false if the call to get it does not resolve', () => {
      loginDeferred.resolve({ username: 'admin', clmUser: true });
      $rootScope.$digest();

      $httpBackend.expectPUT(CLMContextLocations.getPermissionTestUrl(true), ['CONFIGURE_SYSTEM'])
          .respond(['CONFIGURE_SYSTEM']);

      $httpBackend.expectGET(CLMLocations.getShouldDisplayDefaultPasswordWarning())
          .respond(500, 'Some server error message');

      const store = SpecUtil.mockReduxStore(initialState);
      const errorSpy = jasmine.createSpy('errorSpy');
      store.dispatch(userActions.loadUser())
          .catch(errorSpy);

      $httpBackend.flush();
      expect(errorSpy).not.toHaveBeenCalled();
      expect(store.getActions().length).toBe(2);
      expect(store.getActions()[0]).toEqual({
        type: 'LOAD_USER_REQUESTED'
      });
      expect(store.getActions()[1]).toEqual({
        type: 'LOAD_USER_FULFILLED',
        payload: {
          currentUser: {
            username: 'admin',
            clmUser: true
          },
          shouldDisplayWarning: false
        }
      });
    });

    it('should submit telemetry data when the display flag is shown', () => {
      $httpBackend.expectPUT(CLMContextLocations.getPermissionTestUrl(true), ['CONFIGURE_SYSTEM'])
          .respond(['CONFIGURE_SYSTEM']);

      $httpBackend.expectGET(CLMLocations.getShouldDisplayDefaultPasswordWarning())
          .respond('true');

      const store = SpecUtil.mockReduxStore(initialState);
      const successSpy = jasmine.createSpy('successSpy');
      store.dispatch(userActions.loadUser())
          .then(successSpy);

      $httpBackend.flush();

      expect(telemetryService.submitData).not.toHaveBeenCalled();

      loginDeferred.resolve({ username: 'admin', clmUser: true });
      $rootScope.$digest();

      expect(successSpy).toHaveBeenCalled();
      expect(telemetryService.submitData).toHaveBeenCalledWith('ADMIN_PASSWORD_CHANGE', {
        action: 'WARNING_SHOWN'
      });
    });

    it('should not submit telemetry data when the display flag is not shown', () => {
      $httpBackend.expectPUT(CLMContextLocations.getPermissionTestUrl(true), ['CONFIGURE_SYSTEM'])
          .respond(['CONFIGURE_SYSTEM']);

      $httpBackend.expectGET(CLMLocations.getShouldDisplayDefaultPasswordWarning())
          .respond(false);

      const store = SpecUtil.mockReduxStore(initialState);
      const successSpy = jasmine.createSpy('successSpy');
      store.dispatch(userActions.loadUser())
          .then(successSpy);

      $httpBackend.flush();
      loginDeferred.resolve({ username: 'admin', clmUser: true });
      $rootScope.$digest();

      expect(successSpy).toHaveBeenCalled();
      expect(telemetryService.submitData).not.toHaveBeenCalled();
    });

    it('should not submit telemetry data when the permissions call fails', () => {

      $httpBackend.expectPUT(CLMContextLocations.getPermissionTestUrl(true), ['CONFIGURE_SYSTEM']).respond(500);

      const store = SpecUtil.mockReduxStore(initialState);
      const successSpy = jasmine.createSpy('successSpy');
      store.dispatch(userActions.loadUser())
          .then(successSpy);

      $httpBackend.flush();
      loginDeferred.resolve({ username: 'admin', clmUser: true });
      $rootScope.$digest();

      expect(successSpy).toHaveBeenCalled();
      expect(telemetryService.submitData).not.toHaveBeenCalled();
    });

    it('should not submit telemetry data when the flag call fails', () => {
      loginDeferred.resolve({ username: 'admin', clmUser: true });
      $rootScope.$digest();

      $httpBackend.expectPUT(CLMContextLocations.getPermissionTestUrl(true), ['CONFIGURE_SYSTEM'])
          .respond(['CONFIGURE_SYSTEM']);

      $httpBackend.expectGET(CLMLocations.getShouldDisplayDefaultPasswordWarning()).respond(500);

      const store = SpecUtil.mockReduxStore(initialState);
      const successSpy = jasmine.createSpy('successSpy');
      store.dispatch(userActions.loadUser())
          .then(successSpy);

      $httpBackend.flush();

      expect(successSpy).toHaveBeenCalled();
      expect(telemetryService.submitData).not.toHaveBeenCalled();
    });
  });
});
