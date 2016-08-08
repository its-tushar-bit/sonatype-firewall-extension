/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
(function() {
  'use strict';

  function LdapConfigurationStore(clmLocations, StoreFactory) {
    return StoreFactory.getStore({
      id: 'id',
      url: clmLocations.getLdapConfig(),
      template: {
        id: null,
        name: ''
      }
    });
  }
  LdapConfigurationStore.$inject = ['CLMLocations', 'StoreFactory'];

  angular.module('ldap.module').service('LdapConfigurationStore', LdapConfigurationStore);
}());
