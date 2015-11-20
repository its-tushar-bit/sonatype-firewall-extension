/**
 * @license Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
(function(angular) {
  'use strict';

  function DoubleColumnPickerController($filter) {
    var vm = this;

    vm.checkAll = checkAll;
    vm.checkAllLeft = undefined;
    vm.checkAllRight = undefined;
    vm.listFilter = listFilter;
    vm.areAnyItemsChecked = areAnyItemsChecked;
    vm.moveItems = moveItems;
    vm.search = undefined;
    vm.uncheckTheAllCheckbox = uncheckTheAllCheckbox;
    vm.updateChecksOnFilterHandler = updateChecksOnFilterHandler;

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
  }

  DoubleColumnPickerController.$inject = ['$filter'];

  angular.module('utility').controller('DoubleColumnPickerController', DoubleColumnPickerController);

}(angular));
