describe('monitored.stage.editor.controller.spec.js', function() {

  beforeEach(module('Policy'));

  beforeEach(module('owner.manager.module', function($provide) {
    $provide.value('$cookies', {});
  }));

  beforeEach(module('ResourceModule'));

  var vm,
      scope,
      $timeout,
      $httpBackend,
      mockStageTypeStore = StoreUtils().createMockStore('StageTypeStore'),
      mockPolicyMonitoringStore = StoreUtils().createMockStore('PolicyMonitoringStore'),
      mockMonitoredStageService;

  beforeEach(inject(function($rootScope, $q, _$timeout_, _$httpBackend_, $controller) {
    scope = $rootScope.$new();
    $timeout = _$timeout_;
    $httpBackend = _$httpBackend_;
    mockMonitoredStageService = {
      createInheritOrNoMonitorOption: function() {
        return {stageName: 'Inherit from parent (Do not monitor)'};
      },
      getMonitoredStage: function() {
        return {stageName: 'Develop', stageTypeId: 'da_id'};
      }
    };
    vm = $controller('monitored.stage.editor.controller', {
      $scope: scope, 'monitored.stage.service': mockMonitoredStageService
    });

    vm.continuousMonitoringEditorMask = {wrap: SpecUtil.promiseWrapper($q)};
  }));

  it('Sets state correctly on load', function() {
    mockStageTypeStore.resolveGet([{stageName: 'release', stageTypeId: 'foo_id'}]);
    mockPolicyMonitoringStore.resolveGetApplicable(PolicyTileMockData.getPolicyMonitoring());
    $timeout.flush();

    expect(vm.monitoredStage).toBeDefined();
    expect(vm.monitoredStage.stageName).toBe('Develop');
    expect(vm.stages.length).toBe(2);
  });

  it('Saves selected stage', function() {
    mockStageTypeStore.resolveGet([]);
    mockPolicyMonitoringStore.resolveGetApplicable(PolicyTileMockData.getPolicyMonitoring());
    $timeout.flush();
    vm.monitoredStage = {stageTypeId: 'Deploy'};
    vm.save();
    mockPolicyMonitoringStore.resolveSave();
    $timeout.flush();
  });

  it('Removes stage if not selected', function() {
    mockStageTypeStore.resolveGet([]);
    mockPolicyMonitoringStore.resolveGetApplicable(PolicyTileMockData.getPolicyMonitoring());
    $timeout.flush();
    vm.monitoredStage = {stageTypeName: 'Do not monitor'};
    vm.save();
    mockPolicyMonitoringStore.resolveRemove();
    $timeout.flush();
  });

  describe('Page Changes', function() {
    beforeEach(function() {
      mockStageTypeStore.resolveGet([]);
      mockPolicyMonitoringStore.resolveGetApplicable(PolicyTileMockData.getPolicyMonitoring());
      $timeout.flush();
    });

    it('clean', function() {
      spyOn(vm, 'isDirty').andReturn(false);

      SpecUtil.expectStateChangeNotPrevented(scope);
      expect(vm.isDirty).toHaveBeenCalled();
    });

    it('dirty', function() {
      spyOn(vm, 'isDirty').andReturn(true);

      SpecUtil.expectStateChangePrevented(scope);
      expect(vm.isDirty).toHaveBeenCalled();
    });
  });
});
