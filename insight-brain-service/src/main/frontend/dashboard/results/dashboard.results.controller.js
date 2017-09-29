/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
export default
function DashboardResultsController($scope, Modal, EventNameConstant, $state, createDashboardDataRequestPayload, CLMLocations) {
  $scope.maxResults = 100;
  $scope.maxDaysOld = 30;
  $scope.showTrendDialog = showTrendDialog;
  $scope.getViewTitle = getViewTitle;
  $scope.getExportUrl = getExportUrl;
  $scope.getFilterJson = getFilterJson;
  $scope.reload = reload;

  $scope.filters = undefined;
  $scope.filtersAreDirty = undefined;

  $scope.$on(EventNameConstant.UPDATE_DASHBOARD_FILTERS, function(e, newFilters, needsAcknowledgement) {
    $scope.filters = newFilters;
    $scope.maxDaysOld = newFilters.maxDaysOld;
    $scope.needsAcknowledgement = needsAcknowledgement;
  });

  /*
   * Listen for dirtiness changes and set a scope property that children (the three dashboard tables) can listen to.
   * The event listening is done here, instead of in the children themselves, because the child tables don't get
   * created until their tab is clicked on, and they would therefore miss instances of this event that fired
   * before they existed
   */
  $scope.$on(EventNameConstant.UPDATE_DASHBOARD_FILTERS_DIRTINESS, function(e, filtersAreDirty) {
    $scope.filtersAreDirty = filtersAreDirty;
  });

  function showTrendDialog() {
    Modal.open({
      backdrop: 'static',
      keyboard: false,
      templateUrl: 'policy-trends-dialog-template',
      controller: 'PolicyTrendController',
      resolve: {
        filters: function() {
          return $scope.filters;
        }
      }
    });
  }

  function getViewTitle() {
    return $state.current.data.title;
  }

  function getExportUrl() {
    switch ($state.current.name) {
      case 'dashboard.overview.violations':
        return CLMLocations.getNewestRisksExportUrl();

      case 'dashboard.overview.components':
        return CLMLocations.getComponentRisksExportUrl();

      case 'dashboard.overview.applications':
        return CLMLocations.getApplicationRisksExportUrl();

      default:
        throw new Error('Export is not supported for state ' + $state.current.name);
    }
  }

  function getFilterJson() {
    var filterJson = createDashboardDataRequestPayload($scope.filters);
    return JSON.stringify(filterJson);
  }

  function reload() {
    $scope.filters = angular.copy($scope.filters);
  }
}

DashboardResultsController.$inject = [
  '$scope', 'Modal', 'event.name.constant', '$state', 'createDashboardDataRequestPayload', 'CLMLocations'
];
