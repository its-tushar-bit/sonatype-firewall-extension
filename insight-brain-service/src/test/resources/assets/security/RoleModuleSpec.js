describe('RoleModuleSpec.js', function() {
  describe('RoleEditorController', function() {
    var scope, roles = [
      {
        id: 'roleIdOne',
        name: 'Role Name One',
        sortOrder: 10,
        description: 'Role Description One.',
        global: true
      }, {
        id: 'roleIdTwo',
        name: 'Role Name Two',
        sortOrder: 100,
        description: 'Role Description Two.',
        global: true
      }], permissions = {
      'permissionCategories':
      [
        {
          'displayName': 'Category Name',
          'permissions': [
            {
              'id': 'PERMISSION_ID',
              'displayName': 'Permission Name',
              'description': 'Permission Description.',
              'allowed': false
            }
          ]
        }
      ]
    };

    beforeEach(module('RoleModule'));

    beforeEach(inject(function($controller, $rootScope, $httpBackend, CLMLocations) {
      scope = $rootScope.$new();
      $controller('RoleEditorController', {
        $scope: scope,
        $stateParams: {
          roleId: roles[0].id
        },
        isAuthorized: true
      });

      $httpBackend.expectGET(CLMLocations.getRoleListUrl()).respond(roles);
      $httpBackend.expectGET(CLMLocations.getRolePermissionUrl(roles[0].id)).respond(permissions);
      $httpBackend.flush();
    }));

    afterEach(inject(function($httpBackend) {
      $httpBackend.verifyNoOutstandingExpectation();
      $httpBackend.verifyNoOutstandingRequest();
      scope.$destroy();
    }));

    it('it loads roles and permissions into the scope', function() {
      expect(scope.role).toBeDefined();
      expect(scope.role.name).toBe(roles[0].name);
      expect(scope.permissionCategories).toBeDefined();
      expect(scope.permissionCategories.length).toBe(permissions.permissionCategories.length);
      expect(scope.permissionCategories[0].displayName).toBe(permissions.permissionCategories[0].displayName);
      expect(scope.permissionCategories[0].permissions.length).toBe(permissions.permissionCategories[0].permissions.length);
      expect(scope.permissionCategories[0].permissions[0].displayName).toBe(permissions.permissionCategories[0].permissions[0].displayName);
    });
  });
});
