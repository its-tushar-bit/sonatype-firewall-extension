/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import ownerManagerModule from '../../../../main/frontend/owner.manager/owner.manager.module';
import legacyConfigurationModule from '../../../../main/frontend/LegacyConfigurationModule';

describe('category.editor.controller.spec.js', function() {

  beforeEach(angular.mock.module(ownerManagerModule.name, legacyConfigurationModule.name, function($provide) {
    $provide.value('$cookies', {
      get: angular.noop
    });

    $provide.value('$stateParams', {});
  }));

  var vm,
      $q,
      scope,
      $timeout,
      deleteServiceResourceDefer,
      mockDeleteService,
      SameOwnerStateNavigationService = {goEdit: function() {}},
      mockCategoryStore = StoreUtils().createMockStore('TagStore'),
      mockApplicationStore = StoreUtils().createMockStore('ApplicationStore'),
      mockPolicyStore = StoreUtils().createMockStore('PolicyHierarchyStore'),
      mockPolicyTagStore = StoreUtils().createMockStore('PolicyTagStore'),
      mockCategory = ResourceUtils().createMockResource(),
      mockOwner = {store: {create: function() {return 'stub';}}, tags: [mockCategory]};

  beforeEach(inject(function($rootScope, _$q_, _$timeout_) {
    scope = $rootScope.$new();
    $timeout = _$timeout_;
    $q = _$q_;
    deleteServiceResourceDefer = $q.defer();
    mockDeleteService = {
      deleteCustom: function() {
        return deleteServiceResourceDefer.promise;
      }
    };
  }));

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
    resolveLoad([{store: {create: function() {}}, tags: [{id: 'a'}, {id: 'b'}]}, {tags: [{id: 'c'}]}]);
    $timeout.flush();
    expect(vm.siblings.length).toBe(3);
    expect(vm.siblings[0].id).toBe('a');
    expect(vm.siblings[1].id).toBe('b');
    expect(vm.siblings[2].id).toBe('c');
  });

  it('Captures names of associated apps', inject(function($controller, $stateParams) {
    $stateParams.categoryId = 'testCatId';

    vm = $controller('category.editor.controller', {
      $scope: scope,
      DeleteModalService: mockDeleteService
    });

    var spy = spyOn(mockDeleteService, 'deleteCustom').and.returnValue(deleteServiceResourceDefer.promise);
    mockCategory.id = 'testCatId';
    mockCategoryStore.resolveGet([{tags: [{id: 'testCatId_neg'}, mockCategory]}]);
    mockCategoryStore.resolveGetById(mockCategory);
    mockCategoryStore.resolveGetApplied({
      data: {
        applicationTagsByOwner: [
          {
            applicationTags: [
              {applicationId: 'testApp', tagId: 'testCatId'}, {applicationId: 'testApp', tagId: 'testCatId_neg'}
            ]
          }
        ]
      }
    });
    mockApplicationStore.resolveGet([{id: 'testApp_neg', name: 'Test App Neg'}, {id: 'testApp', name: 'Test App'}]);
    mockPolicyStore.resolveGet([]);
    mockPolicyTagStore.resolveGetApplied({data: []});

    $timeout.flush();

    vm.deleteCategory();

    expect(spy.calls.mostRecent().args[1]).toMatch('in use by the following applications: Test App.');
  }));

  it('Updates siblings list after creating new', function() {
    inject(function($controller) {
      vm = $controller('category.editor.controller', {$scope: scope});
    });
    resolveLoad([{store: {create: function() {}}, tags: []}]);
    $timeout.flush();
    mockCategory.$new = true;
    vm.dirtyCategory = mockCategory;
    vm.categoryEditor = {$setPristine: function() {}};
    vm.categoryEditorMask = {wrap: SpecUtil.promiseWrapper($q)};

    vm.save();
    mockCategory.resolveSave();
    $timeout.flush();
    $timeout(function() {}, 1000); // mask delay = 0.8s
    $timeout.flush();
    expect(vm.siblings).toContain(mockCategory);
    expect(vm.siblings.length).toBe(1);
  });

  it('Finds match with URL parameter', inject(function($controller, $stateParams) {
    $stateParams.categoryId = '456';

    vm = $controller('category.editor.controller', {
      $scope: scope
    });
    mockCategory.id = '456';
    resolveLoad([mockOwner]);
    $timeout.flush();
    expect(vm.dirtyCategory.$clone).toHaveBeenCalled();
    expect(vm.dirtyCategory.id).toBe('456');
  }));

  it('Errors if no match found', inject(function($controller, $stateParams) {
    $stateParams.categoryId = '709';

    vm = $controller('category.editor.controller', {
      $scope: scope
    });

    resolveLoad([{tags: [{id: '123'}, {id: '456'}]}]);
    $timeout.flush();
    expect(vm.dirtyCategory).toBeUndefined();
    expect(vm.loadError).toBeDefined();
  }));

  it('Unsuccessful save sets error message', inject(function($controller) {
    vm = $controller('category.editor.controller', {$scope: scope});

    resolveLoad([mockOwner]);
    $timeout.flush();
    vm.dirtyCategory = mockCategory;
    vm.categoryEditorMask = {wrap: SpecUtil.promiseWrapper($q)};
    vm.save();
    mockCategory.rejectSave('dammit');
    $timeout.flush();
    expect(vm.submitError).toBe('dammit');
  }));

  it('After delete goes to create new category', inject(function($controller, $stateParams) {
    $stateParams.categoryId = '1';
    vm = $controller('category.editor.controller', {
      $scope: scope,
      SameOwnerStateNavigationService: SameOwnerStateNavigationService,
      DeleteModalService: mockDeleteService
    });
    spyOn(SameOwnerStateNavigationService, 'goEdit');
    mockCategory.id = '1';
    resolveLoad([mockOwner], mockCategory);
    $timeout.flush();
    // when
    vm.deleteCategory();
    deleteServiceResourceDefer.resolve();
    $timeout.flush();
    // then
    expect(SameOwnerStateNavigationService.goEdit).toHaveBeenCalledWith('create-category');
    expect(mockCategory.$revert).toHaveBeenCalled();
  }));

  describe('Page Changes', function() {
    beforeEach(inject(function($controller) {
      vm = $controller('category.editor.controller', {
        $scope: scope
      });

      resolveLoad([mockOwner]);
      vm.dirtyCategory = mockCategory;
      vm.dirtyCategory.isDirty = angular.noop;
    }));

    it('clean', function() {
      spyOn(vm.dirtyCategory, 'isDirty').and.returnValue(false);

      SpecUtil.expectStateChangeNotPrevented(scope);
      expect(vm.dirtyCategory.isDirty).toHaveBeenCalled();
    });

    it('dirty', function() {
      spyOn(vm.dirtyCategory, 'isDirty').and.returnValue(true);

      SpecUtil.expectStateChangePrevented(scope);
      expect(vm.dirtyCategory.isDirty).toHaveBeenCalled();
    });
  });

  function resolveLoad(categoryStorePayload) {
    inject(function($stateParams) {
      if ($stateParams.categoryId) {
        categoryStorePayload.some(function(owner) {
          owner.tags.some(function(tag) {
            if (tag.id === $stateParams.categoryId) {
              mockCategoryStore.resolveGetById(tag);
              return true;
            }
          });
        }) || mockCategoryStore.rejectGetById('some error');
      }
    });
    categoryStorePayload.forEach(function(owner) {
      owner.tags.forEach(function(cat) {
        cat.$clone = jasmine.createSpy().and.returnValue(cat);
      });
    });

    mockCategoryStore.resolveGet(categoryStorePayload);
    mockApplicationStore.resolveGet([{id: 'testAppId'}]);
    mockPolicyStore.resolveGet([]);
    mockPolicyTagStore.resolveGetApplied({
      data: []
    });
    mockCategoryStore.resolveGetApplied(
        {data: {applicationTagsByOwner: [{applicationTags: [{applicationId: 'testAppId'}]}]}});
  }

});
