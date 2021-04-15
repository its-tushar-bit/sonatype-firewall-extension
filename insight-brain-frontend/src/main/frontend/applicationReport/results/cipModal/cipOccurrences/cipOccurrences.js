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
    pathnames: '<',
  },
};

function OccurrencesController($scope) {
  const vm = this;

  $scope.$watch('vm.pathnames', function () {
    vm.parsedPathnames = map(parsePathname, vm.pathnames);
  });
}

OccurrencesController.$inject = ['$scope'];

const pathnameRegex = /^(dependency:\/)?((.*?)\/)?([^/]+)$/;

// @visibleForTesting
export function parsePathname(pathname) {
  const [
      ,
      /* overall match */ dependency /* dirname including delimiter */,
      ,
      dirname,
      originalBasename,
    ] = pathnameRegex.exec(pathname),
    isDependency = !!dependency;

  // component names which contains '/' are replaced with '\' by the Occurrence pathnames string in the backend. This is
  // to avoid considering them as part of basename. Replacing them back as how it should be after resolving base name -
  // CLM-12606
  const basename = originalBasename.replace(/\\/g, '/');

  return { isDependency, dirname, basename };
}
