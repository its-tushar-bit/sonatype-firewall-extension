/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { unwrapResult } from '@reduxjs/toolkit';
import { actions } from 'MainRoot/productFeatures/productFeaturesSlice';
import { selectIsInnerSourceRepositorySupported } from 'MainRoot/productFeatures/productFeaturesSelectors';
import template from './innersource.repository.tile.html';

export default {
  template: template,
  controllerAs: 'vm',
  controller: InnerSourceRepositoryTileController,
};

function InnerSourceRepositoryTileController(
  $scope,
  EventNameConstant,
  CLMContextLocations,
  OrganizationStore,
  ApplicationStore,
  $q,
  Messages,
  InnerSourceRepositoryService,
  $ngRedux
) {
  var vm = this;

  vm.unsubscribe = $ngRedux.connect(mapStateToThis, {
    loadProductFeatures: actions.fetchProductFeaturesIfNeeded,
  })(vm);

  vm.load = load;
  vm.error = undefined;
  vm.loading = false;
  vm.isOrg = CLMContextLocations.isOrganization();
  vm.isApp = CLMContextLocations.isApplication();
  vm.innerSourceRepositories = [];
  vm.innerSourceRepositoriesInheritedFrom = undefined;
  vm.innerSourceRepositoriesEnabled = undefined;
  vm.editLink = undefined;

  vm.load();

  $scope.$on(EventNameConstant.RELOAD_OWNER_SUMMARY_DATA, function () {
    load();
  });

  $scope.$on('$destroy', function () {
    vm.unsubscribe();
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
      const promises = [ownerPromise, vm.loadProductFeatures()];
      $q.all(promises)
        .then(function (results) {
          unwrapResult(results[1]);
          ownerId = results[0].id;
          vm.editLink = 'repositoryBaseConfigurations.' + ownerType + '({' + ownerType + "Id:'" + ownerId + "'})";
          if (vm.isInnerSourceRepositorySupported) {
            return InnerSourceRepositoryService.getRepositoryConnections(ownerType, ownerId, true);
          }
        })
        .then(function (result) {
          if (!result) {
            return;
          }
          if (Array.isArray(result.repositoryConnections) && result.repositoryConnections.length > 0) {
            vm.innerSourceRepositories = result.repositoryConnections;
          }
          if (!result.repositoryConnectionStatus) {
            return;
          }
          vm.innerSourceRepositoriesEnabled =
            result.repositoryConnectionStatus.inheritedFromOrgEnabled ||
            (result.repositoryConnectionStatus.allowChange && result.repositoryConnectionStatus.enabled);
          vm.innerSourceRepositoriesInheritedFrom = result.repositoryConnectionStatus.inheritedFromOrganizationName;
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
}

const mapStateToThis = (state) => ({
  isInnerSourceRepositorySupported: selectIsInnerSourceRepositorySupported(state),
});

InnerSourceRepositoryTileController.$inject = [
  '$scope',
  'event.name.constant',
  'CLMContextLocations',
  'OrganizationStore',
  'ApplicationStore',
  '$q',
  'Messages',
  'InnerSourceRepositoryService',
  '$ngRedux',
];
