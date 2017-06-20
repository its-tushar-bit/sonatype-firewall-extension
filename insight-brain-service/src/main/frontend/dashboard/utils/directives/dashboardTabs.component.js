/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

var dashboardTabsComponent = {
  controllerAs: 'vm',
  controller: DashboardTabsController,
  templateUrl: 'dashboard-tabs'
};

function DashboardTabsController(dashboardDataService) {
  var vm = this;

  vm.latestResultCounts = dashboardDataService.latestResultCounts;
}

DashboardTabsController.$inject = ['dashboard.data.service'];

export default dashboardTabsComponent;
