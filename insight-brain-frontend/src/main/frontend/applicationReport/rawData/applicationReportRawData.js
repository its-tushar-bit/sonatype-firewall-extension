/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import template from './applicationReportRawData.html';

export default {
  template: template,
  controllerAs: 'vm',
  controller: ApplicationReportRawController
};

function ApplicationReportRawController($ngRedux, applicationReportActions, $state) {
  const vm = this;

  Object.assign(vm, {
    $onInit() {
      vm.unsubscribe = $ngRedux.connect(mapStateToThis, applicationReportActions)(vm);
    },

    $onDestroy() {
      vm.unsubscribe();
    },

    reload() {
      vm.loadReport($state.params.publicId, $state.params.scanId, !!$state.params.unknownjs);
    }
  });
}

function mapStateToThis({applicationReport}) {
  return applicationReport;
}

ApplicationReportRawController.$inject = ['$ngRedux', 'applicationReportActions', '$state'];
