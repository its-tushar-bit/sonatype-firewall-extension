/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
/* global angular */
(function() {
  'use strict';

  angular.module('dashboard.module', ['ui.router', 'Stores', 'AngularCommon', 'ComponentModule', 'FilterModule',
      'ComponentDisplay'],
  // To avoid hacking dependency order, states must be declared with their parent.
  // Fixed https://github.com/angular-ui/ui-router/pull/492
  ['$stateProvider', function($stateProvider) {

    $stateProvider.state('dashboard-new', {
      url: '/dashboard-new',
      templateUrl: 'dashboard/dashboard.view.html?' + clmBuildTimestamp,
      data: {
        title: 'Dashboard',
        crumb: 'Dashboard'
      }
    });
  }]);
}());
