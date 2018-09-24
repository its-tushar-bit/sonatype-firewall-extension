import mainHeaderModule from '../../../../main/frontend/mainHeader/module';

describe('userMenu', function() {
  var scope,
      vm,
      currentUserSuccess,
      dialogScope,
      parentScope,
      pendoFlushDeferred,
      mockPendoService = {
        flush: function() { return pendoFlushDeferred.promise; }
      };

  beforeEach(angular.mock.module(mainHeaderModule.name, function($provide) {

    $provide.value('CurrentUser', {
      then: function (success) {
        currentUserSuccess = success;
        return this;
      }
    });

    $provide.value('ProductFeatures', {
      isDashboardLicensed: function() {
        return true;
      }
    });

    $provide.value('Modal', {
      open: function(config) {
        dialogScope = scope.$new();
        dialogScope.$close = jasmine.createSpy('dialogClose');
        inject(function($controller) {
          $controller(config.controller, {
            $scope: dialogScope
          });
        });
        return {
          result: {
            then: function(success) {
              success();
            }
          }
        };
      }
    });
  }));

  beforeEach(inject(function($q, $rootScope, $componentController) {
    parentScope = $rootScope.$new();
    scope = parentScope.$new();
    pendoFlushDeferred = $q.defer();
    vm = $componentController('userMenu', {
      $scope: scope,
      pendoService: mockPendoService
    });
    vm.$onInit();
  }));

  afterEach(inject(function($httpBackend) {
    if (parentScope) {
      parentScope.$destroy();
    }
    else if (scope) {
      scope.$destroy();
    }
    delete window.clmServerVersion;
    $httpBackend.verifyNoOutstandingExpectation();
    $httpBackend.verifyNoOutstandingRequest();
  }));

  describe('logout()', function () {
    it('provides the ability to log out', inject(function($httpBackend, CLMLocations) {
      var spy = jasmine.createSpy();
      parentScope.$on('logout', spy);

      expect(vm.logout).not.toBeUndefined();
      $httpBackend.expectDELETE(CLMLocations.getSessionLogoutUrl()).respond({});

      vm.logout();

      pendoFlushDeferred.resolve();
      scope.$digest();

      $httpBackend.flush();

      expect(spy).toHaveBeenCalled();
    }));

    it('provides the ability to log out for reverse proxy', inject(function($httpBackend, CLMLocations) {
      var spy = jasmine.createSpy();
      parentScope.$on('logout', spy);
      var headers = {'Location': 'http://localhost/logout'};
      expect(vm.logout).not.toBeUndefined();
      $httpBackend.expectDELETE(CLMLocations.getSessionLogoutUrl()).respond(204, '', headers);

      vm.logout();

      pendoFlushDeferred.resolve();
      scope.$digest();

      $httpBackend.flush();

      expect(spy).toHaveBeenCalledWith(jasmine.any(Object), headers.Location);
    }));

    it('still logs out if the pendo promise is rejected', inject(function($httpBackend, CLMLocations) {
      var spy = jasmine.createSpy();
      parentScope.$on('logout', spy);

      expect(vm.logout).not.toBeUndefined();
      $httpBackend.expectDELETE(CLMLocations.getSessionLogoutUrl()).respond({});

      vm.logout();

      pendoFlushDeferred.reject();
      scope.$digest();

      $httpBackend.flush();

      expect(spy).toHaveBeenCalled();
    }));

    it('doesn\'t log out from the server before the pendo promise completes', inject(
        function($httpBackend, CLMLocations) {
          var spy = jasmine.createSpy();
          parentScope.$on('logout', spy);

          expect(vm.logout).not.toBeUndefined();

          vm.logout();

          // not setting this up until after `logout` is called, to ensure that the test fails if the request occurs too
          // early
          $httpBackend.expectDELETE(CLMLocations.getSessionLogoutUrl()).respond({});

          pendoFlushDeferred.resolve();
          scope.$digest();

          $httpBackend.flush();

          expect(spy).toHaveBeenCalled();
        }
    ));
  });

  describe('canChangePassword()', function () {
    it('Not Loaded', function () {
      expect(vm.canChangePassword()).toBeFalsy();
    });
    it('CLM User', function () {
      currentUserSuccess({
        username: 'foo',
        authenticated: true,
        clmUser: true
      });
      expect(vm.canChangePassword()).toBeTruthy();
    });
    it('Not CLM User', function () {
      currentUserSuccess({
        username: 'foo',
        authenticated: true,
        clmUser: false
      });
      expect(vm.canChangePassword()).toBeFalsy();
    });
  });

  describe('Change Password Dialog', function () {
    var shouldDisplayDefaultPasswordWarningDeferred,
        $httpBackend,
        CLMLocations,
        telemetryService;

    beforeEach(inject(
        function($q, _$httpBackend_, _CLMLocations_, _telemetryService_, defaultAdminPasswordChangedService) {
          $httpBackend = _$httpBackend_;
          CLMLocations = _CLMLocations_;
          telemetryService = _telemetryService_;

          shouldDisplayDefaultPasswordWarningDeferred = $q.defer();

          spyOn(telemetryService, 'submitData');
          spyOn(defaultAdminPasswordChangedService, 'shouldDisplayDefaultPasswordWarning')
              .and.returnValue(shouldDisplayDefaultPasswordWarningDeferred.promise);
        }
    ));

    function doPasswordChange(originalPassword, newPassword) {
      currentUserSuccess({
        username: 'foo',
        authenticated: true,
        clmUser: true
      });
      vm.changePassword();

      dialogScope.result = {
        originalPassword: originalPassword,
        newPassword: newPassword,
        confirmPassword: newPassword
      };
      dialogScope.passwordForm = {
        $valid: true // form validation
      };
    }

    it('calls $close when the password change succeeds', function() {
      doPasswordChange('bar', 'xxx');

      $httpBackend.expectPUT(CLMLocations.getChangeMyPasswordUrl()).respond(200);
      dialogScope.save();
      expect(dialogScope.submitActive).toBeTruthy();
      $httpBackend.flush();

      expect(dialogScope.$close).toHaveBeenCalled();
    });

    it('sets error and does not call $close when password change fails', function() {
      doPasswordChange('bar', 'xxx');

      $httpBackend.expectPUT(CLMLocations.getChangeMyPasswordUrl()).respond(400, 'Super Fail');

      dialogScope.save();
      expect(dialogScope.submitActive).toBeTruthy();

      $httpBackend.flush();

      expect(dialogScope.submitActive).toBeFalsy();
      expect(dialogScope.$close).not.toHaveBeenCalled();
      expect(dialogScope.error).toEqual('Super Fail');
    });

    it('submits telemetryData when the password change succeeds, the new and old password values differ, ' +
        'and the default password service reports true', function() {
      doPasswordChange('bar', 'xxx');

      shouldDisplayDefaultPasswordWarningDeferred.resolve(true);
      $httpBackend.expectPUT(CLMLocations.getChangeMyPasswordUrl()).respond(200);

      dialogScope.save();
      $httpBackend.flush();
      dialogScope.$digest();

      expect(telemetryService.submitData).toHaveBeenCalledWith('ADMIN_PASSWORD_CHANGE', {
        action: 'PASSWORD_CHANGED_FROM_DEFAULT'
      });
    });

    it('does not submit telemetryData when the default password service reports false', function() {
      doPasswordChange('bar', 'xxx');

      shouldDisplayDefaultPasswordWarningDeferred.resolve(false);
      $httpBackend.expectPUT(CLMLocations.getChangeMyPasswordUrl()).respond(200);

      dialogScope.save();
      $httpBackend.flush();
      dialogScope.$digest();

      expect(telemetryService.submitData).not.toHaveBeenCalled();
    });

    it('does not submit telemetryData when the password change fails', function() {
      doPasswordChange('bar', 'xxx');

      shouldDisplayDefaultPasswordWarningDeferred.resolve(true);
      $httpBackend.expectPUT(CLMLocations.getChangeMyPasswordUrl()).respond(400, 'Super Fail');

      dialogScope.save();
      $httpBackend.flush();
      dialogScope.$digest();

      expect(telemetryService.submitData).not.toHaveBeenCalled();
    });

    it('does not submit telemetryData when the new password is the same as the old one', function() {
      doPasswordChange('bar', 'bar');

      shouldDisplayDefaultPasswordWarningDeferred.resolve(true);
      $httpBackend.expectPUT(CLMLocations.getChangeMyPasswordUrl()).respond(200);

      dialogScope.save();
      $httpBackend.flush();
      dialogScope.$digest();

      expect(telemetryService.submitData).not.toHaveBeenCalled();
    });
  });
});
