/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import {
  translateViolationsSortFields,
  translateComponentsSortFields,
  translateApplicationsSortFields
} from '../services/sortFieldsUtils';
import template from './dashboardResultsContainer.html';

export default {
  template,
  controller: dashboardResultsContainerController,
  controllerAs: 'vm'
};

function dashboardResultsContainerController(createRequest, CLMLocations, $ngRedux) {
  const vm = this;

  Object.assign(vm, {
    $onInit() {
      vm.unsubscribe = $ngRedux.connect(mapStateToThis)(vm);
    },

    $onDestroy() {
      vm.unsubscribe();
    },

    getExportUrl() {
      switch (vm.routeStateName) {
        case 'dashboard.overview.violations':
          return CLMLocations.getNewestRisksExportUrl();

        case 'dashboard.overview.components':
          return CLMLocations.getComponentRisksExportUrl();

        case 'dashboard.overview.applications':
          return CLMLocations.getApplicationRisksExportUrl();

        default:
          throw new Error('Export is not supported for state ' + vm.routeStateName);
      }
    },

    getExportRequestJson() {
      switch (vm.routeStateName) {
        case 'dashboard.overview.violations':
          return JSON.stringify(createRequest(vm.filters, null,
              translateViolationsSortFields(vm.violationsSortFields)));

        case 'dashboard.overview.components':
          return JSON.stringify(createRequest(vm.filters, null,
              translateComponentsSortFields(vm.componentsSortFields)));

        case 'dashboard.overview.applications':
          return JSON.stringify(createRequest(vm.filters, null,
              translateApplicationsSortFields(vm.applicationsSortFields)));

        default:
          throw new Error('Export is not supported for state ' + vm.routeStateName);
      }
    }
  });
}

dashboardResultsContainerController.$inject = [
  'createDashboardDataRequestPayload', 'CLMLocations', '$ngRedux'
];

// Which part of the Redux global state does our component want to receive?
function mapStateToThis(state) {
  return {
    filters: state.dashboardFilter.appliedFilter,
    title: state.router.currentState.data.title,
    exportTitle: state.router.currentState.data.exportTitle,
    routeStateName: state.router.currentState.name,
    applicationsSortFields: state.dashboard.applications.sortFields,
    componentsSortFields: state.dashboard.components.sortFields,
    violationsSortFields: state.dashboard.violations.sortFields
  };
}
