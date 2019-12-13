/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import roleModule from '../../../main/frontend/security/RoleModule';

describe('RoleModuleSpec.js', function() {
  var scope,
      roleSummaries = [{
        id: 'roleIdOne',
        name: 'Role Name One',
        description: 'Role Description One.',
        builtIn: false
      }, {
        id: 'roleIdTwo',
        name: 'Role Name Two',
        description: 'Role Description Two.',
        builtIn: true
      }],
      roleOne = {
        id: 'roleIdOne',
        name: 'Role Name One',
        description: 'Role Description One.',
        builtIn: true,
        'permissionCategories': [{
          'displayName': 'Category Name',
          'permissions': [{
            'id': 'PERMISSION_ID',
            'displayName': 'Permission Name',
            'description': 'Permission Description.',
            'allowed': false
          }]
        }]
      };

  describe('RoleListController', function() {

    beforeEach(angular.mock.module(roleModule.name));

    afterEach(inject(function($httpBackend) {
      $httpBackend.verifyNoOutstandingExpectation();
      $httpBackend.verifyNoOutstandingRequest();
      scope.$destroy();
    }));

    function createController(rolePermissions) {
      inject(function ($controller, $rootScope) {
        scope = $rootScope.$new();
        $controller('RoleListController', {
          $scope: scope,
          rolePermissions: rolePermissions
        });
      });
    }

    it('initializes scope with role list if authorized (read+write)', inject(function($httpBackend, CLMLocations) {
      createController({
        editRoles: true,
        viewRoles: true
      });
      $httpBackend.expectGET(CLMLocations.getRoleListUrl()).respond(roleSummaries);
      $httpBackend.flush();

      expect(scope.isAuthorized).toBeTruthy();
      expect(scope.readOnly).toBeFalsy();
      expect(scope.roles).not.toBeUndefined();
      expect(scope.roles.length).toEqual(roleSummaries.length);
      expect(scope.error).toBeNull();
    }));

    it('initializes scope with role list if authorized (read-only)', inject(function($httpBackend, CLMLocations) {
      createController({
        editRoles: false,
        viewRoles: true
      });
      $httpBackend.expectGET(CLMLocations.getRoleListUrl()).respond(roleSummaries);
      $httpBackend.flush();

      expect(scope.isAuthorized).toBeTruthy();
      expect(scope.readOnly).toBeTruthy();
      expect(scope.roles).not.toBeUndefined();
      expect(scope.roles.length).toEqual(roleSummaries.length);
      expect(scope.error).toBeNull();
    }));

    it('initializes scope without role list if unauthorized', function() {
      createController({
        editRoles: false,
        viewRoles: false
      });

      expect(scope.isAuthorized).toBeFalsy();
      expect(scope.roles).toBeUndefined();
      expect(scope.error).toBeUndefined();
    });
  });

  describe('RoleEditorController', function() {

    beforeEach(angular.mock.module(roleModule.name));

    afterEach(inject(function($httpBackend) {
      $httpBackend.verifyNoOutstandingExpectation();
      $httpBackend.verifyNoOutstandingRequest();
      scope.$destroy();
    }));

    describe('Existing Role read only', function() {
      function createController(rolePermissions) {
        inject(function ($controller, $rootScope) {
          scope = $rootScope.$new();
          $controller('RoleEditorController', {
            $scope: scope,
            $stateParams: {
              roleId: 'id'
            },
            rolePermissions: rolePermissions
          });
        });
      }

      it('built-in role treated as read-only', inject(function($httpBackend, CLMLocations) {
        createController({
          editRoles: true,
          viewRoles: true
        });
        $httpBackend.expectGET(CLMLocations.getRoleByIdUrl('id')).respond({
          id: 'id',
          name: 'name',
          description: 'description'
        });
        $httpBackend.flush();
        expect(scope.readOnly).toBeFalsy();
      }));

      it('non built-in role treated as read-only without perms', inject(function($httpBackend, CLMLocations) {
        createController({
          editRoles: false,
          viewRoles: true
        });
        $httpBackend.expectGET(CLMLocations.getRoleByIdUrl('id')).respond({
          id: 'id',
          name: 'name',
          description: 'description',
          builtIn: true
        });
        $httpBackend.flush();
        expect(scope.readOnly).toBeTruthy();
      }));
    });

    describe('Existing Role', function () {
      beforeEach(inject(function($controller, $rootScope, $httpBackend, CLMLocations) {
        scope = $rootScope.$new();
        $controller('RoleEditorController', {
          $scope: scope,
          $stateParams: {
            roleId: roleOne.id
          },
          rolePermissions: {
            editRoles: true,
            viewRoles: true
          }
        });

        $httpBackend.expectGET(CLMLocations.getRoleByIdUrl(roleOne.id)).respond(roleOne);
        $httpBackend.flush();
      }));

      it('loads roles & permissions', function() {
        expect(scope.role).toBeDefined();
        expect(scope.role.name).toBe(roleOne.name);
        expect(scope.role.permissionCategories).toBeDefined();
        expect(scope.role.permissionCategories.length).toBe(roleOne.permissionCategories.length);
        expect(scope.role.permissionCategories[0].displayName).toBe(roleOne.permissionCategories[0].displayName);
        expect(scope.role.permissionCategories[0].permissions.length)
            .toBe(roleOne.permissionCategories[0].permissions.length);
        expect(scope.role.permissionCategories[0].permissions[0].displayName)
            .toBe(roleOne.permissionCategories[0].permissions[0].displayName);
      });

      it('dirty editor triggers preventing the pageChangeStart event', inject(function() {
        var e = scope.$broadcast('pageChangeStarted');
        expect(e.defaultPrevented).toBeFalsy();

        scope.dirtyRole.name = 'foo';
        e = scope.$broadcast('pageChangeStarted');
        expect(e.defaultPrevented).toBeTruthy();
      }));

      it('save', inject(function ($httpBackend, CLMLocations, $state) {
        spyOn($state, 'go');
        scope.$apply(function () {
          scope.role.name = 'foo';
          scope.role.description = 'bar';
        });

        $httpBackend.expectPUT(CLMLocations.getRoleListUrl()).respond(roleOne);
        scope.save();
        expect(scope.submitActive).toBeTruthy();
        $httpBackend.expectGET(CLMLocations.getRoleListUrl()).respond(roleSummaries);
        $httpBackend.flush();
        expect($state.go).toHaveBeenCalledWith('roles');
      }));

      describe('DeleteController', function () {
        var deleteScope,
            dialogPromise;

        beforeEach(inject(function($controller, Dialog) {
          deleteScope = scope.$new();
          $controller('DeleteRoleController', {
            $scope: deleteScope,
            $stateParams: {
              roleId: roleOne.id
            }
          });

          spyOn(Dialog, 'open').and.returnValue({
            result: {
              then: dialogPromise = jasmine.createSpy('promise')
            }
          });
        }));

        it('delete', inject(function ($httpBackend, CLMLocations, $state, Dialog) {
          spyOn($state, 'go');

          deleteScope.deleteRole();
          expect(Dialog.open).toHaveBeenCalled();
          dialogPromise.calls.mostRecent().args[0]();

          // In real-world usage this GET would have already occured in another controller
          $httpBackend.expectGET(CLMLocations.getRoleListUrl()).respond(roleSummaries);
          $httpBackend.expectDELETE(CLMLocations.getRoleByIdUrl(roleOne.id)).respond(204);
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
          rolePermissions: {
            editRoles: true,
            viewRoles: true
          }
        });

        $httpBackend.expectGET(CLMLocations.getRoleForNewUrl()).respond({
          id: null,
          name: null,
          description: null,
          builtIn: false,
          permissionCategories: []
        });
        $httpBackend.flush();
      }));

      it('loads roles & permissions', function () {
        expect(scope.role).toBeDefined();
      });

      it('save', inject(function ($httpBackend, CLMLocations, $state) {
        spyOn($state, 'go');
        scope.$apply(function () {
          scope.role.name = 'foo';
          scope.role.description = 'bar';
        });

        $httpBackend.expectPOST(CLMLocations.getRoleListUrl()).respond(roleOne);
        $httpBackend.expectGET(CLMLocations.getRoleListUrl()).respond(roleSummaries);
        scope.save();
        expect(scope.submitActive).toBeTruthy();
        $httpBackend.flush();
        expect($state.go).toHaveBeenCalledWith('roles');
      }));
    });
  });
});
