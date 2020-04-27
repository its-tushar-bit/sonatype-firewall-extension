/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import resourceModule from '../../Resource';
import commonServicesModule from '../../util/CommonServices';
import angularCommonModule from '../../util/AngularCommon';
import CLMLocationModule from '../../util/CLMLocation';
import EditorToolsModule from '../../EditorTools';
import LdapConfigurationStore from './ldap.configuration.store';
import {
  LdapConfigurationController,
  LdapConnectionController,
  LdapUsermappingController,
  LdapCheckUserMappingController,
  LdapCheckLoginController
} from './LdapConfigurationController';
import LdapServerListController from './ldap.server.list.controller';
import {LdapServerOrderingController, LdapServerOrderingModal} from './ldap.server.ordering.controller';

import listTemplate from '../components/ldap.server.list.html';
import editTemplate from '../components/ldap.html';
import connectionTemplate from '../components/ldap-connection.html';
import userMappingTemplate from '../components/ldap-usermapping.html';

export default angular.module('ldap.module', [
  CLMLocationModule.name, resourceModule.name, 'ui.router', angularCommonModule.name, commonServicesModule.name,
  EditorToolsModule.name
], ldapModuleConfiguration)
    .service('LdapConfigurationStore', LdapConfigurationStore)
    .controller('LdapConfigurationController', LdapConfigurationController)
    .controller('LdapConnectionController', LdapConnectionController)
    .controller('LdapUsermappingController', LdapUsermappingController)
    .controller('LdapCheckUserMappingController', LdapCheckUserMappingController)
    .controller('LdapCheckLoginController', LdapCheckLoginController)
    .controller('ldap.server.list.controller', LdapServerListController)
    .controller('LdapServerOrderingController', LdapServerOrderingController)
    .factory('LdapServerOrderingModal', LdapServerOrderingModal);

function ldapModuleConfiguration($stateProvider) {
  $stateProvider.state('ldap-servers', {
    url: '/ldap-servers',
    controller: 'ldap.server.list.controller',
    controllerAs: 'vm',
    template: listTemplate,
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
    template: editTemplate,
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
    template: editTemplate,
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
    template: connectionTemplate
  }).state('edit-ldap.usermapping', {
    controller: 'LdapUsermappingController',
    template: userMappingTemplate
  });
}

ldapModuleConfiguration.$inject = ['$stateProvider'];
