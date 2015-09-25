describe('label.editor.controller.spec.js', function() {

  beforeEach(module('owner.manager.module', function($provide) {
    $provide.value('$cookies', {});
  }));

  beforeEach(module('ResourceModule'));

  var vm, 
      $timeout,
      mockLabelStore = StoreUtils().createMockStore('LabelStore'),
      mockLabel = ResourceUtils().createMockResource();

  beforeEach(inject(function($controller, _$timeout_) {
      $timeout = _$timeout_;
      vm = $controller('label.editor.controller');
    }
  ));

  it('Creates new on load', function() {
    mockLabelStore.resolveGet([]);
    $timeout.flush();
    expect(vm.dirtyLabel).toBeDefined();
    expect(vm.dirtyLabel.$new).toBe(true);
  })

  it('Captures siblings', function() {
    mockLabelStore.resolveGet(['label_1']);
    $timeout.flush();
    expect(vm.siblings).toContain('label_1');
    expect(vm.siblings.length).toBe(1);
  })

  it('Finds match with URL parameter', function() {
    inject(function($controller) {
      vm = $controller('label.editor.controller', {$stateParams: {labelId:'456'}});
    })
    mockLabel.id = '456';
    mockLabelStore.resolveGet([mockLabel, {id:'123'}]);
    $timeout.flush();
    expect(vm.dirtyLabel.$clone).toHaveBeenCalled();
    expect(vm.dirtyLabel.id).toBe('456');
  })

  it('Errors if no match found', function() {
    inject(function($controller) {
      vm = $controller('label.editor.controller', {$stateParams: {labelId:'789'}});
    })
    mockLabelStore.resolveGet([{id:'123'}, {id:'456'}]);
    $timeout.flush();
    expect(vm.dirtyLabel).toBeUndefined();
    expect(vm.error).toBeDefined();
  })

  it('Resets the label', function() {
    inject(function($controller) {
      vm = $controller('label.editor.controller', {$stateParams: {labelId:'123'}});
    });
    mockLabel.id = '123';
    mockLabelStore.resolveGet([mockLabel]);
    $timeout.flush();
    vm.reset();
    expect(vm.dirtyLabel.$revert).toHaveBeenCalled();
  })

  it('Unsuccessful save sets error message', function() {
    mockLabelStore.resolveGet([]);
    $timeout.flush();
    vm.dirtyLabel = mockLabel;
    vm.save();
    mockLabel.rejectSave('dammit');
    $timeout.flush();
    expect(vm.error).toBe('dammit');
  })

})
