/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { pick } from 'ramda';

import template from './applicationReport.html';

export default {
  template: template,
  controllerAs: 'vm',
  controller: ApplicationReportController
};

function ApplicationReportController($scope, $ngRedux, applicationReportActions) {
  const vm = this;

  Object.assign(vm, {
    $onInit() {
      const actions = pick(['setAggregateReportEntries'], applicationReportActions);

      vm.unsubscribe = $ngRedux.connect(mapStateToThis, actions)(vm);
    },

    $onDestroy() {
      vm.unsubscribe();
    }
  });
}

function mapStateToThis(state) {
  return pick(['aggregate'], state.applicationReport || {});
}

ApplicationReportController.$inject = ['$scope', '$ngRedux', 'applicationReportActions'];
