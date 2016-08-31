/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
(function() {
  'use strict';

  // placeholder - will retrieve tab counts when dashboard summary is fixed

  function dashboardTabs() {
    return {
      restrict: 'A',
      templateUrl: 'dashboard-tabs'
    };
  }

  angular //
      .module('dashboard.utils') //
      .directive('dashboardTabs', dashboardTabs);

}());
