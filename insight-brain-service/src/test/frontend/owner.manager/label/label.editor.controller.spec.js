describe('label.editor.controller.spec.js', function() {

  beforeEach(module('owner.manager.module', function($provide) {
    $provide.value('$cookies', {});
  }));

  beforeEach(module('ResourceModule'));

  var vm,
      scope,
      $timeout,
      $httpBackend,
      deleteServiceResourceDefer,
      mockDeleteService,
      SameOwnerStateNavigationService = {goEdit: function(to, params) {}},
      mockLabelStore = StoreUtils().createMockStore('LabelStore'),
      mockLabel = ResourceUtils().createMockResource();

  beforeEach(inject(function($rootScope, $q, _$timeout_, _$httpBackend_) {
      scope = $rootScope.$new();
      $timeout = _$timeout_;
      deleteServiceResourceDefer = $q.defer();
      mockDeleteService = {
        deleteResource: function() {
          return deleteServiceResourceDefer.promise;
        }
      }
      $httpBackend = _$httpBackend_;
  }));

  it('Creates new on load', function() {
    $httpBackend.whenGET("/rest/label/global/global/applicable").respond({labelsByOwner: []});
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
    $httpBackend.whenGET("/rest/label/global/global/applicable").respond({labelsByOwner: [{labels: ['label_1']}]});
    inject(function($controller) {
      vm = $controller('label.editor.controller', {$scope: scope});
    })
    mockLabelStore.resolveGet();
    $timeout.flush();
    $httpBackend.flush();
    expect(vm.siblings).toContain('label_1');
    expect(vm.siblings.length).toBe(1);
  });

  it('Updates siblings list after creating new', function() {
    $httpBackend.whenGET("/rest/label/global/global/applicable").respond({labelsByOwner: []});
    inject(function($controller) {
      vm = $controller('label.editor.controller', {$scope: scope});
    })
    mockLabelStore.resolveGet();
    $timeout.flush();
    $httpBackend.flush();
    mockLabel.$new = true;
    vm.dirtyLabel = mockLabel;
    vm.labelEditor = {$setPristine: function(){}};
    vm.save();
    mockLabel.resolveSave();
    $timeout.flush();
    $timeout(function(){}, 1000); // mask delay = 0.8s
    $timeout.flush();
    expect(vm.siblings).toContain(mockLabel);
    expect(vm.siblings.length).toBe(1);
  });

  it('Finds match with URL parameter', function() {
    $httpBackend.whenGET("/rest/label/global/global/applicable").respond({labelsByOwner: []});
    inject(function($controller) {
      vm = $controller('label.editor.controller', {$stateParams: {labelId:'456'}, $scope: scope});
    });
    mockLabel.id = '456';
    mockLabelStore.resolveGet([mockLabel, {id:'123'}]);
    $timeout.flush();
    $httpBackend.flush();
    expect(vm.dirtyLabel.$clone).toHaveBeenCalled();
    expect(vm.dirtyLabel.id).toBe('456');
  });

  it('Errors if no match found', function() {
    $httpBackend.whenGET("/rest/label/global/global/applicable").respond({labelsByOwner: []});
    inject(function($controller) {
      vm = $controller('label.editor.controller', {$stateParams: {labelId:'789'}, $scope: scope});
    });
    mockLabelStore.resolveGet([{id:'123'}, {id:'456'}]);
    $timeout.flush();
    $httpBackend.flush();
    expect(vm.dirtyLabel).toBeUndefined();
    expect(vm.error).toBeDefined();
  });
  
  it('Unsuccessful save sets error message', function() {
    $httpBackend.whenGET("/rest/label/global/global/applicable").respond({labelsByOwner: []});
    inject(function($controller) {
      vm = $controller('label.editor.controller', {$scope: scope});
    });
    mockLabelStore.resolveGet([]);
    $timeout.flush();
    $httpBackend.flush();
    vm.dirtyLabel = mockLabel;
    vm.save();
    mockLabel.rejectSave('dammit');
    $timeout.flush();
    expect(vm.error).toBe('dammit');
  });

  it('After delete goes to create new label', function() {
    // given
    $httpBackend.whenGET("/rest/label/global/global/applicable").respond({labelsByOwner: []});
    spyOn(SameOwnerStateNavigationService, 'goEdit');
    inject(function($controller) {
      vm = $controller('label.editor.controller',
        {$scope: scope, SameOwnerStateNavigationService: SameOwnerStateNavigationService, $stateParams: {labelId: '1'}, DeleteModalService: mockDeleteService});
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
  });

})
