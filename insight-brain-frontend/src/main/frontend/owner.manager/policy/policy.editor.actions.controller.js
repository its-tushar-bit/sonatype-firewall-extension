/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { omit } from 'ramda';

import { actions } from 'MainRoot/OrgsAndPolicies/policySlice';
import { selectShouldShowQuarantineWarning } from '../../OrgsAndPolicies/policySelectors';
export default function PolicyEditorActionsController($q, StageTypeStore, ProductFeatures, $ngRedux, $scope) {
  var vm = this;
  vm.actionStages = null;
  vm.loadError = null;
  vm.isEnforcementSupported = null;
  vm.isFirewallSupported = null;
  vm.isEnforcementSupportedForStage = isEnforcementSupportedForStage;
  vm.doLoad = doLoad;
  vm.onActionChange = onActionChange;
  vm.unsubscribe = $ngRedux.connect(mapStateToThis, {
    setActions: actions.setActions,
  })(vm);
  vm.doLoad();

  $scope.$on('$destroy', () => {
    vm.unsubscribe();
  });

  function doLoad() {
    const promises = [StageTypeStore.getActionStages(), ProductFeatures.load()];

    $q.all(promises).then(
      function (results) {
        vm.actionStages = results[0];
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

export const mapStateToThis = (state) => ({
  shouldShowQuarantineWarning: selectShouldShowQuarantineWarning(state),
});

PolicyEditorActionsController.$inject = ['$q', 'StageTypeStore', 'ProductFeatures', '$ngRedux', '$scope'];
