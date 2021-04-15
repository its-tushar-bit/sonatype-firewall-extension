/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
export default function WebhookListController($state, WebhookStore, ProductFeatures) {
  var vm = this;
  vm.newWebhook = newWebhook;
  vm.doLoad = doLoad;
  vm.isEventTypeDisabled = isEventTypeDisabled;
  vm.isWebhooksSupported = undefined;

  vm.doLoad();

  function newWebhook() {
    $state.go('webhooks.create');
  }

  function doLoad() {
    WebhookStore[vm.loadError ? 'refresh' : 'get']().then(
      function (results) {
        vm.webhooks = results;
      },
      function (error) {
        vm.loadError = error;
      }
    );

    ProductFeatures.load().then(function () {
      vm.isWebhooksSupported =
        ProductFeatures.isAvailable('webhooks-for-applications') ||
        ProductFeatures.isAvailable('webhooks-for-repositories');
    });

    delete vm.loadError;
  }

  function isEventTypeDisabled(eventType) {
    return eventType === 'Application Evaluation' && !ProductFeatures.isAvailable('webhooks-for-applications');
  }
}

WebhookListController.$inject = ['$state', 'WebhookStore', 'ProductFeatures'];
