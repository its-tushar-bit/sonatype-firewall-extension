/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

import successMetricsModule from './successMetrics/module';

export default angular.module('labsModule', [successMetricsModule.name])
    .config(configureRoutes);

function configureRoutes($stateProvider) {
  $stateProvider
      .state('labs', {
        abstract: true,
        url: '/labs',
        template: '<div id="labs" class="body-container body-container--labs" fill-vertical><ui-view/></div>'
      })
      .state('labs.successMetrics', {
        url: '/successMetrics',
        template: '<success-metrics-list></success-metrics-list>',
        data: {
          title: 'Success Metrics'
        }
      })
      .state('labs.successMetricsChart', {
        url: '/successMetrics/:successMetricsId',
        component: 'successMetricsChartPage',
        data: {
          title: 'Success Metrics Chart Page'
        }
      });
}

configureRoutes.$inject = ['$stateProvider'];
