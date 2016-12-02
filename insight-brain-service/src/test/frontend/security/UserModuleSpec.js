describe('UserModuleSpec.js', function() {
  var listScope = null;
  var dialogScope = null;
  var userScope = null;

  function setupControllers() {
    inject(function($controller, $rootScope) {
      userScope = $rootScope.$new();
      $controller('UserController', {
        $scope: userScope,
        isAuthorized : true
      });
      listScope = $rootScope.$new();
      $controller('UserListController', {
        $scope: listScope,
        isAuthorized : true
      });
      userScope.context = listScope.context;
    });
  }

  beforeEach(module('UserModule', 'HttpInterceptors', function($provide) {
    $provide.value('$modalInstance', {
      close: function() {}
    });
    $provide.factory('CurrentUser', ['$q', function ($q) {
      var deferred = $q.defer();
      deferred.resolve({
        username : 'user'
      });
      return deferred.promise;
    }]);
    $provide.value('$modal', {
      open: function(config) {
        dialogScope = listScope.$new();
        dialogScope.$close = function() {
        };
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
    SpecUtil.mockPermissionService($provide);
  }));

  beforeEach(inject(function($rootScope) {
    listScope = $rootScope.$new();
  }));

  afterEach(inject(function($httpBackend) {
    $httpBackend.verifyNoOutstandingExpectation();
    $httpBackend.verifyNoOutstandingRequest();
    listScope.$destroy();
  }));

  var data = [{
    "id": "ADMIN",
    "username": "admin",
    "usernameLowercase": "admin",
    "password": "#~FAKE~PASSWORD~#",
    "firstName": "Admin",
    "lastName": "BuiltIn",
    "email": "myemail@mail.com"
  }, {
    "id": "16399c07447d48b9bd00b19522bb5a66",
    "username": "admin2",
    "usernameLowercase": "admin2",
    "password": "#~FAKE~PASSWORD~#",
    "firstName": "clm",
    "lastName": "clm22",
    "email": "test@test.net"
  }];

  it('get list', inject(function($httpBackend, CLMLocations) {
    $httpBackend.expectGET(SpecUtil.toRegExp(CLMLocations.getUserUrl())).respond(data);
    setupControllers();
    $httpBackend.flush();
    expect(listScope.context).not.toBeUndefined();
    expect(listScope.context.userEditMap).toEqual({});
    expect(listScope.context.users.length).toEqual(2);
  }));

  it('get list failure', inject(function($httpBackend, CLMLocations) {
    $httpBackend.expectGET(SpecUtil.toRegExp(CLMLocations.getUserUrl())).respond(500);
    setupControllers();
    $httpBackend.flush();
    expect(listScope.context).not.toBeUndefined();
    expect(listScope.context.userEditMap).toEqual({});
    expect(listScope.context.users.length).toEqual(0);
    expect(listScope.error.status).toEqual(500);

    // expect reload to work
    $httpBackend.expectGET(SpecUtil.toRegExp(CLMLocations.getUserUrl())).respond(data);
    listScope.doLoad();
    $httpBackend.flush();

    expect(listScope.error).toBeFalsy();
    expect(listScope.context.users.length).toEqual(2);
  }));
  
  it('reset password', inject(function($httpBackend, CLMLocations) {
    $httpBackend.expectGET(SpecUtil.toRegExp(CLMLocations.getUserUrl())).respond(data);
    setupControllers();
    $httpBackend.flush();

    listScope.resetPasswordClick({
      id: 'test-id'
    });
    
    expect(dialogScope.state).toEqual('ready');
    
    $httpBackend.expectPUT(SpecUtil.toRegExp(CLMLocations.getUserUrl() + '/test-id/reset')).respond({
      newPassword: '1234567890ab'
    });
    
    dialogScope.resetClick();
    expect(dialogScope.state).toEqual('pending');
    $httpBackend.flush();
    
    expect(dialogScope.newPassword).toEqual('1234567890ab');
    expect(dialogScope.state).toEqual('complete');
    // server failure
    $httpBackend.expectPUT(SpecUtil.toRegExp(CLMLocations.getUserUrl() + '/test-id/reset')).respond(500, 'Error resetting');
    dialogScope.resetClick();
    $httpBackend.flush();
    
    expect(dialogScope.error).toEqual('Error resetting');
    expect(dialogScope.state).toEqual('failed');
  }));

  it('add user', inject(function($rootScope, $httpBackend, CLMLocations) {
    $httpBackend.expectGET(SpecUtil.toRegExp(CLMLocations.getUserUrl())).respond(data);
    setupControllers();
    $httpBackend.flush();

    // first lets validate the dirty field protection
    expect($rootScope.$broadcast('pageChangeStarted').defaultPrevented).toEqual(false);

    userScope.newUser = true;

    userScope.user = {
      isDirty: function() {
        return true;
      }
    };

    // now should get prevented
    expect($rootScope.$broadcast('pageChangeStarted').defaultPrevented).toEqual(true);

    userScope.newUserClick();

    // now lets actually add a user
    userScope.user.firstName = 'first';
    userScope.user.lastName = 'last';
    userScope.user.email = 'email@email.fake';
    userScope.user.password = 'password';
    userScope.user.username = 'username';

    var newUserData = {
      id: null,
      username: 'username',
      password: 'password',
      firstName: 'first',
      lastName: 'last',
      email: 'email@email.fake'
    };

    var userCount = listScope.context.users.length;

    $httpBackend.expectPOST(SpecUtil.toRegExp(CLMLocations.getUserUrl()), newUserData).respond(newUserData);
    userScope.saveClick(userScope.user);
    expect(userScope.saving).toBeTruthy();
    $httpBackend.flush();
    expect(userScope.saving).toBeFalsy();
    expect(listScope.context.users.length).toEqual(userCount + 1);
  }));

  it('update user', inject(function($httpBackend, CLMLocations) {
    $httpBackend.expectGET(SpecUtil.toRegExp(CLMLocations.getUserUrl())).respond(data);
    setupControllers();
    $httpBackend.flush();
    
    userScope.user = listScope.context.users[0];
    listScope.editClick(userScope.user);

    // now lets actually change the user
    userScope.user.firstName = 'firstUp';
    userScope.user.lastName = 'lastUp';
    userScope.user.email = 'email@emailUp.fake';
    userScope.user.password = 'passwordUp';
    userScope.user.username = 'usernameUp';

    var editUserData = {
      id: "ADMIN",
      username: 'usernameUp',
      usernameLowercase: 'admin',
      password: 'passwordUp',
      firstName: 'firstUp',
      lastName: 'lastUp',
      email: 'email@emailUp.fake'
    };

    var userCount = listScope.context.users.length;

    $httpBackend.expectPUT(SpecUtil.toRegExp(CLMLocations.getUserUrl()), editUserData).respond(editUserData);
    userScope.saveClick(userScope.user);
    expect(userScope.saving).toBeTruthy();
    $httpBackend.flush();
    expect(userScope.saving).toBeFalsy();
    expect(listScope.context.users.length).toEqual(userCount);
  }));

  describe('cancelClick', function() {
    beforeEach(inject(function($httpBackend, CLMLocations, Dialog) {
      $httpBackend.expectGET(SpecUtil.toRegExp(CLMLocations.getUserUrl())).respond(data);
      setupControllers();
      $httpBackend.flush();

      userScope.newUserClick();
      userScope.context.userEditMap = {};
      spyOn(Dialog, 'open');
    }));

    it('when dirty', inject(function(Dialog) {
      userScope.user.firstName = 'foo';
      userScope.cancelClick(userScope.user);
      expect(Dialog.open).toHaveBeenCalled();
      expect(userScope.user).toBeTruthy();
      Dialog.open.calls.first().args[0].buttons[0].click();
      expect(userScope.user).toBeFalsy();
    }));

    it('when clean', inject(function(Dialog) {
      userScope.cancelClick(userScope.user);
      expect(userScope.user).toBeFalsy();
      expect(Dialog.open).not.toHaveBeenCalled();
    }));
  });
});
