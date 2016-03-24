describe('access.editor.controller.spec.js', function() {
  var vm,
      $q,
      $httpBackend,
      deleteServiceResourceDefer,
      $timeout,
      mockCLMAppLocations = {
        isApplication: function() {
          return true;
        },
        getEntityId: function() {
          return 'asdf';
        },
        getRoleMappingUrl: function() {
          return '/roleMappingUrl';
        }
      },
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
      mockAccessEditor = {
        $setPristine: angular.noop
      },
      mockSearchEditor = {
        $setPristine: angular.noop
      },
      CLMAppLocations;

  beforeEach(module('owner.manager.module', function($provide) {
    $provide.value('$cookies', {
      get: angular.noop
    });
  }));

  beforeEach(inject(function($rootScope, $controller, _$timeout_, _$q_, _$httpBackend_, _CLMAppLocations_) {
        scope = $rootScope.$new();
        $timeout = _$timeout_;
        $httpBackend = _$httpBackend_;
        $q = _$q_;
        deleteServiceResourceDefer = $q.defer();
        CLMAppLocations = _CLMAppLocations_;
      }
  ));

  afterEach(function() {
    $httpBackend.verifyNoOutstandingExpectation();
    $httpBackend.verifyNoOutstandingRequest();
  });

  it('Sets roles and users', function() {
    inject(function($controller) {
      vm = $controller('access.editor.controller', {$scope: scope, $stateParams: {roleId: '2cb71b3468d649789163ea2e212b5411'},
        isAuthorized: true, CLMAppLocations: mockCLMAppLocations});
    });

    $httpBackend.expectGET(mockCLMAppLocations.getRoleMappingUrl()).respond(AccessMockData.getMoreRoleMappings());
    $httpBackend.flush();

    expect(vm.role).toBeDefined();
    expect(vm.availableRoles.length).toBe(1);
    expect(vm.members.length).toBe(2);
  });

  it('Sets load error if url incorrect', function() {
    inject(function($controller) {
      vm = $controller('access.editor.controller', {$scope: scope, $stateParams: {roleId: 'foo'}, isAuthorized: true});
    });

    $httpBackend.expectGET(CLMAppLocations.getRoleMappingUrl()).respond(AccessMockData.getMoreRoleMappings());
    $httpBackend.flush();

    expect(vm.role).toBeUndefined();
    expect(vm.availableRoles).toBeUndefined();
    expect(vm.members.length).toBe(0);
    expect(vm.loadError).toBeDefined();
  });

  it('Calls remove on update with no picked users', function() {
    inject(function($controller) {
      vm = $controller('access.editor.controller', {$scope: scope, $stateParams: {roleId: '2cb71b3468d649789163ea2e212b5411'}, isAuthorized: true});
    });
    $httpBackend.expectGET(CLMAppLocations.getRoleMappingUrl()).respond(AccessMockData.getRoleMappings());
    $httpBackend.flush();
    vm.removeRole = jasmine.createSpy();
    vm.members.forEach(function(user) { user.picked = false; });
    vm.accessEditorMask = {wrap: SpecUtil.promiseWrapper($q)};

    vm.save();

    expect(vm.removeRole).toHaveBeenCalled();
  });

  it('Remove frees the role, broadcasts update and transfers to create new', function() {
    inject(function($controller) {
      vm = $controller('access.editor.controller', {$scope: scope, DeleteModalService: mockDeleteService, $stateParams: {roleId: '2cb71b3468d649789163ea2e212b5411'}, isAuthorized: true,
      $rootScope: mockRootScope, SameOwnerStateNavigationService: mockSameOwnerStateNavigationService});
    });
    $httpBackend.expectGET(CLMAppLocations.getRoleMappingUrl()).respond(AccessMockData.getRoleMappings());
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
      vm = $controller('access.editor.controller', {$scope: scope, isAuthorized: true,
        $rootScope: mockRootScope, SameOwnerStateNavigationService: mockSameOwnerStateNavigationService, CLMAppLocations: mockCLMAppLocations});
    });
    $httpBackend.expectGET(mockCLMAppLocations.getRoleMappingUrl()).respond(AccessMockData.getMoreRoleMappings());
    $httpBackend.flush();
    expect(vm.availableRoles.length).toBe(1);
    vm.accessEditor = mockAccessEditor;
    vm.accessEditorSearch = mockSearchEditor;
    vm.role = vm.availableRoles[0];
    vm.members = [{internalName: 'testUser', picked: true}];
    vm.accessEditorMask = {wrap: SpecUtil.promiseWrapper($q)};

    vm.save();
    $httpBackend.expectPUT(mockCLMAppLocations.getRoleMappingUrl()).respond(200);
    $httpBackend.flush();
    $timeout(function(){}, 1000); // mask delay = 0.8s
    $timeout.flush();

    expect(vm.availableRoles.length).toBe(0);
    expect(mockRootScope.$broadcast).toHaveBeenCalledWith('resource.data.modified');
    expect(vm.isNew).toBeFalsy();
    expect(mockSameOwnerStateNavigationService.goEdit).toHaveBeenCalledWith('edit-access', {roleId: 'abcdef'});
  });

  it('Search in progress flag properly set', function() {
    inject(function($controller) {
      vm = $controller('access.editor.controller', {
        $scope: scope, isAuthorized: true
      });
    });
    $httpBackend.expectGET(CLMAppLocations.getRoleMappingUrl()).respond(AccessMockData.getMoreRoleMappings());
    $httpBackend.flush();

    vm.query = 'arbitrary';
    vm.accessEditorSearchMask = {wrap: SpecUtil.promiseWrapper($q)};
    $httpBackend.expectGET(CLMAppLocations.getFindUsersUrl() +
        '?q=arbitrary').respond(AccessMockData.getMoreRoleMappings());

    expect(vm.searchInProgress).toBeFalsy();
    vm.search();
    expect(vm.searchInProgress).toBeTruthy();
    $httpBackend.flush();
    expect(vm.searchInProgress).toBeFalsy();
  });

  it('Creates correct tooltip message', function() {
    inject(function($controller) {
      vm = $controller('access.editor.controller', {
        $scope: scope
      });
    });
    $httpBackend.expectGET(CLMAppLocations.getRoleMappingUrl()).respond(AccessMockData.getMoreRoleMappings());
    $httpBackend.flush();

    expect(vm.getTooltip({realm: 'foo'})).toBe('foo');
    expect(vm.getTooltip({realm: 'foo', email: 'test@test.com'})).toBe('foo\ntest@test.com');
    // existing LDAP entry but connection is down so no realm/email
    expect(vm.getTooltip({displayName: 'test'})).toBe(null);
  });

  describe('Page Changes', function() {
    beforeEach(inject(function($controller) {
      vm = $controller('access.editor.controller', {
        $scope: scope
      });

      $httpBackend.expectGET(CLMAppLocations.getRoleMappingUrl()).respond(AccessMockData.getMoreRoleMappings());
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
