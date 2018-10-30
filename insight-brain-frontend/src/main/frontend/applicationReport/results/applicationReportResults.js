/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import template from './applicationReportResults.html';
import cipModalWrapper from './cipModalWrapper.html';

export default {
  template,
  controllerAs: 'vm',
  controller: ApplicationReportResultsController
};

function ApplicationReportResultsController($state, $ngRedux, $scope, applicationReportActions, Modal, OwnerContext) {
  const vm = this;

  Object.assign(vm, {
    $onInit() {
      vm.unsubscribe = $ngRedux.connect(mapStateToThis, applicationReportActions)(vm);
      vm.doLoad();
    },

    $onDestroy() {
      vm.unsubscribe();
    },

    doLoad() {
      $scope.$watch('vm.selectedReport', function(selectedReport) {
        if (selectedReport) {
          OwnerContext.setOwnerId(selectedReport.application.publicId);
        }
      });
      vm.loadReport($state.params.publicId, $state.params.scanId, !!$state.params.unknownjs);
    },

    coveragePercent() {
      const { totalArtifactCount, knownArtifactCount } = vm.selectedReport;

      return totalArtifactCount ? Math.round(100 * knownArtifactCount / totalArtifactCount) : 0;
    },

    openCipModal(componentIndex) {
      vm.selectComponent(componentIndex);
      Modal.open({
        template: cipModalWrapper,
        windowClass: 'iq-modal iq-modal__cip',
        backdropClass: 'iq-modal-backdrop'
      });
    },

    onDerivedComponentNameFilterChange() {
      vm.setStringFieldFilter('derivedComponentName', vm.substringFilters.derivedComponentName);
    },

    onPolicyNameFilterChange() {
      vm.setStringFieldFilter('policyName', vm.substringFilters.policyName);
    }
  });
}

function mapStateToThis({applicationReport}) {
  return applicationReport;
}

ApplicationReportResultsController.$inject = [
  '$state', '$ngRedux', '$scope', 'applicationReportActions', 'Modal', 'OwnerContext'
];
