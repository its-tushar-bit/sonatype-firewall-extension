/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

export default function cipVersionGraph($window) {
  return {
    templateUrl: $window.CLM.assetsPath + 'version-graph/version-graph.html',
    controllerAs: 'vm',
    controller: 'CIPController'
  };
}

cipVersionGraph.$inject = ['$window'];
