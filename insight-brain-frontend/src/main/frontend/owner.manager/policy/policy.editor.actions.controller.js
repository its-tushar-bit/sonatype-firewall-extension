/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
export default function PolicyEditorActionsController(
  $q,
  StageTypeStore,
  ProductFeatures
) {
  var vm = this;

  vm.doLoad = doLoad;
  vm.actionStages = undefined;
  vm.loadError = undefined;
  vm.isEnforcementSupported = undefined;
  vm.isFirewallSupported = undefined;
  vm.isEnforcementSupportedForStage =
    ProductFeatures.isEnforcementSupportedForStage;

  vm.doLoad();

  function doLoad() {
    const promises = [StageTypeStore.getActionStages(), ProductFeatures.load()];

    $q.all(promises).then(
      function (results) {
        vm.actionStages = results[0];

        vm.isEnforcementSupported = ProductFeatures.isAvailable('enforcement');
        vm.isFirewallSupported = ProductFeatures.isAvailable('firewall');
      },
      function (error) {
        vm.loadError = error;
      }
    );

    delete vm.loadError;
  }
}

PolicyEditorActionsController.$inject = [
  '$q',
  'StageTypeStore',
  'ProductFeatures',
];
