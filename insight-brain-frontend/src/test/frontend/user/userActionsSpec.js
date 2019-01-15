import changeDefaultAdminPasswordNoticeModule from '../../../main/frontend/changeDefaultAdminPasswordNotice/module';

describe('userActions', function() {
  let userActions, initialState, CLMLocations, telemetryService, $httpBackend;

  beforeEach(angular.mock.module(changeDefaultAdminPasswordNoticeModule.name));

  beforeEach(inject((_$httpBackend_, _CLMLocations_, _userActions_, _telemetryService_) => {
    $httpBackend = _$httpBackend_;
    CLMLocations = _CLMLocations_;
    userActions = _userActions_;
    telemetryService = _telemetryService_;

    spyOn(telemetryService, 'submitData');

    initialState = {
      user: {
        currentUser: null,
        isDefaultUser: false,
        shouldDisplayNotice: false,
        canChangePassword: false
      }
    };
  }));

  describe('passwordChanged', () => {
    it('should dispatch action if the password was changed from default', () => {
      // ShouldDisplayNotice means that the user is admin and has the default passwd
      initialState.user.shouldDisplayNotice = true;

      const store = SpecUtil.mockReduxStore(initialState);
      store.dispatch(userActions.passwordChanged());

      expect(store.getActions().length).toBe(1);
      expect(store.getActions()[0]).toEqual({ type: 'ADMIN_PASSWORD_CHANGED' });
    });

    it('should NOT dispatch the action if the password was not the default', () => {
      initialState.user.shouldDisplayNotice = false;
      const store = SpecUtil.mockReduxStore(initialState);
      store.dispatch(userActions.passwordChanged());

      //No action is dispatched.
      expect(store.getActions().length).toBe(0);
    });

    it('should fire telemetry if the password was changed from default', () => {
      initialState.user.shouldDisplayNotice = true;

      const store = SpecUtil.mockReduxStore(initialState);
      store.dispatch(userActions.passwordChanged());

      expect(telemetryService.submitData).toHaveBeenCalledWith('ADMIN_PASSWORD_CHANGE', {
        action: 'PASSWORD_CHANGED_FROM_DEFAULT'
      });
    });

    it('should not fire telemetry if the password was not changed from default', () => {
      initialState.user.shouldDisplayNotice = false;
      const store = SpecUtil.mockReduxStore(initialState);
      store.dispatch(userActions.passwordChanged());

      expect(telemetryService.submitData).not.toHaveBeenCalled();
    });
  });

  describe('loadUser', () => {
    afterEach(() => {
      $httpBackend.verifyNoOutstandingExpectation();
      $httpBackend.verifyNoOutstandingRequest();
    });

    it('should query the backend for the current user and display flag', () => {
      $httpBackend.expectGET(CLMLocations.getSessionUrl()).respond({
        username: 'admin',
        clmUser: true
      });

      $httpBackend.expectGET(CLMLocations.getShouldDisplayDefaultPasswordWarning())
          .respond('true');

      const store = SpecUtil.mockReduxStore(initialState);
      const successSpy = jasmine.createSpy('successSpy');
      store.dispatch(userActions.loadUser())
          .then(successSpy);

      $httpBackend.flush();

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

    it('should dispatch error if the call to get the current user does not resolve', () => {
      $httpBackend.expectGET(CLMLocations.getSessionUrl())
          .respond(500, 'Some server error message');

      $httpBackend.expectGET(CLMLocations.getShouldDisplayDefaultPasswordWarning())
          .respond('true');

      const store = SpecUtil.mockReduxStore(initialState);
      const errorSpy = jasmine.createSpy('errorSpy');
      store.dispatch(userActions.loadUser())
          .catch(errorSpy);

      $httpBackend.flush();
      expect(errorSpy).toHaveBeenCalled();
      expect(store.getActions().length).toBe(2);
      expect(store.getActions()[0]).toEqual({
        type: 'LOAD_USER_REQUESTED'
      });
      expect(store.getActions()[1]).toEqual({
        type: 'LOAD_USER_FAILED',
        payload: 'Some server error message'
      });
    });

    it('should set the display flag to false if the call to get it does not resolve', () => {
      $httpBackend.expectGET(CLMLocations.getSessionUrl())
          .respond({
            username: 'admin',
            clmUser: true
          });

      $httpBackend.expectGET(CLMLocations.getShouldDisplayDefaultPasswordWarning())
          .respond(403, 'forbidden');

      const store = SpecUtil.mockReduxStore(initialState);
      const successSpy = jasmine.createSpy('successSpy');
      store.dispatch(userActions.loadUser())
          .then(successSpy);

      $httpBackend.flush();
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

    it('should submit error when both calls fail', () => {
      $httpBackend.expectGET(CLMLocations.getSessionUrl())
          .respond(500, 'Some server error message');

      $httpBackend.expectGET(CLMLocations.getShouldDisplayDefaultPasswordWarning())
          .respond(403, 'forbidden');

      const store = SpecUtil.mockReduxStore(initialState);
      const errorSpy = jasmine.createSpy('errorSpy');
      store.dispatch(userActions.loadUser())
          .catch(errorSpy);

      $httpBackend.flush();
      expect(errorSpy).toHaveBeenCalled();
      expect(store.getActions().length).toBe(2);
      expect(store.getActions()[0]).toEqual({
        type: 'LOAD_USER_REQUESTED'
      });
      expect(store.getActions()[1]).toEqual({
        type: 'LOAD_USER_FAILED',
        payload: 'Some server error message'
      });
    });

    it('should submit telemetry data when the display flag is shown', () => {
      $httpBackend.expectGET(CLMLocations.getSessionUrl()).respond({
        username: 'admin',
        clmUser: true
      });

      $httpBackend.expectGET(CLMLocations.getShouldDisplayDefaultPasswordWarning())
          .respond('true');

      const store = SpecUtil.mockReduxStore(initialState);
      const successSpy = jasmine.createSpy('successSpy');
      store.dispatch(userActions.loadUser())
          .then(successSpy);

      $httpBackend.flush();

      expect(successSpy).toHaveBeenCalled();
      expect(telemetryService.submitData).toHaveBeenCalledWith('ADMIN_PASSWORD_CHANGE', {
        action: 'WARNING_SHOWN'
      });
    });

    it('should not submit telemetry data when the display flag is not shown', () => {
      $httpBackend.expectGET(CLMLocations.getSessionUrl()).respond({
        username: 'admin',
        clmUser: true
      });

      $httpBackend.expectGET(CLMLocations.getShouldDisplayDefaultPasswordWarning())
          .respond(false);

      const store = SpecUtil.mockReduxStore(initialState);
      const successSpy = jasmine.createSpy('successSpy');
      store.dispatch(userActions.loadUser())
          .then(successSpy);

      $httpBackend.flush();

      expect(successSpy).toHaveBeenCalled();
      expect(telemetryService.submitData).not.toHaveBeenCalled();
    });

    it('should not submit telemetry data when the display flag call does not resolve', () => {
      $httpBackend.expectGET(CLMLocations.getSessionUrl()).respond({
        username: 'admin',
        clmUser: true
      });

      $httpBackend.expectGET(CLMLocations.getShouldDisplayDefaultPasswordWarning())
          .respond(403, 'forbidden');

      const store = SpecUtil.mockReduxStore(initialState);
      const successSpy = jasmine.createSpy('successSpy');
      store.dispatch(userActions.loadUser())
          .then(successSpy);

      $httpBackend.flush();

      expect(successSpy).toHaveBeenCalled();
      expect(telemetryService.submitData).not.toHaveBeenCalled();
    });

    it('should not submit telemetry data when both calls fail', () => {
      $httpBackend.expectGET(CLMLocations.getSessionUrl())
          .respond(500, 'Some server error message');

      $httpBackend.expectGET(CLMLocations.getShouldDisplayDefaultPasswordWarning())
          .respond(403, 'forbidden');

      const store = SpecUtil.mockReduxStore(initialState);
      const errorSpy = jasmine.createSpy('errorSpy');
      store.dispatch(userActions.loadUser())
          .catch(errorSpy);

      $httpBackend.flush();
      expect(errorSpy).toHaveBeenCalled();
      expect(telemetryService.submitData).not.toHaveBeenCalled();
    });
  });
});
