/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

import SbomManagerDashboard from 'MainRoot/sbomManager/features/dashboard/SbomManagerDashboard';
import iqReact2Angular from 'MainRoot/reactAdapter/iqReact2Angular';

export default angular
  .module('sbomManagerModule', ['ngRedux'])
  .component('sbomManagerDashboard', iqReact2Angular(SbomManagerDashboard, [], ['$ngRedux', '$state']))
  .config(routes);

function routes($stateProvider) {
  $stateProvider
    .state('sbomManager', {
      url: '/sbomManager',
      component: 'sbomManagerDashboard',
      data: {
        title: 'SBOM Manager',
        authenticationRequired: true,
      },
    })
    .state('sbomManager.dashboard', {
      url: '/dashboard',
      component: 'sbomManagerDashboard',
      data: {
        title: 'SBOM Manager - Dashboard',
        authenticationRequired: true,
      },
    });
}

routes.$inject = ['$stateProvider'];
