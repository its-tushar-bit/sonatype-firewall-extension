/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
(function(angular) {
  'use strict';

  function PolicyEditorNotificationsController($scope, $q, $http, CLMAppLocations, StageTypeStore, JiraService)
  {
    var vm = this,
        availableRoles,
        roleNames,
        jiraProjects,
        jiraIssueTypes,
        jiraProjectNames;

    vm.addRecipientForm = undefined;
    vm.loadError = undefined;
    vm.jiraError = undefined;
    vm.actionStages = undefined;
    vm.recipients = undefined;
    vm.recipientTypes = {EMAIL: 'Email', ROLE: 'Role', JIRA: 'JIRA'};
    vm.recipientType = vm.recipientTypes.EMAIL;
    vm.recipientToAdd = undefined;
    vm.recipientTypeOptions = [vm.recipientTypes.EMAIL, vm.recipientTypes.ROLE];
    vm.availableJiraProjects = undefined;
    vm.addRecipient = addRecipient;
    vm.hasStage = hasStage;
    vm.isStageApplicable = isStageApplicable;
    vm.removeRecipient = removeRecipient;
    vm.toggleStage = toggleStage;
    vm.addEmailRecipient = addEmailRecipient;
    vm.addRoleRecipient = addRoleRecipient;
    vm.getDisplayName = getDisplayName;
    vm.hasRecipients = hasRecipients;
    vm.getAvailableRoles = getAvailableRoles;
    vm.getEmails = getEmails;
    vm.doLoad = doLoad;
    vm.isAddButtonDisabled = isAddButtonDisabled;
    vm.resetNotifications = resetNotifications;

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
        $http.get(CLMAppLocations.getRoleMappingUrl()),
        JiraService.isEnabled().then(function(isEnabled) {
          if (isEnabled) {
            var getJiraDeferred = $q.defer();
            JiraService.getJiraProjects().then(function(results) {
              getJiraDeferred.resolve({
                projects: results
              });
            }, function(error) {
              getJiraDeferred.resolve({
                error: error
              });
            });
            return getJiraDeferred.promise;
          }
        })
      ];

      $q.all(promises).then(function(results) {
        vm.actionStages = results[0];
        vm.roles = results[1].data.membersByRole;
        var jiraResults = results[2];

        if (!jiraResults.error) {
          if (vm.recipientTypeOptions.indexOf(vm.recipientTypes.JIRA) === -1) {
            vm.recipientTypeOptions.push(vm.recipientTypes.JIRA);
          }
          jiraProjects = jiraResults.projects;
        }
        else {
          vm.jiraError = jiraResults.error;
        }

        roleNames = vm.roles ? mapRoleNames() : {};

        mapJiraProjectsAndIssueTypes();

        updateAvailableRoles();
        updateAvailableJiraProjects();
        loadRecipients();

      }, function(error) {
        vm.loadError = error;
      });

      delete vm.loadError;
      delete vm.jiraError;
    }

    // produces sorted Array of all Recipients
    function loadRecipients() {
      var userNotifications = vm.notifications.userNotifications || [];
      var roleNotifications = vm.notifications.roleNotifications || [];
      var jiraNotifications = vm.notifications.jiraNotifications || [];
      vm.recipients = userNotifications.concat(roleNotifications).concat(jiraNotifications).sort(function(a, b) {
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
      else if (vm.recipientType === vm.recipientTypes.ROLE) {
        addRoleRecipient(vm.recipientToAdd.roleId);
      }
      else if (vm.recipientType === vm.recipientTypes.JIRA) {
        addJiraRecipient();
      }

      resetNotifications();
      vm.addRecipientForm.$setPristine();
    }

    function hasStage(notification, stage) {
      return notification.stageIds.indexOf(stage) !== -1;
    }

    function isStageApplicable(notification, stage) {
      return !notification.projectKey || stage !== 'proxy';
    }

    function removeRecipient(recipient) {
      vm.recipients.splice(vm.recipients.indexOf(recipient), 1);

      // remove notifications from original policy notifications
      if (recipient.roleId) {
        vm.notifications.roleNotifications.splice(vm.notifications.roleNotifications.indexOf(recipient), 1);
        updateAvailableRoles();
      }
      else if (recipient.emailAddress) {
        vm.notifications.userNotifications.splice(vm.notifications.userNotifications.indexOf(recipient), 1);
      }
      else if (recipient.projectKey) {
        vm.notifications.jiraNotifications.splice(vm.notifications.jiraNotifications.indexOf(recipient), 1);
        updateAvailableJiraProjects();
      }
    }

    function getDisplayName(recipient) {
      return recipient.emailAddress || roleNames[recipient.roleId] || getJiraDisplayName(recipient);
    }

    function getJiraDisplayName(recipient) {
      if (!vm.jiraError && jiraProjectNames[recipient.projectKey] && jiraIssueTypes[recipient.issueTypeId]) {
        return jiraProjectNames[recipient.projectKey] + ' (' + jiraIssueTypes[recipient.issueTypeId] + ')';
      }
      return recipient.projectKey + ' (Issue Type ID: ' + recipient.issueTypeId + ')';
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

    function addJiraRecipient() {
      var newNotification = {
        projectKey: vm.recipientToAdd.key,
        issueTypeId: vm.recipientToAddIssueType.id,
        stageIds: []
      };
      vm.notifications.jiraNotifications.push(newNotification);
      vm.recipients.push(newNotification);
      updateAvailableJiraProjects();
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

    function mapJiraProjectsAndIssueTypes() {
      jiraProjectNames = {};
      jiraIssueTypes = {};

      if (jiraProjects) {
        jiraProjects.forEach(function(project) {
          jiraProjectNames[project.key] = project.name;
          project.issueTypes.forEach(function(issueType) {
            jiraIssueTypes[issueType.id] = issueType.name;
          });
        });
      }
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

    function updateAvailableJiraProjects() {
      if (!jiraProjects) {
        return;
      }

      if (!vm.notifications.jiraNotifications || vm.notifications.jiraNotifications.length === 0) {
        vm.availableJiraProjects = jiraProjects;
        return;
      }

      vm.availableJiraProjects = jiraProjects.filter(function(project) {
        return !vm.notifications.jiraNotifications.some(function(notification) {
          return project.key === notification.projectKey;
        });
      });
    }

    function getEmails() {
      return vm.notifications.userNotifications.map(function(entry) {
        return entry.emailAddress;
      });
    }

    function isAddButtonDisabled() {
      return (vm.recipientType !== vm.recipientTypes.JIRA && !vm.recipientToAdd) ||
          (vm.recipientType === vm.recipientTypes.JIRA && (!vm.recipientToAdd || !vm.recipientToAddIssueType)) ||
          vm.disabled;
    }

    function resetNotifications() {
      vm.recipientToAdd = undefined;
      vm.recipientToAddIssueType = undefined;
    }
  }

  PolicyEditorNotificationsController.$inject = [
    '$scope', '$q', '$http', 'CLMAppLocations', 'StageTypeStore', 'jira.service'
  ];

  angular //
      .module('owner.manager.module') //
      .controller('policy.editor.notifications.controller', PolicyEditorNotificationsController);

}(angular));
