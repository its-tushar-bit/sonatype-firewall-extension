describe('policy.editor.controller.spec.js', function() {

  beforeEach(module('owner.manager.module', function($provide) {
    $provide.value('$cookies', {});
  }));

  beforeEach(module('ResourceModule'));

  var vm,
      scope,
      $timeout,
      deleteServiceResourceDefer,
      mockDeleteService,
      SameOwnerStateNavigationService = {
        goEdit: angular.noop
      },
      mockPolicyStore = StoreUtils().createMockStore('PolicyHierarchyStore'),
      mockPolicyStoreData = StoreUtils().createMockHierarchyStoreData(PolicyResourceMockData.getApplicablePolicies(),
          'policiesByOwner'),
      mockPolicy = ResourceUtils().createMockResource();

  beforeEach(inject(function($rootScope, $q, _$timeout_) {
    scope = $rootScope.$new();
    $timeout = _$timeout_;

    deleteServiceResourceDefer = $q.defer();

    mockDeleteService = {
      deleteResource: function() {
        return deleteServiceResourceDefer.promise;
      }
    };
  }));

  it('Creates new on load', inject(function($controller) {
    vm = $controller('policy.editor.controller', {$scope: scope});
    mockPolicyStore.resolveGet(mockPolicyStoreData);

    $timeout.flush();

    expect(mockPolicyStoreData[0].store.create).toHaveBeenCalled();
  }));

  it('Captures siblings', inject(function($controller) {
    vm = $controller('policy.editor.controller', {$scope: scope});

    mockPolicyStore.resolveGet(mockPolicyStoreData);
    $timeout.flush();

    expect(vm.siblings.length).toBe(1);
    expect(vm.siblings).toContain(mockPolicyStoreData[0].policies[0]);
  }));

  it('Updates siblings list after creating new', inject(function($controller) {
    vm = $controller('policy.editor.controller', {$scope: scope});

    mockPolicyStore.resolveGet(mockPolicyStoreData);
    $timeout.flush();

    mockPolicy.$new = true;
    mockPolicy.isDirty = function() {
      return true;
    };

    vm.dirtyPolicy = mockPolicy;
    vm.policyEditor = {
      $valid: true,
      $setPristine: angular.noop
    };

    vm.save();
    mockPolicy.resolveSave();
    $timeout.flush();

    // mask delay
    $timeout.flush();

    expect(vm.siblings.length).toBe(2);
    expect(mockPolicyStoreData[0].store.create).toHaveBeenCalled();
    expect(vm.siblings).toContain(mockPolicy);
  }));

  it('Finds match with URL parameter', inject(function($controller) {
    vm = $controller('policy.editor.controller',
        {$scope: scope, $stateParams: {policyId: '456'}});
    mockPolicy.id = '456';

    mockPolicyStore.resolveGet([{policies: [mockPolicy, {id: '123'}], store: {create: angular.noop}}]);
    $timeout.flush();

    expect(vm.dirtyPolicy.$clone).toHaveBeenCalled();
    expect(vm.dirtyPolicy.id).toBe('456');
  }));

  it('Errors if no match found', inject(function($controller) {
    vm = $controller('policy.editor.controller',
        {$scope: scope, $stateParams: {policyId: '456'}});

    mockPolicyStore.resolveGet([{policies: [{id: '123'}, {id: '123'}], store: {create: angular.noop}}]);
    $timeout.flush();

    expect(vm.dirtyPolicy).toBeUndefined();
    expect(vm.loadError).toBe('Unable to locate Policy.');
  }));

  it('Unsuccessful save sets error message', inject(function($controller) {
    vm = $controller('policy.editor.controller', {$scope: scope});

    mockPolicyStore.resolveGet(mockPolicyStoreData);
    $timeout.flush();

    mockPolicy.isDirty = function() {
      return true;
    };

    vm.dirtyPolicy = mockPolicy;
    vm.policyEditor = {
      $valid: true
    };

    vm.save();
    mockPolicy.rejectSave('dagnabbit');

    $timeout.flush();
    expect(vm.submitError).toBe('dagnabbit');
  }));

  it('After delete goes to create new policy', inject(function($controller) {
    spyOn(SameOwnerStateNavigationService, 'goEdit');
    vm = $controller('policy.editor.controller', {
      $scope: scope,
      SameOwnerStateNavigationService: SameOwnerStateNavigationService,
      $stateParams: {policyId: '1'},
      DeleteModalService: mockDeleteService
    });

    mockPolicy.id = '1';
    mockPolicyStore.resolveGet([{policies: [mockPolicy, {id: '123'}], store: {create: angular.noop}}]);
    $timeout.flush();

    vm.deletePolicy();
    deleteServiceResourceDefer.resolve();
    $timeout.flush();

    expect(SameOwnerStateNavigationService.goEdit).toHaveBeenCalledWith('create-policy');
  }));
});
