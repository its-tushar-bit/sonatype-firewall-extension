/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
export default function WebhookListController($state, WebhookStore) {
  var vm = this;
  vm.newWebhook = newWebhook;
  vm.doLoad = doLoad;

  vm.doLoad();

  function newWebhook() {
    $state.go('webhooks.create');
  }

  function doLoad() {
    WebhookStore[vm.loadError ? 'refresh' : 'get']().then(function(results) {
      vm.webhooks = results;
    }, function(error) {
      vm.loadError = error;
    });

    delete vm.loadError;
  }
}

WebhookListController.$inject = [
  '$state', 'WebhookStore'
];
