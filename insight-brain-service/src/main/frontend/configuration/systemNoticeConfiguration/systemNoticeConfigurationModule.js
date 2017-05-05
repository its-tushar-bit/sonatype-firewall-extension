/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved. Includes the third-party code listed at
 * http://links.sonatype.com/products/clm/attributions. "Sonatype" is a trademark of Sonatype, Inc.
 */
(function() {
  'use strict';

  function systemNoticeConfigurationModuleConfiguration($stateProvider) {
    $stateProvider.state('systemNotice', {
      url: '/systemNotice',
      controller: 'systemNoticeConfigurationController',
      controllerAs: 'vm',
      templateUrl: 'configuration/systemNoticeConfiguration/systemNoticeConfiguration.html',
      data: {
        title: 'System Notice'
      },
      resolve: {
        'isAuthorized': [
          'PermissionService', function(PermissionService) {
            return PermissionService.isAuthorized(['CONFIGURE_SYSTEM'], true);
          }
        ]
      }
    });
  }

  systemNoticeConfigurationModuleConfiguration.$inject = ['$stateProvider'];

  angular.module('systemNoticeConfigurationModule', [], systemNoticeConfigurationModuleConfiguration);
}());
