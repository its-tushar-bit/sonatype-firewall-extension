/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import ownerManagerModule from '../../../../main/frontend/owner.manager/owner.manager.module';

describe('policy.editor.actions.controller.spec.js', function () {
  beforeEach(
    angular.mock.module(ownerManagerModule.name, function ($provide) {
      $provide.value('$cookies', {
        get: angular.noop,
      });
      SpecUtil.mockNgRedux($provide);
    })
  );

  var vm, stageTypeStoreDefer, CLMLocations, $httpBackend;

  beforeEach(inject(function ($q, _$timeout_, _$httpBackend_, $controller, _CLMLocations_, StageTypeStore) {
    CLMLocations = _CLMLocations_;
    $httpBackend = _$httpBackend_;

    stageTypeStoreDefer = $q.defer();
    spyOn(stageTypeStoreDefer.promise, 'then').and.callThrough();
    spyOn(StageTypeStore, 'getActionStages').and.returnValue(stageTypeStoreDefer.promise);
    stageTypeStoreDefer.resolve(MockData.getActionStageData());
    vm = $controller('policy.editor.actions.controller', {}, { actions: [] });
  }));

  it('Properly loads action info', function () {
    $httpBackend.expectGET(CLMLocations.getProductFeaturesUrl()).respond([]);
    expect(stageTypeStoreDefer.promise.then).toHaveBeenCalled();
    $httpBackend.flush();

    expect(vm.actionStages.length).toBe(6);
  });

  describe('vm.shouldShowQuarantineWarning', function () {
    it('returns false when root org and action is not fail', function () {
      inject(function ($controller) {
        vm = $controller(
          'policy.editor.actions.controller',
          {},
          { actions: [{ proxy: 'fail' }, { build: undefined }], isRootOrg: true, originalProxyStageAction: 'warn' }
        );
      });
      vm.actions['proxy'] = 'warn';
      expect(vm.shouldShowQuarantineWarning()).toBe(false);
    });

    it('returns false when root org and action not changed to fail', function () {
      inject(function ($controller) {
        vm = $controller(
          'policy.editor.actions.controller',
          {},
          { actions: [{ proxy: 'warn' }, { build: undefined }], isRootOrg: true, originalProxyStageAction: 'fail' }
        );
      });
      vm.actions['proxy'] = 'fail';
      expect(vm.shouldShowQuarantineWarning()).toBe(false);
    });

    it('returns true when root org and action changed to fail', function () {
      inject(function ($controller) {
        vm = $controller(
          'policy.editor.actions.controller',
          {},
          { actions: [{ proxy: 'warn' }, { build: undefined }], isRootOrg: true, originalProxyStageAction: 'warn' }
        );
      });
      vm.actions['proxy'] = 'fail';
      expect(vm.shouldShowQuarantineWarning()).toBe(true);
    });

    it('returns false when not root org', function () {
      inject(function ($controller) {
        vm = $controller(
          'policy.editor.actions.controller',
          {},
          { actions: [{ proxy: 'warn' }, { build: undefined }], isRootOrg: false, originalProxyStageAction: 'warn' }
        );
      });
      vm.actions['proxy'] = 'fail';
      expect(vm.shouldShowQuarantineWarning()).toBe(false);
    });
  });
});
