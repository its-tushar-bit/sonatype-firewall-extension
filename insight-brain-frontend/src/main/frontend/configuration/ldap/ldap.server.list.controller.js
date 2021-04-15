/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
export default function LdapServerListController(
  ldapStore,
  isAuthorized,
  LdapServerOrderingModal
) {
  var vm = this;

  vm.doLoad = doLoad;
  vm.ldapList = undefined;
  vm.error = undefined;
  vm.isAuthorized = isAuthorized;
  vm.reorder = reorder;

  vm.doLoad();

  function doLoad() {
    if (vm.isAuthorized) {
      delete vm.error;
      handleStoreLoad(ldapStore.get());
    }
  }

  function handleStoreLoad(promise) {
    promise.then(
      function (results) {
        vm.ldapList = results;
      },
      function (error) {
        vm.error = error;
      }
    );
  }

  function reorder() {
    LdapServerOrderingModal.open().then(function () {
      vm.ldapList = undefined;
      handleStoreLoad(ldapStore.refresh());
    });
  }
}

LdapServerListController.$inject = [
  'LdapConfigurationStore',
  'isAuthorized',
  'LdapServerOrderingModal',
];
