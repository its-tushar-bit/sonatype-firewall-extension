/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { head, tail } from 'ramda';

import template from './cipSimilar.html';

export default {
  template,
  controller: CipSimilarController,
  controllerAs: 'vm',
  bindings: {
    similarComponents: '<',
  },
};

function CipSimilarController($scope) {
  const vm = this;

  $scope.$watch('vm.similarComponents', function (similarComponents = []) {
    vm.mostSimilarComponent = head(similarComponents);
    vm.otherSimilarComponents = tail(similarComponents);
  });
}

CipSimilarController.$inject = ['$scope'];
