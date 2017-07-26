/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
function StableBodyService() {
  return angular.getTestability(angular.element('body'));
}

angular.module('utility.services').service('stable.body.service', StableBodyService);
