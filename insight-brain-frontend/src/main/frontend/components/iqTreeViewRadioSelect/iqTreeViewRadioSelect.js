/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { find, propEq } from 'ramda';

import template from './iqTreeViewRadioSelect.html';

/**
 * @name iqTreeViewRadioSelect
 *
 * @param available array of all available options: [{id:Any, name:String}]
 * @param selectedId ID of selected option
 * @param readOnly if true, renders collapsed and disabled tree view (defaults to 'false')
 * @param onChange callback expression - called with the id of the selected option. Context: {selected:Any}
 */
const iqTreeViewRadioSelect = {
  transclude: true,
  template: template,
  bindings: {
    available: '<',
    selectedId: '<',
    readOnly: '<?',
    onChange: '&',
  },
  controller: IqTreeViewRadioSelectController,
  controllerAs: 'vm',
};

export default iqTreeViewRadioSelect;

function IqTreeViewRadioSelectController() {
  var vm = this;

  Object.assign(vm, {
    getSelectedName() {
      const selectedEntry = find(propEq('id', vm.selectedId), vm.available);
      return selectedEntry && selectedEntry.name;
    },

    select(id) {
      vm.onChange({ selected: id });
    },

    isSelected(id) {
      return vm.selectedId === id;
    },

    shouldShowSelected() {
      return vm.selectedId !== undefined;
    },
  });
}
