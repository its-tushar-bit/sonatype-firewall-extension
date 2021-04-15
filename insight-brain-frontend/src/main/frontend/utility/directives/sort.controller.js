/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
export default function SortController() {
  var vm = this;
  vm.setSort = setSort;
  vm.extractSortField = extractSortField;

  function setSort(newFields) {
    if (angular.equals(vm.sortFields, newFields)) {
      var column = extractSortField(newFields[0]);
      if (vm.sortFields[0] !== column) {
        vm.onSortChange({ sortFields: [column, ...vm.sortFields.slice(1)] });
      } else {
        vm.onSortChange({
          sortFields: ['-' + column, ...vm.sortFields.slice(1)],
        });
      }
    } else {
      vm.onSortChange({ sortFields: newFields });
    }
  }

  function extractSortField(orderedField) {
    if (orderedField && orderedField.indexOf('-') === 0) {
      return orderedField.substring(1);
    } else {
      return orderedField;
    }
  }
}
