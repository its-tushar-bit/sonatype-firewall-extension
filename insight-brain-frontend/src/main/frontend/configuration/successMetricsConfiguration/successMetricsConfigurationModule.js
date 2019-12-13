/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
/* global angular */

import successMetricsConfiguration from './successMetricsConfiguration';
import utilityServicesModule from '../../utility/services/utility.services.module';
import permissionServiceModule from '../../util/PermissionService';

var successMetricsConfigurationModule = angular.module('successMetricsConfigurationModule',
    ['ui.router', utilityServicesModule.name, permissionServiceModule.name])
    .component('successMetricsConfiguration', successMetricsConfiguration)
    .config([
      '$stateProvider', function($stateProvider) {
        $stateProvider.state('successMetricsConfiguration', {
          url: '/successMetricsConfiguration',
          component: 'successMetricsConfiguration',
          data: {
            title: 'Success Metrics'
          },
          resolve: {
            isAuthorized: ['PermissionService', function(PermissionService) {
              return PermissionService.isAuthorized(['CONFIGURE_SYSTEM'], true);
            }]
          }
        });
      }
    ]);

export default successMetricsConfigurationModule;
