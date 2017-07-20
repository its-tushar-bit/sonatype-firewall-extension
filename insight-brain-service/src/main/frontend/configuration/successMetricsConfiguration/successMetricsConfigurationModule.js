/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved. Includes the third-party code listed at
 * http://links.sonatype.com/products/clm/attributions. "Sonatype" is a trademark of Sonatype, Inc.
 */
/* global angular */

import successMetricsConfiguration from './successMetricsConfiguration';

var successMetricsConfigurationModule = angular.module('successMetricsConfigurationModule',
    ['ui.router', 'utility.services'])
    .component('successMetricsConfiguration', successMetricsConfiguration)
    .config([
      '$stateProvider', function($stateProvider) {
        $stateProvider.state('successMetricsConfiguration', {
          url: '/successMetricsConfiguration',
          template: '<success-metrics-configuration></success-metrics-configuration>',
          data: {
            title: 'Success Metrics'
          }
        });
      }
    ]);

export default successMetricsConfigurationModule;
