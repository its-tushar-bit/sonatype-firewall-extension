/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { prop, pick, contains, map } from 'ramda';

import template from './manageFilterMenu.html';

var manageFilterMenu = {
  template: template,
  controller: ManageFilterMenuController,
  controllerAs: 'vm',
  bindings: {
    isSaveFilterDisabled: '<',
    onActiveFilterDeleted: '&',
    onFilterSelected: '&',
    onFilterSaved: '&'
  }
};

export default manageFilterMenu;

function mapStateToThis({ manageFilters }) {
  return pick(['appliedFilterName', 'savedFilters', 'savedFilterListError', 'currentlyOpenModal', 'filtersToDelete',
    'pageChangePending'], manageFilters);
}

function ManageFilterMenuController($ngRedux, $scope, SaveFilterModal, DeleteFiltersModal, DeleteModalService,
                                    filterService, manageFiltersActions) {
  const vm = this;

  Object.assign(vm, {
    $onInit() {
      vm.unsubscribe = $ngRedux.connect(mapStateToThis, manageFiltersActions)(vm);

      vm.fetchSavedFilters();
    },

    $onDestroy() {
      vm.unsubscribe();
    },

    openSaveFilterModal($event) {
      if (vm.isSaveFilterDisabled) {
        $event.stopPropagation();
        return;
      }

      SaveFilterModal.open().then(function(name) {
        vm.onFilterSaved({filterName: name});
      }).finally(vm.resetSaveFilterStatus);
    },

    openDeleteFiltersModal($event) {
      const currentSavedFilterName = vm.appliedFilterName;

      if (!vm.hasSavedFilters()) {
        $event.stopPropagation();
        return;
      }

      DeleteFiltersModal.open(vm.savedFilters)
          .then(function(newSavedFilters) {
            // NOTE by the time this executes the manageFilterMenu will be closed and thus
            // won't be subscribed to the redux store anymore
            if (!contains(currentSavedFilterName, map(prop('name'), newSavedFilters))) {
              // legacy - remove when dashboardFilters is redux-ified
              vm.onActiveFilterDeleted();
            }
          })
          .finally(vm.resetDeleteFiltersStatus);
    },

    doApplySavedFilter(savedFilter) {
      // redux
      vm.applySavedFilter(savedFilter);

      // legacy, remove when dashboardFilter is fully redux-ified
      vm.onFilterSelected({ savedFilter: { ...savedFilter } });
    },

    isLoadingSavedFilters() {
      return vm.savedFilters === null && !vm.savedFilterListError;
    },

    hasSavedFilters() {
      return vm.savedFilters !== null && vm.savedFilters.length > 0;
    }
  });
}

ManageFilterMenuController.$inject = [
  '$ngRedux', '$scope', 'saveFilterModal', 'deleteFiltersModal', 'DeleteModalService', 'dashboardFilterService',
  'manageFiltersActions'
];
