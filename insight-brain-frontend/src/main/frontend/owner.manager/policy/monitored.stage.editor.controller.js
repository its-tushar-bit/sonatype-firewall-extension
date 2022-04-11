/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { actions } from 'MainRoot/OrgsAndPolicies/orgsAndPoliciesPolicyMonitoringSlice';
import { actions as stagesActions } from 'MainRoot/OrgsAndPolicies/orgsAndPoliciesStagesSlice';
import { selectIsMonitoringSupported } from 'MainRoot/productFeatures/productFeaturesSelectors';
import {
  selectLastSavedMonitoredStage,
  selectPolicyMonitoringByOwner,
  selectPolicyMonitoringLoadError,
  selectPolicyMonitoringLoading,
  selectPolicyMonitoringSubmitError,
  selectSelectedMonitoredStage,
} from 'MainRoot/OrgsAndPolicies/orgsAndPoliciesPolicyMonitoringSelectors';
import { selectCliStagesWithInheritOrNoMonitorOption } from 'MainRoot/OrgsAndPolicies/orgsAndPoliciesStagesSelectors';

export default function MonitoredStageEditorController($scope, $ngRedux) {
  const vm = this;

  Object.assign(vm, {
    continuousMonitoringEditorMask: undefined,

    $onInit() {
      vm.unsubscribe = $ngRedux.connect(mapStateToThis, {
        loadApplicablePolicyMonitoring: actions.loadApplicablePolicyMonitoring,
        savePolicyMonitoring: actions.savePolicyMonitoring,
        removePolicyMonitoring: actions.removePolicyMonitoring,
        setMonitoredStage: actions.setMonitoredStage,
        loadCliStageTypes: stagesActions.loadCliStages,
      })(vm);

      $scope.$on('pageChangeStarted', (event) => {
        if (vm.isDirty()) {
          event.preventDefault();
        }
      });

      vm.doLoad();
    },

    $onDestroy() {
      vm.unsubscribe();
    },

    doLoad() {
      vm.loadCliStageTypes();
      vm.loadApplicablePolicyMonitoring();
    },

    save() {
      vm.continuousMonitoringEditorMask.wrap(
        vm.monitoredStage.stageTypeId ? vm.savePolicyMonitoring() : vm.removePolicyMonitoring()
      );
    },

    isDirty() {
      return vm.originalStage?.stageTypeId !== vm.monitoredStage?.stageTypeId;
    },
  });
}

export const mapStateToThis = (state) => {
  return {
    loading: selectPolicyMonitoringLoading(state),
    loadError: selectPolicyMonitoringLoadError(state),
    submitError: selectPolicyMonitoringSubmitError(state),
    policyMonitoringByOwner: selectPolicyMonitoringByOwner(state),
    stages: selectCliStagesWithInheritOrNoMonitorOption(state),
    monitoredStage: selectSelectedMonitoredStage(state),
    originalStage: selectLastSavedMonitoredStage(state),
    isMonitoringSupported: selectIsMonitoringSupported(state),
  };
};

MonitoredStageEditorController.$inject = ['$scope', '$ngRedux'];
