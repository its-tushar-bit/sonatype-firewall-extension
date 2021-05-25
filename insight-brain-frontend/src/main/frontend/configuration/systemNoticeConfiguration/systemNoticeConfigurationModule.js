/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
/* global angular */

import systemNoticeConfiguration from './systemNoticeConfiguration';
import utilityServicesModule from '../../utility/services/utility.services.module';
import permissionServiceModule from '../../util/PermissionService';

var systemNoticeConfigurationModule = angular
  .module('systemNoticeConfigurationModule', ['ui.router', utilityServicesModule.name, permissionServiceModule.name])
  .component('systemNoticeConfiguration', systemNoticeConfiguration)
  .config([
    '$stateProvider',
    function ($stateProvider) {
      $stateProvider.state('systemNoticeConfiguration', {
        url: '/systemNoticeConfiguration',
        component: 'systemNoticeConfiguration',
        data: {
          title: 'System Notice',
        },
        resolve: {
          isAuthorized: [
            'PermissionService',
            function (PermissionService) {
              return PermissionService.isAuthorized(['CONFIGURE_SYSTEM'], true);
            },
          ],
        },
      });
    },
  ]);

export default systemNoticeConfigurationModule;
