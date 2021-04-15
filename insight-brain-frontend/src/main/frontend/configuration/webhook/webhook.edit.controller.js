/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
export default function WebhookEditController(
  $q,
  $scope,
  $http,
  $stateParams,
  $state,
  CLMLocations,
  WebhookStore,
  DeleteModalService,
  ProductFeatures
) {
  var vm = this;

  vm.dirtyWebhook = undefined;
  vm.loadError = undefined;
  vm.submitError = undefined;
  vm.webhookEditorMask = undefined;
  vm.isWebhooksSupported = undefined;
  vm.isWebhooksForApplicationsSupported = undefined;

  vm.doLoad = doLoad;
  vm.deleteWebhook = deleteWebhook;
  vm.saveWebhook = saveWebhook;
  vm.hasEventTypeSelected = hasEventTypeSelected;
  vm.toggleEventTypeSelected = toggleEventTypeSelected;
  vm.isEventTypeDisabled = isEventTypeDisabled;

  vm.doLoad();

  $scope.$on('pageChangeStarted', function (event) {
    if (vm.dirtyWebhook && vm.dirtyWebhook.isDirty()) {
      event.preventDefault();
    }
  });

  function doLoad() {
    var promises = [
      $http.get(CLMLocations.getWebhookEventTypesUrl()),
      ProductFeatures.load(),
    ];

    if ($stateParams.webhookId) {
      promises.push(WebhookStore.getById($stateParams.webhookId));
    }

    $q.all(promises).then(
      function (results) {
        vm.webhookEventTypes = results[0].data;

        if (!$stateParams.webhookId) {
          vm.dirtyWebhook = WebhookStore.create();
        } else {
          vm.dirtyWebhook = results[2].$clone();
        }

        if (!vm.dirtyWebhook) {
          vm.loadError = 'Unable to locate webhook.';
        }

        vm.isWebhooksSupported =
          ProductFeatures.isAvailable('webhooks-for-applications') ||
          ProductFeatures.isAvailable('webhooks-for-repositories');
        vm.isWebhooksForApplicationsSupported = ProductFeatures.isAvailable(
          'webhooks-for-applications'
        );
      },
      function (error) {
        vm.loadError = error;
      }
    );

    delete vm.loadError;
  }

  function deleteWebhook() {
    DeleteModalService.deleteResource(
      'Webhook',
      vm.dirtyWebhook.url,
      vm.dirtyWebhook
    ).then(function () {
      // Model needs to be clean in order to navigate
      vm.dirtyWebhook.$revert();
      $state.go('webhooks.list');
    });
  }

  function saveWebhook() {
    vm.webhookEditorMask.wrap(vm.dirtyWebhook.$save()).then(
      function () {
        $state.go('webhooks.list');
      },
      function (error) {
        vm.submitError = error;
      }
    );
  }

  function hasEventTypeSelected(eventType) {
    return vm.dirtyWebhook.eventTypes.indexOf(eventType) > -1;
  }

  function toggleEventTypeSelected(eventType) {
    if (hasEventTypeSelected(eventType)) {
      var index = vm.dirtyWebhook.eventTypes.indexOf(eventType);
      vm.dirtyWebhook.eventTypes.splice(index, 1);
    } else {
      vm.dirtyWebhook.eventTypes.push(eventType);
    }
  }

  function isEventTypeDisabled(eventType) {
    return (
      eventType === 'Application Evaluation' &&
      !vm.isWebhooksForApplicationsSupported
    );
  }
}

WebhookEditController.$inject = [
  '$q',
  '$scope',
  '$http',
  '$stateParams',
  '$state',
  'CLMLocations',
  'WebhookStore',
  'DeleteModalService',
  'ProductFeatures',
];
