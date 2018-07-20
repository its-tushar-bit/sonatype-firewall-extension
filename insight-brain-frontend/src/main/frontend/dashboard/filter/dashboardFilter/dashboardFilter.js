/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import template from './dashboardFilter.html';
import {pick} from 'ramda';

var dashboardFilter = {
  template: template,
  controller: DashboardFilterController,
  controllerAs: 'vm'
};

export default dashboardFilter;

function DashboardFilterController(filterService, $ngRedux, actions) {
  const vm = this;

  Object.assign(vm, {

    isManageFiltersDropdownOpen: false,

    $onInit() {
      vm.unsubscribe = $ngRedux.connect(mapStateToThis, actions)(vm);
      vm.loadFilter();
    },

    $onDestroy() {
      vm.unsubscribe();
    },

    applyCurrentFilter() {
      if (!vm.filtersAreDirty && !vm.needsAcknowledgement) {
        return;
      }
      vm.applyFilter(filterService.filterToJson(vm.selected), vm.appliedFilterName);
    },

    toggleManageFiltersDropdown(open) {
      vm.isManageFiltersDropdownOpen = open;
    }
  });
}

// Which part of the Redux global state does our component want to receive?
function mapStateToThis({dashboardFilter, manageFilters}) {
  return {...dashboardFilter, ...pick(['appliedFilterName', 'showDirtyAsterisk'], manageFilters)};
}

DashboardFilterController.$inject = ['dashboardFilterService', '$ngRedux', 'dashboardFilterActions'];
