/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
/*global angular, CLM */
(function () {
  'use strict';

  function CIPLabelEditor() {
    return {
      templateUrl: CLM.assetsPath + 'cip/cip-label-editor.html',
      controllerAs: 'vm',
      controller: 'LabelsController'
    };
  }

  angular.module('cip.label.editor').directive('cipLabelEditor', CIPLabelEditor);
}());
