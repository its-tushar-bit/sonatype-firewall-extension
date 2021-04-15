/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
export default function LdapConfigurationStore(clmContextLocations, StoreFactory) {
  return StoreFactory.getStore({
    id: 'id',
    url: clmContextLocations.getLdapConfig(),
    template: {
      id: null,
      name: '',
    },
    type: 'LDAP server',
  });
}
LdapConfigurationStore.$inject = ['CLMContextLocations', 'StoreFactory'];
