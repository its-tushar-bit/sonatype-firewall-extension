/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import handleOnEnterPermissions from 'MainRoot/routeProductLicenseValidator/RouteProductLicenseValidator';

function routerTransitionsListener($transitions) {
  /**
   * Check application enter state transition is permitted for product license, if the path is not permitted
   * redirect to home.
   */
  $transitions.onEnter({}, (transition, state) =>
    handleOnEnterPermissions(transition.router.stateService.target, state)
  );
}

routerTransitionsListener.$inject = ['$transitions'];

export default angular.module('routeProductLicenseValidator', ['ui.router']).run(routerTransitionsListener);
