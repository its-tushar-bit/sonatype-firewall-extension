/**
 * @license Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
/*global angular, $, clmBuildTimestamp */
/*jslint plusplus: true */
(function() {
  'use strict';

  var policyModule = angular.module('Policy',
      ['PolicyEditor', 'CLMAppLocation', 'AngularCommon', 'CommonServices', 'Stores', 'ProductFeaturesModule', 'Tags']);

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
        remove: function(){
          return $http['delete'](CLMAppLocations.getPolicyMonitoringUrl());
        }
      };
    }
  ]);

  policyModule.controller('PolicyController', [
    '$scope', '$location', '$http', '$rootScope', '$q', 'PolicyStore', 'StageTypeStore', 'ErrorDialog',
    'CLMAppLocations', 'Dialog', 'ownerChange', 'PolicyMonitoringStore', 'Messages', 'ProductFeatures', 'TagStore',
    function($scope, $location, $http, $rootScope, $q, policyStore, StageTypeStore, ErrorDialog, clmAppLocations, Dialog,
            ownerChange, PolicyMonitoringStore, messages, ProductFeatures, TagStore) {

      $scope.alerts = [];
      $scope.location = $location;
      $scope.policyMonitoringAlerts = [];
      $scope.policyTagMap = {};

      $scope.isPolicyMonitoringLicensed = function() {
        return ProductFeatures.isAvailable('policy-monitoring');
      };

      $scope.viewRemovePolicy = function(policy) {
        Dialog.open({
          title : 'Delete Policy',
          body : 'Are you sure you want to delete the Policy named "' + policy.name + '"? This action is not reversible.',
          buttons : [{
            name : 'Cancel',
            type : 'cancel'
          },{
            name : 'Delete',
            type : 'danger',
            click : function () {
              policy.$delete().then(angular.noop, function() {
                ErrorDialog.open(arguments[0]);
              });
            }
          }]
        });
      };

      $scope.doLoad = function() {
        $scope.error = null;
        $scope.applicablePolicies = null;
        $scope.actionStageList = null;
        $scope.tags = [];

        var promises = [
          policyStore.get().then(function(store) {
            return store.refresh();
          }),
          $http.get(clmAppLocations.getApplicablePolicies()),
          StageTypeStore.get(),
          PolicyMonitoringStore.getApplicable()
        ];
        if (!clmAppLocations.isApplication()) {
          promises.push(TagStore.refresh());
        }

        $q.all(promises).then(function(results) {
          $scope.applicablePolicies = results[1].data.policiesByOwner;
          $scope.actionStageList = results[2];

          var policyMonitoringByOwner = results[3].data.policyMonitoringByOwner;
          $scope.policyMonitoring = policyMonitoringByOwner[0].policyMonitoring || {};
          policyMonitoringByOwner.some(function(policyMonitoringOwner, ownerIndex) {
            if (ownerIndex === 0) {
              return false;
            }
            if (policyMonitoringOwner.policyMonitoring) {
              var stageName = $.grep($scope.actionStageList, function(e) {
                return e.id === policyMonitoringOwner.policyMonitoring.stageTypeId;
              })[0].name;
              $scope.policyMonitoringPlaceHolder =  stageName + ' (inherited from ' + policyMonitoringOwner.ownerName + ')';
              return true;
            }
          });
          if (!$scope.policyMonitoringPlaceHolder) {
            $scope.policyMonitoringPlaceHolder = '-- do not monitor --';
          }

          angular.forEach($scope.applicablePolicies, function(applicablePolicy, index) {
            applicablePolicy.editable = index === 0;
            if (index === 0) {
              applicablePolicy.policies = results[0];
            }
            angular.forEach(applicablePolicy.policyTags, function(policyTag){
              var tags = $scope.policyTagMap[policyTag.policyId] || [];
              tags.push(policyTag);
              $scope.policyTagMap[policyTag.policyId] = tags;
            });
          });

          if (results.length > 3) {
            $scope.tags = [];
            angular.forEach(results[4], function (owner) {
              $scope.tags.push.apply($scope.tags, owner.tags);
            });
          }
        }, function(errors) {
          $scope.error = angular.isArray(errors) ? errors[0] : errors;
        });
      };

      $scope.$on('ownerChanged', ownerChange.getEventHandler($scope, 'applicablePolicies'));
      $scope.$on('refresh', $scope.doLoad);
      //update mapping of Policy -> Tag whenever we save a Policy
      $scope.$on('policySaveComplete', function(event, isOrganization, policyId, policyTags){
        if (isOrganization) {
          $scope.policyTagMap[policyId] = policyTags;
        }
      });

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
          PolicyMonitoringStore.remove().then(clearPolicyMonitoringAlerts, function(error) {
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
    'StageTypeStore', function(StageTypeStore) {
      var actionStageList = null;
      StageTypeStore.get().then(function(data) {
        actionStageList = data;
      });
      return {
        restrict: 'A',
        templateUrl: '../policy-assets/components/policy/policy-items.html?' + clmBuildTimestamp,
        scope: {
          ownerName: '=',
          policies: '=policyItems',
          editable: '=editable',
          remove: '=',
          tags: '=',
          policyTagMap: '='
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
          scope.getStageClass = function(stage, policy) {
            if (policy.actions[stage.id]) {
              for (var i = 0; i < policy.actions[stage.id].length; i++) {
                if (policy.actions[stage.id][i].actionTypeId === 'warn') {
                  return 'sonatype-icons warn';
                }
                else if (policy.actions[stage.id][i].actionTypeId === 'fail') {
                  return 'sonatype-icons fail';
                }
              }
            }
          };

          scope.edit = function(policy) {
            scope.policyEditMap[policy.id] = !scope.policyEditMap[policy.id];
            $('#collapse' + policy.id).collapse(scope.policyEditMap[policy.id] ? 'show' : 'hide');
          };

          scope.isTagged = function(policy){
            return (policy.id in scope.policyTagMap && scope.policyTagMap[policy.id].length > 0);
          };

          scope.getTitle = function(policy){
            var title = '';
            if(scope.isTagged(policy)){
              title = 'This Policy applies only to Applications with one of the corresponding Tags';
            }
            return title;
          };
        }
      };
    }
  ]);
}());
