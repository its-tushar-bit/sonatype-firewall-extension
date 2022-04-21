/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { omit } from 'ramda';
import { actions } from 'MainRoot/OrgsAndPolicies/policySlice';
import { actions as productFeaturesActions } from 'MainRoot/productFeatures/productFeaturesSlice';
import { actions as stagesActions } from 'MainRoot/OrgsAndPolicies/orgsAndPoliciesStagesSlice';
import {
  selectIsEnforcementSupported,
  selectIsFirewallSupported,
} from 'MainRoot/productFeatures/productFeaturesSelectors';
import {
  selectActionStagesLoadError,
  selectActionStageTypes,
} from 'MainRoot/OrgsAndPolicies/orgsAndPoliciesStagesSelectors';
import { selectShouldShowQuarantineWarning } from '../../OrgsAndPolicies/policySelectors';

export default function PolicyEditorActionsController($scope, $ngRedux) {
  var vm = this;
  vm.actionStages = null;
  vm.loadError = null;
  vm.isEnforcementSupportedForStage = isEnforcementSupportedForStage;
  vm.doLoad = doLoad;
  vm.onActionChange = onActionChange;

  vm.unsubscribe = $ngRedux.connect(mapStateToThis, {
    loadActionStageTypes: stagesActions.loadActionStages,
    setActions: actions.setActions,
    loadProductFeatures: productFeaturesActions.fetchProductFeaturesIfNeeded,
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

  function onActionChange(stageTypeId, value) {
    const updatedActions = value ? { ...vm.actions, [stageTypeId]: value } : omit([stageTypeId], vm.actions);

    vm.setActions(updatedActions);
  }
}

export const mapStateToThis = (state) => ({
  isEnforcementSupported: selectIsEnforcementSupported(state),
  isFirewallSupported: selectIsFirewallSupported(state),
  shouldShowQuarantineWarning: selectShouldShowQuarantineWarning(state),
  actionStages: selectActionStageTypes(state),
  loadError: selectActionStagesLoadError(state),
});

PolicyEditorActionsController.$inject = ['$scope', '$ngRedux'];
