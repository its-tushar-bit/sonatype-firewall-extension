/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { actions } from 'MainRoot/productFeatures/productFeaturesSlice';
import {
  selectIsInnerSourceRepositorySupported,
  selectLoadErrorFeaturesSlice,
  selectLoadingFeaturesSlice,
} from 'MainRoot/productFeatures/productFeaturesSelectors';
import template from './innersource.repository.tile.html';
import { selectIsApplication, selectIsOrganization } from 'MainRoot/reduxUiRouter/routerSelectors';
import { selectSelectedOwnerId } from 'MainRoot/OrgsAndPolicies/orgsAndPoliciesSelectors';
export default {
  template: template,
  controllerAs: 'vm',
  controller: InnerSourceRepositoryTileController,
};

function InnerSourceRepositoryTileController($scope, Messages, InnerSourceRepositoryService, $ngRedux) {
  var vm = this;

  vm.unsubscribe = $ngRedux.connect(mapStateToThis, {
    loadProductFeatures: actions.fetchProductFeaturesIfNeeded,
  })(vm);

  vm.loading = false;
  vm.innerSourceRepositories = [];
  vm.ownerType = vm.isOrg ? 'organization' : 'application';
  vm.loadRepositoryConnections = loadRepositoryConnections;

  $scope.$on('$destroy', function () {
    vm.unsubscribe();
  });

  $scope.$watch('vm.ownerId', function (newValue, oldValue) {
    if (newValue && newValue !== oldValue) {
      vm.editLink = `repositoryBaseConfigurations.${vm.ownerType}({${vm.ownerType}Id:'${vm.ownerId}'})`;
      if (vm.isInnerSourceRepositorySupported) vm.loadRepositoryConnections();
    }
  });

  $scope.$watch('vm.ownerType', function (newValue) {
    if (newValue) {
      vm.editLink = `repositoryBaseConfigurations.${vm.ownerType}({${vm.ownerType}Id:'${vm.ownerId}'})`;
    }
  });

  function loadRepositoryConnections() {
    vm.loading = true;
    vm.error = undefined;
    InnerSourceRepositoryService.getRepositoryConnections(vm.ownerType, vm.ownerId, true)
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
  }
}

const mapStateToThis = (state) => ({
  isInnerSourceRepositorySupported: selectIsInnerSourceRepositorySupported(state),
  isOrg: selectIsOrganization(state),
  isApp: selectIsApplication(state),
  ownerId: selectSelectedOwnerId(state),
  loadingFeatures: selectLoadingFeaturesSlice(state),
  loadError: selectLoadErrorFeaturesSlice(state),
});

InnerSourceRepositoryTileController.$inject = ['$scope', 'Messages', 'InnerSourceRepositoryService', '$ngRedux'];
