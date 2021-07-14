/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import userModule from '../../../main/frontend/security/UserModule';
import { httpInterceptors } from '../../../main/frontend/util/HttpInterceptors';

describe('UserModuleSpec.js', function () {
  var listScope = null;

  function setupControllers() {
    inject(function ($controller, $rootScope) {
      listScope = $rootScope.$new();
      $controller('UserListController', {
        $scope: listScope,
        isAuthorized: true,
      });
    });
  }

  beforeEach(
    angular.mock.module(userModule.name, httpInterceptors.name, function ($provide) {
      $provide.factory('CurrentUser', [
        '$q',
        function ($q) {
          var deferred = $q.defer();
          deferred.resolve({
            username: 'user',
          });

          return {
            waitForLogin: () => deferred.promise,
          };
        },
      ]);

      SpecUtil.mockPermissionService($provide);
      SpecUtil.mockNgRedux($provide);
    })
  );

  beforeEach(inject(function ($rootScope) {
    listScope = $rootScope.$new();
  }));

  afterEach(inject(function ($httpBackend) {
    $httpBackend.verifyNoOutstandingExpectation();
    $httpBackend.verifyNoOutstandingRequest();
    listScope.$destroy();
  }));

  var data = [
    {
      id: 'ADMIN',
      username: 'admin',
      usernameLowercase: 'admin',
      password: '#~FAKE~PASSWORD~#',
      firstName: 'Admin',
      lastName: 'BuiltIn',
      email: 'myemail@mail.com',
    },
    {
      id: '16399c07447d48b9bd00b19522bb5a66',
      username: 'admin2',
      usernameLowercase: 'admin2',
      password: '#~FAKE~PASSWORD~#',
      firstName: 'clm',
      lastName: 'clm22',
      email: 'test@test.net',
    },
  ];

  it('get list', inject(function ($httpBackend, CLMLocations) {
    $httpBackend.expectGET(SpecUtil.toRegExp(CLMLocations.getUserUrl())).respond(data);
    setupControllers();
    $httpBackend.flush();
    expect(listScope.context).not.toBeUndefined();
    expect(listScope.context.users.length).toEqual(2);
  }));

  it('get list failure', inject(function ($httpBackend, CLMLocations) {
    $httpBackend.expectGET(SpecUtil.toRegExp(CLMLocations.getUserUrl())).respond(500);
    setupControllers();
    $httpBackend.flush();
    expect(listScope.context).not.toBeUndefined();
    expect(listScope.context.users.length).toEqual(0);
    expect(listScope.error.status).toEqual(500);

    // expect reload to work
    $httpBackend.expectGET(SpecUtil.toRegExp(CLMLocations.getUserUrl())).respond(data);
    listScope.doLoad();
    $httpBackend.flush();

    expect(listScope.error).toBeFalsy();
    expect(listScope.context.users.length).toEqual(2);
  }));
});
