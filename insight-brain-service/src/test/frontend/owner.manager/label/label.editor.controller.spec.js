describe('label.editor.controller.spec.js', function() {

  beforeEach(module('owner.manager.module', function($provide) {
    $provide.value('$cookies', {});
  }));

  beforeEach(module('ResourceModule'));

  var vm,
      scope,
      $timeout,
      deleteServiceResourceDefer,
      mockDeleteService,
      $state = {go: function(state, params) {}},
      mockLabelStore = StoreUtils().createMockStore('LabelStore'),
      mockLabel = ResourceUtils().createMockResource();

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
    inject(function($controller) {
      vm = $controller('label.editor.controller', {$scope: scope});
    });
    mockLabelStore.resolveGet([]);
    $timeout.flush();
    expect(vm.dirtyLabel).toBeDefined();
    expect(vm.dirtyLabel.$new).toBe(true);
  });

  it('Captures siblings', function() {
    inject(function($controller) {
      vm = $controller('label.editor.controller', {$scope: scope});
    })
    mockLabelStore.resolveGet(['label_1']);
    $timeout.flush();
    expect(vm.siblings).toContain('label_1');
    expect(vm.siblings.length).toBe(1);
  });

  it('Finds match with URL parameter', function() {
    inject(function($controller) {
      vm = $controller('label.editor.controller', {$stateParams: {labelId:'456'}, $scope: scope});
    });
    mockLabel.id = '456';
    mockLabelStore.resolveGet([mockLabel, {id:'123'}]);
    $timeout.flush();
    expect(vm.dirtyLabel.$clone).toHaveBeenCalled();
    expect(vm.dirtyLabel.id).toBe('456');
  });

  it('Errors if no match found', function() {
    inject(function($controller) {
      vm = $controller('label.editor.controller', {$stateParams: {labelId:'789'}, $scope: scope});
    });
    mockLabelStore.resolveGet([{id:'123'}, {id:'456'}]);
    $timeout.flush();
    expect(vm.dirtyLabel).toBeUndefined();
    expect(vm.error).toBeDefined();
  });
  
  it('Unsuccessful save sets error message', function() {
    inject(function($controller) {
      vm = $controller('label.editor.controller', {$scope: scope});
    });
    mockLabelStore.resolveGet([]);
    $timeout.flush();
    vm.dirtyLabel = mockLabel;
    vm.save();
    mockLabel.rejectSave('dammit');
    $timeout.flush();
    expect(vm.error).toBe('dammit');
  });

  it('After delete goes to create new label', function() {
    // given
    spyOn($state, 'go');
    inject(function($controller) {
      vm = $controller('label.editor.controller',
        {$scope: scope, $state: $state, $stateParams: {labelId: '1'}, DeleteModalService: mockDeleteService});
    });
    mockLabel.id = '1';
    mockLabelStore.resolveGet([mockLabel]);
    $timeout.flush();
    // when
    vm.deleteLabel();
    deleteServiceResourceDefer.resolve();
    $timeout.flush();
    // then
    expect($state.go).toHaveBeenCalledWith('^.create-label');
  });

})
