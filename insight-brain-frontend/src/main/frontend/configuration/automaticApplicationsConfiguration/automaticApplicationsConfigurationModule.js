/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import * as angular from 'angular';

import automaticApplicationsConfiguration from './automaticApplicationsConfiguration';
import automaticApplicationsConfigurationService from './automaticApplicationsConfigurationService';
import CLMLocationModule from '../../util/CLMLocation';
import permissionServiceModule from '../../util/PermissionService';
import storesModule from '../../util/Stores';

const automaticApplicationsConfigurationModule = angular
  .module('automaticApplicationsConfigurationModule', [
    'ui.router',
    permissionServiceModule.name,
    storesModule.name,
    CLMLocationModule.name,
  ])
  .component('automaticApplicationsConfiguration', automaticApplicationsConfiguration)
  .service('automaticApplicationsConfigurationService', automaticApplicationsConfigurationService)
  .config([
    '$stateProvider',
    function ($stateProvider) {
      $stateProvider.state('automaticApplicationsConfiguration', {
        url: '/automaticApplicationsConfiguration',
        component: 'automaticApplicationsConfiguration',
        data: {
          title: 'Automatic Applications',
        },
        resolve: {
          isAuthorized: [
            'PermissionService',
            function (PermissionService) {
              return PermissionService.isAuthorized(['MANAGE_AUTOMATIC_APPLICATION_CREATION'], true);
            },
          ],
        },
      });
    },
  ]);

export default automaticApplicationsConfigurationModule;
