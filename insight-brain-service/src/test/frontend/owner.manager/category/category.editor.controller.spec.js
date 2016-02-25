describe('category.editor.controller.spec.js', function() {

  beforeEach(module('owner.manager.module', function($provide) {
    $provide.value('$cookies', {
      get: angular.noop
    });
  }));

  beforeEach(module('ResourceModule'));

  var vm,
      $q,
      scope,
      $timeout,
      deleteServiceResourceDefer,
      mockDeleteService,
      SameOwnerStateNavigationService = {goEdit: function(to, params) {}},
      mockCategoryStore = StoreUtils().createMockStore('TagStore'),
      mockApplicationStore = StoreUtils().createMockStore('ApplicationStore'),
      mockCategory = ResourceUtils().createMockResource(),
      mockOwner = {store: {create: function(){return 'stub';}}, tags: [mockCategory]};

  beforeEach(inject(function($rootScope, _$q_, _$timeout_) {
        scope = $rootScope.$new();
        $timeout = _$timeout_;
        $q = _$q_;
        deleteServiceResourceDefer = $q.defer();
        mockDeleteService = {
          deleteCustom: function() {
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
    resolveLoad([mockOwner]);
    $timeout.flush();
    expect(mockOwner.store.create).toHaveBeenCalled();
  });

  it('Captures siblings', function() {
    inject(function($controller) {
      vm = $controller('category.editor.controller', {$scope: scope});
    });
    resolveLoad([{store: {create: function(){}}, tags: ['a', 'b']},{tags: ['c']}]);
    $timeout.flush();
    expect(vm.siblings).toContain('a');
    expect(vm.siblings).toContain('b');
    expect(vm.siblings).toContain('c');
    expect(vm.siblings.length).toBe(3);
  });

  it('Captures names of associated apps', function() {
    inject(function($controller) {
      vm = $controller('category.editor.controller',
          {$stateParams: {categoryId: 'testCatId'}, $scope: scope, DeleteModalService: mockDeleteService});
    });
    var spy = spyOn(mockDeleteService, 'deleteCustom').andReturn(deleteServiceResourceDefer.promise);
    mockCategoryStore.resolveGet([{tags: [{id: 'testCatId_neg'}, {id: 'testCatId', $clone: angular.noop}]}]);
    mockCategoryStore.resolveGetApplied(
        {data: {applicationTagsByOwner: [{applicationTags: [{applicationId: 'testApp'}]}]}});
    mockApplicationStore.resolveGet([{id: 'testApp_neg', name: 'Test App Neg'}, {id: 'testApp', name: 'Test App'}]);

    $timeout.flush();

    vm.dirtyCategory = {$delete: angular.noop};

    vm.deleteCategory();

    expect(spy.mostRecentCall.args[1]).toMatch('in use by the following applications: Test App.');
  });

  it('Updates siblings list after creating new', function() {
    inject(function($controller) {
      vm = $controller('category.editor.controller', {$scope: scope});
    });
    resolveLoad([{store: {create: function(){}}, tags:[]}]);
    $timeout.flush();
    mockCategory.$new = true;
    vm.dirtyCategory = mockCategory;
    vm.categoryEditor = {$setPristine: function(){}};
    vm.categoryEditorMask = {wrap: SpecUtil.promiseWrapper($q)};

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
    resolveLoad([mockOwner]);
    $timeout.flush();
    expect(vm.dirtyCategory.$clone).toHaveBeenCalled();
    expect(vm.dirtyCategory.id).toBe('456');
  });

  it('Errors if no match found', function() {
    inject(function($controller) {
      vm = $controller('category.editor.controller', {$stateParams: {categoryId:'789'}, $scope: scope});
    });
    resolveLoad([{tags: [{id:'123'}, {id:'456'}]}]);
    $timeout.flush();
    expect(vm.dirtyCategory).toBeUndefined();
    expect(vm.loadError).toBeDefined();
  });

  it('Unsuccessful save sets error message', function() {
    inject(function($controller) {
      vm = $controller('category.editor.controller', {$scope: scope});
    });
    resolveLoad([mockOwner]);
    $timeout.flush();
    vm.dirtyCategory = mockCategory;
    vm.categoryEditorMask = {wrap: SpecUtil.promiseWrapper($q)};
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
    resolveLoad([mockOwner]);
    $timeout.flush();
    // when
    vm.deleteCategory();
    deleteServiceResourceDefer.resolve();
    $timeout.flush();
    // then
    expect(SameOwnerStateNavigationService.goEdit).toHaveBeenCalledWith('create-category');
  });

  function resolveLoad(categoryStorePayload) {
    categoryStorePayload[0].tags.forEach(function(cat) {
      cat.$clone = jasmine.createSpy().andReturn(cat);
    });
    mockCategoryStore.resolveGet(categoryStorePayload);
    mockApplicationStore.resolveGet([{id: 'testAppId'}]);
    mockCategoryStore.resolveGetApplied(
        {data: {applicationTagsByOwner: [{applicationTags: [{applicationId: 'testAppId'}]}]}});
  }

});
