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
      spyOn(policySelectors, 'selectIsInherited').and.returnValue(true);
      spyOn(policySelectors, 'selectOverrideActionsFlag').and.returnValue(false);

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
        isInherited,
        overrideParentActions,
      } = mapStateToThis({});

      expect(shouldShowQuarantineWarning).toBeTrue();
      expect(isEnforcementSupported).toBeTrue();
      expect(isFirewallSupported).toBeTrue();
      expect(actionStages).toEqual([]);
      expect(loadError).toEqual('error');
      expect(isInherited).toBeTrue();
      expect(overrideParentActions).toBeFalse();
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

  describe('onPolicyActionsOverride', () => {
    it('calls setOverrideParentActions if selected option is override', () => {
      vm.onPolicyActionsOverride(true);
      expect(vm.setOverrideParentActions).toHaveBeenCalledTimes(1);
      expect(vm.setActionsOverride).toHaveBeenCalledTimes(1);
    });

    it('calls unSetOverrideParentActions if selected option is inherit', () => {
      vm.onPolicyActionsOverride(false);
      expect(vm.unSetOverrideParentActions).toHaveBeenCalledTimes(1);
    });
  });

  describe('onActionChange', () => {
    beforeEach(() => {
      vm.ownerInternalId = 'testOwnerId';
      vm.actions = { build: 'fail', release: 'fail' };
    });

    describe('when isActionOverrideEnabled is false', () => {
      beforeEach(() => {
        vm.isActionOverrideEnabled = false;
      });

      it('calls setActions with updated actions', () => {
        vm.onActionChange('build', 'warn');
        expect(vm.setActions).toHaveBeenCalledWith({ build: 'warn', release: 'fail' });
      });

      it('removes the stage from payload if the action value is undefined', () => {
        vm.onActionChange('build');
        expect(vm.setActions).toHaveBeenCalledWith({ release: 'fail' });
      });
    });

    describe('when isActionOverrideEnabled is true', () => {
      beforeEach(() => {
        vm.isActionOverrideEnabled = true;
      });

      it('calls setActionsOverride with updated actions', () => {
        vm.onActionChange('build', 'warn');
        expect(vm.setActionsOverride).toHaveBeenCalledWith({
          ownerId: 'testOwnerId',
          actionsOverride: { build: 'warn', release: 'fail' },
        });
      });

      it('removes the stage from payload if the action value is undefined', () => {
        vm.onActionChange('build');
        expect(vm.setActionsOverride).toHaveBeenCalledWith({
          ownerId: 'testOwnerId',
          actionsOverride: { release: 'fail' },
        });
      });
    });
  });
});
