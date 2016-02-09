describe('policy.editor.actions.and.notifications.controller.spec.js', function() {

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
    vm = $controller('policy.editor.actions.and.notifications.controller', {}, {actions: [], monitorNotifyActions: []});
  }));

  it('Properly loads action info', inject(function() {
    vm.actions = {};
    vm.actions.proxy = [
      {actionTypeId: 'warn', target: null},
      {actionTypeId: 'notify', target: 'test@test.com'},
      {actionTypeId: 'notify', target: 'test2@test.com'},
      {actionTypeId: 'notify', target: '2cb71b3468d649789163ea2e212b5411', targetType: 'role'}
    ];
    vm.actions.build = [{actionTypeId: 'warn', target: null}];
    vm.monitorNotifyActions = [
      {actionTypeId: 'notify', target: 'test@test.com'},
      {actionTypeId: 'notify', target: '2cb71b3468d649789163ea2e212b5411', targetType: 'role'}
    ];
    resolveLoadData();

    expect(vm.actionStages.length).toBe(7);
    expect(vm.roles.length).toBe(1);
    expect(vm.actions.proxy.length).toBe(4);
    expect(vm.monitorNotifyActions.length).toBe(2);
    expect(vm.getEmailList(vm.actionStages[0].stageTypeId).length).toBe(2);
    expect(vm.getRolesList(vm.actionStages[0].stageTypeId).length).toBe(1);
    expect(vm.getEmailList(vm.actionStages[6].stageTypeId).length).toBe(1);
    expect(vm.getRolesList(vm.actionStages[6].stageTypeId).length).toBe(1);
    expect(vm.hasNotifications('proxy')).toBe(true);
    expect(vm.hasNotifications('build')).toBe(false);
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

  it('Removes notification', function() {
    vm.actions = {proxy: [{target: 'this', actionTypeId: 'notify'}, {target: 'other', actionTypeId: 'notify'}]};
    vm.roles = [{roleId: 'foo', roleName: 'Le Foo'}, {roleId: 'bar', roleName: 'Le Bar'}];
    vm.availableRoles['proxy'] = [];
    vm.removeNotification('proxy', 'this');
    expect(vm.availableRoles['proxy'].length).toBe(2);
    expect(vm.actions.proxy.length).toBe(1);
    expect(vm.actions.proxy).toContain({target: 'other', actionTypeId: 'notify'});
  });

  it('Finds the correct name for role notification', function() {
    vm.roles = [{roleId: 'foo', roleName: 'Le Foo'}, {roleId: 'bar', roleName: 'Le Bar'}];
    var name = vm.getNotificationTargetName({targetType: 'role', target: 'bar'});
    expect(name).toBe('Le Bar');
  });

  it('Add stage notifications', function() {
    vm.roles = [{roleId: 'foo', roleName: 'Le Foo'}, {roleId: 'bar', roleName: 'Le Bar'}];
    vm.updateAvailableRoles('proxy');
    vm.actions = {proxy: [{target: 'this', actionTypeId: 'notify'}, {target: 'other', actionTypeId: 'notify'}]};
    vm.notificationValueMap = {};
    vm.notificationTypeMap = {};

    expect(vm.notificationTypes.length).toBe(2)
    vm.notificationTypeMap['proxy'] = vm.notificationTypes[0];

    vm.notificationValueMap['proxy'] = 'test@sonatype.com';
    vm.addNotification('proxy');
    expect(vm.actions.proxy.length).toBe(3);
    expect(vm.actions.proxy).toContain({target: 'test@sonatype.com', actionTypeId: 'notify'});
    expect(vm.notificationValueMap['proxy']).toBeUndefined();

    vm.notificationTypeMap['proxy'] = vm.notificationTypes[1];
    vm.notificationValueMap['proxy'] = {roleId: "foo", roleName: 'Le Foo'};
    expect(vm.availableRoles['proxy'].length).toBe(2);
    vm.addNotification('proxy');
    expect(vm.availableRoles['proxy'].length).toBe(1);
    expect(vm.actions.proxy.length).toBe(4);
    expect(vm.actions.proxy).toContain({target: 'foo', actionTypeId: 'notify', targetType: 'role'});
    expect(vm.notificationValueMap['proxy']).toBeUndefined();
  });

  it('Add monitoring notifications', function() {
    vm.roles = [{roleId: 'foo', roleName: 'Le Foo'}, {roleId: 'bar', roleName: 'Le Bar'}];
    vm.monitorNotifyActions = [{target: 'this', actionTypeId: 'notify'}, {target: 'other', actionTypeId: 'notify'}];
    vm.notificationValueMap = {};
    vm.notificationTypeMap = {};

    expect(vm.notificationTypes.length).toBe(2)

    vm.notificationValueMap['monitoring'] = 'test@sonatype.com';
    vm.addNotification('monitoring');
    expect(vm.monitorNotifyActions.length).toBe(3);
    expect(vm.monitorNotifyActions).toContain({target: 'test@sonatype.com', actionTypeId: 'notify'});
    expect(vm.notificationValueMap['monitoring']).toBeUndefined();

    vm.notificationTypeMap['monitoring'] = vm.notificationTypes[1];
    vm.notificationValueMap['monitoring'] = {roleId: "foo", roleName: 'Le Foo'};
    vm.addNotification('monitoring');
    expect(vm.monitorNotifyActions.length).toBe(4);
    expect(vm.monitorNotifyActions).toContain({target: 'foo', actionTypeId: 'notify', targetType: 'role'});
    expect(vm.notificationValueMap['monitoring']).toBeUndefined();
  });

  function resolveLoadData() {
    expect(stageTypeStoreDefer.promise.then).toHaveBeenCalled();
    stageTypeStoreDefer.resolve(MockData.getActionStageData());
    $httpBackend.expectGET(CLMAppLocations.getRoleMappingUrl()).respond(AccessMockData.getRoleMappings());

    $httpBackend.flush();
    $timeout.flush();
  }
});
