/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
function ManageFilterMenu($http, CLMLocations, SaveFilterModal, DeleteFiltersModal, filterService) {
  var vm = this;

  vm.savedFiltersHasError = false;
  vm.savedNamedFilters = null;

  vm.applySavedFilter = applySavedFilter;
  vm.openSaveFilterModal = openSaveFilterModal;
  vm.openDeleteFiltersModal = openDeleteFiltersModal;
  vm.isLoadingSavedFilters = isLoadingSavedFilters;
  vm.hasSavedFilters = hasSavedFilters;
  vm.$onInit = load;

  function load() {
    vm.savedFiltersHasError = false;

    return getSavedFilters().then(function(data) {
      vm.savedNamedFilters = data;
    }, function() {
      vm.savedFiltersHasError = true;
    });
  }

  /**
   * move this to dashboard.filter.service
   * @returns Promise resolving to array of saved filters
   */
  function getSavedFilters() {
    return $http.get(CLMLocations.getDashboardSavedFilters()).then(function(response) {
      return response.data;
    });
  }

  function openSaveFilterModal($event) {
    if (vm.isSaveFilterDisabled) {
      $event.stopPropagation();
      return;
    }
    SaveFilterModal.open(filterService.filterToJson(vm.currentFilter), vm.activeFilterName, vm.savedNamedFilters).then(function(name) {
      vm.onFilterSaved({filterName: name});
    });
  }

  function openDeleteFiltersModal($event) {
    if (!vm.hasSavedFilters()) {
      $event.stopPropagation();
      return;
    }

    DeleteFiltersModal.open(vm.savedNamedFilters).finally(function() {
      getSavedFilters().then(function(savedNamedFilters) {
        // see if the active filter was deleted
        var deletedActiveFilter = !savedNamedFilters.some(function(filter) {
          return filter.name === vm.activeFilterName;
        });

        if (deletedActiveFilter) {
          vm.onActiveFilterDeleted();
        }
      });
    });
  }

  function applySavedFilter(savedFilter) {
    vm.onFilterSelected({savedFilter: savedFilter});
  }

  function isLoadingSavedFilters() {
    return vm.savedNamedFilters === null && !vm.savedFiltersHasError;
  }

  function hasSavedFilters() {
    return vm.savedNamedFilters !== null && vm.savedNamedFilters.length > 0;
  }

}

ManageFilterMenu.$inject = [
  '$http', 'CLMLocations', 'save.filter.modal', 'delete.filters.modal', 'dashboard.filter.service'
];

var manageFilterMenuComponent = {
  templateUrl: 'dashboard/manage.filter.menu/manage.filter.menu.html',
  controller: ManageFilterMenu,
  controllerAs: 'vm',
  bindings: {
    activeFilterName: '<',
    isSaveFilterDisabled: '<',
    currentFilter: '<',
    onActiveFilterDeleted: '&',
    onFilterSelected: '&',
    onFilterSaved: '&'
  }
};

export default manageFilterMenuComponent;
