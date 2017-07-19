/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved. Includes the third-party code listed at
 * http://links.sonatype.com/products/clm/attributions. "Sonatype" is a trademark of Sonatype, Inc.
 */
(function() {
  'use strict';

  function ldapModuleConfiguration($stateProvider) {
    $stateProvider.state('ldap-servers', {
      url: '/ldap-servers',
      controller: 'ldap.server.list.controller',
      controllerAs: 'vm',
      templateUrl: 'configuration/components/ldap.server.list.html?' + clmBuildTimestamp,
      data: {
        title: 'LDAP Servers'
      },
      resolve: {
        'isAuthorized': ['PermissionService', function(PermissionService) {
          return PermissionService.isAuthorized(['CONFIGURE_SYSTEM'], true);
        }]
      }
    }).state('edit-ldap', {
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
    }).state('edit-ldap.connection', {
      controller: 'LdapConnectionController',
      templateUrl: 'configuration/components/ldap-connection.html?' + clmBuildTimestamp
    }).state('edit-ldap.usermapping', {
      controller: 'LdapUsermappingController',
      templateUrl: 'configuration/components/ldap-usermapping.html?' + clmBuildTimestamp
    });
  }

  ldapModuleConfiguration.$inject = ['$stateProvider'];

  angular.module('ldap.module', [
    'CLMLocation', 'ResourceModule', 'ui.router', 'AngularCommon', 'CommonServices',
    'EditorTools'
  ], ldapModuleConfiguration);
}());
