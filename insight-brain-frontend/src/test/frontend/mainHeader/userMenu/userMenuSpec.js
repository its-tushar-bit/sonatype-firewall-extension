/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import mainHeaderModule from '../../../../main/frontend/mainHeader/module';

describe('userMenu', function() {
  let scope,
      vm,
      modal,
      dialogScope,
      parentScope,
      CLMLocations,
      pendoFlushDeferred,
      $httpBackend,
      $componentController,
      mockPendoService,
      mockActions,
      mockMaskController;

  beforeEach(angular.mock.module(mainHeaderModule.name, function($provide) {
    mockMaskController = {
      wrap: jasmine.createSpy()
    };
    mockActions = jasmine.createSpyObj('userActions', ['loadUser', 'passwordChanged']);
    $provide.value('userActions', mockActions);
    SpecUtil.mockNgRedux($provide);
  }));

  beforeEach(inject(function($q, $rootScope, _$componentController_, _$httpBackend_, _CLMLocations_) {
    $httpBackend = _$httpBackend_;
    CLMLocations = _CLMLocations_;
    $componentController = _$componentController_;
    parentScope = $rootScope.$new();
    scope = parentScope.$new();
    pendoFlushDeferred = $q.defer();
    mockPendoService = {
      flush: () => pendoFlushDeferred.promise
    };
    modal = {
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
    };

    vm = $componentController('userMenu', {
      $scope: scope,
      pendoService: mockPendoService,
      Modal: modal
    });

    vm.logoutMask = mockMaskController;

    vm.$onInit();
    expect(vm.loadUser).toHaveBeenCalled();
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

  describe('$onInit', function () {
    it('fires loadUser', function() {
      expect(vm.loadUser).toHaveBeenCalled();
    });
  });

  describe('$onDestroy', function() {
    it('unsubscribes from the redux store', function() {
      expect(vm.unsubscribe).not.toHaveBeenCalled();
      vm.$onDestroy();
      expect(vm.unsubscribe).toHaveBeenCalledTimes(1);
    });
  });

  describe('logout()', function () {
    it('provides the ability to log out', function() {
      var logoutSpy = jasmine.createSpy();
      parentScope.$on('logout', logoutSpy);

      expect(vm.logout).not.toBeUndefined();
      $httpBackend.expectDELETE(CLMLocations.getSessionLogoutUrl()).respond({});

      expect(mockMaskController.wrap).not.toHaveBeenCalled();

      vm.logout();
      expect(mockMaskController.wrap).toHaveBeenCalled();

      let maskArg = mockMaskController.wrap.calls.mostRecent().args[0];
      let maskArgResolvedSpy = jasmine.createSpy('promiseSpy');
      maskArg.then(maskArgResolvedSpy);

      pendoFlushDeferred.resolve();
      scope.$digest();

      expect(maskArgResolvedSpy).not.toHaveBeenCalled();
      $httpBackend.flush();
      expect(maskArgResolvedSpy).toHaveBeenCalled();

      expect(logoutSpy).toHaveBeenCalled();
      expect(mockMaskController.wrap).toHaveBeenCalled();
    });

    it('provides the ability to log out for reverse proxy', function() {
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
    });

    it('still logs out if the pendo promise is rejected', function() {
      var spy = jasmine.createSpy();
      parentScope.$on('logout', spy);

      expect(vm.logout).not.toBeUndefined();
      $httpBackend.expectDELETE(CLMLocations.getSessionLogoutUrl()).respond({});

      vm.logout();

      pendoFlushDeferred.reject();
      scope.$digest();

      $httpBackend.flush();

      expect(spy).toHaveBeenCalled();
    });

    it('doesn\'t log out from the server before the pendo promise completes', function() {
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
    });
  });

  describe('Change Password Dialog', function () {
    function doPasswordChange(originalPassword, newPassword) {
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

    it('fires the passwordChanged action when the password change succeeds, ' +
     'and the new and old password values differ', function() {
      doPasswordChange('bar', 'xxx');
      $httpBackend.expectPUT(CLMLocations.getChangeMyPasswordUrl()).respond(200);

      dialogScope.save();
      $httpBackend.flush();
      dialogScope.$digest();

      expect(vm.passwordChanged).toHaveBeenCalled();
    });

    it('does not fires the passwordChanged action when the password change succeeds, ' +
    'but the new and old password values dont differ', function () {
      doPasswordChange('bar', 'bar');
      $httpBackend.expectPUT(CLMLocations.getChangeMyPasswordUrl()).respond(200);

      dialogScope.save();
      $httpBackend.flush();
      dialogScope.$digest();

      expect(vm.passwordChanged).not.toHaveBeenCalled();
    });

    it('does not calls passwordChanged when the password change fails', function() {
      doPasswordChange('bar', 'xxx');

      $httpBackend.expectPUT(CLMLocations.getChangeMyPasswordUrl()).respond(400, 'Super Fail');

      dialogScope.save();
      $httpBackend.flush();
      dialogScope.$digest();

      expect(vm.passwordChanged).not.toHaveBeenCalled();
    });
  });

  describe('details', function() {
    it('opens the current user details modal', function() {
      modal.open = jasmine.createSpy();

      vm.details();

      expect(modal.open).toHaveBeenCalled();
    });
  });
});
