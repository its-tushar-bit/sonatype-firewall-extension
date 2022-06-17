/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { omit } from 'ramda';
import { actions } from 'MainRoot/OrgsAndPolicies/policySlice';
import { actions as productFeaturesActions } from 'MainRoot/productFeatures/productFeaturesSlice';
import { actions as stagesActions } from 'MainRoot/OrgsAndPolicies/stagesSlice';
import {
  selectIsEnforcementSupported,
  selectIsFirewallSupported,
} from 'MainRoot/productFeatures/productFeaturesSelectors';
import {
  selectShouldShowQuarantineWarning,
  selectIsActionOverrideEnabled,
  selectIsInherited,
  selectOverrideActionsFlag,
} from '../../OrgsAndPolicies/policySelectors';
import { selectSelectedOwnerId } from 'MainRoot/OrgsAndPolicies/orgsAndPoliciesSelectors';
import { selectActionStagesLoadError, selectActionStageTypes } from 'MainRoot/OrgsAndPolicies/stagesSelectors';

export default function PolicyEditorActionsController($scope, $ngRedux) {
  var vm = this;
  vm.actionStages = null;
  vm.loadError = null;
  vm.isEnforcementSupportedForStage = isEnforcementSupportedForStage;
  vm.doLoad = doLoad;
  vm.onActionChange = onActionChange;
  vm.onPolicyActionsOverride = onPolicyActionsOverride;
  vm.isActionsGridDisabled = isActionsGridDisabled;

  vm.unsubscribe = $ngRedux.connect(mapStateToThis, {
    loadActionStageTypes: stagesActions.loadActionStages,
    setActions: actions.setActions,
    setActionsOverride: actions.setActionsOverride,
    loadProductFeatures: productFeaturesActions.fetchProductFeaturesIfNeeded,
    setOverrideParentActions: actions.setOverrideParentActions,
    unSetOverrideParentActions: actions.unSetOverrideParentActions,
  })(vm);

  vm.doLoad();

  $scope.$on('$destroy', () => {
    vm.unsubscribe();
  });

  function doLoad() {
    delete vm.loadError;
    vm.loadActionStageTypes();
    vm.loadProductFeatures();
  }

  function isEnforcementSupportedForStage(stage) {
    return (vm.isFirewallSupported && stage === 'proxy') || vm.isEnforcementSupported;
  }

  function onPolicyActionsOverride(isOverride) {
    if (isOverride) {
      vm.setOverrideParentActions();
    } else {
      vm.unSetOverrideParentActions(vm.ownerInternalId);
    }
  }

  function isActionsGridDisabled(stageTypeId) {
    const isInheritedDisabled = vm.isInherited && !vm.overrideParentActions;
    return vm.disabled || !vm.isEnforcementSupportedForStage(stageTypeId) || isInheritedDisabled;
  }

  function onActionChange(stageTypeId, value) {
    const updatedActions = value ? { ...vm.actions, [stageTypeId]: value } : omit([stageTypeId], vm.actions);

    if (vm.isActionOverrideEnabled) {
      vm.setActionsOverride({
        ownerId: vm.ownerInternalId,
        actionsOverride: updatedActions,
      });
    } else {
      vm.setActions(updatedActions);
    }
  }
}

export const mapStateToThis = (state) => ({
  isEnforcementSupported: selectIsEnforcementSupported(state),
  isFirewallSupported: selectIsFirewallSupported(state),
  shouldShowQuarantineWarning: selectShouldShowQuarantineWarning(state),
  actionStages: selectActionStageTypes(state),
  loadError: selectActionStagesLoadError(state),
  isActionOverrideEnabled: selectIsActionOverrideEnabled(state),
  ownerInternalId: selectSelectedOwnerId(state),
  isInherited: selectIsInherited(state),
  overrideParentActions: selectOverrideActionsFlag(state),
});

PolicyEditorActionsController.$inject = ['$scope', '$ngRedux'];
