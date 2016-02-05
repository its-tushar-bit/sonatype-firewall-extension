/**
 * @license Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
/*global angular */
(function () {
  'use strict';

  function ProprietaryConfigurationEditor () {
    return {
      restrict: 'A',
      scope: {
        prefixes: '=',
        regexes: '='
      },
      templateUrl: 'config-editor',
      controller: 'proprietary.configuration.editor.controller',
      controllerAs: 'vm',
      bindToController: true
    };
  }

  angular.module('proprietary.configuration.module').directive('proprietaryConfigurationEditor',
      ProprietaryConfigurationEditor);
}());
