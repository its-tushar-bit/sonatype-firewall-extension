/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

import successMetricsModule from './successMetrics/module';

export default angular.module('labsModule', [successMetricsModule.name]).config(configureRoutes);

function configureRoutes($stateProvider) {
  $stateProvider
    .state('labs', {
      abstract: true,
      url: '/labs',
      template: `<main id="labs" class="nx-page-main">
          <ui-view></ui-view>
        </main>`,
    })
    .state('labs.successMetrics', {
      url: '/successMetrics',
      component: 'successMetricsReportList',
      data: {
        title: 'Success Metrics',
      },
    })
    .state('labs.successMetricsReport', {
      url: '/successMetrics/:successMetricsReportId',
      component: 'successMetricsReport',
      data: {
        title: 'Success Metrics Report',
      },
    });
}

configureRoutes.$inject = ['$stateProvider'];
