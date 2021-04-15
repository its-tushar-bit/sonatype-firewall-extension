/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
/**
 * "sort" directive is used together with children "sortColumn" directives.
 * It holds current sortFields, used by children "sortColumn" directives to determine the state of sort arrows.
 * When one of the sortColumns is clicked to change sort order, this directive will call onSortChange callback
 * with new fields.
 * Note, this directive is one-directionally bounded - it only updates its state through "sort" attribute.
 *
 * Attributes:
 * sort: [String] array of current sortFields
 * on-sort-change: callback to notify change to current sortFields. Provided context: {sortFields: [String]}
 *
 * Example: <table sort="vm.sortFields" on-sort-change="vm.sortFields = sortFields">
 */
export default function Sort() {
  return {
    restrict: 'A',
    controller: 'sort.controller',
    controllerAs: 'vm',
    bindToController: true,
    scope: {
      sortFields: '<sort',
      onSortChange: '&',
    },
  };
}
