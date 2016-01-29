/**
 * @license Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
(function(angular) {
  'use strict';

  function PolicyEditorActionsController($q, $http, CLMAppLocations, StageTypeStore)
  {
    var vm = this;

    vm.doLoad = doLoad;
    vm.actionStages = undefined;
    vm.conditionallyAddOrRemoveAction = conditionallyAddOrRemoveAction;
    vm.roles = undefined;
    vm.getEmailList = getEmailList;
    vm.getRolesList = getRolesList;
    vm.getMonitoringEmailList = getMonitoringEmailList;
    vm.getMonitoringRolesList = getMonitoringRolesList;
    vm.getStageNotificationCount = getStageNotificationCount;
    vm.getMonitoringNotificationCount = getMonitoringNotificationCount;
    vm.getActionItem = getActionItem;
    vm.loadError = undefined;

    vm.doLoad();

    function doLoad() {
      var promises = [
        StageTypeStore.getActionStages(), $http.get(CLMAppLocations.getRoleMappingUrl())
      ];

      $q.all(promises).then(function(results) {
        vm.actionStages = results[0];
        vm.roles = results[1].data.membersByRole;
      }, function(error) {
        vm.loadError = error;
      });

      delete vm.loadError;
    }

    /*
     * Here we check for a warn/fail action for the specified stageId. If warn/fail is in the actions array and we are 
     * toggling between warn and fail, this becomes a noop. If we are going from no-action to warn/fail, we add the 
     * necessary object to the actions array. If no action is selected, we simply remove the warn/fail object from the 
     * array (if it exists).
     */
    function conditionallyAddOrRemoveAction(stageId, action) {
      var addAction = action !== null;

      if (vm.actions[stageId]) {
        vm.actions[stageId].some(function(item, index) {
          if (item.actionTypeId === 'warn' || item.actionTypeId === 'fail') {
            // no action selected, remove the existing warn/fail action
            if (action === null) {
              vm.actions[stageId].splice(index, 1);
              // needed for dirty check to work correctly
              if (vm.actions[stageId].length === 0) {
                vm.actions[stageId] = undefined;
              }
            }
            // we have already removed the action or we are toggling between warn/fail so no need to add anything
            addAction = false;
            return true;
          }
        });
      }
      if (addAction) {
        vm.actions = vm.actions || {};
        if (!vm.actions[stageId]) {
          vm.actions[stageId] = [];
        }
        vm.actions[stageId].push({
          actionTypeId: action,
          target: null
        });
      }
    }

    function getActionItem(stageId) {
      if (vm.actions[stageId]) {
        var actionItem = vm.actions[stageId].filter(function(action) {
          return action.actionTypeId === 'warn' || action.actionTypeId === 'fail';
        });
        if (actionItem.length > 0) {
          return actionItem;
        }
      }
      return null;
    }
    
    function getStageNotificationCount(actionStage) {
      return getEmailList(actionStage).length + getRolesList(actionStage).length; 
    }
    
    function getMonitoringNotificationCount() {
      return getMonitoringEmailList().length + getMonitoringRolesList().length;
    }

    function getEmailList(stage) {
      return extractAddresses(vm.actions[stage.stageTypeId] || []);
    }

    function getRolesList(stage) {
      return extractRoles(vm.actions[stage.stageTypeId] || []);
    }

    function getMonitoringEmailList() {
      return extractAddresses(vm.monitorNotifyActions || []);
    }

    function getMonitoringRolesList() {
      return extractRoles(vm.monitorNotifyActions || []);
    }

    function extractAddresses(actions) {
      // extract the emails for notification action types
      var addresses = [];
      for (var i = 0; i < actions.length; i++) {
        if (actions[i].actionTypeId === 'notify' && !actions[i].targetType) {
          addresses.push(actions[i].target);
        }
      }
      return addresses;
    }

    function extractRoles(actions) {
      if (!vm.roles) {
        return [];
      }
      var roles = [];
      for (var i = 0; i < actions.length; i++) {
        if (actions[i].actionTypeId === 'notify' && actions[i].targetType === 'role') {
          for (var j = 0; j < vm.roles.length; j++) {
            if (actions[i].target === vm.roles[j].roleId) {
              roles.push(vm.roles[j]);
              break;
            }
          }
        }
      }
      return roles;
    }
  }

  PolicyEditorActionsController.$inject = ['$q', '$http', 'CLMAppLocations', 'StageTypeStore'];

  angular //
      .module('owner.manager.module') //
      .controller('policy.editor.actions.controller', PolicyEditorActionsController);

}(angular));
