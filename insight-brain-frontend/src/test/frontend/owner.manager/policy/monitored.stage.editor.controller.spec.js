/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import ownerManagerModule from '../../../../main/frontend/owner.manager/owner.manager.module';

describe('monitored.stage.editor.controller.spec.js', function () {
  beforeEach(
    angular.mock.module(ownerManagerModule.name, function ($provide) {
      $provide.value('$cookies', {});
      SpecUtil.mockNgRedux($provide);
    })
  );

  var vm,
    scope,
    $timeout,
    $httpBackend,
    mockStageTypeStore = StoreUtils().createMockStore('StageTypeStore'),
    mockPolicyMonitoringStore = StoreUtils().createMockStore('PolicyMonitoringStore'),
    mockMonitoredStageService,
    CLMLocations;

  beforeEach(inject(function ($rootScope, $q, _$timeout_, _$httpBackend_, $controller, _CLMLocations_) {
    scope = $rootScope.$new();
    $timeout = _$timeout_;
    $httpBackend = _$httpBackend_;
    CLMLocations = _CLMLocations_;
    $httpBackend.expectGET(CLMLocations.getProductFeaturesUrl()).respond(['policy-monitoring']);
    mockMonitoredStageService = {
      createInheritOrNoMonitorOption: function () {
        return { stageName: 'Inherit from parent (Do not monitor)' };
      },
      getMonitoredStage: function () {
        return { stageName: 'Develop', stageTypeId: 'da_id' };
      },
    };
    vm = $controller('monitored.stage.editor.controller', {
      $scope: scope,
      'monitored.stage.service': mockMonitoredStageService,
    });

    vm.continuousMonitoringEditorMask = { wrap: SpecUtil.promiseWrapper($q) };
  }));

  it('Sets state correctly on load', function () {
    mockStageTypeStore.resolveGet([{ stageName: 'release', stageTypeId: 'foo_id' }]);
    mockPolicyMonitoringStore.resolveGetApplicable(PolicyTileMockData.getPolicyMonitoring());
    $timeout.flush();
    $httpBackend.flush();

    expect(vm.monitoredStage).toBeDefined();
    expect(vm.monitoredStage.stageName).toBe('Develop');
    expect(vm.stages.length).toBe(2);
    expect(vm.isMonitoringSupported).toBe(true);
  });

  it('Saves selected stage', function () {
    mockStageTypeStore.resolveGet([]);
    mockPolicyMonitoringStore.resolveGetApplicable(PolicyTileMockData.getPolicyMonitoring());
    $timeout.flush();
    $httpBackend.flush();
    vm.monitoredStage = { stageTypeId: 'Deploy' };
    vm.save();
    mockPolicyMonitoringStore.resolveSave();
    $timeout.flush();
  });

  it('Removes stage if not selected', function () {
    mockStageTypeStore.resolveGet([]);
    mockPolicyMonitoringStore.resolveGetApplicable(PolicyTileMockData.getPolicyMonitoring());
    $timeout.flush();
    $httpBackend.flush();
    vm.monitoredStage = { stageTypeName: 'Do not monitor' };
    vm.save();
    mockPolicyMonitoringStore.resolveRemove();
    $timeout.flush();
  });

  describe('Page Changes', function () {
    beforeEach(function () {
      mockStageTypeStore.resolveGet([]);
      mockPolicyMonitoringStore.resolveGetApplicable(PolicyTileMockData.getPolicyMonitoring());
      $timeout.flush();
      $httpBackend.flush();
    });

    it('clean', function () {
      spyOn(vm, 'isDirty').and.returnValue(false);

      SpecUtil.expectStateChangeNotPrevented(scope);
      expect(vm.isDirty).toHaveBeenCalled();
    });

    it('dirty', function () {
      spyOn(vm, 'isDirty').and.returnValue(true);

      SpecUtil.expectStateChangePrevented(scope);
      expect(vm.isDirty).toHaveBeenCalled();
    });
  });
});
