/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import iqReact2Angular from 'MainRoot/reactAdapter/iqReact2Angular';

import commonServicesModule from '../../utilAngular/CommonServices';
import CLMLocationModule from '../../util/CLMLocation';
import CreateLdapContainer from '../ldap/CreateLdapContainer';
import EditLdapConnectionContainer from '../ldap/EditLdapConnectionContainer';
import EditLdapUsermappingContainer from '../ldap/EditLdapUsermappingContainer';
import LdapListContainer from './ldapServersList/LdapListContainer';

export default angular
  .module('ldap.module', [CLMLocationModule.name, 'ui.router', commonServicesModule.name], ldapModuleConfiguration)
  .component('ldapList', iqReact2Angular(LdapListContainer, [], ['$ngRedux', '$state']))
  .component('createLdap', iqReact2Angular(CreateLdapContainer, [], ['$ngRedux', '$state']))
  .component('editLdapConnection', iqReact2Angular(EditLdapConnectionContainer, [], ['$ngRedux', '$state']))
  .component('editLdapUserMapping', iqReact2Angular(EditLdapUsermappingContainer, [], ['$ngRedux', '$state']));

function ldapModuleConfiguration($stateProvider) {
  $stateProvider
    .state('create-ldap', {
      url: '/ldap/create',
      component: 'createLdap',
      data: {
        title: 'Create LDAP Configuration',
        isDirty: ['ldapConfig', 'isDirty'],
      },
    })
    .state('edit-ldap-connection', {
      url: '/ldap/edit/{ldapId}',
      component: 'editLdapConnection',
      data: {
        title: 'Edit LDAP Configuration',
        isDirty: ['ldapConfig', 'isDirty'],
      },
    })
    .state('edit-ldap-usermapping', {
      url: '/ldap/edit/{ldapId}/userMapping',
      component: 'editLdapUserMapping',
      data: {
        title: 'Edit LDAP Configuration',
        isDirty: ['ldapConfig', 'isDirty'],
      },
    })
    .state('ldap-list', {
      url: '/ldap-servers',
      component: 'ldapList',
      data: {
        title: 'LDAP Servers',
        isDirty: ['ldapList', 'isDirty'],
      },
    });
}

ldapModuleConfiguration.$inject = ['$stateProvider'];
