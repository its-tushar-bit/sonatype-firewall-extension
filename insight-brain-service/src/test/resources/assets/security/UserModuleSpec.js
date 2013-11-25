describe('UserModuleSpec.js', function() {
  var listScope = null;
  var dialogScope = null;
  var userScope = null;

  function setupControllers() {
    inject(function($controller, $rootScope) {
      userScope = $rootScope.$new();
      $controller('UserController', {
        $scope: userScope
      });
      listScope = $rootScope.$new();
      $controller('UserListController', {
        $scope: listScope
      });
      userScope.context = listScope.context;
    });
  }

  beforeEach(module('UserModule', function($provide) {
    $provide.factory('hudson', ['$http', function($http) {
      return $http;
    }]);
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
    "password": "#~FAKE~CLM~PASSWORD~#",
    "firstName": "Admin",
    "lastName": "BuiltIn",
    "email": "myemail@mail.com"
  }, {
    "id": "16399c07447d48b9bd00b19522bb5a66",
    "username": "admin2",
    "usernameLowercase": "admin2",
    "password": "#~FAKE~CLM~PASSWORD~#",
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

  it('change password', inject(function($httpBackend, CLMLocations) {
    $httpBackend.expectGET(SpecUtil.toRegExp(CLMLocations.getUserUrl())).respond(data);
    setupControllers();
    $httpBackend.flush();

    listScope.changePasswordClick({
      id: 'test-id'
    });

    dialogScope.changePasswordForm = {
      $valid: false,
    };

    // invalid, so no requests should be made
    dialogScope.save();
    $httpBackend.verifyNoOutstandingExpectation();
    $httpBackend.verifyNoOutstandingRequest();

    // server failure
    dialogScope.changePasswordForm.$valid = true;
    dialogScope.currentPassword = 'old';
    dialogScope.newPassword = 'new';
    $httpBackend.expectPUT(CLMLocations.getUserUrl() + '/test-id' + '/password', {
      oldPassword: 'old',
      newPassword: 'new'
    }).respond(401, 'Error');

    dialogScope.save();
    $httpBackend.flush();
    expect(dialogScope.errorMsg).toEqual('Error');

    // all good
    $httpBackend.expectPUT(CLMLocations.getUserUrl() + '/test-id' + '/password', {
      oldPassword: 'old',
      newPassword: 'new'
    }).respond(204);
    dialogScope.save();
    $httpBackend.flush();
    expect(dialogScope.errorMsg).toBeFalsy();
  }));
  
  it('reset password', inject(function($httpBackend, CLMLocations) {
    $httpBackend.expectGET(SpecUtil.toRegExp(CLMLocations.getUserUrl())).respond(data);
    setupControllers();
    $httpBackend.flush();

    listScope.resetPasswordClick({
      id: 'test-id'
    });
    
    $httpBackend.expectPUT(CLMLocations.getUserUrl() + '/test-id/reset').respond({
      newPassword: '1234567890ab'
    });
    
    dialogScope.resetClick();
    $httpBackend.flush();
    
    expect(dialogScope.newPassword).toEqual('1234567890ab');

    // server failure
    $httpBackend.expectPUT(CLMLocations.getUserUrl() + '/test-id/reset').respond(500, 'Error resetting');
    dialogScope.resetClick();
    $httpBackend.flush();
    
    expect(dialogScope.error).toEqual('Error resetting');
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
});
