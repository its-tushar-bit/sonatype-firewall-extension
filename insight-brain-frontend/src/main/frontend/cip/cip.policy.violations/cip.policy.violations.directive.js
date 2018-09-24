/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
/*global CLM */
export default function cipPolicyViolationsDirective() {
  return {
    templateUrl: CLM.assetsPath + 'cip/cip-policy-violations.html',
    controllerAs: 'vm',
    controller: 'PolicyViolationsController'
  };
}
