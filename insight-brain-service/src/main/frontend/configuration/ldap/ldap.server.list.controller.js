/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
(function(angular) {
  'use strict';

  function LdapServerListController($state, ldapStore, isAuthorized)
  {
    var vm = this;

    vm.doLoad = doLoad;
    vm.ldapList = undefined;
    vm.error = undefined;
    vm.isAuthorized = isAuthorized;

    vm.doLoad();

    function doLoad() {
      if (vm.isAuthorized) {
        delete vm.error;
        ldapStore.get().then(function(results) {
          vm.ldapList = results;
        }, function(error) {
          vm.error = error;
        });
      }
    }
  }

  LdapServerListController.$inject = [
    '$state', 'LdapConfigurationStore', 'isAuthorized'
  ];

  angular//
      .module('ldap.module')//
      .controller('ldap.server.list.controller', LdapServerListController);
}(angular));
