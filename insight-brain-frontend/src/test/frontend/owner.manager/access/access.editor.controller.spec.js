/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import ownerManagerModule from '../../../../main/frontend/owner.manager/owner.manager.module';
import accessMockData from '../../stores/access/access.mock.data';

describe('access.editor.controller.spec.js', function() {
  var vm,
      $q,
      scope,
      $httpBackend,
      deleteServiceResourceDefer,
      $timeout,
      mockDeleteService = {
        deleteCustom: function() {
          return deleteServiceResourceDefer.promise;
        }
      },
      mockRootScope = {
        $broadcast: jasmine.createSpy()
      },
      mockSameOwnerStateNavigationService = {
        goEdit: jasmine.createSpy()
      },
      mockRolePicker = {
        $setPristine: angular.noop
      },
      CLMContextLocations;

  beforeEach(angular.mock.module(ownerManagerModule.name, function($provide) {
    $provide.value('$cookies', {
      get: angular.noop
    });
  }));

  beforeEach(inject(function($rootScope, $controller, _$timeout_, _$q_, _$httpBackend_, _CLMContextLocations_, $state,
                             ApplicationId) {
    scope = $rootScope.$new();
    $timeout = _$timeout_;
    $httpBackend = _$httpBackend_;
    $q = _$q_;
    deleteServiceResourceDefer = $q.defer();
    CLMContextLocations = _CLMContextLocations_;

    $state.current.name = 'application'; // used by CLMContextLocations
    spyOn(ApplicationId, 'encoded').and.returnValue('abc');
  }));

  afterEach(function() {
    $httpBackend.verifyNoOutstandingExpectation();
    $httpBackend.verifyNoOutstandingRequest();
  });

  it('Sets roles and originalMembers', function() {
    inject(function($controller) {
      vm = $controller('access.editor.controller', {
        $scope: scope,
        $stateParams: {roleId: '2cb71b3468d649789163ea2e212b5411'},
        isAuthorized: true});
    });

    $httpBackend.expectGET(CLMContextLocations.getRoleMappingUrl()).respond(accessMockData.getMoreRoleMappings());
    $httpBackend.flush();

    expect(vm.role).toBeDefined();
    expect(vm.availableRoles.length).toBe(1);
    expect(vm.originalMembers.length).toBe(2);
    expect(vm.groupSearchEnabled).toBe(true);
  });

  it('Sets load error if url incorrect', function() {
    inject(function($controller) {
      vm = $controller('access.editor.controller', {$scope: scope, $stateParams: {roleId: 'foo'}, isAuthorized: true});
    });

    $httpBackend.expectGET(CLMContextLocations.getRoleMappingUrl()).respond(accessMockData.getMoreRoleMappings());
    $httpBackend.flush();

    expect(vm.role).toBeUndefined();
    expect(vm.availableRoles).toBeUndefined();
    expect(vm.originalMembers.length).toBe(0);
    expect(vm.loadError).toBeDefined();
  });

  it('Calls remove on update with no picked users', function() {
    inject(function($controller) {
      vm = $controller('access.editor.controller', {
        $scope: scope,
        $stateParams: {roleId: '2cb71b3468d649789163ea2e212b5411'},
        isAuthorized: true
      });

      vm.getCurrentMembersToSave = function() { return []; };
      vm.isMembershipDirty = function() { return true; };
    });
    $httpBackend.expectGET(CLMContextLocations.getRoleMappingUrl()).respond(accessMockData.getRoleMappings());
    $httpBackend.flush();
    vm.removeRole = jasmine.createSpy();
    vm.accessEditorMask = {wrap: SpecUtil.promiseWrapper($q)};

    vm.save();

    expect(vm.removeRole).toHaveBeenCalled();
  });

  it('Remove frees the role, broadcasts update and transfers to create new', function() {
    inject(function($controller) {
      vm = $controller('access.editor.controller', {
        $scope: scope,
        DeleteModalService: mockDeleteService,
        $stateParams: {roleId: '2cb71b3468d649789163ea2e212b5411'},
        isAuthorized: true,
        $rootScope: mockRootScope,
        SameOwnerStateNavigationService: mockSameOwnerStateNavigationService
      });
    });
    $httpBackend.expectGET(CLMContextLocations.getRoleMappingUrl()).respond(accessMockData.getRoleMappings());
    $httpBackend.flush();
    expect(vm.availableRoles.length).toBe(0);

    vm.removeRole();
    deleteServiceResourceDefer.resolve();
    $timeout.flush();
    expect(vm.availableRoles.length).toBe(1);
    expect(mockRootScope.$broadcast).toHaveBeenCalledWith('resource.data.modified');
    expect(mockSameOwnerStateNavigationService.goEdit).toHaveBeenCalledWith('add-access');
    SpecUtil.expectStateChangeNotPrevented(scope);
  });

  it('Adding the last role removes it from available, broadcasts update and transfers to edit', function() {
    inject(function($controller) {
      vm = $controller('access.editor.controller', {
        $scope: scope,
        isAuthorized: true,
        $rootScope: mockRootScope,
        SameOwnerStateNavigationService: mockSameOwnerStateNavigationService
      });
    });
    $httpBackend.expectGET(CLMContextLocations.getRoleMappingUrl()).respond(accessMockData.getMoreRoleMappings());
    $httpBackend.flush();
    expect(vm.availableRoles.length).toBe(1);
    vm.rolePicker = mockRolePicker;
    vm.role = vm.availableRoles[0];
    vm.getCurrentMembers = function() { return [{internalName: 'testUser', picked: true}]; };
    vm.getCurrentMembersToSave = function() { return [{internalName: 'testUser'}]; };
    vm.accessEditorMask = {wrap: SpecUtil.promiseWrapper($q)};

    vm.save();
    $httpBackend.expectPUT(CLMContextLocations.getRoleMappingUrl(vm.role.roleId)).respond(200);
    $httpBackend.flush();
    $timeout(function() {}, 1000); // mask delay = 0.8s
    $timeout.flush();

    expect(vm.availableRoles.length).toBe(0);
    expect(mockRootScope.$broadcast).toHaveBeenCalledWith('resource.data.modified');
    expect(vm.isNew).toBeFalsy();
    expect(mockSameOwnerStateNavigationService.goEdit).toHaveBeenCalledWith('edit-access', {roleId: 'abcdef'});
  });

  describe('Page Changes', function() {
    beforeEach(inject(function($controller) {
      vm = $controller('access.editor.controller', {
        $scope: scope
      });

      vm.getCurrentMembers = function() { return []; };

      $httpBackend.expectGET(CLMContextLocations.getRoleMappingUrl()).respond(accessMockData.getMoreRoleMappings());
      $httpBackend.flush();
    }));

    it('clean', function() {
      SpecUtil.expectStateChangeNotPrevented(scope);
    });

    it('dirty', function() {
      vm.role = 'dirty';

      SpecUtil.expectStateChangePrevented(scope);
    });
  });
});
