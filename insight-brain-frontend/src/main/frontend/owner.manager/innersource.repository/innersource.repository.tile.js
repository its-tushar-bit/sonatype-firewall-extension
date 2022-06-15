/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { actions as innerSourceRepositoryBaseConfigurationsActions } from 'MainRoot/innerSourceRepositoryConfiguration/innerSourceRepositoryBaseConfigurationsSlice';
import { selectIsInnerSourceRepositorySupported } from 'MainRoot/productFeatures/productFeaturesSelectors';
import template from './innersource.repository.tile.html';
import { selectIsApplication, selectIsOrganization } from 'MainRoot/reduxUiRouter/routerSelectors';
import { selectSelectedOwnerId } from 'MainRoot/OrgsAndPolicies/orgsAndPoliciesSelectors';
import {
  selectEditLink,
  selectInheritedFromOrganizationName,
  selectInnerSourceRepositoriesEnabled,
  selectLoadError,
  selectLoading,
  selectRepositoryConnections,
} from 'MainRoot/innerSourceRepositoryConfiguration/innerSourceRepositoryBaseConfigurationsSelectors';
export default {
  template: template,
  controllerAs: 'vm',
  controller: InnerSourceRepositoryTileController,
};

function InnerSourceRepositoryTileController($scope, $ngRedux) {
  var vm = this;
  vm.doLoad = doLoad;

  vm.unsubscribe = $ngRedux.connect(mapStateToThis, {
    loadRepositoryConnections: innerSourceRepositoryBaseConfigurationsActions.load,
  })(vm);

  $scope.$on('$destroy', function () {
    vm.unsubscribe();
  });

  vm.doLoad();

  function doLoad() {
    if (vm.isInnerSourceRepositorySupported) {
      vm.loadRepositoryConnections({ ownerId: vm.ownerId, inherit: true });
    }
  }
}

const mapStateToThis = (state) => ({
  isInnerSourceRepositorySupported: selectIsInnerSourceRepositorySupported(state),
  isOrg: selectIsOrganization(state),
  isApp: selectIsApplication(state),
  ownerId: selectSelectedOwnerId(state),
  editLink: selectEditLink(state),
  loading: selectLoading(state),
  error: selectLoadError(state),
  innerSourceRepositories: angular.copy(selectRepositoryConnections(state)),
  innerSourceRepositoriesInheritedFrom: selectInheritedFromOrganizationName(state),
  innerSourceRepositoriesEnabled: selectInnerSourceRepositoriesEnabled(state),
});

InnerSourceRepositoryTileController.$inject = ['$scope', '$ngRedux'];
