/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import template from './double.column.picker.directive.html';

export default function DoubleColumnPicker() {
  return {
    restrict: 'E',
    scope: {
      list: '=',
      hideFilter: '@',
      filterPlaceholder: '@',
      leftColumnName: '@',
      rightColumnName: '@',
      itemNameParam: '@',
      disabled: '=?ngDisabled',
      iconFn: '&?',
      tooltipFn: '&?'
    },
    template,
    controller: DoubleColumnPickerController,
    controllerAs: 'vm',
    bindToController: true,
    require: '^form',
    link: DoubleColumnPickerLink
  };

  function DoubleColumnPickerLink(scope, element, attrs, formCtrl) {
    scope.$watch(function() {
      return element.find('.available-list iq-checkbox').length;
    }, scope.vm.updateChecksOnFilterHandler(false));

    scope.$watch(function() {
      return element.find('.picked-list iq-checkbox').length;
    }, scope.vm.updateChecksOnFilterHandler(true));

    scope.$watch(function() {
      return formCtrl.$pristine;
    }, function(isPristine, wasPristine) {
      if (isPristine && !wasPristine) {
        scope.vm.search = {};
        scope.vm.checkAllRight = false;
        scope.vm.checkAllLeft = false;

        scope.vm.list.forEach(function(item) {
          item.checked = false;
        });
      }
    });
  }
}

function DoubleColumnPickerController($filter) {
  var vm = this;

  vm.checkAll = checkAll;
  vm.checkAllLeft = undefined;
  vm.checkAllRight = undefined;
  vm.listFilter = listFilter;
  vm.areAnyItemsChecked = areAnyItemsChecked;
  vm.moveItems = moveItems;
  vm.search = undefined;
  vm.toggleLeftSelectAll = toggleLeftSelectAll;
  vm.toggleRightSelectAll = toggleRightSelectAll;
  vm.toggleChecked = toggleChecked;
  vm.updateChecksOnFilterHandler = updateChecksOnFilterHandler;
  vm.showTooltipOnlyOnOverflow = showTooltipOnlyOnOverflow;

  function checkAll(isPickedList, isChecked) {
    var filteredList = $filter('filter')(vm.list, vm.search);

    vm.list.forEach(function(item) {
      if (Boolean(item.picked) === isPickedList) {
        item.checked = filteredList.indexOf(item) > -1 ? isChecked : false;
      }
    });
  }

  function listFilter(isPickedList) {
    var filteredList = $filter('filter')(vm.list, vm.search);

    return function(item) {
      return Boolean(item.picked) === isPickedList && filteredList.indexOf(item) > -1;
    };
  }

  function areAnyItemsChecked(isPickedList) {
    return $filter('filter')(vm.list, vm.search).some(function(item) {
      return Boolean(item.picked) === isPickedList && item.checked;
    });
  }

  function moveItems(isPickedList) {
    vm.list.forEach(function(item) {
      if (item.checked && Boolean(item.picked) === isPickedList) {
        item.picked = !item.picked;
      }
    });

    if (isPickedList) {
      vm.checkAllRight = false;
    }
    else {
      vm.checkAllLeft = false;
    }
  }

  function toggleLeftSelectAll(isPickedList) {
    vm.checkAllLeft = !vm.checkAllLeft;
    vm.checkAll(isPickedList, vm.checkAllLeft);
  }

  function toggleRightSelectAll(isPickedList) {
    vm.checkAllRight = !vm.checkAllRight;
    vm.checkAll(isPickedList, vm.checkAllRight);
  }

  function toggleChecked(item, uncheckAll) {
    item.checked = !item.checked;
    uncheckTheAllCheckbox(uncheckAll);
  }

  function uncheckTheAllCheckbox(isPickedList) {
    vm[isPickedList ? 'checkAllRight' : 'checkAllLeft'] = false;
  }

  function uncheckFilteredItems(isPickedList) {
    var filteredList = $filter('filter')(vm.list, vm.search);

    vm.list.forEach(function(item) {
      if (Boolean(item.picked) === isPickedList) {
        item.checked = filteredList.indexOf(item) > -1 ? item.checked : false;
      }
    });
  }

  function updateChecksOnFilterHandler(isPickedList) {
    var checkAll = isPickedList ? 'checkAllRight' : 'checkAllLeft';

    return function(newLength, oldLength) {
      if (!oldLength || newLength < oldLength) {
        if (vm[checkAll]) {
          vm.checkAll(isPickedList, true);
        }
        else {
          uncheckFilteredItems(isPickedList);
        }
      }
      else if (newLength > oldLength) {
        vm[checkAll] = false;
      }
    };
  }

  function showTooltipOnlyOnOverflow(item) {
    return !!vm.tooltipFn && (vm.tooltipFn({ item }) === item[vm.itemNameParam]);
  }
}

DoubleColumnPickerController.$inject = ['$filter'];
