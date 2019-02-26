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

function ApplicationReportRawController($ngRedux, applicationReportActions) {
  const vm = this;

  Object.assign(vm, {
    $onInit() {
      vm.unsubscribe = $ngRedux.connect(mapStateToThis, applicationReportActions)(vm);
      vm.load();
    },

    $onDestroy() {
      vm.unsubscribe();
    },

    load() {
      vm.loadReportRawData();
    }
  });
}

function mapStateToThis({applicationReport}) {
  return applicationReport;
}

ApplicationReportRawController.$inject = ['$ngRedux', 'applicationReportActions', '$state'];
