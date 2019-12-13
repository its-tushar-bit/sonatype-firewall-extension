/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
export default function AccessTile() {
  return {
    restrict: 'E',
    replace: true,
    templateUrl: 'owner.manager/access/access.tile.directive.html',
    controller: 'AccessTileController',
    controllerAs: 'vm',
    bindToController: true
  };
}
