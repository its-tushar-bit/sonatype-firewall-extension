/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import ownerManagerModule from 'MainRoot/owner.manager/owner.manager.module';
import { mapStateToThis } from 'MainRoot/owner.manager/policy/monitored.stage.editor.controller';

describe('monitored.stage.editor.controller', function () {
  beforeEach(
    angular.mock.module(ownerManagerModule.name, function ($provide) {
      $provide.value('$cookies', {});
      SpecUtil.mockNgRedux($provide);
    })
  );

  let vm, scope;

  beforeEach(inject(function ($rootScope, $controller) {
    scope = $rootScope.$new();
    vm = $controller('monitored.stage.editor.controller', { $scope: scope });

    vm.continuousMonitoringEditorMask = { wrap: jasmine.createSpy('wrap').and.callFake((promise) => promise) };
    vm.$onInit();
  }));

  describe('$onInit()', () => {
    it('subscribes to the redux store', () => {
      expect(vm.unsubscribe).toBeDefined();
    });

    it('calls loadApplicablePolicyMonitoring', () => {
      expect(vm.loadApplicablePolicyMonitoring).toHaveBeenCalledTimes(1);
    });
  });

  describe('$onDestroy()', () => {
    it('unsubscribes from redux store', () => {
      expect(vm.unsubscribe).not.toHaveBeenCalled();
      vm.$onDestroy();
      expect(vm.unsubscribe).toHaveBeenCalledTimes(1);
    });
  });

  describe('Page Changes', () => {
    it('navigates away if form is not dirty', () => {
      spyOn(vm, 'isDirty').and.returnValue(false);
      SpecUtil.expectStateChangeNotPrevented(scope);
      expect(vm.isDirty).toHaveBeenCalled();
    });

    it('does not navigate away if form is dirty', () => {
      spyOn(vm, 'isDirty').and.returnValue(true);
      SpecUtil.expectStateChangePrevented(scope);
      expect(vm.isDirty).toHaveBeenCalled();
    });
  });

  it('Saves selected stage', function () {
    vm.monitoredStage = { stageTypeId: 'Deploy' };
    vm.save();

    expect(vm.savePolicyMonitoring).toHaveBeenCalledTimes(1);
  });

  it('Removes stage if not selected', function () {
    vm.monitoredStage = { stageName: 'Do not monitor' };
    vm.save();

    expect(vm.removePolicyMonitoring).toHaveBeenCalledTimes(1);
  });

  describe('mapStateToThis', () => {
    it('sets policyMonitoringByOwner, stages, originalStage, monitoredStage, loading, loadError, submitError', () => {
      const state = {
        productFeatures: {
          'policy-monitoring': true,
        },
        orgsAndPolicies: {
          policyMonitoring: {
            loading: false,
            loadError: null,
            submitError: null,
            policyMonitoringByOwner: [],
            stages: [
              { stageName: 'Develop', stageTypeId: 1 },
              { stageName: 'Build', stageTypeId: 1 },
            ],
            monitoredStage: { stageName: 'Develop', stageTypeId: 1 },
            originalStage: { stageName: 'Build', stageTypeId: 2 },
          },
        },
      };

      const output = mapStateToThis(state);

      expect(output.policyMonitoringByOwner).toEqual([]);
      expect(output.stages).toEqual([
        { stageName: 'Develop', stageTypeId: 1 },
        { stageName: 'Build', stageTypeId: 1 },
      ]);
      expect(output.originalStage).toEqual({ stageName: 'Build', stageTypeId: 2 });
      expect(output.monitoredStage).toEqual({ stageName: 'Develop', stageTypeId: 1 });
      expect(output.loading).toBeFalse();
      expect(output.loadError).toBeNull();
      expect(output.submitError).toBeNull();
      expect(output.isMonitoringSupported).toBeTrue();
    });
  });
});
