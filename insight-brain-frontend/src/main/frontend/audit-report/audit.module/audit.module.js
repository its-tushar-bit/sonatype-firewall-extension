/**
 * @license Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
/*global angular, angularDebug*/

import legacyConfigurationModule from '../../LegacyConfigurationModule';

window.CLM = {
  path: '../../',
  assetsPath : '../'
};

(function () {
  'use strict';
  
  function init($rootScope, ComponentUpdateService) {
    $rootScope.$on('reevaluate.component', function (event, componentKey) {
      ComponentUpdateService.reevaluate(componentKey, true);
    });
    $rootScope.$on('reload.component', function (event, componentKey) {
      ComponentUpdateService.reevaluate(componentKey, false);
    });
  }
  init.$inject = ['$rootScope', 'component.update.service'];

  function config($compileProvider) {
    $compileProvider.debugInfoEnabled(angularDebug);
  }
  config.$inject = ['$compileProvider'];

  angular.module('audit',
          ['AngularCommon', 'UnauthenticatedResponseHttpInterceptor', 'ui.bootstrap', 'CLMLocation',
              'component.information.panel', legacyConfigurationModule.name]).run(init).config(config);
}());
