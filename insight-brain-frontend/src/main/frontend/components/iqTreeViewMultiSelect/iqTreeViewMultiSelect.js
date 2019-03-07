/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import template from './iqTreeViewMultiSelect.html';

const iqTreeViewMultiSelect = {
  transclude: true,
  template,
  bindings: {
    available: '<',
    selected: '<',
    filterPlaceholder: '@',
    providedFilterThreshold: '<?filterThreshold',
    name: '@',
    tooltipGenerator: '&?',
    sortEntities: '<?',
    onChange: '&',
    isDisabled: '<?',
    disabledTooltip: '@',
    tooltipModifierClass: '@'
  },
  controller: IqTreeViewMultiSelectController,
  controllerAs: 'vm'
};

export default iqTreeViewMultiSelect;

function IqTreeViewMultiSelectController(fuzzyFilter) {
  const vm = this;

  vm.filter = '';
  vm.filterThreshold = 10;
  vm.selected = new Set();

  vm.allSelected = allSelected;
  vm.toggleSelectAll = toggleSelectAll;
  vm.toggle = toggle;
  vm.showFilter = showFilter;
  vm.generateCheckboxId = generateCheckboxId;
  vm.getTooltipText = getTooltipText;
  vm.isComponentDisabled = isComponentDisabled;

  vm.$onChanges = function({selected, providedFilterThreshold}) {
    if (selected) {
      // clone original Set
      vm.selected = new Set(selected.currentValue);
    }

    if (providedFilterThreshold && angular.isNumber(providedFilterThreshold.currentValue)) {
      vm.filterThreshold = providedFilterThreshold.currentValue;
    }

  };

  function isComponentDisabled() {
    return !vm.available.length || vm.isDisabled;
  }

  function getTooltipText() {
    if (!vm.isComponentDisabled()) {
      return '';
    }
    return vm.disabledTooltip || 'There are no ' + vm.name + ' to filter.';
  }

  function showFilter() {
    return vm.available.length > vm.filterThreshold;
  }

  function generateCheckboxId(parentName, elementName) {
    let id = 'iq-tree-view-checkbox-' + parentName + '-' + elementName;
    id = id.replace(' ', '-');
    return id.toLowerCase();
  }

  function toggleSelectAll() {
    if (vm.filter) {
      const selected = new Set(vm.selected);
      const optionsToUpdate = fuzzyFilter(vm.available, vm.filter, 'name');

      if (areOptionsSelected(optionsToUpdate)) {
        optionsToUpdate.forEach(option => selected.delete(option.id));
      }
      else {
        optionsToUpdate.forEach(option => selected.add(option.id));
      }

      vm.onChange({selected});
    }
    else {
      if (areAllAvailableOptionsSelected()) {
        vm.onChange({selected: new Set()});
      }
      else {
        const allIds = vm.available.map(option => option.id);
        vm.onChange({selected: new Set(allIds)});
      }
    }
  }

  function allSelected() {
    if (vm.filter) {
      return areOptionsSelected(fuzzyFilter(vm.available, vm.filter, 'name'));
    }
    else {
      return areAllAvailableOptionsSelected();
    }
  }

  function areAllAvailableOptionsSelected() {
    return vm.available.length === vm.selected.size;
  }

  function areOptionsSelected(options) {
    return !options.some(item => !vm.selected.has(item.id));
  }

  function toggle(id) {
    const selected = new Set(vm.selected);
    if (selected.has(id)) {
      selected.delete(id);
    }
    else {
      selected.add(id);
    }

    vm.onChange({selected, toggledId: id});
  }
}

IqTreeViewMultiSelectController.$inject = ['fuzzyFilter'];
