/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
(function() {
  'use strict';

  /**
   * @ngDoc directive
   * @name dashboardFilterRadioDimension
   * @restrict E
   *
   * @description
   *
   * Shows a filter dimension in a tree where each available item is shown as a leaf with a radio
   *
   * @param available array of all available entities in this filter type
   * @param selectedEntry object representing selected entry
   * @param idField field used by the entity for the id (defaults to 'id')
   * @param nameField field used by the entity for the name (defaults to 'name')
   * @param name used for the 'name' attribute of radio inputs
   */
  function dashboardFilterRadioDimension() {
    return {
      restrict: 'E',
      transclude: true,
      templateUrl: 'entity-filter-radio-template',
      scope: {
        available: '<',
        selectedEntry: '=',
        idField: '@?',
        nameField: '@?',
        name: '@',
        readOnly: '<?'
      },
      controller: DashboardFilterRadioDimensionController,
      controllerAs: 'vm',
      link: function ($scope) {
        $scope.idField = $scope.idField || 'id';
        $scope.nameField = $scope.nameField || 'name';
      }
    };
  }

  function DashboardFilterRadioDimensionController($scope) {
    var vm = this;

    vm.select = select;
    vm.isChecked = isChecked;

    function select(item) {
      $scope.selectedEntry = item;
    }

    function isChecked(entity) {
      return $scope.selectedEntry[$scope.idField] === entity[$scope.idField];
    }
  }

  DashboardFilterRadioDimensionController.$inject = ['$scope'];

  angular.module('dashboard.module').directive('dashboardFilterRadioDimension', dashboardFilterRadioDimension);
}());
