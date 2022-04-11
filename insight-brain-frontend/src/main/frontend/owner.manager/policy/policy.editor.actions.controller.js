/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { omit } from 'ramda';

import { actions } from 'MainRoot/OrgsAndPolicies/policySlice';
import { actions as stagesActions } from 'MainRoot/OrgsAndPolicies/orgsAndPoliciesStagesSlice';
import { selectShouldShowQuarantineWarning } from '../../OrgsAndPolicies/policySelectors';
import {
  selectActionStagesLoadError,
  selectActionStageTypes,
} from 'MainRoot/OrgsAndPolicies/orgsAndPoliciesStagesSelectors';

export default function PolicyEditorActionsController($scope, $q, ProductFeatures, $ngRedux) {
  var vm = this;
  vm.actionStages = null;
  vm.loadError = null;
  vm.isEnforcementSupported = null;
  vm.isFirewallSupported = null;
  vm.isEnforcementSupportedForStage = isEnforcementSupportedForStage;
  vm.doLoad = doLoad;
  vm.onActionChange = onActionChange;
  vm.unsubscribe = $ngRedux.connect(mapStateToThis, {
    loadActionStageTypes: stagesActions.loadActionStages,
    setActions: actions.setActions,
  })(vm);
  vm.doLoad();

  $scope.$on('$destroy', () => {
    vm.unsubscribe();
  });

  function doLoad() {
    vm.loadActionStageTypes();
    const promises = [ProductFeatures.load()];

    $q.all(promises).then(
      function () {
        vm.isEnforcementSupported = ProductFeatures.isAvailable('enforcement');
        vm.isFirewallSupported = ProductFeatures.isAvailable('firewall');
      },
      function (error) {
        vm.loadError = error;
      }
    );

    delete vm.loadError;
  }

  function isEnforcementSupportedForStage(stage) {
    return (vm.isFirewallSupported && stage === 'proxy') || vm.isEnforcementSupported;
  }

  function onActionChange(stageTypeId, value) {
    const updatedActions = value ? { ...vm.actions, [stageTypeId]: value } : omit([stageTypeId], vm.actions);

    vm.setActions(updatedActions);
  }
}

export const mapStateToThis = (state) => {
  return {
    actionStages: selectActionStageTypes(state),
    loadError: selectActionStagesLoadError(state),
    shouldShowQuarantineWarning: selectShouldShowQuarantineWarning(state),
  };
};

PolicyEditorActionsController.$inject = ['$scope', '$q', 'ProductFeatures', '$ngRedux', '$scope'];
