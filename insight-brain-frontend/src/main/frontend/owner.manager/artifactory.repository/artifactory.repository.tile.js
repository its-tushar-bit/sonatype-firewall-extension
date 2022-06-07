/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { actions as artifactoryRepositoryBaseConfigurationsActions } from 'MainRoot/artifactoryRepositoryConfiguration/artifactoryRepositoryBaseConfigurationsSlice';
import {
  selectIsArtifactoryRepositorySupported,
  selectLoadErrorFeaturesSlice,
  selectLoadingFeaturesSlice,
} from 'MainRoot/productFeatures/productFeaturesSelectors';
import template from './artifactory.repository.tile.html';
import { selectIsApplication, selectIsOrganization } from 'MainRoot/reduxUiRouter/routerSelectors';
import { selectSelectedOwnerId } from 'MainRoot/OrgsAndPolicies/orgsAndPoliciesSelectors';
import {
  selectEditLink,
  selectInheritedFromOrganizationName,
  selectArtifactoryRepositoriesEnabled,
  selectLoadError,
  selectLoading,
  selectArtifactoryConnection,
} from 'MainRoot/artifactoryRepositoryConfiguration/artifactoryRepositoryBaseConfigurationsSelectors';
export default {
  template: template,
  controllerAs: 'vm',
  controller: ArtifactoryRepositoryTileController,
};

function ArtifactoryRepositoryTileController($scope, $ngRedux) {
  var vm = this;
  vm.doLoad = doLoad;

  vm.unsubscribe = $ngRedux.connect(mapStateToThis, {
    loadArtifactoryConnection: artifactoryRepositoryBaseConfigurationsActions.load,
  })(vm);

  $scope.$on('$destroy', function () {
    vm.unsubscribe();
  });

  vm.doLoad();

  function doLoad() {
    if (vm.isArtifactoryRepositorySupported) {
      vm.loadArtifactoryConnection({ ownerId: vm.ownerId, inherit: true });
    }
  }
}

const mapStateToThis = (state) => ({
  isArtifactoryRepositorySupported: selectIsArtifactoryRepositorySupported(state),
  isOrg: selectIsOrganization(state),
  isApp: selectIsApplication(state),
  ownerId: selectSelectedOwnerId(state),
  loadingFeatures: selectLoadingFeaturesSlice(state),
  loadError: selectLoadErrorFeaturesSlice(state),
  editLink: selectEditLink(state),
  loading: selectLoading(state),
  error: selectLoadError(state),
  artifactoryRepositories: angular.copy(selectArtifactoryConnection(state)),
  artifactoryRepositoriesInheritedFrom: selectInheritedFromOrganizationName(state),
  artifactoryRepositoriesEnabled: selectArtifactoryRepositoriesEnabled(state),
});

ArtifactoryRepositoryTileController.$inject = ['$scope', '$ngRedux'];
