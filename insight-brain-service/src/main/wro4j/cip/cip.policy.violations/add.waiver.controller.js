/**
 * @license Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
/*global angular, CLM */
(function() {
  'use strict';

  function AddWaiverController($http, $scope, OwnerContext, SelectedComponent, messages, policy) {
    function doLoad() {
      $scope.policy = policy;
      $scope.component = SelectedComponent.get();
      $scope.waiverLoading = true;

      if (!$scope.component.componentDisplayText && $scope.component.displayName &&
          $scope.component.displayName.parts) {
        $scope.component.componentDisplayText = '';
        $scope.component.displayName.parts.forEach(function(part) {
          $scope.component.componentDisplayText += part.value;
        });
      }

      //get the tree of contexts, and flatten down into a list we can display properly
      $http.get(CLM.path + 'rest/policyWaiver/' + OwnerContext.ownerType + '/' + OwnerContext.ownerId +
              '/applicable/context/' + $scope.policy.id).success(function(data) {
        function processContext(context) {
          if (context.children) {
            angular.forEach(context.children, function (child) {
              processContext(child);
            });
          }

          var type = context.type;

          $scope.waiverTargets.push({
            id : context.id,
            name : context.name,
            type : type,
            label : type === 'repository_container' ? '' : type.charAt(0).toUpperCase() + type.slice(1)
          });
        }

        //if only application present, no need to show the app/org radio buttons
        $scope.waiverSelectOwner = (data.children && data.children.length);
        $scope.waiverTargets = [];
        $scope.waiverLoading = false;
        processContext(data);

        $scope.waiver = {
          hash : SelectedComponent.get().hash,
          policyId : $scope.policy.id,
          ownerId : $scope.waiverTargets[0].id,
          comment : ''
        };
        $scope.owner = {
          type : $scope.waiverTargets[0].type
        };
      }).error(function() {
        $scope.waiverLoading = false;
        $scope.waiveAssignError = messages.getHttpErrorMessage(arguments);
      });
    }
    doLoad();

    //user really wants to waive the component, so send the request on down
    $scope.acceptWaiveComponent = function() {
      $scope.waiverSaving = true;
      $scope.waiveAssignError = null;

      $http.post(CLM.path + 'rest/policyWaiver/' + $scope.owner.type + '/' + $scope.waiver.ownerId,
              $scope.waiver).success(function() {
        $scope.waiverSaving = false;
        $scope.$emit('reevaluate.component', $scope.waiver.hash ? { hash: $scope.waiver.hash } : null);
        $scope.$close();
      }).error(function() {
        $scope.waiverSaving = false;
        $scope.waiveAssignError = messages.getHttpErrorMessage(arguments);
      });
    };
  }
  AddWaiverController.$inject = ['$http', '$scope', 'OwnerContext', 'SelectedComponent', 'Messages', 'policy'];

  angular.module('cip.policy.violations').controller('AddWaiverController', AddWaiverController);
}());
