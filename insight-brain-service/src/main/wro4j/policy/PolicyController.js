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
      ['PolicyEditor', 'CLMAppLocation', 'AngularCommon', 'CommonServices', 'Stores']);

  policyModule.service('PolicyMonitoringStore', [
    'CLMAppLocations', '$http', function(CLMAppLocations, $http) {
      return {
        get: function() {
          return $http.get(CLMAppLocations.getPolicyMonitoringUrl());
        },
        getApplicable: function() {
          return $http.get(CLMAppLocations.getApplicablePolicyMonitoring());
        },
        save: function(policyMonitoring) {
          return $http.put(CLMAppLocations.getPolicyMonitoringUrl(), policyMonitoring);
        },
        delete: function(){
          return $http.delete(CLMAppLocations.getPolicyMonitoringUrl());
        }
      };
    }
  ]);

  policyModule.controller('PolicyController', [
    '$scope', '$location', '$http', '$rootScope', '$q', 'PolicyStore', 'ActionStore',
    'CLMAppLocations', 'Dialog', 'ownerChange', 'PolicyMonitoringStore', 'Messages',
    function($scope, $location, $http, $rootScope, $q, policyStore, actionStore, clmAppLocations, Dialog, ownerChange,
             PolicyMonitoringStore, messages) {

      $scope.alerts = [];
      $scope.location = $location;
      $scope.policyMonitoringAlerts = [];
      $scope.monitoringHelp = 'Each day the latest scan from this stage will be evaluated. Notifications for new ' +
        'violations can be configured per policy (below).';

      $scope.viewRemovePolicy = function(policy) {
        Dialog.open({
          title : 'Delete Policy',
          body : 'Are you sure you want to delete the Policy named "' + policy.name + '"? This action is not reversible.',
          buttons : [{
            name : 'Cancel'
          },{
            name : 'Delete',
            type : 'danger',
            click : function () {
              policy.$delete().then(angular.noop, function() {
                $scope.$broadcast('showServerError', arguments);
              });
            }
          }]
        });
      };

      $scope.doLoad = function() {
        $scope.error = null;
        $scope.applicablePolicies = null;
        $scope.actionStageList = null;

        var promises = [
          policyStore.get().refresh(),
          $http.get(clmAppLocations.getApplicablePolicies(), {
            params: { timestamp: new Date().getTime() }
          }),
          actionStore.get(),
          PolicyMonitoringStore.getApplicable()
        ];

        /**
         * Conditionally render text for the select box to take into account inheritance.
         */
        function createPlaceHolderText(isApplication, orgPolicyMonitor) {
          if(isApplication && orgPolicyMonitor !== null){
            var stageName = $.grep($scope.actionStageList, function(e){ return e.id === orgPolicyMonitor.stageTypeId; })[0].name;
            return stageName + ' (inherited from parent)';
          }
          return '-- do not monitor --';
        }

        $q.all(promises).then(function(results) {
          $scope.applicablePolicies = results[1].data.policiesByOwner;
          $scope.actionStageList = results[2][1];
          $scope.policyMonitoring = clmAppLocations.isApplication() ? results[3].data.appPolicyMonitor : results[3].data.orgPolicyMonitor;
          $scope.policyMonitoringPlaceHolder = createPlaceHolderText(clmAppLocations.isApplication(), results[3].data.orgPolicyMonitor);
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

      function toggleExpanded(selector, action) {
        selector.find('.accordion-body').collapse(action);
        //TODO: to work around collapse bug, fixed in newer release of bootstrap
        //https://github.com/twitter/bootstrap/pull/7424/files
        selector.find('.policy-top')[action ===
          'hide' ? 'addClass' : 'removeClass']('collapsed');
      }

      $scope.toggleAll = function(applicablePolicy) {
        var action = $scope.allExpanded[applicablePolicy.ownerId] ? 'hide' : 'show';
        toggleExpanded($('#' + applicablePolicy.ownerId), action);
        $scope.allExpanded[applicablePolicy.ownerId] = !($scope.allExpanded[applicablePolicy.ownerId] || false);
      };

      $scope.toggleSection = function(id, expanded) {
        var action = expanded === true ? 'hide' : 'show';
        toggleExpanded($('#' + id), action);
      };

      $scope.isExpanded = function(applicablePolicy) {
        return $scope.allExpanded[applicablePolicy.ownerId] || false;
      };

      function clearPolicyMonitoringAlerts(){
        $scope.policyMonitoringAlerts.length = 0;
      }

      $scope.savePolicyMonitoring = function() {
        if ($scope.policyMonitoring.stageTypeId === null) {
          PolicyMonitoringStore.delete().then(clearPolicyMonitoringAlerts, function(error) {
            $scope.policyMonitoringAlerts.push({
              type: 'error',
              msg: 'An error occurred while turning off policy monitoring. (' +
                messages.getHttpErrorMessage(error) + ')'
            });
          });
        }
        else {
          PolicyMonitoringStore.save($scope.policyMonitoring).then(clearPolicyMonitoringAlerts, function(error) {
            $scope.policyMonitoringAlerts.push({
              type: 'error',
              msg: 'An error occurred while saving your policy monitoring configuration. (' +
                messages.getHttpErrorMessage(error) + ')'
            });
          });
        }
      };

      $scope.doLoad();

      $scope.encodeURIComponent = window.encodeURIComponent;

      $scope.allExpanded = {};
    }
  ]);

  policyModule.directive('policyItems', [
    'ActionStore', function(ActionStore) {
      var actionStageList = null;
      ActionStore.get().then(function(data) {
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
        link: function(scope) {
          scope.policyEditMap = {};
          scope.getActionCount = function(policy) {
            var actionCount = 0;
            angular.forEach(policy.actions, function(value) {
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
                if (policy.actions[stage.id][i].actionTypeId === 'warn') {
                  return '../assets/img/policyalert.png';
                }
                else if (policy.actions[stage.id][i].actionTypeId === 'fail') {
                  return '../assets/img/policyerror.png';
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
