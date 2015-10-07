/**
 * @license Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
/*global angular */
(function () {
  'use strict';

  function repositoryViolationTableFilter() {
    return {
      scope: {
        filterArgs: '=repositoryViolationTableFilter'
      },
      bindToController: true,
      templateUrl: 'repository-violation-table-filter',
      controllerAs: 'vm',
      controller: ['$scope', function($scope) {
        var vm = this;

        vm.filter = {
          matchState: 'all',
          violationState: 'summary'
        };
        vm.filterArgs = undefined;

        function setFilter() {
          var filterArgs = {};
          if (vm.filter.matchState !== 'all') {
            filterArgs.matchState = vm.filter.matchState;
          }
          switch (vm.filter.violationState) {
            case 'waived':
              filterArgs.waived = true;
              break;
            case 'quarantined':
              filterArgs.quarantined = true;
              break;
            case 'summary':
              filterArgs.highestThreatLevel = true;
              filterArgs.waived = false;
              break;
          }
          vm.filterArgs = filterArgs;
        }

        $scope.$watch('vm.filter.matchState', setFilter);
        $scope.$watch('vm.filter.violationState', setFilter);
      }]
    };
  }

  angular.module('Audit').directive('repositoryViolationTableFilter', repositoryViolationTableFilter);
}());
