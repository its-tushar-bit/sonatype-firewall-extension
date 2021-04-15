/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
/*global angular, CLM */
import getThreatColor from './threatColorUtil';

export default function AddWaiverController(
  $http,
  $scope,
  OwnerContext,
  SelectedComponent,
  messages,
  policy
) {
  $scope.getThreatColor = getThreatColor;

  $scope.reset = function () {
    $scope.owner = $scope.waiverTargets[0];
    $scope.waiver.ownerId = $scope.owner.id;
    $scope.waiver.hash = $scope.component.hash;
  };

  function doLoad() {
    $scope.policy = policy;
    $scope.component = SelectedComponent.get();
    $scope.waiverLoading = true;
    $scope.waiveViolationOnly = true;
    $scope.owner = null;

    // Prior to IQ Brain 1.53 policy violations stored in the report did not include constraint facts
    $scope.legacyReport = !policy.constraintFactsJson;

    if (
      !$scope.component.componentDisplayText &&
      $scope.component.displayName &&
      $scope.component.displayName.parts
    ) {
      $scope.component.componentDisplayText = '';
      $scope.component.displayName.parts.forEach(function (part) {
        $scope.component.componentDisplayText += part.value;
      });
    }

    //get the tree of contexts, and flatten down into a list we can display properly
    $http
      .get(
        CLM.path +
          'rest/policyWaiver/' +
          OwnerContext.ownerType +
          '/' +
          OwnerContext.ownerId +
          '/applicable/context/' +
          $scope.policy.id
      )
      .then(
        function (response) {
          function processContext(context) {
            if (context.children) {
              angular.forEach(context.children, function (child) {
                processContext(child);
              });
            }

            var type = context.type;

            $scope.waiverTargets.push({
              id: context.id,
              name: context.name,
              type: type,
              label:
                type === 'repository_container'
                  ? ''
                  : type.charAt(0).toUpperCase() + type.slice(1),
            });
          }

          var data = response.data;

          //if only application present, no need to show the app/org radio buttons
          $scope.waiverSelectOwner = data.children && data.children.length;
          $scope.waiverTargets = [];
          $scope.waiverLoading = false;
          processContext(data);

          $scope.owner = $scope.waiverTargets[0];
          $scope.waiver = {
            hash: SelectedComponent.get().hash,
            policyId: $scope.policy.id,
            ownerId: $scope.owner.id,
            constraintFactsJson: $scope.policy.constraintFactsJson || null,
            comment: '',
          };
        },
        function (error) {
          $scope.waiverLoading = false;
          $scope.waiveAssignError = messages.getHttpErrorMessage(error);
        }
      );
  }

  doLoad();

  //user really wants to waive the component, so send the request on down
  $scope.acceptWaiveComponent = function () {
    $scope.waiverSaving = true;
    $scope.waiveAssignError = null;

    $http
      .post(
        CLM.path +
          'rest/policyWaiver/' +
          $scope.owner.type +
          '/' +
          $scope.waiver.ownerId,
        $scope.waiver
      )
      .then(
        function () {
          $scope.waiverSaving = false;
          $scope.$emit(
            'reevaluate.component',
            $scope.waiver.hash ? { hash: $scope.waiver.hash } : null
          );
          $scope.$close();
        },
        function (error) {
          $scope.waiverSaving = false;
          $scope.waiveAssignError = messages.getHttpErrorMessage(error);
        }
      );
  };
}
AddWaiverController.$inject = [
  '$http',
  '$scope',
  'OwnerContext',
  'SelectedComponent',
  'Messages',
  'policy',
];
