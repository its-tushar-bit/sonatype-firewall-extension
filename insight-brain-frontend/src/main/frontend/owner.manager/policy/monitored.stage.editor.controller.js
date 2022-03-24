/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { actions } from 'MainRoot/OrgsAndPolicies/orgsAndPoliciesPolicyMonitoringSlice';
import { selectIsMonitoringSupported } from 'MainRoot/productFeatures/productFeaturesSelectors';
import {
  selectPolicyMonitoringByOwner,
  selectPolicyMonitoringLoadError,
  selectPolicyMonitoringLoading,
  selectPolicyMonitoringMonitoredStage,
  selectPolicyMonitoringOriginalStage,
  selectPolicyMonitoringStages,
  selectPolicyMonitoringSubmitError,
} from 'MainRoot/OrgsAndPolicies/orgsAndPoliciesPolicyMonitoringSelectors';

export default function MonitoredStageEditorController($scope, StageTypeStore, $ngRedux) {
  const vm = this;

  Object.assign(vm, {
    continuousMonitoringEditorMask: undefined,

    $onInit() {
      vm.unsubscribe = $ngRedux.connect(mapStateToThis, {
        loadApplicablePolicyMonitoring: actions.loadApplicablePolicyMonitoring,
        savePolicyMonitoring: actions.savePolicyMonitoring,
        removePolicyMonitoring: actions.removePolicyMonitoring,
        setMonitoredStage: actions.setMonitoredStage,
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
      vm.loadApplicablePolicyMonitoring({
        promises: () => StageTypeStore.get().then((stages) => ({ stages })),
      });
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

export const mapStateToThis = (state) => ({
  loading: selectPolicyMonitoringLoading(state),
  loadError: selectPolicyMonitoringLoadError(state),
  submitError: selectPolicyMonitoringSubmitError(state),
  policyMonitoringByOwner: selectPolicyMonitoringByOwner(state),
  stages: selectPolicyMonitoringStages(state),
  monitoredStage: selectPolicyMonitoringMonitoredStage(state),
  originalStage: selectPolicyMonitoringOriginalStage(state),
  isMonitoringSupported: selectIsMonitoringSupported(state),
});

MonitoredStageEditorController.$inject = ['$scope', 'StageTypeStore', '$ngRedux'];
