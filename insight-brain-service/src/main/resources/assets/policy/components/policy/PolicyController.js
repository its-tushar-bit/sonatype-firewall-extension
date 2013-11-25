/**
 * @license Copyright (c) 2012-2013 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
/*global angular, $, clmBuildTimestamp */
/*jslint plusplus: true */
(function() {
  'use strict';

  var policyModule = angular.module('Policy',
      ['Hudson', 'PolicyEditor', 'CLMAppLocation', 'AngularCommon', 'CommonServices']);

  policyModule.controller('PolicyController', [
    '$scope', '$location', '$http', 'hudson', '$timeout', '$rootScope', '$q', 'PolicyStore', 'ActionStore',
    'CLMAppLocations', 'Dialog', 'ownerChange',
    function($scope, $location, $http, hudson, $timeout, $rootScope, $q, policyStore, actionStore, clmAppLocations, Dialog, ownerChange) {

      $scope.alerts = [];
      $scope.location = $location;
      $scope.state = {};

      $scope.viewRemovePolicy = function(policy) {
        Dialog.open({
          title : 'Delete Policy',
          body : "Are you sure you want to delete the Policy named '" + policy.name + "'? This action is not reversible.",
          buttons : [{
            name : 'Cancel'
          },{
            name : 'Delete',
            type : 'danger',
            click : function () {
              policy.$delete().then(angular.noop, function(error) {
                $scope.$broadcast('showServerError', arguments);
              });
            }
          }]
        });
      };

      $scope.doLoad = function() {
        $scope.error = null;
        $scope.applicablePolicies = null;
        var promises = [
          policyStore.get().refresh(), $http.get(clmAppLocations.getApplicablePolicies(), {
            params: { timestamp: new Date().getTime() }
          })
        ];

        $q.all(promises).then(function(results) {
          $scope.applicablePolicies = results[1].data.policiesByOwner;
          angular.forEach($scope.applicablePolicies, function(applicablePolicy, index) {
            applicablePolicy.editable = index === 0;
            if (index === 0) {
              applicablePolicy.policies = results[0];
            }
          });
        }, function(errors) {
          $scope.error = angular.isArray(errors) ? errors[0] : errors;
        });
      };

      $scope.$on('ownerChanged', ownerChange.getEventHandler($scope, 'applicablePolicies'));
      $scope.$on('refresh', $scope.doLoad);

      $scope.toggleAll = function(applicablePolicy) {
        var action = $scope.allExpanded[applicablePolicy.ownerId] ? 'hide' : 'show';
        $('#' + applicablePolicy.ownerId).find('.accordion-body').collapse(action);
        //TODO: to work around collapse bug, fixed in newer release of bootstrap
        //https://github.com/twitter/bootstrap/pull/7424/files
        $('#' + applicablePolicy.ownerId).find('.policy-top')[action ==
            'hide' ? 'addClass' : 'removeClass']('collapsed');
        $scope.allExpanded[applicablePolicy.ownerId] = !($scope.allExpanded[applicablePolicy.ownerId] || false);
      };

      $scope.isExpanded = function(applicablePolicy) {
        return $scope.allExpanded[applicablePolicy.ownerId] || false;
      };

      $scope.doLoad();

      $scope.encodeURIComponent = window.encodeURIComponent;

      $scope.allExpanded = {};
    }
  ]);

  policyModule.directive('policyItems', [
    'ActionStore', function(actionStore) {
      function capitalize(text) {
        if (text && text.length > 1) {
          return text.substring(0, 1).toUpperCase() + text.substring(1);
        }
        return text;
      }

      var actionStageList = null;
      actionStore.get().then(function(data) {
        actionStageList = data[1];
      });
      return {
        restrict: 'A',
        templateUrl: '../policy-assets/components/policy/policy-items.html?' + clmBuildTimestamp,
        scope: {
          policies: '=policyItems',
          editable: '=editable',
          remove: '='
        },
        priority: 99,
        link: function(scope, elem, attr, ctrl) {
          scope.policyEditMap = {};
          scope.getActionCount = function(policy) {
            var actionCount = 0;
            angular.forEach(policy.actions, function(value, key) {
              if (value.length > 0) {
                actionCount++;
              }
            });
            return actionCount;
          };
          scope.getActionStages = function() {
            return actionStageList;
          };
          scope.getStageIconPath = function(stage, policy) {
            if (policy.actions[stage.id]) {
              for (var i = 0; i < policy.actions[stage.id].length; i++) {
                if (policy.actions[stage.id][i].actionTypeId == 'warn') {
                  return "../assets/img/policyalert.png";
                }
                else if (policy.actions[stage.id][i].actionTypeId == 'fail') {
                  return "../assets/img/policyerror.png";
                }
              }
            }
          };

          scope.edit = function(policy) {
            scope.policyEditMap[policy.id] = true;
            $('#collapse' + policy.id).collapse('show');
          };
        }
      };
    }
  ]);
}());
