/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import ownerManagerModule from '../../../../main/frontend/owner.manager/owner.manager.module';

describe('label.editor.controller.spec.js', function() {

  beforeEach(angular.mock.module(ownerManagerModule.name, function($provide) {
    $provide.value('$cookies', {
      get: angular.noop
    });
  }));

  var vm,
      scope,
      $q,
      $timeout,
      $httpBackend,
      deleteServiceResourceDefer,
      mockDeleteService,
      SameOwnerStateNavigationService = {goEdit: function() {}},
      mockLabelStore = StoreUtils().createMockStore('LabelStore'),
      mockLabel = ResourceUtils().createMockResource();

  beforeEach(inject(function($rootScope, _$q_, _$timeout_, _$httpBackend_) {
    scope = $rootScope.$new();
    $timeout = _$timeout_;
    $q = _$q_;
    deleteServiceResourceDefer = $q.defer();
    mockDeleteService = {
      deleteResource: function() {
        return deleteServiceResourceDefer.promise;
      }
    };
    $httpBackend = _$httpBackend_;
  }));

  it('Creates new on load', function() {
    $httpBackend.whenGET('/api/v2/labels/global/global/applicable').respond({labelsByOwner: []});
    inject(function($controller) {
      vm = $controller('label.editor.controller', {$scope: scope});
    });
    mockLabelStore.resolveGet([]);
    $httpBackend.flush();
    $timeout.flush();
    expect(vm.dirtyLabel).toBeDefined();
    expect(vm.dirtyLabel.$new).toBe(true);
  });

  it('Captures siblings', function() {
    $httpBackend.whenGET('/api/v2/labels/global/global/applicable').respond({labelsByOwner: [{labels: ['label_1']}]});
    inject(function($controller) {
      vm = $controller('label.editor.controller', {$scope: scope});
    });
    mockLabelStore.resolveGet();
    $timeout.flush();
    $httpBackend.flush();
    expect(vm.siblings).toContain('label_1');
    expect(vm.siblings.length).toBe(1);
  });

  it('Updates siblings list after creating new', function() {
    $httpBackend.whenGET('/api/v2/labels/global/global/applicable').respond({labelsByOwner: []});
    inject(function($controller) {
      vm = $controller('label.editor.controller', {$scope: scope});
    });
    mockLabelStore.resolveGet();
    $timeout.flush();
    $httpBackend.flush();
    mockLabel.$new = true;
    vm.dirtyLabel = mockLabel;
    vm.labelEditor = {$setPristine: function() {}};
    vm.labelEditorMask = {wrap: SpecUtil.promiseWrapper($q)};

    vm.save();
    mockLabel.resolveSave();
    $timeout.flush();
    $timeout(function() {}, 1000); // mask delay = 0.8s
    $timeout.flush();
    expect(vm.siblings).toContain(mockLabel);
    expect(vm.siblings.length).toBe(1);
  });

  it('Finds match with URL parameter', function() {
    $httpBackend.whenGET('/api/v2/labels/global/global/applicable').respond({labelsByOwner: []});
    inject(function($controller) {
      vm = $controller('label.editor.controller', {$stateParams: {labelId: '456'}, $scope: scope});
    });
    mockLabel.id = '456';
    mockLabelStore.resolveGet([mockLabel, {id: ' 123'}]);
    $timeout.flush();
    $httpBackend.flush();
    expect(vm.dirtyLabel.$clone).toHaveBeenCalled();
    expect(vm.dirtyLabel.id).toBe('456');
  });

  it('Errors if no match found', function() {
    $httpBackend.whenGET('/api/v2/labels/global/global/applicable').respond({labelsByOwner: []});
    inject(function($controller) {
      vm = $controller('label.editor.controller', {$stateParams: {labelId: '789'}, $scope: scope});
    });
    mockLabelStore.resolveGet([{id: '123'}, {id: '456'}]);
    $timeout.flush();
    $httpBackend.flush();
    expect(vm.dirtyLabel).toBeUndefined();
    expect(vm.loadError).toBeDefined();
  });

  it('Unsuccessful save sets error message', function() {
    $httpBackend.whenGET('/api/v2/labels/global/global/applicable').respond({labelsByOwner: []});
    inject(function($controller) {
      vm = $controller('label.editor.controller', {$scope: scope});
    });
    mockLabelStore.resolveGet([]);
    $timeout.flush();
    $httpBackend.flush();
    vm.dirtyLabel = mockLabel;
    vm.labelEditorMask = {wrap: SpecUtil.promiseWrapper($q)};
    vm.save();
    mockLabel.rejectSave('dammit');
    $timeout.flush();
    expect(vm.submitError).toBe('dammit');
  });

  it('After delete goes to create new label', function() {
    // given
    $httpBackend.whenGET('/api/v2/labels/global/global/applicable').respond({labelsByOwner: []});
    spyOn(SameOwnerStateNavigationService, 'goEdit');
    inject(function($controller) {
      vm = $controller('label.editor.controller', {
        $scope: scope,
        SameOwnerStateNavigationService: SameOwnerStateNavigationService,
        $stateParams: {labelId: '1'},
        DeleteModalService: mockDeleteService
      });
    });
    mockLabel.id = '1';
    mockLabelStore.resolveGet([mockLabel]);
    $timeout.flush();
    $httpBackend.flush();
    // when
    vm.deleteLabel();
    deleteServiceResourceDefer.resolve();
    $timeout.flush();
    // then
    expect(SameOwnerStateNavigationService.goEdit).toHaveBeenCalledWith('create-label');
    expect(mockLabel.$revert).toHaveBeenCalled();
  });

  describe('Page Changes', function() {
    beforeEach(inject(function($controller) {
      $httpBackend.whenGET('/api/v2/labels/global/global/applicable').respond({labelsByOwner: []});
      vm = $controller('label.editor.controller', {
        $scope: scope
      });

      mockLabelStore.resolveGet([mockLabel]);
      $timeout.flush();
      $httpBackend.flush();

      vm.dirtyLabel = mockLabel;
      vm.dirtyLabel.isDirty = angular.noop;
    }));

    it('clean', function() {
      spyOn(vm.dirtyLabel, 'isDirty').and.returnValue(false);

      SpecUtil.expectStateChangeNotPrevented(scope);
      expect(vm.dirtyLabel.isDirty).toHaveBeenCalled();
    });

    it('dirty', function() {
      spyOn(vm.dirtyLabel, 'isDirty').and.returnValue(true);

      SpecUtil.expectStateChangePrevented(scope);
      expect(vm.dirtyLabel.isDirty).toHaveBeenCalled();
    });
  });
});
