/**
 * @license Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
/*global angular, angularDebug*/
window.CLM = {
  path: '../'
};

(function () {
  'use strict';
  
  function init($rootScope, ComponentUpdateService) {
    $rootScope.$on('component.data.changed', function (event, hash) {
      ComponentUpdateService.update(hash);
    });
  }
  init.$inject = ['$rootScope', 'component.update.service'];

  function config($compileProvider) {
    $compileProvider.debugInfoEnabled(angularDebug);
  }
  config.$inject = ['$compileProvider'];

  angular.module('audit',
          ['AngularCommon', 'UnauthenticatedResponseHttpInterceptor', 'ui.bootstrap', 'CLMLocation',
              'component.information.panel']).run(init).config(config);
}());
