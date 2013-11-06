describe('UserControlsSpec', function () {
  var scope = null,
      parentScope = null,
      dialogScope = null,
      currentUserSuccess = null,
      currentUserFail = null;

  beforeEach(module('UserControls', function ($provide) {
    $provide.value('CurrentUser', {
      then : function (success, fail) {
        userControlSuccess = success;
        userControlFail = fail;
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

  afterEach(inject(function($httpBackend) {
    if (scope) {
      scope.$destroy();
    }
    $httpBackend.verifyNoOutstandingExpectation();
    $httpBackend.verifyNoOutstandingRequest();
  }));

  describe('LogoutController', function () {
    beforeEach(inject(function ($controller, $rootScope) {
      parentScope = $rootScope.$new();

      scope = parentScope.$new();

      $controller('LogoutController', {
        $scope : scope
      });
    }));

    it('provides the ability to log out', inject(function($httpBackend, CLMLocations){
      var spy = jasmine.createSpy();
      parentScope.$on('logout', spy);

      expect(scope.logout).not.toBeUndefined();
      $httpBackend.expectDELETE(CLMLocations.getSessionUrl()).respond({});

      scope.logout();
      $httpBackend.flush();

      expect(spy).toHaveBeenCalled();
    }));
  });

  describe('ChangePassword', function () {
    beforeEach(inject(function ($controller, $rootScope) {
      parentScope = $rootScope.$new();

      scope = parentScope.$new();

      $controller('ChangePassword', {
        $scope : scope
      });

      userControlSuccess({
        username : 'foo',
        authenticated : true,
        isClmUser : true
      });
      scope.change();

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
      $httpBackend.expectPUT(CLMLocations.getChangePasswordUrl('foo')).respond(200);
      dialogScope.save();
      expect(dialogScope.submitActive).toBeTruthy();
      $httpBackend.flush();

      expect(dialogScope.$close).toHaveBeenCalled();
    }));

    it('With Invalid Auth', inject(function ($httpBackend, CLMLocations, Messages) {
      $httpBackend.expectPUT(CLMLocations.getChangePasswordUrl('foo')).respond(400);

      dialogScope.save();
      expect(dialogScope.submitActive).toBeTruthy();

      $httpBackend.flush();

      expect(dialogScope.submitActive).toBeFalsy();
      expect(dialogScope.$close).not.toHaveBeenCalled();
      expect(dialogScope.error).toEqual(Messages.getHttpErrorMessage([undefined, 400]));
    }));
  });

  describe('match', function () {
    var element = null,
        input = null;

    beforeEach(inject(function ($compile, $rootScope) {
      scope = $rootScope.$new();
      element = $compile('<form name="myForm"><input name="myInput" match="{{matchVal}}" ng-model="inputVal"></form>')(scope);
      angular.element('body').append(element);
      input = angular.element('input', element);
    }));

    afterEach(function () {
      element.remove();
    });

    it('mismatch', function () {
      scope.$apply(function () {
        scope.matchVal = 'bar';
      });
      SpecUtil.setInput(input, 'foo');
      expect(scope.myForm.myInput.$invalid).toBeTruthy();
      expect(scope.myForm.myInput.$error.match).toBeTruthy();
    });

    it('match', function () {
      scope.$apply(function () {
        scope.matchVal = 'foo';
      });
      SpecUtil.setInput(input, 'foo');
      expect(scope.myForm.myInput.$valid).toBeTruthy();
      expect(scope.myForm.myInput.$error.match).toBeFalsy();
    });

    it('backwards match', function () {
      SpecUtil.setInput(input, 'foo');
      expect(scope.myForm.myInput.$invalid).toBeTruthy();
      expect(scope.myForm.myInput.$error.match).toBeTruthy();

      scope.$apply(function () {
        scope.matchVal = 'foo';
      });

      expect(scope.myForm.myInput.$valid).toBeTruthy();
      expect(scope.myForm.myInput.$error.match).toBeFalsy();
    });
  });
});