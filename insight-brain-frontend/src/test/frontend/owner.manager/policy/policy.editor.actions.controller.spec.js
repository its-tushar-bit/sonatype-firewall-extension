/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import * as policySelectors from 'MainRoot/OrgsAndPolicies/policySelectors';
import * as stagesSelectors from 'MainRoot/OrgsAndPolicies/stagesSelectors';
import ownerManagerModule from 'MainRoot/owner.manager/owner.manager.module';
import { mapStateToThis } from 'MainRoot/owner.manager/policy/policy.editor.actions.controller';
import * as productFeaturesSelectors from 'MainRoot/productFeatures/productFeaturesSelectors';
import { actions } from 'MainRoot/productFeatures/productFeaturesSlice';

describe('policy.editor.actions.controller', function () {
  beforeEach(
    angular.mock.module(ownerManagerModule.name, function ($provide) {
      $provide.value('$cookies', {
        get: angular.noop,
      });
      SpecUtil.mockNgRedux($provide);
    })
  );

  var vm, $scope;

  beforeEach(inject(function ($controller, $rootScope) {
    $scope = $rootScope.$new();

    spyOn(actions, 'fetchProductFeaturesIfNeeded').and.returnValue({ payload: [] });
    vm = $controller('policy.editor.actions.controller', { $scope }, { actions: [] });
  }));

  describe('mapStateToThis', () => {
    it('sets shouldShowQuarantineWarning, actionStages and loadError to component', () => {
      spyOn(policySelectors, 'selectShouldShowQuarantineWarning').and.returnValue(true);
      spyOn(productFeaturesSelectors, 'selectIsFirewallSupported').and.returnValue(true);
      spyOn(productFeaturesSelectors, 'selectIsEnforcementSupported').and.returnValue(true);
      spyOn(stagesSelectors, 'selectActionStageTypes').and.returnValue([]);
      spyOn(stagesSelectors, 'selectActionStagesLoadError').and.returnValue('error');

      const {
        shouldShowQuarantineWarning,
        isEnforcementSupported,
        isFirewallSupported,
        actionStages,
        loadError,
      } = mapStateToThis({});

      expect(shouldShowQuarantineWarning).toBeTrue();
      expect(isEnforcementSupported).toBeTrue();
      expect(isFirewallSupported).toBeTrue();
      expect(actionStages).toEqual([]);
      expect(loadError).toEqual('error');
    });
  });

  describe('on create', () => {
    it('subscribes to the redux store', () => {
      expect(vm.unsubscribe).toBeDefined();
    });

    it('calls loadActionStageTypes', () => {
      expect(vm.loadActionStageTypes).toHaveBeenCalledTimes(1);
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
