/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { selectLoading, selectLoadError } from 'MainRoot/OrgsAndPolicies/ownerSummarySelectors';

export default function OwnerSummaryTilesContainerController($scope, $ngRedux) {
  var vm = this;

  vm.unsubscribe = $ngRedux.connect(mapStateToThis, null)(vm);

  $scope.$on('$destroy', function () {
    vm.unsubscribe();
  });
}

export const mapStateToThis = (state) => ({
  loading: selectLoading(state),
  loadError: selectLoadError(state),
});

OwnerSummaryTilesContainerController.$inject = ['$scope', '$ngRedux'];
