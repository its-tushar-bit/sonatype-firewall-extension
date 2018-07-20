/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import template from './applicationReportResults.html';

export default {
  template,
  controllerAs: 'vm',
  controller: ApplicationReportResultsController
};

function ApplicationReportResultsController($state, $ngRedux, applicationReportActions) {
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
      vm.loadReport($state.params.publicId, $state.params.scanId);
    }
  });
}

function mapStateToThis({applicationReport}) {
  return applicationReport;
}

ApplicationReportResultsController.$inject = ['$state', '$ngRedux', 'applicationReportActions'];
