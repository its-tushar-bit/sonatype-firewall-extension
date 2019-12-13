/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
/* eslint no-console: "off" */
export default function iqTreeViewMultipleChoiceFilterExampleController() {
  const vm = this;

  vm.reset = function() {
    vm.available = [];

    for (var i = 6; i > 0; i--) {
      vm.available.push({
        id: 'id' + i,
        name: 'app ' + i,
        organizationName: 'org ' + i
      });
    }

    vm.selected = new Set(['id1', 'id2', 'id3']);
  };

  vm.reset();

  vm.onChange = function(selected, toggledId) {
    console.log('onChange', selected, toggledId);

    // update iqTreeViewMultiSelect component with new state
    vm.selected = selected;
  };
}
