/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { isEmpty, keys, pick } from 'ramda';

import template from './applicationReport.html';

export default {
  template: template,
  controllerAs: 'vm',
  controller: ApplicationReportController
};

function ApplicationReportController($scope, $ngRedux, applicationReportActions) {
  const vm = this;

  $scope.$watch('vm.filters', function(filters) {
    vm.hasFilter = !isEmpty(keys(filters));
  });

  Object.assign(vm, {
    $onInit() {
      const actions = pick(['setAggregateReportEntries', 'setFiltering'], applicationReportActions);

      vm.unsubscribe = $ngRedux.connect(mapStateToThis, actions)(vm);
    },

    $onDestroy() {
      vm.unsubscribe();
    }
  });
}

function mapStateToThis(state) {
  return pick(['aggregate', 'filters'], state.applicationReport || {});
}

ApplicationReportController.$inject = ['$scope', '$ngRedux', 'applicationReportActions'];
