/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
/*global angular */
import getThreatColor from './threatColorUtil';

export default function RequestWaiverController(
  $scope,
  $state,
  CLMLocations,
  SelectedComponent,
  policy
) {
  $scope.getThreatColor = getThreatColor;

  function doLoad() {
    $scope.policy = policy;
    $scope.component = SelectedComponent.get();
    $scope.conditionReasons = getConditionReasons(policy.constraints);

    if (policy.policyViolationId) {
      $scope.curlExample = getCurlExample(policy.policyViolationId);
      $scope.policyViolationPageUrl = getPolicyViolationDetailsPageURL(
        policy.policyViolationId
      );
    }
  }

  function getConditionReasons(constraints) {
    let conditionReasons = [];
    constraints.forEach((constraint) => {
      constraint.conditions.forEach((condition) => {
        conditionReasons.push(condition.conditionReason);
      });
    });
    return conditionReasons;
  }

  function getCurlExample(policyViolationId) {
    return (
      'curl -X POST -u user:pass -H "Content-Type: text/plain; charset=UTF-8" ' +
      `${CLMLocations.getRequestWaiverUrl(
        policyViolationId
      )} --data-binary 'waiver comment (optional)'`
    );
  }

  function getPolicyViolationDetailsPageURL(policyViolationId) {
    return $state.href(
      'sidebarView.violation',
      { id: policyViolationId },
      { absolute: true }
    );
  }

  doLoad();
}
RequestWaiverController.$inject = [
  '$scope',
  '$state',
  'CLMLocations',
  'SelectedComponent',
  'policy',
];
