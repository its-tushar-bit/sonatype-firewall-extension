/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
(function(angular) {
  'use strict';

  function SortController($attrs) {
    var vm = this;
    vm.setSort = setSort;
    vm.extractSortField = extractSortField;
    vm.sortFields =  $attrs.sort.split(',');

    function setSort(newFields) {
      if (angular.equals(vm.sortFields, newFields)) {
        var column = extractSortField(newFields[0]);
        if (vm.sortFields[0] !== column) {
          vm.sortFields[0] = column;
        }
        else {
          vm.sortFields[0] = '-' + column;
        }
      }
      else {
        vm.sortFields = newFields;
      }
    }

    function extractSortField(orderedField) {
      if (orderedField.indexOf('-') === 0) {
        return orderedField.substring(1);
      }
      else {
        return orderedField;
      }
    }
  }

  SortController.$inject = ['$attrs'];

  angular.module('utility').controller('sort.controller', SortController);

}(angular));
