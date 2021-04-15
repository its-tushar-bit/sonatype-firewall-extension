/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import * as angular from 'angular';

import automaticSourceControlConfiguration from './automaticSourceControlConfiguration';
import automaticSourceControlConfigurationService from './automaticSourceControlConfigurationService';
import CLMLocationModule from '../../util/CLMLocation';
import permissionServiceModule from '../../util/PermissionService';
import storesModule from '../../util/Stores';

const automaticSourceControlConfigurationModule = angular
  .module('automaticSourceControlConfigurationModule', [
    'ui.router',
    permissionServiceModule.name,
    storesModule.name,
    CLMLocationModule.name,
  ])
  .component('automaticSourceControlConfiguration', automaticSourceControlConfiguration)
  .service('automaticSourceControlConfigurationService', automaticSourceControlConfigurationService)
  .config([
    '$stateProvider',
    function ($stateProvider) {
      $stateProvider.state('automaticSourceControlConfiguration', {
        url: '/automaticSourceControlConfiguration',
        component: 'automaticSourceControlConfiguration',
        data: {
          title: 'Automatic Source Control Configuration',
        },
        resolve: {
          isAuthorized: [
            'PermissionService',
            function (PermissionService) {
              return PermissionService.isAuthorized(['MANAGE_AUTOMATIC_SCM_CONFIGURATION'], true);
            },
          ],
        },
      });
    },
  ]);

export default automaticSourceControlConfigurationModule;
