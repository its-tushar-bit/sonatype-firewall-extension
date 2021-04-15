/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import template from './dashboardTabs.html';

var dashboardTabsComponent = {
  controllerAs: 'vm',
  controller: DashboardTabsController,
  template,
};

function DashboardTabsController($ngRedux) {
  const vm = this;

  Object.assign(vm, {
    $onInit() {
      vm.unsubscribe = $ngRedux.connect(mapStateToThis)(vm);
    },

    $onDestroy() {
      vm.unsubscribe();
    },
  });
}

function mapStateToThis(state) {
  return {
    violationsCount: state.dashboard.violations.numResults,
    componentsCount: state.dashboard.components.numResults,
    applicationsCount: state.dashboard.applications.numResults,
  };
}

DashboardTabsController.$inject = ['$ngRedux'];

export default dashboardTabsComponent;
