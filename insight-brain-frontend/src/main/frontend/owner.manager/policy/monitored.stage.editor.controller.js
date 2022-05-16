/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { actions } from 'MainRoot/OrgsAndPolicies/policyMonitoringSlice';
import { actions as stagesActions } from 'MainRoot/OrgsAndPolicies/stagesSlice';
import { selectIsMonitoringSupported } from 'MainRoot/productFeatures/productFeaturesSelectors';
import {
  selectLastSavedMonitoredStage,
  selectPolicyMonitoringByOwner,
  selectPolicyMonitoringLoadError,
  selectPolicyMonitoringLoading,
  selectPolicyMonitoringSubmitError,
  selectSelectedMonitoredStage,
} from 'MainRoot/OrgsAndPolicies/policyMonitoringSelectors';
import { selectCliStagesWithInheritOrNoMonitorOption } from 'MainRoot/OrgsAndPolicies/stagesSelectors';

export default function MonitoredStageEditorController($scope, $ngRedux) {
  const vm = this;

  vm.unsubscribe = $ngRedux.connect(mapStateToThis, {
    loadApplicablePolicyMonitoring: actions.loadApplicablePolicyMonitoring,
    savePolicyMonitoring: actions.savePolicyMonitoring,
    removePolicyMonitoring: actions.removePolicyMonitoring,
    setMonitoredStage: actions.setMonitoredStage,
    loadCliStageTypes: stagesActions.loadCliStages,
  })(vm);

  Object.assign(vm, {
    continuousMonitoringEditorMask: undefined,

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

  $scope.$on('pageChangeStarted', (event) => {
    if (vm.isDirty()) {
      event.preventDefault();
    }
  });

  vm.doLoad();

  $scope.$on('$destroy', function () {
    vm.unsubscribe();
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
