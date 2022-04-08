/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import * as policySelectors from 'MainRoot/OrgsAndPolicies/policySelectors';
import ownerManagerModule from 'MainRoot/owner.manager/owner.manager.module';
import { mapStateToThis } from 'MainRoot/owner.manager/policy/policy.editor.actions.controller';

describe('policy.editor.actions.controller', function () {
  beforeEach(
    angular.mock.module(ownerManagerModule.name, function ($provide) {
      $provide.value('$cookies', {
        get: angular.noop,
      });
      SpecUtil.mockNgRedux($provide);
    })
  );

  var vm, stageTypeStoreDefer, CLMLocations, $httpBackend, $scope;

  beforeEach(inject(function ($q, _$timeout_, _$httpBackend_, $controller, _CLMLocations_, StageTypeStore, $rootScope) {
    $scope = $rootScope.$new();
    CLMLocations = _CLMLocations_;
    $httpBackend = _$httpBackend_;

    stageTypeStoreDefer = $q.defer();
    spyOn(stageTypeStoreDefer.promise, 'then').and.callThrough();
    spyOn(StageTypeStore, 'getActionStages').and.returnValue(stageTypeStoreDefer.promise);
    stageTypeStoreDefer.resolve(MockData.getActionStageData());
    vm = $controller('policy.editor.actions.controller', { $scope }, { actions: [] });
  }));

  describe('mapStateToThis', () => {
    it('sets shouldShowQuarantineWarning to component', () => {
      spyOn(policySelectors, 'selectShouldShowQuarantineWarning').and.returnValue(true);

      const { shouldShowQuarantineWarning } = mapStateToThis({});

      expect(shouldShowQuarantineWarning).toBeTrue();
    });
  });

  describe('on create', () => {
    it('subscribes to the redux store', () => {
      expect(vm.unsubscribe).toBeDefined();
    });

    it('Properly loads action info', function () {
      $httpBackend.expectGET(CLMLocations.getProductFeaturesUrl()).respond([]);
      expect(stageTypeStoreDefer.promise.then).toHaveBeenCalled();
      $httpBackend.flush();

      expect(vm.actionStages.length).toBe(6);
    });
  });

  describe('$destroy()', () => {
    it('unsubscribes from redux store', () => {
      expect(vm.unsubscribe).not.toHaveBeenCalled();
      $scope.$destroy();
      expect(vm.unsubscribe).toHaveBeenCalledTimes(1);
    });
  });
});
