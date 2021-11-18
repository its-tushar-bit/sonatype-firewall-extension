/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

import template from './innersource.repository.tile.html';

export default {
  template: template,
  controllerAs: 'vm',
  controller: InnerSourceRepositoryTileController,
};

function InnerSourceRepositoryTileController(
  $scope,
  SameOwnerStateNavigationService,
  EventNameConstant,
  CLMContextLocations,
  OrganizationStore,
  ApplicationStore,
  $q,
  Messages,
  InnerSourceRepositoryService,
  ProductFeatures
) {
  var vm = this;
  vm.load = load;
  vm.error = undefined;
  vm.loading = false;
  vm.isOrg = CLMContextLocations.isOrganization();
  vm.isApp = CLMContextLocations.isApplication();
  vm.isInnerSourceRepositorySupported = undefined;
  vm.innerSourceRepository = undefined;
  vm.editInnerSourceRepository = editInnerSourceRepository;
  vm.load();

  $scope.$on(EventNameConstant.RELOAD_OWNER_SUMMARY_DATA, function () {
    load();
  });

  function load() {
    vm.error = undefined;
    vm.loading = true;

    let ownerPromise;
    let ownerId;
    let ownerType;

    if (vm.isOrg) {
      ownerPromise = OrganizationStore.getById(CLMContextLocations.getEntityId());
      ownerType = 'organization';
    } else if (vm.isApp) {
      ownerPromise = ApplicationStore.getById(CLMContextLocations.getEntityId());
      ownerType = 'application';
    }

    if (ownerPromise !== undefined) {
      const promises = [ownerPromise, ProductFeatures.load()];
      $q.all(promises)
        .then(function (results) {
          ownerId = results[0].id;
          vm.isInnerSourceRepositorySupported = ProductFeatures.isAvailable('inner-source-repository-integration');
          if (vm.isInnerSourceRepositorySupported) {
            return InnerSourceRepositoryService.getRepositoryConnections(ownerType, ownerId, true);
          }
        })
        .then(function (result) {
          vm.innerSourceRepository = Array.isArray(result) && result.length > 0 ? result[0] : undefined;
          if (vm.innerSourceRepository !== undefined && vm.innerSourceRepository.ownerId !== ownerId) {
            vm.innerSourceRepository.inherited = true;
            return OrganizationStore.getById(vm.innerSourceRepository.ownerId).then(function (result) {
              vm.innerSourceRepository.ownerName = result.name;
            });
          }
        })
        .catch(function (e) {
          vm.error = Messages.getHttpErrorMessage(e);
        })
        .finally(function () {
          vm.loading = false;
        });
    } else {
      vm.loading = false;
    }
  }

  function editInnerSourceRepository() {
    SameOwnerStateNavigationService.goEdit('edit-innersource-repository');
  }
}

InnerSourceRepositoryTileController.$inject = [
  '$scope',
  'SameOwnerStateNavigationService',
  'event.name.constant',
  'CLMContextLocations',
  'OrganizationStore',
  'ApplicationStore',
  '$q',
  'Messages',
  'InnerSourceRepositoryService',
  'ProductFeatures',
];
