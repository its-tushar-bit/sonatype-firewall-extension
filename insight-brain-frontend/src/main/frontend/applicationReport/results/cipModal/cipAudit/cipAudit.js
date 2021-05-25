/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { map, negate, pipe, prop, sort, startsWith } from 'ramda';
import { processAuditRecord } from '../../../../componentDetails/componentDetailsUtils';

import template from './cipAudit.html';

export default {
  template,
  controller: CipAuditController,
  controllerAs: 'vm',
  bindings: {
    scanId: '<',
    applicationPublicId: '<',
    component: '<',
  },
};

function CipAuditController($scope, $http, CLMLocations, Messages) {
  const vm = this;

  Object.assign(vm, {
    sort: '-time',

    $onInit() {
      $scope.$watchGroup(['vm.applicationPublicId', 'vm.scanId', 'vm.component'], vm.doLoad);
    },

    doLoad() {
      vm.error = undefined;
      vm.auditRecords = undefined;

      $http.get(CLMLocations.getReportAuditLogUrl(vm.applicationPublicId, vm.scanId, vm.component)).then(
        function (response) {
          const { data } = response,
            records = (data && data.aaData) || [];

          vm.auditRecords = map(processAuditRecord, records);
          vm.sortRecords();
        },
        function (response) {
          vm.auditRecords = [];
          vm.error = Messages.getHttpErrorMessage(response);
        }
      );
    },

    onSortChange([sortCol]) {
      vm.sort = sortCol;
      vm.sortRecords();
    },

    sortRecords() {
      const reverse = startsWith('-', vm.sort),
        sortCol = reverse ? vm.sort.substring(1) : vm.sort,
        sortKeyFn = prop(sortCol),
        baseSortFn = (a, b) => {
          const aComp = sortKeyFn(a),
            bComp = sortKeyFn(b);

          return aComp < bComp ? -1 : aComp > bComp ? 1 : 0;
        },
        sortFn = reverse ? pipe(baseSortFn, negate) : baseSortFn;

      vm.auditRecords = sort(sortFn, vm.auditRecords);
    },
  });
}

CipAuditController.$inject = ['$scope', '$http', 'CLMLocations', 'Messages'];
