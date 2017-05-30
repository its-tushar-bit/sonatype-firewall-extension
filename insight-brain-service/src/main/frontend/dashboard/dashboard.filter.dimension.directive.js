/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

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
export default
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
  vm.notifySelectionChanged = notifySelectionChanged;
  vm.clearIfUnselected = clearIfUnselected;
  vm.toggle = toggle;

  $scope.$watch('selected', function() {
    vm.selectedCount = Object.keys($scope.selected).filter(function(id) {
      return $scope.selected[id];
    }).length;
  });

  function toggleSelectAll() {
    // since $scope.selected is a map we need to copy so $watches are triggered
    $scope.selected = angular.copy($scope.selected);
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
    }
    else {
      if (vm.selectedCount !== $scope.available.length) {
        $scope.available.forEach(function(entity) {
          $scope.selected[entity[$scope.idField]] = true;
        });
      }
      else {
        $scope.selected = {};
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

  function notifySelectionChanged() {
    // since $scope.selected is a map we need to copy so $watches are triggered
    $scope.selected = angular.copy($scope.selected);
  }

  function clearIfUnselected(item) {
    if (!$scope.selected[item]) {
      delete $scope.selected[item];
    }
  }

  function toggle(item) {
    $scope.selected[item] = !$scope.selected[item];
    vm.clearIfUnselected(item);
    vm.notifySelectionChanged();
  }
}

DashboardFilterDimensionController.$inject = ['$scope', 'fuzzyFilter'];
