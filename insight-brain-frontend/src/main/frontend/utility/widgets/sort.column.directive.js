/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
export default function SortColumn() {
  return {
    restrict: 'A',
    require: '^sort',
    scope: {
      field: '@sortColumn', // comma separated list
      inverted: '@?sortInverted', // is the data logically inverted, i.e. AGE vs TIME
    },
    transclude: true,
    template:
      '<a class="iq-column-sort-trigger tm-column-sort-trigger" ng-click="vm.setSort()">' +
      '<div class="iq-column-sort-icons">' +
      '<i class="fa fa-caret-up" ng-class="{up : vm.isUp() }"></i>' +
      '<i class="fa fa-caret-down" ng-class="{down : vm.isDown() }"></i>' +
      '</div>' +
      '<span ng-transclude></span></a>',
    controller: angular.noop,
    controllerAs: 'vm',
    bindToController: true,
    link: function (scope, element, attrs, sortController) {
      var mainSort = scope.vm.field.split(',')[0];
      var isInverted = scope.vm.inverted === 'true';

      scope.vm.isUp = function () {
        var sortColumn = sortController.extractSortField(
          sortController.sortFields[0]
        );
        var reversed = sortColumn !== sortController.sortFields[0];
        var currentColumn = sortController.extractSortField(mainSort);
        return (
          sortColumn === currentColumn && (isInverted ? reversed : !reversed)
        );
      };

      scope.vm.isDown = function () {
        var sortColumn = sortController.extractSortField(
          sortController.sortFields[0]
        );
        var reversed = sortColumn !== sortController.sortFields[0];
        var currentColumn = sortController.extractSortField(mainSort);
        return (
          sortColumn === currentColumn && (!isInverted ? reversed : !reversed)
        );
      };

      scope.vm.updateHeader = function () {
        var isUp = scope.vm.isUp();
        var isDown = scope.vm.isDown();
        if (!isUp && !isDown) {
          element.removeClass('selected-column');
        } else if (isUp || isDown) {
          element.addClass('selected-column');
        }
      };

      scope.vm.setSort = function () {
        sortController.setSort(scope.vm.field.split(','));
      };

      scope.$watchGroup([scope.vm.isDown, scope.vm.isUp], function () {
        scope.vm.updateHeader();
      });
    },
  };
}
