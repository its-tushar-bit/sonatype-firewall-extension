/**
 * @license Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
/*global angular, CLM */
(function () {
  'use strict';

  function CIPLabelEditor() {
    return {
      templateUrl: CLM.path + 'cip/cip-policy-violations.html',
      controllerAs: 'vm',
      controller: 'PolicyViolationsController'
    };
  }

  angular.module('cip.policy.violations').directive('cipPolicyViolations', CIPLabelEditor);
}());
