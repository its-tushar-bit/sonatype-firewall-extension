/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

import { actions } from 'MainRoot/OrgsAndPolicies/policySlice';
import { findIndex, propEq } from 'ramda';
export default function PolicyEditorNotificationsController(
  $scope,
  $q,
  RoleMappingService,
  StageTypeStore,
  JiraService,
  ProductFeatures,
  NotificationWebhookService,
  $ngRedux
) {
  var vm = this,
    availableRoles,
    availableWebhooks,
    roleNames,
    jiraProjects,
    jiraIssueTypes,
    jiraProjectNames;

  vm.addRecipientForm = undefined;
  vm.loadError = undefined;
  vm.jiraError = undefined;
  vm.actionStages = undefined;
  vm.recipients = undefined;
  vm.recipientTypes = {
    EMAIL: 'Email',
    ROLE: 'Role',
    JIRA: 'JIRA',
    WEBHOOK: 'Webhook',
  };
  vm.recipientType = vm.recipientTypes.EMAIL;
  vm.recipientToAdd = '';
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
  vm.isMonitoringSupported = undefined;
  vm.isNotificationsSupported = undefined;
  vm.isFirewallSupported = undefined;
  vm.resetNotifications = resetNotifications;
  vm.isNotificationsSupportedForStage = ProductFeatures.isNotificationsSupportedForStage;
  vm.isNotificationsFormDisabled = isNotificationsFormDisabled;
  vm.isCheckboxForStageDisabled = isCheckboxForStageDisabled;
  vm.getAvailableWebhooks = getAvailableWebhooks;
  vm.unsubscribe = $ngRedux.connect(null, {
    setUserNotificationsAction: actions.setUserNotifications,
    setRoleNotificationsAction: actions.setRoleNotifications,
    setJiraNotificationsAction: actions.setJiraNotifications,
    setWebhookNotificationsAction: actions.setWebhookNotifications,
    setUserNotificationStageIdsAction: actions.setUserNotificationStageIds,
    setRoleNotificationStageIdsAction: actions.setRoleNotificationStageIds,
    setJiraNotificationStageIdsAction: actions.setJiraNotificationStageIds,
    setWebhookNotificationStageIdsAction: actions.setWebhookNotificationStageIds,
  })(vm);
  vm.doLoad();

  $scope.$watch('vm.notifications', function (newValue, oldValue) {
    if (newValue === oldValue) {
      return;
    }
    loadRecipients();
  });

  $scope.$on('$destroy', () => {
    vm.unsubscribe();
  });

  function doLoad() {
    var promises = [
      StageTypeStore.getActionStages(),
      RoleMappingService.get(),
      JiraService.isEnabled().then(function (isEnabled) {
        if (isEnabled) {
          var getJiraDeferred = $q.defer();
          JiraService.getJiraProjects().then(
            function (results) {
              getJiraDeferred.resolve({
                projects: results,
              });
            },
            function (error) {
              getJiraDeferred.resolve({
                error: error,
              });
            }
          );
          return getJiraDeferred.promise;
        }
      }),
      ProductFeatures.load().then(function () {
        return loadWebhooksIfSupported();
      }),
    ];

    $q.all(promises).then(
      function (results) {
        vm.actionStages = results[0];
        vm.roles = results[1].membersByRole;
        var jiraResults = results[2];
        var webhookResults = results[3];

        if (!jiraResults) {
          // JIRA is disabled
        } else if (jiraResults.error) {
          vm.jiraError = jiraResults.error;
        } else {
          if (vm.recipientTypeOptions.indexOf(vm.recipientTypes.JIRA) === -1) {
            vm.recipientTypeOptions.push(vm.recipientTypes.JIRA);
          }
          jiraProjects = jiraResults.projects;
        }

        if (!webhookResults || !vm.isWebhooksSupported) {
          // webhooks is disabled or not licensed
        } else if (webhookResults.webhookError) {
          vm.webhookError = webhookResults.webhookError;
        } else {
          vm.webhooks = webhookResults.webhooks;
        }

        roleNames = vm.roles ? mapRoleNames() : {};

        mapJiraProjectsAndIssueTypes();

        updateAvailableRoles();
        updateAvailableJiraProjects();
        loadRecipients();

        vm.isMonitoringSupported = ProductFeatures.isAvailable('policy-monitoring');
        vm.isNotificationsSupported = ProductFeatures.isAvailable('notifications');
        vm.isFirewallSupported = ProductFeatures.isAvailable('firewall');

        if (vm.isWebhooksSupported) {
          updateAvailableWebhooks();
          if (vm.recipientTypeOptions.indexOf(vm.recipientTypes.WEBHOOK) === -1) {
            vm.recipientTypeOptions.push(vm.recipientTypes.WEBHOOK);
          }
        }
      },
      function (error) {
        vm.loadError = error;
      }
    );

    delete vm.loadError;
    delete vm.jiraError;
    delete vm.webhookError;
  }

  function loadWebhooksIfSupported() {
    vm.isWebhooksSupported =
      ProductFeatures.isAvailable('webhooks-for-applications') ||
      ProductFeatures.isAvailable('webhooks-for-repositories');
    if (vm.isWebhooksSupported) {
      var getWebhooksDeferred = $q.defer();
      NotificationWebhookService.get().then(
        function (results) {
          getWebhooksDeferred.resolve({
            webhooks: results,
          });
        },
        function (error) {
          getWebhooksDeferred.resolve({
            webhookError: error,
          });
        }
      );
      return getWebhooksDeferred.promise;
    }
  }

  // produces sorted Array of all Recipients
  function loadRecipients() {
    var userNotifications = vm.notifications.userNotifications || [];
    var roleNotifications = vm.notifications.roleNotifications || [];
    var jiraNotifications = vm.notifications.jiraNotifications || [];

    vm.recipients = userNotifications
      .concat(roleNotifications)
      .concat(jiraNotifications)
      .sort(function (a, b) {
        return getDisplayName(a).localeCompare(getDisplayName(b));
      });

    var webhookNotifications = vm.notifications.webhookNotifications || [];
    vm.recipients = userNotifications
      .concat(roleNotifications)
      .concat(jiraNotifications)
      .concat(webhookNotifications)
      .sort(function (a, b) {
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
    } else if (vm.recipientType === vm.recipientTypes.ROLE) {
      addRoleRecipient(vm.recipientToAdd.roleId);
    } else if (vm.recipientType === vm.recipientTypes.JIRA) {
      addJiraRecipient();
    } else if (vm.recipientType === vm.recipientTypes.WEBHOOK) {
      addWebhookRecipient();
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

      vm.setRoleNotificationsAction(vm.notifications.roleNotifications);
      updateAvailableRoles();
    } else if (recipient.emailAddress) {
      vm.notifications.userNotifications.splice(vm.notifications.userNotifications.indexOf(recipient), 1);

      vm.setUserNotificationsAction(vm.notifications.userNotifications);
    } else if (recipient.projectKey) {
      vm.notifications.jiraNotifications.splice(vm.notifications.jiraNotifications.indexOf(recipient), 1);

      vm.setJiraNotificationsAction(vm.notifications.jiraNotifications);
      updateAvailableJiraProjects();
    } else if (recipient.webhookId) {
      vm.notifications.webhookNotifications.splice(vm.notifications.webhookNotifications.indexOf(recipient), 1);

      vm.setWebhookNotificationsAction(vm.notifications.webhookNotifications);
      updateAvailableWebhooks();
    }
  }

  function getDisplayName(recipient) {
    return (
      recipient.emailAddress ||
      roleNames?.[recipient.roleId] ||
      getWebhookDisplayName(recipient) ||
      getJiraDisplayName(recipient)
    );
  }

  function getJiraDisplayName(recipient) {
    if (!vm.jiraError && jiraProjectNames?.[recipient.projectKey] && jiraIssueTypes[recipient.issueTypeId]) {
      return jiraProjectNames[recipient.projectKey] + ' (' + jiraIssueTypes[recipient.issueTypeId] + ')';
    }
    return recipient.projectKey + ' (Issue Type ID: ' + recipient.issueTypeId + ')';
  }

  function getWebhookDisplayName(recipient) {
    if (recipient.webhookId) {
      var webhook = vm.webhooks
        ? vm.webhooks.find(function (webhook) {
            return recipient.webhookId === webhook.id;
          })
        : undefined;
      if (webhook) {
        return 'Webhook: ' + (webhook.description ? webhook.description : webhook.url);
      } else {
        return 'Undefined webhook: ' + recipient.webhookId;
      }
    }
  }

  function toggleStage(recipient, stage) {
    var index = recipient.stageIds.indexOf(stage);
    if (index !== -1) {
      recipient.stageIds.splice(index, 1);
    } else {
      recipient.stageIds.push(stage);
    }

    const updatedStageIds = recipient.stageIds;

    let notificationIndexToUpdate;

    if (recipient.roleId) {
      notificationIndexToUpdate = findIndex(propEq('roleId', recipient.roleId), vm.notifications.roleNotifications);

      vm.setRoleNotificationStageIdsAction({
        index: notificationIndexToUpdate,
        value: updatedStageIds,
      });
    } else if (recipient.emailAddress) {
      notificationIndexToUpdate = findIndex(
        propEq('emailAddress', recipient.emailAddress),
        vm.notifications.userNotifications
      );
      vm.setUserNotificationStageIdsAction({ index: notificationIndexToUpdate, value: updatedStageIds });
    } else if (recipient.projectKey) {
      notificationIndexToUpdate = findIndex(
        propEq('projectKey', recipient.projectKey),
        vm.notifications.jiraNotifications
      );
      vm.setJiraNotificationStageIdsAction({ index: notificationIndexToUpdate, value: updatedStageIds });
    } else if (recipient.webhookId) {
      notificationIndexToUpdate = findIndex(
        propEq('webhookId', recipient.webhookId),
        vm.notifications.webhookNotifications
      );
      vm.setWebhookNotificationStageIdsAction({ index: notificationIndexToUpdate, value: updatedStageIds });
    }
  }

  function addEmailRecipient(email) {
    if (emailExists(email)) {
      return;
    }

    var newNotification = {
      emailAddress: email,
      stageIds: [],
    };

    vm.notifications.userNotifications.push(newNotification);
    vm.setUserNotificationsAction(vm.notifications.userNotifications);
    vm.recipients.push(newNotification);
  }

  function addRoleRecipient(roleId) {
    var newNotification = {
      roleId: roleId,
      stageIds: [],
    };
    vm.notifications.roleNotifications.push(newNotification);
    vm.setRoleNotificationsAction(vm.notifications.roleNotifications);
    vm.recipients.push(newNotification);
    updateAvailableRoles();
  }

  function addJiraRecipient() {
    var newNotification = {
      projectKey: vm.recipientToAdd.key,
      issueTypeId: vm.recipientToAddIssueType.id,
      stageIds: [],
    };
    vm.notifications.jiraNotifications.push(newNotification);
    vm.setJiraNotificationsAction(vm.notifications.jiraNotifications);
    vm.recipients.push(newNotification);
    updateAvailableJiraProjects();
  }

  function addWebhookRecipient() {
    var newNotification = {
      webhookId: vm.recipientToAdd.id,
      stageIds: [],
    };
    vm.notifications.webhookNotifications.push(newNotification);
    vm.setWebhookNotificationsAction(vm.notifications.webhookNotifications);
    vm.recipients.push(newNotification);
    updateAvailableWebhooks();
  }

  function emailExists(email) {
    return vm.notifications.userNotifications.some(function (entry) {
      return entry.emailAddress === email;
    });
  }

  function hasRecipients() {
    return vm.recipients.length !== 0;
  }

  function mapRoleNames() {
    return vm.roles.reduce(function (map, role) {
      map[role.roleId] = role.roleName;
      return map;
    }, {});
  }

  function mapJiraProjectsAndIssueTypes() {
    jiraProjectNames = {};
    jiraIssueTypes = {};

    if (jiraProjects) {
      jiraProjects.forEach(function (project) {
        jiraProjectNames[project.key] = project.name;
        project.issueTypes.forEach(function (issueType) {
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

    availableRoles = vm.roles.filter(function (role) {
      return !vm.notifications.roleNotifications.some(function (notification) {
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

    vm.availableJiraProjects = jiraProjects.filter(function (project) {
      return !vm.notifications.jiraNotifications.some(function (notification) {
        return project.key === notification.projectKey;
      });
    });
  }

  function getEmails() {
    return vm.notifications.userNotifications.map(function (entry) {
      return entry.emailAddress;
    });
  }

  function updateAvailableWebhooks() {
    if (!vm.notifications.webhookNotifications || vm.notifications.webhookNotifications.length === 0 || !vm.webhooks) {
      availableWebhooks = vm.webhooks;
    } else {
      availableWebhooks = vm.webhooks.filter(function (webhook) {
        return !vm.notifications.webhookNotifications.some(function (notification) {
          return webhook.id === notification.webhookId;
        });
      });
    }
    if (availableWebhooks) {
      availableWebhooks.forEach(function (webhook) {
        webhook.displayName = webhook.description ? webhook.description : webhook.url;
      });
    }
  }

  function getAvailableWebhooks() {
    return availableWebhooks;
  }

  function isAddButtonDisabled() {
    return (
      (vm.recipientType !== vm.recipientTypes.JIRA && !vm.recipientToAdd) ||
      (vm.recipientType === vm.recipientTypes.JIRA && (!vm.recipientToAdd || !vm.recipientToAddIssueType)) ||
      vm.isNotificationsFormDisabled()
    );
  }

  function resetNotifications() {
    vm.recipientToAdd = undefined;
    vm.recipientToAddIssueType = undefined;
  }

  function isNotificationsFormDisabled() {
    return vm.disabled || !ProductFeatures.isNotificationsSupportedForAnyStage();
  }

  function isCheckboxForStageDisabled(recipient, stageTypeId) {
    return (
      (recipient?.webhookId && stageTypeId === 'proxy') ||
      vm.disabled ||
      !vm.isStageApplicable(recipient, stageTypeId) ||
      !vm.isNotificationsSupportedForStage(stageTypeId)
    );
  }
}

PolicyEditorNotificationsController.$inject = [
  '$scope',
  '$q',
  'role.mapping.service',
  'StageTypeStore',
  'jira.service',
  'ProductFeatures',
  'notification.webhook.service',
  '$ngRedux',
];
