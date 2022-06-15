/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { selectIsSourceControlForSourceTileSupported } from 'MainRoot/productFeatures/productFeaturesSelectors';
import { selectSelectedOwnerName } from 'MainRoot/OrgsAndPolicies/orgsAndPoliciesSelectors';

import template from './source.control.tile.html';
import {
  selectSourceControl,
  selectEffectiveProvider,
  selectItemSubText,
  selectItemText,
  selectLoading,
  selectLoadError,
} from 'MainRoot/OrgsAndPolicies/sourceControlSelectors';
import {
  selectIsApplication,
  selectIsOrganization,
  selectIsRootOrganization,
} from 'MainRoot/reduxUiRouter/routerSelectors';

export default {
  template: template,
  controllerAs: 'vm',
  controller: SourceControlTileController,
};

function SourceControlTileController($scope, SameOwnerStateNavigationService, $ngRedux) {
  const vm = this;

  vm.editSourceControl = editSourceControl;

  vm.unsubscribe = $ngRedux.connect(mapStateToThis)(vm);

  $scope.$on('$destroy', function () {
    vm.unsubscribe();
  });

  function editSourceControl() {
    SameOwnerStateNavigationService.goEdit('edit-source-control');
  }
}
const mapStateToThis = (state) => ({
  isSourceControlSupported: selectIsSourceControlForSourceTileSupported(state),
  ownerName: selectSelectedOwnerName(state),
  sourceControl: selectSourceControl(state),
  isApp: selectIsApplication(state),
  isOrg: selectIsOrganization(state),
  isRootOrg: selectIsRootOrganization(state),
  effectiveProvider: selectEffectiveProvider(state),
  itemText: selectItemText(state),
  itemSubText: selectItemSubText(state),
  loading: selectLoading(state),
  error: selectLoadError(state),
});

SourceControlTileController.$inject = ['$scope', 'SameOwnerStateNavigationService', '$ngRedux'];
