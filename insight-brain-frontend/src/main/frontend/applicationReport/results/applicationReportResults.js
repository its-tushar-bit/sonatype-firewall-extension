/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { isNil, splitAt } from 'ramda';

import template from './applicationReportResults.html';
import cipModalWrapper from './cipModalWrapper.html';

export default {
  template,
  controllerAs: 'vm',
  controller: ApplicationReportResultsController
};

function ApplicationReportResultsController($state, $ngRedux, $scope, $timeout, applicationReportActions, Modal,
                                            OwnerContext, CLMLocations) {
  const vm = this;

  Object.assign(vm, {
    renderedEntries: [],

    updateRenderedEntriesPromise: null,

    $onInit() {
      vm.unsubscribe = $ngRedux.connect(mapStateToThis, applicationReportActions)(vm);
      $scope.$watch('vm.reportParameters', function(reportParameters) {
        if (reportParameters) {
          OwnerContext.setOwnerId(reportParameters.appId);
          OwnerContext.setScanId(reportParameters.scanId);
        }
      });

      $scope.$watch('vm.selectedReport.displayedEntries', updateRenderedEntries);
    },

    $onDestroy() {
      vm.unsubscribe();
    },

    reload() {
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
      vm.setStringFieldFilter('derivedComponentName', vm.derivedComponentNameSubstringFilter);
    },

    onPolicyNameFilterChange() {
      vm.setStringFieldFilter('policyName', vm.policyNameSubstringFilter);
    },

    getReportPdfDownloadUrl: function() {
      return CLMLocations.getReportPdfDownloadUrl(vm.metadata.application.publicId, vm.reportParameters.scanId);
    }
  });

  // rendering thousands of rows at once can cause a noticeable UI freeze while all the angular code runs.
  // To help mitigate this, add the rows in chunks
  function updateRenderedEntries() {
    if (vm.updateRenderedEntriesPromise) {
      $timeout.cancel(vm.updateRenderedEntriesPromise);
    }

    vm.renderedEntries = [];

    if (!isNil(vm.selectedReport)) {
      doUpdateStep(vm.selectedReport.displayedEntries);
    }
  }

  function doUpdateStep(remainingEntries) {
    const [firstChunk, furtherRemainingEntries] = splitAt(100, remainingEntries);

    vm.renderedEntries = vm.renderedEntries.concat(firstChunk);

    if (furtherRemainingEntries.length) {
      // NOTE the delay of 1 is only necessary due to unit tests - angular-mock's fake implementation
      // of $timeout gets confused when everything has a delay of zero and flushes chained timeouts as
      // opposed to only timeouts that existed at the time flush was called
      vm.updateRenderedEntriesPromise = $timeout(doUpdateStep, 1, true, furtherRemainingEntries);
    }
  }
}

export function mapStateToThis({applicationReport}) {
  const { policyName, derivedComponentName } = applicationReport.substringFilters;

  return {
    ...applicationReport,
    loading: !!applicationReport.pendingLoads.size,
    policyNameSubstringFilter: policyName,
    derivedComponentNameSubstringFilter: derivedComponentName
  };
}

ApplicationReportResultsController.$inject = [
  '$state', '$ngRedux', '$scope', '$timeout', 'applicationReportActions', 'Modal', 'OwnerContext', 'CLMLocations'
];
