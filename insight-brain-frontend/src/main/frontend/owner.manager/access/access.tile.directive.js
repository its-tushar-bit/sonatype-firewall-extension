/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import template from './access.tile.directive.html';

export default function AccessTile() {
  return {
    restrict: 'E',
    replace: true,
    template,
    controller: 'AccessTileController',
    controllerAs: 'vm',
    bindToController: true
  };
}
