/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { react2angular } from 'react2angular';
import withStoreProvider from '../../reactAdapter/StoreProvider';
import withRouterStateProvider from '../../reactAdapter/RouterStateProvider';

import resourceModule from '../../Resource';
import commonServicesModule from '../../util/CommonServices';
import angularCommonModule from '../../util/AngularCommon';
import CLMLocationModule from '../../util/CLMLocation';
import EditorToolsModule from '../../EditorTools';
import BootstrapAddonsModule from '../../util/BootstrapAddonsModule';
import CreateLdapContainer from '../ldap/CreateLdapContainer';
import EditLdapConnectionContainer from '../ldap/EditLdapConnectionContainer';
import EditLdapUsermappingContainer from '../ldap/EditLdapUsermappingContainer';
import LdapListContainer from './ldapServersList/LdapListContainer';

export default angular
  .module(
    'ldap.module',
    [
      CLMLocationModule.name,
      resourceModule.name,
      'ui.router',
      angularCommonModule.name,
      commonServicesModule.name,
      EditorToolsModule.name,
      BootstrapAddonsModule.name,
    ],
    ldapModuleConfiguration
  )
  .component(
    'ldapList',
    react2angular(withStoreProvider(withRouterStateProvider(LdapListContainer)), [], ['$ngRedux', '$state'])
  )
  .component(
    'createLdap',
    react2angular(withStoreProvider(withRouterStateProvider(CreateLdapContainer)), [], ['$ngRedux', '$state'])
  )
  .component(
    'editLdapConnection',
    react2angular(withStoreProvider(withRouterStateProvider(EditLdapConnectionContainer)), [], ['$ngRedux', '$state'])
  )
  .component(
    'editLdapUserMapping',
    react2angular(withStoreProvider(withRouterStateProvider(EditLdapUsermappingContainer)), [], ['$ngRedux', '$state'])
  );

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
