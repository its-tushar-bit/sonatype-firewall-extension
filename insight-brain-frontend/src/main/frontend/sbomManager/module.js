/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

import SbomManagerDashboard from 'MainRoot/sbomManager/features/dashboard/SbomManagerDashboard';
import iqReact2Angular from 'MainRoot/reactAdapter/iqReact2Angular';
import advancedSearchModule from 'MainRoot/advancedSearch/module';

export default angular
  .module('sbomManagerModule', ['ngRedux', advancedSearchModule.name])
  .component('sbomManagerDashboard', iqReact2Angular(SbomManagerDashboard, [], ['$ngRedux', '$state']))
  .config(routes);

function routes($stateProvider) {
  $stateProvider
    .state('sbomManager', {
      url: '/sbomManager',
      template: '<ui-view></ui-view>',
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
    })
    .state('sbomManager.advancedSearch', {
      url: '/advancedSearch',
      component: 'advancedSearch',
      data: {
        title: 'SBOM Manager - Advanced Search',
        authenticationRequired: true,
      },
    });
}

routes.$inject = ['$stateProvider'];
