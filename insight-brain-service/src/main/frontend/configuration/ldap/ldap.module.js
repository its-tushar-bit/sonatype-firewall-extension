/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved. Includes the third-party code listed at
 * http://links.sonatype.com/products/clm/attributions. "Sonatype" is a trademark of Sonatype, Inc.
 */
(function() {
  'use strict';

  function ldapModuleConfiguration($stateProvider) {
    $stateProvider.state('ldap', {
      url: '/ldap/edit/{ldapId}',
      controller: 'LdapConfigurationController',
      templateUrl: 'configuration/components/ldap.html?' + clmBuildTimestamp,
      data: {
        title: 'Edit LDAP Configuration'
      },
      resolve: {
        'isAuthorized': ['PermissionService', function(PermissionService) {
          return PermissionService.isAuthorized(['CONFIGURE_SYSTEM'], true);
        }]
      }
    }).state('create-ldap', {
      url: '/ldap/create',
      controller: 'LdapConfigurationController',
      templateUrl: 'configuration/components/ldap.html?' + clmBuildTimestamp,
      data: {
        title: 'Create LDAP Configuration'
      },
      resolve: {
        'isAuthorized': ['PermissionService', function(PermissionService) {
          return PermissionService.isAuthorized(['CONFIGURE_SYSTEM'], true);
        }]
      }
    }).state('ldap.connection', {
      parent: 'ldap',
      controller: 'LdapConnectionController',
      templateUrl: 'configuration/components/ldap-connection.html?' + clmBuildTimestamp
    }).state('ldap.usermapping', {
      parent: 'ldap',
      controller: 'LdapUsermappingController',
      templateUrl: 'configuration/components/ldap-usermapping.html?' + clmBuildTimestamp
    });
  }

  ldapModuleConfiguration.$inject = ['$stateProvider'];

  angular.module('ldap.module', ['CLMLocation', 'ResourceModule', 'ui.router', 'AngularCommon', 'CommonServices',
      'EditorTools'], ldapModuleConfiguration);
}());
