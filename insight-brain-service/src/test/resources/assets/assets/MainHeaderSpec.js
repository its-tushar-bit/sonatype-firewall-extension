describe('mainHeader', function() {
  var scope, state,
    parentScope = null,
    dialogScope = null,
    currentUserSuccess = null,
    currentUserFail = null;

  beforeEach(module('MainHeader', function($provide) {
    $provide.value('$window', {
      location: {
        href: 'http://blah/index.html',
        replace: jasmine.createSpy()
      },
      navigator: {
        userAgent: {}
      },
      document: {
        createElement: function() {
          return null;
        }
      }
    });

    $provide.value('CurrentUser', {
      then : function (success, fail) {
        currentUserSuccess = success;
        currentUserFail = fail;
        return this;
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

  beforeEach(inject(function($rootScope, $state, $controller) {
    scope = $rootScope.$new();
    state = $state;

    $controller('mainHeaderController', {
      $scope: scope,
      $state: state
    });
  }));

  afterEach(inject(function($httpBackend) {
    if (parentScope) {
      parentScope.$destroy();
    } else if (scope) {
      scope.$destroy();
    }
    $httpBackend.verifyNoOutstandingExpectation();
    $httpBackend.verifyNoOutstandingRequest();
  }));

  describe('Main Header User Controls', function () {
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

    describe('ChangePasswordController', function () {
      beforeEach(inject(function ($controller, $rootScope) {
        scope = $rootScope.$new();

        $controller('ChangePassword', {
          $scope : scope
        });
      }));

      describe('canChangePassword', function () {
        it('Not Loaded', function () {
          expect(scope.canChangePassword()).toBeFalsy();
        });
        it('CLM User', function () {
          currentUserSuccess({
            username : 'foo',
            authenticated : true,
            clmUser : true
          });
          expect(scope.canChangePassword()).toBeTruthy();
        });
        it('Not CLM User', function () {
          currentUserSuccess({
            username : 'foo',
            authenticated : true,
            clmUser : false
          });
          expect(scope.canChangePassword()).toBeFalsy();
        });
      });

      describe('Dialog', function () {
        beforeEach(inject(function ($controller, $rootScope) {
          currentUserSuccess({
            username : 'foo',
            authenticated : true,
            clmUser : true
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
          $httpBackend.expectPUT(CLMLocations.getChangeMyPasswordUrl()).respond(200);
          dialogScope.save();
          expect(dialogScope.submitActive).toBeTruthy();
          $httpBackend.flush();

          expect(dialogScope.$close).toHaveBeenCalled();
        }));

        it('With Invalid Auth', inject(function ($httpBackend, CLMLocations, Messages) {
          $httpBackend.expectPUT(CLMLocations.getChangeMyPasswordUrl()).respond(400);

          dialogScope.save();
          expect(dialogScope.submitActive).toBeTruthy();

          $httpBackend.flush();

          expect(dialogScope.submitActive).toBeFalsy();
          expect(dialogScope.$close).not.toHaveBeenCalled();
          expect(dialogScope.error).toEqual(Messages.getHttpErrorMessage([undefined, 400]));
        }));
      });
    });
  });
});