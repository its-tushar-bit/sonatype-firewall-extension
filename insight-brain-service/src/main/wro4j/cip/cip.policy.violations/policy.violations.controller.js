/**
 * @license Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
/*global angular */
(function() {
  'use strict';

  function PolicyViolationsController($http, $scope, $q, $modal, SelectedComponent, OwnerContext, PolicyViolations) {

    $scope.getThreatColor = function (threatLevel) {
      return threatLevel > 7 ? 'red' : threatLevel > 3 ? 'orange' : threatLevel > 1 ? 'yellow' : threatLevel > 0 ? 'darkblue' : 'blue';
    };

    function sortPolicyAlerts() {
      $scope.processedPolicyAlerts.sort(function(a, b) {
        return b.threatLevel - a.threatLevel;
      });
    }

    $scope.doLoad = function() {
      $scope.processedPolicyAlerts = null;
      $scope.error = null;

      PolicyViolations.get().then(function (policyThreats) {
        $scope.processedPolicyAlerts = policyThreats;
        sortPolicyAlerts();
      }, function (err) {
        $scope.error = err;
      });
      return;
    };

    $scope.waiveComponent = function(policyAlert) {
      $modal.open({
        templateUrl: 'add-waiver-modal-tmpl',
        controller: 'AddWaiverController',
        backdrop: 'static',
        keyboard: false,
        resolve: {
          policy: function() {
            return policyAlert;
          }
        }
      });
    };

    $scope.viewWaivers = function() {
      $modal.open({
        templateUrl: 'view-waivers-modal-tmpl',
        controller: 'ViewWaiverController',
        backdrop: 'static',
        keyboard: false
      });
    };
    $scope.alerts = [];

    $scope.$watch(function () {
      return SelectedComponent.get();
    }, function (component) {
      if (component) {
        $scope.doLoad();
      }
    });
  }

  PolicyViolationsController.$inject = ['$http', '$scope', '$q', '$modal', 'SelectedComponent', 'OwnerContext', 'PolicyViolations'];

  angular.module('cip.policy.violations').controller('PolicyViolationsController', PolicyViolationsController);
}());
