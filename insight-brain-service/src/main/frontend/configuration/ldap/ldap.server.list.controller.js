/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
(function(angular) {
  'use strict';

  function LdapServerListController($q, ldapStore, isAuthorized, ProductFeatures)
  {
    var vm = this;

    vm.doLoad = doLoad;
    vm.ldapList = undefined;
    vm.error = undefined;
    vm.isAuthorized = isAuthorized;
    vm.isMultipleLdapServersEnabled = ProductFeatures.isMultipleLdapServersEnabled;

    vm.doLoad();

    function doLoad() {
      if (vm.isAuthorized) {
        delete vm.error;
        $q.all([ldapStore.get(), ProductFeatures.load()]).then(function(results) {
          vm.ldapList = results[0];
        }, function(error) {
          vm.error = error;
        });
      }
    }
  }

  LdapServerListController.$inject = [
    '$q', 'LdapConfigurationStore', 'isAuthorized', 'ProductFeatures'
  ];

  angular//
      .module('ldap.module')//
      .controller('ldap.server.list.controller', LdapServerListController);
}(angular));
