/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

import template from './applicationReportCipModal.html';

export default {
  template: template,
  controller: CipModalController,
  controllerAs: 'vm',
  bindings: {
    dismiss: '&',
  },
};

function CipModalController($ngRedux, $scope, applicationReportActions) {
  const vm = this;

  Object.assign(vm, {
    $onInit() {
      vm.unsubscribeFromReduxStore = $ngRedux.connect(mapStateToThis, applicationReportActions)(vm);
    },

    $onDestroy() {
      vm.unsubscribeFromReduxStore();
    },

    reloadReportAndHandleError() {
      if (vm.reloadReport) {
        return vm.reloadReport().catch(() => vm.dismiss());
      }
    },
  });
}

export function mapStateToThis({ applicationReport }) {
  let { selectedReport, selectedComponentIndex, selectedRootAncestor, metadata, selectedComponent } = applicationReport;
  let previousComponent = null;

  if (selectedRootAncestor) {
    previousComponent = selectedComponent;
    selectedComponent = selectedRootAncestor;
  }

  return {
    selectedReport,
    selectedComponent,
    selectedComponentIndex,
    selectedRootAncestor,
    previousComponent,
    metadata,
  };
}

CipModalController.$inject = ['$ngRedux', '$scope', 'applicationReportActions'];
