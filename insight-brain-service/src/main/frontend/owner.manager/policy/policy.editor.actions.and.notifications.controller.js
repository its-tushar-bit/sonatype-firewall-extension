/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
(function(angular) {
  'use strict';

  function PolicyEditorActionsAndNotificationsController($q, $http, CLMAppLocations, StageTypeStore)
  {
    var vm = this,
        monitoringStage = 'monitoring';

    vm.doLoad = doLoad;

    vm.hasNotifications = hasNotifications;
    vm.removeNotification = removeNotification;
    vm.addNotification = addNotification;
    vm.getNotificationTargetName = getNotificationTargetName;
    vm.actionStages = undefined;
    vm.conditionallyAddOrRemoveAction = conditionallyAddOrRemoveAction;
    vm.roles = undefined;
    vm.availableRoles = {};
    vm.getEmailList = getEmailList;
    vm.getRolesList = getRolesList;
    vm.getNotificationCount = getNotificationCount;
    vm.getActionItem = getActionItem;
    vm.notificationTypes = ['Email', 'Role'];
    vm.notificationTypeMap = undefined;
    vm.notificationValueMap = undefined;
    vm.loadError = undefined;
    vm.updateAvailableRoles = updateAvailableRoles;

    vm.doLoad();

    function doLoad() {
      var promises = [
        StageTypeStore.getActionStages(), $http.get(CLMAppLocations.getRoleMappingUrl())
      ];

      $q.all(promises).then(function(results) {
        vm.actionStages = angular.copy(results[0]);
        vm.roles = results[1].data.membersByRole;
        vm.notificationValueMap = {};
        vm.notificationTypeMap = {};
        vm.actionStages.push({stageName: 'Continuous Monitoring', stageTypeId: monitoringStage});
        vm.actionStages.forEach(function(action) {
          vm.notificationTypeMap[action.stageTypeId] = vm.notificationTypes[0];
        });
      }, function(error) {
        vm.loadError = error;
      });

      delete vm.loadError;
    }

    function hasNotifications(stageId) {
      if (stageId === monitoringStage) {
        return vm.monitorNotifyActions;
      }
      else {
        return vm.actions[stageId] && vm.actions[stageId].some(function(action) {
              return action.actionTypeId === 'notify';
            });
      }
    }

    function removeNotification(stageId, removalTarget) {
      if (stageId === monitoringStage) {
        vm.monitorNotifyActions = vm.monitorNotifyActions.filter(function(action) {
          return action.target !== removalTarget;
        });
      }
      else {
        vm.actions[stageId] = vm.actions[stageId].filter(function(action) {
          return action.target !== removalTarget || action.actionTypeId !== 'notify';
        });
      }
      vm.updateAvailableRoles(stageId);
    }

    function addNotification(stageId) {
      var target = vm.notificationValueMap[stageId];
      if (!targetExists(stageId, target)) {
        if (stageId === monitoringStage) {
          vm.monitorNotifyActions = vm.monitorNotifyActions || [];
        }
        else {
          vm.actions[stageId] = vm.actions[stageId] || [];
        }
        if (target.roleId) {
          (stageId === monitoringStage ? vm.monitorNotifyActions : vm.actions[stageId]).push({
            actionTypeId: 'notify',
            target: target.roleId,
            targetType: 'role'
          });
          vm.updateAvailableRoles(stageId);
        }
        else {
          (stageId === monitoringStage ? vm.monitorNotifyActions : vm.actions[stageId]).push({
            actionTypeId: 'notify',
            target: target
          });
        }
      }
      vm.notificationValueMap[stageId] = undefined;
    }

    function getNotificationTargetName(action) {
      var result;
      if (action.targetType === 'role') {
        vm.roles.some(function(role) {
          if (role.roleId === action.target) {
            result = role.roleName;
            return true;
          }
        });
      }
      else {
        result = action.target;
      }
      return result;
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

    function getNotificationCount(actionStage) {
      return getEmailList(actionStage.stageTypeId).length + getRolesList(actionStage.stageTypeId).length;
    }

    function getEmailList(stageTypeId) {
      return extractAddresses((stageTypeId ===
          monitoringStage ? vm.monitorNotifyActions : vm.actions[stageTypeId] ) || []);
    }

    function getRolesList(stageTypeId) {
      return extractRoles((stageTypeId ===
          monitoringStage ? vm.monitorNotifyActions : vm.actions[stageTypeId] ) || []);
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

    function targetExists(stageId, target) {
      target = target.roleId || target;
      var stageActions = stageId === monitoringStage ? vm.monitorNotifyActions : vm.actions[stageId];
      return stageActions && stageActions.some(function(action) {
            return action.actionTypeId === 'notify' && action.target === target;
          });
    }

    function updateAvailableRoles(stageTypeId) {
      var roles = getRolesList(stageTypeId);
      var availableRoles = angular.copy(vm.roles) || [];
      vm.availableRoles[stageTypeId] = availableRoles.filter(function(availableRole) {
        return !roles.some(function(role) {
          return role.roleId === availableRole.roleId; 
        });
      });
    }
  }

  PolicyEditorActionsAndNotificationsController.$inject = ['$q', '$http', 'CLMAppLocations', 'StageTypeStore'];

  angular //
      .module('owner.manager.module') //
      .controller('policy.editor.actions.and.notifications.controller', PolicyEditorActionsAndNotificationsController);

}(angular));
