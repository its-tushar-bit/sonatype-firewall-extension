/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { createSelector } from '@reduxjs/toolkit';
import { prop, isNil, map, indexBy, flatten, any, includes, equals } from 'ramda';

import { selectRouterCurrentParams } from '../reduxUiRouter/routerSelectors';
import {
  selectPoliciesByOwner as mainSelectPoliciesByOwner,
  selectOrgsAndPoliciesSlice,
} from './orgsAndPoliciesSelectors';
import { eqValues, isNilOrEmpty } from 'MainRoot/util/jsUtil';
import { selectConditionTypesMap } from 'MainRoot/OrgsAndPolicies/constraintSelectors';
import { getActionsOverride, getNotificationsOverride } from 'MainRoot/OrgsAndPolicies/utility/util';
import { getDisabledConditions } from 'MainRoot/OrgsAndPolicies/utility/constraintUtil';
import {
  selectIsEnforcementSupported,
  selectIsFirewallSupported,
  selectIsNotificationsSupported,
  selectIsWebhooksSupported,
} from 'MainRoot/productFeatures/productFeaturesSelectors';
import { RECIPIENT_TYPES } from './policySlice';
import { MSG_NO_CHANGES_TO_SAVE } from 'MainRoot/util/constants';

export const selectPolicySlice = createSelector(selectOrgsAndPoliciesSlice, prop('policy'));

export const selectIsEditMode = createSelector(selectRouterCurrentParams, ({ policyId }) => !isNil(policyId));

export const selectHasEditIqPermission = createSelector(selectPolicySlice, prop('hasEditIqPermission'));
export const selectValidationError = createSelector(selectPolicySlice, prop('validationError'));

export const selectIsOrgOwner = createSelector(selectPolicySlice, prop('isOrgOwner'));

export const selectIsInherited = createSelector(selectPolicySlice, prop('isInherited'));

export const selectSiblings = createSelector(selectPolicySlice, prop('siblings'));

export const selectSubmitError = createSelector(selectPolicySlice, prop('submitError'));

export const selectIsRootOrg = createSelector(selectPolicySlice, prop('isRootOrg'));

export const selectOriginalProxyStageAction = createSelector(selectPolicySlice, prop('originalProxyStageAction'));

export const selectPolicyLoadError = createSelector(selectPolicySlice, prop('loadError'));

export const selectPolicyDeleteError = createSelector(selectPolicySlice, prop('deleteError'));

export const selectCategoriesForPolicyLoadError = createSelector(
  selectPolicySlice,
  prop('categoriesForPolicyLoadError')
);

export const selectLoadError = createSelector(
  selectPolicyLoadError,
  selectCategoriesForPolicyLoadError,
  (policyLoadError, categoriesForPolicyLoadError) => {
    return policyLoadError || categoriesForPolicyLoadError;
  }
);

export const selectLoading = createSelector(
  selectPolicySlice,
  (policy) => policy.loadingSavePolicy || policy.loadingCategories || policy.loadingPolicyEditor
);

export const selectDeleteModal = createSelector(selectPolicySlice, prop('deleteModal'));

export const selectCurrentPolicy = createSelector(selectPolicySlice, prop('currentPolicy'));
export const selectCurrentPolicyConstraints = createSelector(selectCurrentPolicy, prop('constraints'));

export const selectIsActionOverrideEnabled = createSelector(
  selectIsInherited,
  selectCurrentPolicy,
  (isInherited, currentPolicy) => isInherited && currentPolicy.policyActionsOverrideAllowed
);

export const selectIsNotificationOverrideEnabled = createSelector(
  selectIsInherited,
  selectCurrentPolicy,
  (isInherited, currentPolicy) => isInherited && currentPolicy.policyNotificationsOverrideAllowed
);

export const selectIsDirty = createSelector(selectPolicySlice, prop('isDirty'));

export const selectHasPolicyCategories = createSelector(selectPolicySlice, prop('hasPolicyCategories'));

export const selectOriginalCategories = createSelector(selectPolicySlice, prop('originalCategories'));

export const selectCategories = createSelector(selectPolicySlice, prop('categories'));

export const selectOriginalHasPolicyCategories = createSelector(selectPolicySlice, prop('originalHasPolicyCategories'));

export const selectIsInheritanceDirty = createSelector(
  selectIsOrgOwner,
  selectHasPolicyCategories,
  selectOriginalHasPolicyCategories,
  selectCategories,
  selectOriginalCategories,
  (isOrgOwner, hasPolicyCategories, originalHasPolicyCategories, categories, originalCategories) =>
    isOrgOwner &&
    ((hasPolicyCategories && !eqValues(originalCategories, categories)) ||
      originalHasPolicyCategories !== hasPolicyCategories)
);

export const selectCurrentPolicyActions = createSelector(selectCurrentPolicy, prop('actions'));
export const selectCurrentPolicyName = createSelector(selectCurrentPolicy, prop('name'));
export const selectCurrentPolicyThreatLevel = createSelector(selectCurrentPolicy, prop('threatLevel'));
export const selectCurrentPolicyViolationGrandfatheringAllowed = createSelector(
  selectCurrentPolicy,
  prop('policyViolationGrandfatheringAllowed')
);
export const selectShouldShowQuarantineWarning = createSelector(
  selectCurrentPolicyActions,
  selectOriginalProxyStageAction,
  selectIsRootOrg,
  (actions, originalProxyStageAction, isRootOrg) =>
    actions?.proxy === 'fail' && originalProxyStageAction !== 'fail' && isRootOrg
);

export const selectIsCurrentPolicyDirty = createSelector(
  selectIsDirty,
  selectIsInheritanceDirty,
  (isDirty, isInheritanceDirty) => isDirty || isInheritanceDirty
);

export const selectIfSubmitButtonShouldBeDisabled = createSelector(
  selectValidationError,
  selectCurrentPolicyConstraints,
  selectConditionTypesMap,
  selectIsCurrentPolicyDirty,
  selectCurrentPolicyName,
  selectIsInherited,
  selectIsActionOverrideEnabled,
  selectIsNotificationOverrideEnabled,
  (
    validationError,
    currentConstraints,
    conditionTypesMap,
    isPolicyDirty,
    policyName,
    isInherited,
    isActionOverrideEnabled,
    isNotificationOverrideEnabled
  ) => {
    const disabled = getDisabledConditions(conditionTypesMap);
    if (!currentConstraints) return;
    const conditions = flatten(map(prop('conditions'), currentConstraints));
    const hasUnsupportedConditions = any((condition) => includes(condition.conditionTypeId, disabled), conditions)
      ? 'Unable to save: unsupported conditions added'
      : null;
    const isNotDirtyMessage = !isPolicyDirty ? MSG_NO_CHANGES_TO_SAVE : null;
    const isNameNotValid =
      policyName.validationErrors?.length > 0 && !policyName.isPristine
        ? 'Unable to save: fields with invalid or missing data'
        : null;
    return (
      (isInherited && !isActionOverrideEnabled && !isNotificationOverrideEnabled) ||
      isNotDirtyMessage ||
      validationError ||
      hasUnsupportedConditions ||
      isNameNotValid
    );
  }
);

export const selectCurrentPolicyOwner = createSelector(selectPolicySlice, prop('currentPolicyOwner'));
export const selectCurrentSubmitMaskState = createSelector(selectPolicySlice, prop('submitMaskState'));
export const selectCurrentPolicyOwnerName = createSelector(selectCurrentPolicyOwner, prop('name'));
export const selectOriginalPolicy = createSelector(selectPolicySlice, prop('originalPolicy'));
export const selectOriginalPolicyName = createSelector(selectOriginalPolicy, prop('name'));

export const selectShowActionsOverridesConfirmationModal = createSelector(
  selectPolicySlice,
  prop('showActionsOverridesConfirmationModal')
);
export const selectShowNotificationsOverridesConfirmationModal = createSelector(
  selectPolicySlice,
  prop('showNotificationsOverridesConfirmationModal')
);

export const selectActionsOverridesCount = createSelector(
  selectOriginalPolicy,
  (originalPolicy) => Object.keys(originalPolicy?.policyActionsOverrides ?? {}).length
);

export const selectNotificationsOverridesCount = createSelector(
  selectOriginalPolicy,
  (originalPolicy) => Object.keys(originalPolicy?.policyNotificationsOverrides ?? {}).length
);

export const selectOverrideActionsFlag = createSelector(selectPolicySlice, prop('overrideActionsFlag'));
export const selectOriginalOverrideActionsFlag = createSelector(selectPolicySlice, prop('originalOverrideActionsFlag'));

export const selectOverrideNotificationsFlag = createSelector(selectPolicySlice, prop('overrideNotificationsFlag'));

export const selectOriginalOverrideNotificationsFlag = createSelector(
  selectPolicySlice,
  prop('originalOverrideNotificationsFlag')
);

export const selectActionsOverrideNeedsToBeAdded = createSelector(
  selectOriginalOverrideActionsFlag,
  selectOverrideActionsFlag,
  (originalOverrideFlag, overrideFlag) => !originalOverrideFlag && overrideFlag
);
export const selectActionsOverrideNeedsToBeRemoved = createSelector(
  selectOriginalOverrideActionsFlag,
  selectOverrideActionsFlag,
  (originalOverrideFlag, overrideFlag) => originalOverrideFlag && !overrideFlag
);

export const selectActionsOverrideNeedsToBeUpdated = createSelector(
  selectOriginalPolicy,
  selectCurrentPolicy,
  (originalPolicy, currentPolicy) =>
    !equals(originalPolicy?.policyActionsOverrides, currentPolicy?.policyActionsOverrides)
);

export const selectNotificationsOverrideNeedsToBeAdded = createSelector(
  selectOriginalOverrideNotificationsFlag,
  selectOverrideNotificationsFlag,
  (originalOverrideFlag, overrideFlag) => !originalOverrideFlag && overrideFlag
);

export const selectNotificationsOverrideNeedsToBeRemoved = createSelector(
  selectOriginalOverrideNotificationsFlag,
  selectOverrideNotificationsFlag,
  (originalOverrideFlag, overrideFlag) => originalOverrideFlag && !overrideFlag
);

export const selectNotificationsOverrideNeedsToBeUpdated = createSelector(
  selectOriginalPolicy,
  selectCurrentPolicy,
  (originalPolicy, currentPolicy) =>
    !equals(originalPolicy?.policyNotificationsOverrides, currentPolicy?.policyNotificationsOverrides)
);

export const selectOverrideNeedsToBeAdded = createSelector(
  selectActionsOverrideNeedsToBeAdded,
  selectNotificationsOverrideNeedsToBeAdded,
  (actionsOverrideNeedsToBeAdded, notificationsOverrideNeedsToBeAdded) =>
    actionsOverrideNeedsToBeAdded || notificationsOverrideNeedsToBeAdded
);

export const selectOverrideNeedsToBeRemoved = createSelector(
  selectActionsOverrideNeedsToBeRemoved,
  selectNotificationsOverrideNeedsToBeRemoved,
  (actionsOverrideNeedsToBeRemoved, notificationsOverrideNeedsToBeRemoved) =>
    actionsOverrideNeedsToBeRemoved || notificationsOverrideNeedsToBeRemoved
);

export const selectOverrideNeedsToBeUpdated = createSelector(
  selectActionsOverrideNeedsToBeUpdated,
  selectNotificationsOverrideNeedsToBeUpdated,
  (actionsOverrideNeedsToBeUpdated, notificationsOverrideNeedsToBeUpdated) =>
    actionsOverrideNeedsToBeUpdated || notificationsOverrideNeedsToBeUpdated
);

export const selectActionsOverridesForCurrentPolicy = createSelector(
  mainSelectPoliciesByOwner,
  selectCurrentPolicy,
  (policiesByOwner, currentPolicy) => {
    const ownerIds = policiesByOwner?.map(prop('ownerId'));
    const actionsOverrideInfo = getActionsOverride(ownerIds, currentPolicy);

    return actionsOverrideInfo?.actionsOverride;
  }
);

export const selectNotificationsOverridesForCurrentPolicy = createSelector(
  mainSelectPoliciesByOwner,
  selectCurrentPolicy,
  (policiesByOwner, currentPolicy) => {
    const ownerIds = policiesByOwner?.map(prop('ownerId'));
    const notificationsOverrideInfo = getNotificationsOverride(ownerIds, currentPolicy);

    return notificationsOverrideInfo?.notificationsOverride;
  }
);

export const selectNotificationsEditor = createSelector(selectPolicySlice, prop('notificationsEditor'));

export const selectNotificationsEditorLoading = createSelector(selectNotificationsEditor, prop('loading'));

export const selectNotificationsEditorLoadError = createSelector(selectNotificationsEditor, prop('loadError'));

export const selectNotificationWebhooks = createSelector(selectNotificationsEditor, prop('notificationWebhooks'));

export const selectNotificationsEditorFormState = createSelector(selectNotificationsEditor, prop('formState'));

export const selectIsNotificationsInheritOverrideEnabled = createSelector(
  selectHasEditIqPermission,
  selectIsNotificationsSupported,
  selectIsFirewallSupported,
  selectIsInherited,
  selectIsNotificationOverrideEnabled,
  (hasEditIqPermission, isNotificationsSupported, isFirewallSupported, isInherited, isNotificationOverrideEnabled) => {
    if (!hasEditIqPermission) {
      return false;
    }
    if (!isNotificationsSupported && !isFirewallSupported) {
      return false;
    }
    if (isInherited && !isNotificationOverrideEnabled) {
      return false;
    }
    return true;
  }
);

export const selectIsNotificationsTableEnabled = createSelector(
  selectIsNotificationsInheritOverrideEnabled,
  selectIsInherited,
  selectOverrideNotificationsFlag,
  (isNotificationsInheritOverrideEnabled, isInherited, overrideNotificationsFlag) => {
    if (!isNotificationsInheritOverrideEnabled) {
      return false;
    }
    if (isInherited && !overrideNotificationsFlag) {
      return false;
    }
    return true;
  }
);

export const selectIsActionsInheritOverrideEnabled = createSelector(
  selectHasEditIqPermission,
  selectIsEnforcementSupported,
  selectIsFirewallSupported,
  selectIsInherited,
  selectIsActionOverrideEnabled,
  (hasEditIqPermission, isEnforcementSupported, isFirewallSupported, isInherited, isActionOverrideEnabled) => {
    if (!hasEditIqPermission) {
      return false;
    }
    if (!isEnforcementSupported && !isFirewallSupported) {
      return false;
    }
    if (isInherited && !isActionOverrideEnabled) {
      return false;
    }
    return true;
  }
);

export const selectIsActionsTableEnabled = createSelector(
  selectIsActionsInheritOverrideEnabled,
  selectIsInherited,
  selectOverrideActionsFlag,
  (isActionsInheritOverrideEnabled, isInherited, overrideActionsFlag) => {
    if (!isActionsInheritOverrideEnabled) {
      return false;
    }
    if (isInherited && !overrideActionsFlag) {
      return false;
    }
    return true;
  }
);

export const selectNotifications = createSelector(
  selectIsNotificationOverrideEnabled,
  selectOverrideNotificationsFlag,
  selectNotificationsOverridesForCurrentPolicy,
  selectCurrentPolicy,
  (notificationOverrideEnabled, overrideNotificationsFlag, notificationOverrides, currentPolicy = {}) => {
    return notificationOverrideEnabled && overrideNotificationsFlag
      ? notificationOverrides ?? {}
      : currentPolicy?.notifications ?? {};
  }
);

export const selectApplicableWebhooks = createSelector(
  selectNotifications,
  selectNotificationWebhooks,
  (notifications, webhooks) => {
    const { webhookNotifications } = notifications;
    const isNotAlreadyUsedForNotifications = (webhook) => !webhookNotifications.some((n) => webhook.id === n.webhookId);
    const toWebhookWithDisplayName = (webhook) => ({ ...webhook, displayName: webhook.description ?? webhook.url });

    // If there are webhooks already used for notifications, we remove them from the list
    const applicableWebhooks =
      isNilOrEmpty(webhookNotifications) || isNilOrEmpty(webhooks)
        ? webhooks
        : webhooks?.filter(isNotAlreadyUsedForNotifications);

    return (applicableWebhooks ?? []).map(toWebhookWithDisplayName);
  }
);

export const selectRolesForCurrentOwner = createSelector(selectNotificationsEditor, prop('roles'));
export const selectAvailableRoles = createSelector(
  selectNotifications,
  selectRolesForCurrentOwner,
  (notifications, roles) => {
    const { roleNotifications = [] } = notifications;
    const isNotPresentInNotificationSettings = ({ roleId }) => !roleNotifications.some((n) => roleId === n.roleId);

    if (isNilOrEmpty(roleNotifications)) return roles;
    else return roles?.filter(isNotPresentInNotificationSettings);
  }
);

export const selectIsJiraEnabled = createSelector(selectNotificationsEditor, prop('isJiraEnabled'));

export const selectJiraProjects = createSelector(selectNotificationsEditor, prop('jiraProjects'));

export const selectJiraProjectNames = createSelector(selectJiraProjects, (jiraProjects) => {
  if (isNilOrEmpty(jiraProjects)) return {};
  return jiraProjects.reduce((names, project) => ({ ...names, [project.key]: project.name }), {});
});

export const selectJiraIssueTypeNames = createSelector(selectJiraProjects, (jiraProjects) => {
  if (isNilOrEmpty(jiraProjects)) return {};
  return jiraProjects.reduce((issueTypes, project) => {
    const projectIssueTypes = project.issueTypes.reduce(
      (issueTypes, issueType) => ({ ...issueTypes, [issueType.id]: issueType.name }),
      {}
    );

    return { ...issueTypes, ...projectIssueTypes };
  }, {});
});

export const selectNotificationRecipientTypeOptions = createSelector(
  selectIsJiraEnabled,
  selectIsWebhooksSupported,
  (isJiraEnabled, isWebhooksSupported) => {
    const recipientTypeOptions = [RECIPIENT_TYPES.EMAIL, RECIPIENT_TYPES.ROLE];
    if (isWebhooksSupported) {
      recipientTypeOptions.push(RECIPIENT_TYPES.WEBHOOK);
    }
    if (isJiraEnabled) {
      recipientTypeOptions.push(RECIPIENT_TYPES.JIRA);
    }

    return recipientTypeOptions;
  }
);

export const selectAvailableJiraProjects = createSelector(
  selectNotifications,
  selectJiraProjects,
  (notifications, jiraProjects = []) => {
    const { jiraNotifications = [] } = notifications;
    if (isNilOrEmpty(jiraProjects)) return [];

    return jiraProjects.filter((project) => {
      return !jiraNotifications.some((notification) => {
        return project.key === notification.projectKey;
      });
    });
  }
);

export const selectNotificationRecipients = createSelector(
  selectNotifications,
  selectNotificationWebhooks,
  selectRolesForCurrentOwner,
  selectJiraProjectNames,
  selectJiraIssueTypeNames,
  (notifications, notificationWebhooks, roles = [], jiraProjectNames = {}, jiraIssueTypes = {}) => {
    const rolesIndexedById = indexBy(prop('roleId'), roles ?? []);
    const {
      roleNotifications = [],
      userNotifications = [],
      jiraNotifications = [],
      webhookNotifications = [],
    } = notifications;

    const getJiraDisplayName = (recipient) => {
      if (jiraProjectNames?.[recipient.projectKey] && jiraIssueTypes[recipient.issueTypeId]) {
        return jiraProjectNames[recipient.projectKey] + ' (' + jiraIssueTypes[recipient.issueTypeId] + ')';
      }
      return recipient.projectKey + ' (Issue Type ID: ' + recipient.issueTypeId + ')';
    };

    const getWebhookDisplayName = (recipient) => {
      if (recipient.webhookId) {
        const webhook = !isNilOrEmpty(notificationWebhooks)
          ? notificationWebhooks.find((webhook) => recipient.webhookId === webhook.id)
          : undefined;
        if (webhook) return 'Webhook: ' + (webhook.description ? webhook.description : webhook.url);
        else return 'Undefined webhook: ' + recipient.webhookId;
      }
    };

    const getDisplayName = (recipient) => {
      return (
        recipient.emailAddress ||
        rolesIndexedById?.[recipient.roleId]?.roleName ||
        getWebhookDisplayName(recipient) ||
        getJiraDisplayName(recipient) ||
        ''
      );
    };

    const recipients = userNotifications
      .concat(roleNotifications, webhookNotifications, jiraNotifications)
      .map((recipient) => ({ ...recipient, displayName: getDisplayName(recipient) }))
      .sort((a, b) => a.displayName.localeCompare(b.displayName));

    return recipients;
  }
);

export const selectSelectedJiraProject = createSelector(
  selectAvailableJiraProjects,
  selectNotificationsEditorFormState,
  (availableJiraProjects, notificationsEditorFormState) =>
    availableJiraProjects.find((p) => p.key === notificationsEditorFormState?.recipientProjectKey?.value)
);

export const selectPolicyTile = createSelector(selectPolicySlice, prop('policyTile'));
export const selectPoliciesByOwner = createSelector(selectPolicyTile, prop('policiesByOwner'));
export const selectPolicyTileLoading = createSelector(selectPolicyTile, prop('loading'));
export const selectPolicyTileLoadError = createSelector(selectPolicyTile, prop('loadError'));
export const selectPolicyTileSorting = createSelector(selectPolicyTile, prop('sorting'));
