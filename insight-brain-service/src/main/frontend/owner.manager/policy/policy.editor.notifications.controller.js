/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
(function(angular) {
  'use strict';

  function PolicyEditorNotificationsController($scope, $q, $http, CLMAppLocations, StageTypeStore)
  {
    var vm = this,
        availableRoles,
        roleNames;

    vm.loadError = undefined;
    vm.actionStages = undefined;
    vm.recipients = undefined;
    vm.recipientTypes = {EMAIL: 'Email', ROLE: 'Role'};
    vm.recipientType = vm.recipientTypes.EMAIL;
    vm.recipientToAdd = undefined;
    vm.recipientTypeOptions = [vm.recipientTypes.EMAIL, vm.recipientTypes.ROLE];
    vm.addRecipient = addRecipient;
    vm.hasStage = hasStage;
    vm.removeRecipient = removeRecipient;
    vm.toggleStage = toggleStage;
    vm.addEmailRecipient = addEmailRecipient;
    vm.addRoleRecipient = addRoleRecipient;
    vm.getDisplayName = getDisplayName;
    vm.hasRecipients = hasRecipients;
    vm.getAvailableRoles = getAvailableRoles;
    vm.getEmails = getEmails;
    vm.doLoad = doLoad;

    vm.doLoad();

    $scope.$watch('vm.notifications', function(newValue, oldValue) {
      if (newValue === oldValue) {
        return;
      }
      loadRecipients();
    });

    function doLoad() {
      var promises = [
        StageTypeStore.getActionStages(),
        $http.get(CLMAppLocations.getRoleMappingUrl())
      ];

      $q.all(promises).then(function(results) {
        vm.actionStages = results[0];
        vm.roles = results[1].data.membersByRole;
        roleNames = vm.roles ? mapRoleNames() : {};

        updateAvailableRoles();
        loadRecipients();

      }, function(error) {
        vm.loadError = error;
      });

      delete vm.loadError;
    }

    // produces sorted Array of all Recipients
    function loadRecipients() {
      var userNotifications = vm.notifications.userNotifications || [];
      var roleNotifications = vm.notifications.roleNotifications || [];
      vm.recipients = userNotifications.concat(roleNotifications).sort(function(a, b) {
        return getDisplayName(a).localeCompare(getDisplayName(b));
      });
    }

    function addRecipient(keypressEvent) {
      if (keypressEvent) {
        keypressEvent.preventDefault();
      }
      
      if (!vm.recipientToAdd) {
        return;
      }

      if (vm.recipientType === vm.recipientTypes.EMAIL) {
        addEmailRecipient(vm.recipientToAdd);
      }
      else {
        addRoleRecipient(vm.recipientToAdd.roleId);
      }
      vm.recipientToAdd = undefined;
    }

    function hasStage(notification, stage) {
      return notification.stageIds.indexOf(stage) !== -1;
    }

    function removeRecipient(recipient) {
      vm.recipients.splice(vm.recipients.indexOf(recipient), 1);

      // remove notifications from original policy notifications
      if (recipient.roleId) {
        vm.notifications.roleNotifications.splice(vm.notifications.roleNotifications.indexOf(recipient), 1);
        updateAvailableRoles();
      }
      else {
        vm.notifications.userNotifications.splice(vm.notifications.userNotifications.indexOf(recipient), 1);
      }
    }

    function getDisplayName(recipient) {
      return recipient.emailAddress || roleNames[recipient.roleId];
    }

    function toggleStage(recipient, stage) {
      var index = recipient.stageIds.indexOf(stage);
      if (index !== -1) {
        recipient.stageIds.splice(index, 1);
      }
      else {
        recipient.stageIds.push(stage);
      }
    }

    function addEmailRecipient(email) {
      if (emailExists(email)) {
        return;
      }

      var newNotification = {
        emailAddress: email,
        stageIds: []
      };
      vm.notifications.userNotifications.push(newNotification);
      vm.recipients.push(newNotification);
    }

    function addRoleRecipient(roleId) {
      var newNotification = {
        roleId: roleId,
        stageIds: []
      };
      vm.notifications.roleNotifications.push(newNotification);
      vm.recipients.push(newNotification);
      updateAvailableRoles();
    }

    function emailExists(email) {
      return vm.notifications.userNotifications.some(function(entry) {
        return entry.emailAddress === email;
      });
    }

    function hasRecipients() {
      return vm.recipients.length !== 0;
    }

    function mapRoleNames() {
      return vm.roles.reduce(function(map, role) {
        map[role.roleId] = role.roleName;
        return map;
      }, {});
    }

    function updateAvailableRoles() {
      if (!vm.notifications.roleNotifications || vm.notifications.roleNotifications.length === 0) {
        availableRoles = vm.roles;
        return;
      }

      availableRoles = vm.roles.filter(function(role) {
        return !vm.notifications.roleNotifications.some(function(notification) {
          return role.roleId === notification.roleId;
        });
      });
    }

    function getAvailableRoles() {
      return availableRoles;
    }

    function getEmails() {
      return vm.notifications.userNotifications.map(function(entry) {
        return entry.emailAddress;
      });
    }

  }

  PolicyEditorNotificationsController.$inject = [
    '$scope', '$q', '$http', 'CLMAppLocations', 'StageTypeStore'
  ];

  angular //
      .module('owner.manager.module') //
      .controller('policy.editor.notifications.controller', PolicyEditorNotificationsController);

}(angular));
