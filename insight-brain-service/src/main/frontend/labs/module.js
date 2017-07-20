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
        template: '<success-metrics></success-metrics>',
        data: {
          title: 'Success Metrics'
        }
      })
      .state('labs.rootOrg', {
        url: '/successMetrics/root-org',
        template: '<root-organization></root-organization>',
        data: {
          title: 'Success Metrics Root Organization'
        }
      });
}

configureRoutes.$inject = ['$stateProvider'];
