/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
(function() {
  'use strict';

  var dashboardUtilsModule = angular.module('dashboard.utils');

  dashboardUtilsModule.controller('NewestRiskTableController', [
    '$scope', 'StageTypeStore', 'ComponentDisplayNameUtil', '$filter',
    function($scope, StageTypeStore, ComponentDisplayNameUtil, $filter) {
      StageTypeStore.getDashboardStages().then(function(data) {
        $scope.stageTypes = data;
      });
      // to aid sortability:
      // - copy the times from each stage to a property on the row
      // - provide a single sortable string for the component name
      for (var i = 0; i < $scope.data.length; i++) {
        var risk = $scope.data[i];
        if (risk.stageDetails) {
          for (var j = 0; j < risk.stageDetails.length; j++) {
            var stageDetail = risk.stageDetails[j];
            var propName = $filter('removeDashes')(stageDetail.stageTypeId) + 'Time';
            risk[propName] = stageDetail.time > 0 ? stageDetail.time : null;
          }
        }
        risk.gavName = ComponentDisplayNameUtil.deriveComponentName(risk);
      }
    }
  ]);

}());
