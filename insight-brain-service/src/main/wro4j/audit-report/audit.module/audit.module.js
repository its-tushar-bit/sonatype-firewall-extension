/**
 * @license Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
/*global angular */
window.CLM = {
  path: '../'
};

(function () {
  'use strict';

  angular.module('audit', [
    'AngularCommon', 'UnauthenticatedResponseHttpInterceptor', 'ui.bootstrap', 'CLMLocation',
    'component.information.panel'
  ]);
}());
