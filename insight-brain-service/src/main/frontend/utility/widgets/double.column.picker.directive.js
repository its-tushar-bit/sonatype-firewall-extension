/**
 * @license Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
(function(angular) {
  'use strict';

  function DoubleColumnPicker() {
    return {
      restrict: 'E',
      scope: {
        list: '=',
        filterPlaceholder: '@',
        leftColumnName: '@',
        rightColumnName: '@',
        itemNameParam: '@'
      },
      templateUrl: 'utility/widgets/double.column.picker.directive.html',
      controller: 'DoubleColumnPickerController',
      controllerAs: 'vm',
      bindToController: true,
      require: '^form',
      link: function(scope, element, attrs, formCtrl) {
        scope.$watch(function() {
          return element.find('.available-list label').length;
        }, scope.vm.updateChecksOnFilterHandler(false));

        scope.$watch(function() {
          return element.find('.picked-list label').length;
        }, scope.vm.updateChecksOnFilterHandler(true));

        scope.$watch(function() {
          return formCtrl.$pristine;
        }, function(isPristine) {
          if (isPristine) {
            scope.vm.search = '';
            scope.vm.checkAllRight = false;
            scope.vm.checkAllLeft = false;

            scope.vm.list.forEach(function(item) {
              item.checked = false;
            });
          }
        });
      }
    };
  }

  angular.module('utility').directive('doubleColumnPicker', DoubleColumnPicker);

}(angular));
