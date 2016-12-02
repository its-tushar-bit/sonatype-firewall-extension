describe('policy.editor.actions.controller.spec.js', function() {

  beforeEach(module('owner.manager.module', function($provide) {
    $provide.value('$cookies', {
      get: angular.noop
    });
  }));

  beforeEach(module('ResourceModule'));

  var vm,
      $timeout,
      stageTypeStoreDefer;

  beforeEach(inject(function($q, _$timeout_, $controller, StageTypeStore) {
    $timeout = _$timeout_;

    stageTypeStoreDefer = $q.defer();
    spyOn(stageTypeStoreDefer.promise, 'then').and.callThrough();
    spyOn(StageTypeStore, 'getActionStages').and.returnValue(stageTypeStoreDefer.promise);
    vm = $controller('policy.editor.actions.controller', {}, {actions: []});
  }));

  it('Properly loads action info', inject(function() {
    expect(stageTypeStoreDefer.promise.then).toHaveBeenCalled();
    stageTypeStoreDefer.resolve(MockData.getActionStageData());
    $timeout.flush();

    expect(vm.actionStages.length).toBe(6);
  }));

});
