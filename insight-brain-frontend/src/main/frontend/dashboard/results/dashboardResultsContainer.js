/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import template from './dashboardResultsContainer.html';
import { DEFAULT_FILTER_NAME } from '../filter/defaultFilter';
import { loadFilter, toggleFilterSidebar } from '../filter/dashboardFilterActions';

export default {
  template,
  controller: dashboardResultsContainerController,
  controllerAs: 'vm'
};

function dashboardResultsContainerController(createRequest, CLMLocations, $ngRedux) {
  const vm = this;

  Object.assign(vm, {
    $onInit() {
      vm.unsubscribe = $ngRedux.connect(mapStateToThis, { toggleFilterSidebar, loadFilter })(vm);
      vm.loadFilter();
    },

    $onDestroy() {
      vm.unsubscribe();
    },

    isFilterLoaded() {
      return !vm.filterLoading && !vm.loadFilterError;
    },

    DEFAULT_FILTER_NAME
  });
}

dashboardResultsContainerController.$inject = [
  'createDashboardDataRequestPayload', 'CLMLocations', '$ngRedux'
];

// Which part of the Redux global state does our component want to receive?
function mapStateToThis(state) {
  return {
    appliedFilterName: state.manageFilters.appliedFilterName,
    showDirtyAsterisk: state.manageFilters.showDirtyAsterisk,
    filterSidebarOpen: state.dashboardFilter.filterSidebarOpen,
    filters: state.dashboardFilter.appliedFilter,
    filterLoading: state.dashboardFilter.loading,
    loadFilterError: state.dashboardFilter.loadError,
    title: state.router.currentState.data.title,
    exportTitle: state.router.currentState.data.exportTitle,
    routeStateName: state.router.currentState.name,
    applicationsSortFields: state.dashboard.applications.sortFields,
    componentsSortFields: state.dashboard.components.sortFields,
    violationsSortFields: state.dashboard.violations.sortFields
  };
}
