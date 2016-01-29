describe('policy.editor.actions.controller.spec.js', function() {

  beforeEach(module('owner.manager.module', function($provide) {
    $provide.value('$cookies', {
      get: angular.noop
    });
  }));

  beforeEach(module('ResourceModule'));

  var vm,
      $httpBackend,
      $timeout,
      stageTypeStoreDefer;
  ;

  beforeEach(inject(function($q, _$httpBackend_, _$timeout_, $controller, _CLMAppLocations_, StageTypeStore) {
    $httpBackend = _$httpBackend_;
    $timeout = _$timeout_;
    CLMAppLocations = _CLMAppLocations_;

    stageTypeStoreDefer = $q.defer();
    spyOn(stageTypeStoreDefer.promise, 'then').andCallThrough();
    spyOn(StageTypeStore, 'getActionStages').andReturn(stageTypeStoreDefer.promise);
    vm = $controller('policy.editor.actions.controller', {}, {actions: [], monitorNotifyActions: []});
  }));

  it('Properly loads action info', inject(function() {
    vm.actions = {};
    vm.actions.proxy = [
      {actionTypeId: 'warn', target: null},
      {actionTypeId: 'notify', target: 'test@test.com'},
      {actionTypeId: 'notify', target: 'test2@test.com'},
      {actionTypeId: 'notify', target: '2cb71b3468d649789163ea2e212b5411', targetType: 'role'}
    ];
    vm.monitorNotifyActions = [
      {actionTypeId: 'notify', target: 'test@test.com'},
      {actionTypeId: 'notify', target: '2cb71b3468d649789163ea2e212b5411', targetType: 'role'}
    ];
    resolveLoadData();

    expect(vm.actionStages.length).toBe(6);
    expect(vm.roles.length).toBe(1);
    expect(vm.actions.proxy.length).toBe(4);
    expect(vm.monitorNotifyActions.length).toBe(2);
    expect(vm.getEmailList(vm.actionStages[0]).length).toBe(2);
    expect(vm.getRolesList(vm.actionStages[0]).length).toBe(1);
    expect(vm.getMonitoringEmailList().length).toBe(1);
    expect(vm.getMonitoringRolesList().length).toBe(1);
  }));

  it('Conditionally add or remove actions', inject(function() {
    vm.actions = {};
    vm.actions.proxy = [
      {actionTypeId: 'warn', target: null},
      {actionTypeId: 'notify', target: 'test@test.com'},
      {actionTypeId: 'notify', target: 'test2@test.com'},
      {actionTypeId: 'notify', target: '2cb71b3468d649789163ea2e212b5411', targetType: 'role'}
    ];
    vm.monitorNotifyActions = [
      {actionTypeId: 'notify', target: 'test@test.com'},
      {actionTypeId: 'notify', target: '2cb71b3468d649789163ea2e212b5411', targetType: 'role'}
    ];
    resolveLoadData();

    expect(vm.actions.proxy.length).toBe(4);
    expect(vm.monitorNotifyActions.length).toBe(2);
    vm.conditionallyAddOrRemoveAction(vm.actionStages[0].stageTypeId, null);
    expect(vm.actions.proxy.length).toBe(3);
    expect(vm.actions.proxy).not.toContain({actionTypeId: 'warn', target: null});
    // go back to original state
    vm.conditionallyAddOrRemoveAction(vm.actionStages[0].stageTypeId, 'warn');
    expect(vm.actions.proxy.length).toBe(4);
    expect(vm.actions.proxy).toContain({actionTypeId: 'warn', target: null});
  }));

  function resolveLoadData() {
    expect(stageTypeStoreDefer.promise.then).toHaveBeenCalled();
    stageTypeStoreDefer.resolve(MockData.getActionStageData());
    $httpBackend.expectGET(CLMAppLocations.getRoleMappingUrl()).respond(AccessMockData.getRoleMappings());

    $httpBackend.flush();
    $timeout.flush();
  }
});
