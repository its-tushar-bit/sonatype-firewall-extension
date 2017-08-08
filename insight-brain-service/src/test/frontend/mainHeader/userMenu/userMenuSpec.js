describe('userMenu', function() {
  var scope, vm,
    parentScope = null,
    dialogScope = null,
    currentUserSuccess = null,
    currentUserFail = null;

  beforeEach(module('mainHeader', function($provide) {

    $provide.value('CurrentUser', {
      then : function (success, fail) {
        currentUserSuccess = success;
        currentUserFail = fail;
        return this;
      }
    });

    $provide.value('ProductFeatures', {
      isDashboardLicensed : function() {
        return true;
      }
    });

    $provide.value('$modal', {
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
            then: function(success, failure) {
              success();
            }
          }
        };
      }
    });
  }));

  beforeEach(inject(function($rootScope, $componentController) {
    parentScope = $rootScope.$new();
    scope = parentScope.$new();
    vm = $componentController('userMenu', {
      $scope: scope
    });
    vm.$onInit();
  }));

  afterEach(inject(function($httpBackend) {
    if (parentScope) {
      parentScope.$destroy();
    } else if (scope) {
      scope.$destroy();
    }
    delete window.clmServerVersion;
    $httpBackend.verifyNoOutstandingExpectation();
    $httpBackend.verifyNoOutstandingRequest();
  }));

  describe('logout()', function () {

    it('provides the ability to log out', inject(function($httpBackend, CLMLocations){
      var spy = jasmine.createSpy();
      parentScope.$on('logout', spy);

      expect(vm.logout).not.toBeUndefined();
      $httpBackend.expectDELETE(CLMLocations.getSessionLogoutUrl()).respond({});

      vm.logout();
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
      $httpBackend.flush();

      expect(spy).toHaveBeenCalledWith(jasmine.any(Object), headers.Location);
    }));
  });

  describe('canChangePassword()', function () {
    it('Not Loaded', function () {
      expect(vm.canChangePassword()).toBeFalsy();
    });
    it('CLM User', function () {
      currentUserSuccess({
        username : 'foo',
        authenticated : true,
        clmUser : true
      });
      expect(vm.canChangePassword()).toBeTruthy();
    });
    it('Not CLM User', function () {
      currentUserSuccess({
        username : 'foo',
        authenticated : true,
        clmUser : false
      });
      expect(vm.canChangePassword()).toBeFalsy();
    });
  });

  describe('Change Password Dialog', function () {
    beforeEach(inject(function () {
      currentUserSuccess({
        username : 'foo',
        authenticated : true,
        clmUser : true
      });
      vm.changePassword();

      dialogScope.result = {
        originalPassword : 'bar',
        newPassword : 'xxx',
        confirmPassword : 'xxx'
      };
      dialogScope.passwordForm = {
        $valid : true // form validation
      };
    }));

    it('With Valid Auth', inject(function ($httpBackend, CLMLocations) {
      $httpBackend.expectPUT(CLMLocations.getChangeMyPasswordUrl()).respond(200);
      dialogScope.save();
      expect(dialogScope.submitActive).toBeTruthy();
      $httpBackend.flush();

      expect(dialogScope.$close).toHaveBeenCalled();
    }));

    it('With Invalid Auth', inject(function ($httpBackend, CLMLocations, Messages) {
      $httpBackend.expectPUT(CLMLocations.getChangeMyPasswordUrl()).respond(400, 'Super Fail');

      dialogScope.save();
      expect(dialogScope.submitActive).toBeTruthy();

      $httpBackend.flush();

      expect(dialogScope.submitActive).toBeFalsy();
      expect(dialogScope.$close).not.toHaveBeenCalled();
      expect(dialogScope.error).toEqual('Super Fail');
    }));
  });
});
