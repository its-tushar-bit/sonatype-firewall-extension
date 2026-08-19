/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import router from 'MainRoot/router/routerInstance';
import CreateLdapContainer from './CreateLdapContainer';
import EditLdapConnectionContainer from './EditLdapConnectionContainer';
import EditLdapUsermappingContainer from './EditLdapUsermappingContainer';
import LdapListContainer from './ldapServersList/LdapListContainer';

router.stateRegistry.register({
  name: 'create-ldap',
  url: '/ldap/create',
  component: CreateLdapContainer,
  data: {
    title: 'Create LDAP Configuration',
    isDirty: ['ldapConfig', 'isDirty'],
  },
});

router.stateRegistry.register({
  name: 'edit-ldap-connection',
  url: '/ldap/edit/{ldapId}',
  component: EditLdapConnectionContainer,
  data: {
    title: 'Edit LDAP Configuration',
    isDirty: ['ldapConfig', 'isDirty'],
  },
});

router.stateRegistry.register({
  name: 'edit-ldap-usermapping',
  url: '/ldap/edit/{ldapId}/userMapping',
  component: EditLdapUsermappingContainer,
  data: {
    title: 'Edit LDAP Configuration',
    isDirty: ['ldapConfig', 'isDirty'],
  },
});

router.stateRegistry.register({
  name: 'ldap-list',
  url: '/ldap-servers',
  component: LdapListContainer,
  data: {
    title: 'LDAP Servers',
    isDirty: ['ldapList', 'isDirty'],
  },
});
