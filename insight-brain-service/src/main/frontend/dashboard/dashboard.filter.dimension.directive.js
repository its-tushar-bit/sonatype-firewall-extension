/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
(function() {
  'use strict';

  /**
   * @ngDoc directive
   * @name dashboardFilterDimension
   * @restrict E
   *
   * @description
   *
   * Shows a filter dimension in a tree where each available item is shown as a leaf with a checkbox
   *
   * @param available array of all available entities in this filter type
   * @param selected map of currently selected entities in this dimension (id -> true/false)
   * @param shortName short name for the dimension
   * @param longName long name for the dimension
   * @param idField field used by the entity for the id (defaults to 'id')
   * @param nameField field used by the entity for the name (defaults to 'name')
   */
  function DashboardFilterDimension() {
    return {
      restrict: 'E',
      transclude: true,
      templateUrl: 'entity-filter-template',
      scope: {
        available: '=',
        selected: '=',
        shortName: '@',
        longName: '@',
        idField: '@?',
        nameField: '@?',
        tooltipField: '@?',
        sortEntities: '=?'
      },
      controller: DashboardFilterDimensionController,
      controllerAs: 'vm',
      link: function ($scope) {
        $scope.idField = $scope.idField || 'id';
        $scope.nameField = $scope.nameField || 'name';
      }
    };
  }

  function DashboardFilterDimensionController($scope, fuzzyFilter) {
    var vm = this;

    vm.filter = '';
    vm.selectedCount = 0;

    vm.allSelected = allSelected;
    vm.toggleSelectAll = toggleSelectAll;
    vm.updateSelectedCount = updateSelectedCount;
    vm.clearIfUnselected = clearIfUnselected;

    $scope.$watch('selected', function() {
      vm.selectedCount = Object.keys($scope.selected).filter(function(id) {
        return $scope.selected[id];
      }).length;
    });

    function toggleSelectAll() {
      if (vm.filter) {
        var filtered = fuzzyFilter($scope.available, vm.filter, $scope.nameField);

        if (areAllSelected(filtered)) {
          filtered.forEach(function(entity) {
            $scope.selected[entity[$scope.idField]] = false;
          });
        }
        else {
          filtered.forEach(function(entity) {
            $scope.selected[entity[$scope.idField]] = true;
          });
        }

        vm.selectedCount = 0;
        Object.keys($scope.selected).forEach(function(key) {
          if ($scope.selected[key]) {
            vm.selectedCount++;
          }
        });
      }
      else {
        if (vm.selectedCount !== $scope.available.length) {
          $scope.available.forEach(function(entity) {
            $scope.selected[entity[$scope.idField]] = true;
          });
          vm.selectedCount = $scope.available.length;
        }
        else {
          $scope.selected = {};
          vm.selectedCount = 0;
        }
      }
    }

    function allSelected() {
      if (vm.filter) {
        return areAllSelected(fuzzyFilter($scope.available, vm.filter, $scope.nameField));
      }
      else {
        return $scope.available.length === vm.selectedCount;
      }
    }

    function areAllSelected(array) {
      return !array.some(function(item) {
        return !$scope.selected[item[$scope.idField]];
      });
    }

    function updateSelectedCount(id) {
      if ($scope.selected[id]) {
        vm.selectedCount++;
      }
      else {
        vm.selectedCount--;
      }
    }

    function clearIfUnselected(item) {
      if (!$scope.selected[item]) {
        delete $scope.selected[item];
      }
    }
  }

  DashboardFilterDimensionController.$inject = ['$scope', 'fuzzyFilter'];

  angular.module('dashboard.module').directive('dashboardFilterDimension', DashboardFilterDimension);
}());
