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

    afterEach(inject(function($httpBackend) {
      $httpBackend.verifyNoOutstandingExpectation();
      $httpBackend.verifyNoOutstandingRequest();
      scope.$destroy();
    }));

    describe('Existing Role', function () {
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

      it('loads roles & permissions', function() {
        expect(scope.role).toBeDefined();
        expect(scope.role.name).toBe(roles[0].name);
        expect(scope.permissionCategories).toBeDefined();
        expect(scope.permissionCategories.length).toBe(permissions.permissionCategories.length);
        expect(scope.permissionCategories[0].displayName).toBe(permissions.permissionCategories[0].displayName);
        expect(scope.permissionCategories[0].permissions.length).toBe(permissions.permissionCategories[0].permissions.length);
        expect(scope.permissionCategories[0].permissions[0].displayName).toBe(permissions.permissionCategories[0].permissions[0].displayName);
      });

      it('save', inject(function ($httpBackend, CLMLocations, $state) {
        spyOn($state, 'go');
        scope.$apply(function () {
          scope.role.name = 'foo';
          scope.role.description = 'bar';
        });

        $httpBackend.expectPUT(CLMLocations.getRoleListUrl()).respond(roles[0]);
        scope.save();
        expect(scope.submitActive).toBeTruthy();
        $httpBackend.flush();
        expect($state.go).toHaveBeenCalledWith('roles');
      }));

      describe('DeleteController', function () {
        var deleteScope,
            dialogPromise;

        beforeEach(inject(function($controller, Dialog) {
          deleteScope = scope.$new();
          $controller('DeleteRoleController', {
            $scope : deleteScope
          });

          spyOn(Dialog, 'open').andReturn({
            result : {
              then : dialogPromise = jasmine.createSpy('promise')
            }
          });
        }));

        it('delete', inject(function ($httpBackend, CLMLocations, $state, Dialog) {
          spyOn($state, 'go');

          deleteScope.deleteRole();
          expect(Dialog.open).toHaveBeenCalled();
          dialogPromise.mostRecentCall.args[0]();

          $httpBackend.expectDELETE(CLMLocations.getRoleListUrl() + '/' + roles[0].id).respond(204);
          $httpBackend.flush();
          expect($state.go).toHaveBeenCalledWith('roles');
        }));
      });
    });

    describe('New Role', function () {
      beforeEach(inject(function($controller, $rootScope, $httpBackend, CLMLocations) {
        scope = $rootScope.$new();
        $controller('RoleEditorController', {
          $scope: scope,
          $stateParams: {
            roleId: '_new_'
          },
          isAuthorized: true
        });

        $httpBackend.expectGET(CLMLocations.getRoleListUrl()).respond(roles);
        $httpBackend.expectGET(CLMLocations.getRolePermissionUrl()).respond(permissions);
        $httpBackend.flush();
      }));

      it('loads roles & permissions', function () {
        expect(scope.role).toBeDefined();
        expect(scope.role.name).toEqual('');
        expect(scope.permissionCategories.length).toBe(permissions.permissionCategories.length);
        expect(scope.permissionCategories[0].displayName).toBe(permissions.permissionCategories[0].displayName);
        expect(scope.permissionCategories[0].permissions.length).toBe(permissions.permissionCategories[0].permissions.length);
        expect(scope.permissionCategories[0].permissions[0].displayName).toBe(permissions.permissionCategories[0].permissions[0].displayName);
      });

      it('save', inject(function ($httpBackend, CLMLocations, $state) {
        spyOn($state, 'go');
        scope.$apply(function () {
          scope.role.name = 'foo';
          scope.role.description = 'bar';
        });

        $httpBackend.expectPOST(CLMLocations.getRoleListUrl()).respond(roles[0]);
        scope.save();
        expect(scope.submitActive).toBeTruthy();
        $httpBackend.flush();
        expect($state.go).toHaveBeenCalledWith('roles');
      }));
    });
  });
});
