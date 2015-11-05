describe('category.editor.controller.spec.js', function() {

  beforeEach(module('owner.manager.module', function($provide) {
    $provide.value('$cookies', {});
  }));

  beforeEach(module('ResourceModule'));

  var vm,
      scope,
      $timeout,
      deleteServiceResourceDefer,
      mockDeleteService,
      SameOwnerStateNavigationService = {goEdit: function(to, params) {}},
      mockCategoryStore = StoreUtils().createMockStore('TagStore'),
      mockCategory = ResourceUtils().createMockResource(),
      mockOwner = {store: {create: function(){return 'stub';}}, tags: [mockCategory]};

  beforeEach(inject(function($rootScope, $q, _$timeout_) {
        scope = $rootScope.$new();
        $timeout = _$timeout_;
        deleteServiceResourceDefer = $q.defer();
        mockDeleteService = {
          deleteResource: function() {
            return deleteServiceResourceDefer.promise;
          }
        }
      }
  ));

  it('Creates new on load', function() {
    spyOn(mockOwner.store, 'create');
    inject(function($controller) {
      vm = $controller('category.editor.controller', {$scope: scope});
    });
    mockCategoryStore.resolveGet([mockOwner]);
    $timeout.flush();
    expect(mockOwner.store.create).toHaveBeenCalled();
  });

  it('Captures siblings', function() {
    inject(function($controller) {
      vm = $controller('category.editor.controller', {$scope: scope});
    });
    mockCategoryStore.resolveGet([{store: {create: function(){}}, tags: ['a', 'b']},{tags: ['c']}]);
    $timeout.flush();
    expect(vm.siblings).toContain('a');
    expect(vm.siblings).toContain('b');
    expect(vm.siblings).toContain('c');
    expect(vm.siblings.length).toBe(3);
  });

  it('Updates siblings list after creating new', function() {
    inject(function($controller) {
      vm = $controller('category.editor.controller', {$scope: scope});
    })
    mockCategoryStore.resolveGet([{store: {create: function(){}}, tags:[]}]);
    $timeout.flush();
    mockCategory.$new = true;
    vm.dirtyCategory = mockCategory;
    vm.categoryEditor = {$setPristine: function(){}};
    vm.save();
    mockCategory.resolveSave();
    $timeout.flush();
    $timeout(function(){}, 1000); // mask delay = 0.8s
    $timeout.flush();
    expect(vm.siblings).toContain(mockCategory);
    expect(vm.siblings.length).toBe(1);
  });

  it('Finds match with URL parameter', function() {
    inject(function($controller) {
      vm = $controller('category.editor.controller', {$stateParams: {categoryId:'456'}, $scope: scope});
    });
    mockCategory.id = '456';
    mockCategoryStore.resolveGet([mockOwner]);
    $timeout.flush();
    expect(vm.dirtyCategory.$clone).toHaveBeenCalled();
    expect(vm.dirtyCategory.id).toBe('456');
  });

  it('Errors if no match found', function() {
    inject(function($controller) {
      vm = $controller('category.editor.controller', {$stateParams: {categoryId:'789'}, $scope: scope});
    });
    mockCategoryStore.resolveGet([{tags: [{id:'123'}, {id:'456'}]}]);
    $timeout.flush();
    expect(vm.dirtyCategory).toBeUndefined();
    expect(vm.loadError).toBeDefined();
  });

  it('Unsuccessful save sets error message', function() {
    inject(function($controller) {
      vm = $controller('category.editor.controller', {$scope: scope});
    });
    mockCategoryStore.resolveGet([mockOwner]);
    $timeout.flush();
    vm.dirtyCategory = mockCategory;
    vm.save();
    mockCategory.rejectSave('dammit');
    $timeout.flush();
    expect(vm.submitError).toBe('dammit');
  });

  it('After delete goes to create new category', function() {
    // given
    spyOn(SameOwnerStateNavigationService, 'goEdit');
    inject(function($controller) {
      vm = $controller('category.editor.controller',
          {$scope: scope, SameOwnerStateNavigationService: SameOwnerStateNavigationService, $stateParams: {categoryId: '1'}, DeleteModalService: mockDeleteService});
    });
    mockCategory.id = '1';
    mockCategoryStore.resolveGet([mockOwner]);
    $timeout.flush();
    // when
    vm.deleteCategory();
    deleteServiceResourceDefer.resolve();
    $timeout.flush();
    // then
    expect(SameOwnerStateNavigationService.goEdit).toHaveBeenCalledWith('create-category');
  });

})
