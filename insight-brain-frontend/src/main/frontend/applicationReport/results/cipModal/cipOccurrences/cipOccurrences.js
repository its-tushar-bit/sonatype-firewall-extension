/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { map } from 'ramda';

import template from './cipOccurrences.html';

export default {
  template,
  controller: OccurrencesController,
  controllerAs: 'vm',
  bindings: {
    pathnames: '<'
  }
};

function OccurrencesController($scope) {
  const vm = this;

  $scope.$watch('vm.pathnames', function() {
    vm.parsedPathnames = map(parsePathname, vm.pathnames);
  });
}

OccurrencesController.$inject = ['$scope'];

const pathnameRegex = /^(dependency:\/)?((.*?)\/)?([^/]+)$/;

// @visibleForTesting
export function parsePathname(pathname) {
  const [/* overall match */, dependency, /* dirname including delimiter */, dirname, basename] =
        pathnameRegex.exec(pathname),
      isDependency = !!dependency;

  return { isDependency, dirname, basename };
}
