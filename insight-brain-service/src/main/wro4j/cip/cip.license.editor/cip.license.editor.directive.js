/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
/*global angular, CLM */
(function() {
  'use strict';

  function CIPLicenseEditor() {
    return {
      templateUrl: CLM.path + 'cip/cip-license-editor.html',
      controllerAs: 'vm',
      controller: 'LicenseEditorController'
    };
  }

  angular.module('cip.license.editor').directive('cipLicenseEditor', CIPLicenseEditor);
}());
