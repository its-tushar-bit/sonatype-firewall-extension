/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { createSelector } from '@reduxjs/toolkit';
import {
  prop,
  isNil,
  map,
  indexBy,
  flatten,
  any,
  includes,
  equals,
  isEmpty,
  path,
  curryN,
  values,
  omit,
  mapObjIndexed,
} from 'ramda';

import {
  selectIsRepositoriesRelated,
  selectIsRepositoryContainer,
  selectRouterCurrentParams,
} from '../reduxUiRouter/routerSelectors';
import {
  selectPoliciesByOwner as mainSelectPoliciesByOwner,
  selectOrgsAndPoliciesSlice,
} from './orgsAndPoliciesSelectors';
import { anyIndexed, eqValues, isNilOrEmpty } from 'MainRoot/util/jsUtil';
import { selectConditionTypesMap } from 'MainRoot/OrgsAndPolicies/constraintSelectors';
import { getActionsOverride, getNotificationsOverride } from 'MainRoot/OrgsAndPolicies/utility/util';
import { conditionsWithoutValue, getDisabledConditions } from 'MainRoot/OrgsAndPolicies/utility/constraintUtil';
import {
  selectIsEnforcementSupported,
  selectIsFirewallSupported,
  selectIsNotificationsSupported,
  selectIsPolicyWebhooksSupported,
} from 'MainRoot/productFeatures/productFeaturesSelectors';
import { initialState, RECIPIENT_TYPES } from './policySlice';
import { MSG_NO_CHANGES_TO_SAVE } from 'MainRoot/util/constants';
import { validateForm } from 'MainRoot/util/validationUtil';
import {
  A_NAME,
  CARGO,
  COCOAPODS,
  COMPOSER,
  CONAN,
  HF_MODEL,
  MAVEN,
  NPM,
  PYPI,
} from 'MainRoot/OrgsAndPolicies/formats';

export const selectPolicySlice = createSelector(selectOrgsAndPoliciesSlice, prop('policy'));

export const selectIsEditMode = createSelector(selectRouterCurrentParams, ({ policyId }) => !isNil(policyId));

export const selectHasEditIqPermission = createSelector(selectPolicySlice, prop('hasEditIqPermission'));

const computeValidatableFieldsForCoordinates = (fields) => {
  if (fields.format === MAVEN) {
    return values(omit(['format', isEmpty(fields.classifier?.trimmedValue) ? 'classifier' : null], fields));
  } else if (fields.format === A_NAME) {
    return values(omit(['format', isEmpty(fields.qualifier?.trimmedValue) ? 'qualifier' : null], fields));
  } else if (fields.format === PYPI) {
    return values(
      omit(
        [
          'format',
          isEmpty(fields.qualifier?.trimmedValue) ? 'qualifier' : null,
          isEmpty(fields.extension?.trimmedValue) ? 'extension' : null,
        ],
        fields
      )
    );
  } else if (fields.format === CONAN) {
    return values(
      omit(
        [
          'format',
          isEmpty(fields.channel?.trimmedValue) ? 'channel' : null,
          isEmpty(fields.owner?.trimmedValue) ? 'owner' : null,
        ],
        fields
      )
    );
  } else if (fields.format === CARGO) {
    return values(omit(['format', isEmpty(fields.type?.trimmedValue) ? 'type' : null], fields));
  } else if ([NPM, COCOAPODS, COMPOSER, HF_MODEL].includes(fields.format)) {
    return values(omit(['format'], fields));
  }
};

export const selectValidationError = createSelector(selectPolicySlice, (state) => {
  const { currentPolicy } = state,
    constraints = currentPolicy?.constraints ?? [],
    fields = [];

  constraints.forEach((constraint) => {
    fields.push(constraint.name);

    constraint.conditions.forEach((condition) => {
      if (!includes(condition.conditionTypeId, conditionsWithoutValue)) {
        if (condition.conditionTypeId === 'Coordinates') {
          fields.push(...computeValidatableFieldsForCoordinates(condition.value));
        } else {
          fields.push(condition.value);
        }
      }
    });
  });

  const validationError = validateForm(fields);
  return validationError;
});

export const selectIsOrgOwner = createSelector(selectPolicySlice, prop('isOrgOwner'));

export const selectIsRepositoryContainerOwner = createSelector(selectPolicySlice, prop('isRepositoryContainerOwner'));

export const selectIsRepositoryManagerOwner = createSelector(selectPolicySlice, prop('isRepositoryManagerOwner'));

export const selectIsRepositoryOwner = createSelector(selectPolicySlice, prop('isRepositoryOwner'));

export const selectIsInherited = createSelector(selectPolicySlice, prop('isInherited'));

export const selectSiblings = createSelector(selectPolicySlice, prop('siblings'));

export const selectSubmitError = createSelector(selectPolicySlice, prop('submitError'));

export const selectIsRootOrg = createSelector(selectPolicySlice, prop('isRootOrg'));

export const selectOriginalProxyStageAction = createSelector(selectPolicySlice, prop('originalProxyStageAction'));

export const selectPolicyLoadError = createSelector(selectPolicySlice, prop('loadError'));

export const selectPolicyDeleteError = createSelector(selectPolicySlice, prop('deleteError'));

export const selectOriginalPolicy = createSelector(selectPolicySlice, prop('originalPolicy'));

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

const hasDirtyProps = curryN(3, (observedProps, originalPolicy, currentPolicy) => {
  return isNil(originalPolicy)
    ? false // currently loading or error state so no props to dirty
    : any((prop) => !equals(path(prop, currentPolicy), path(prop, originalPolicy)), observedProps);
});

const isDirtyConstraints = (originalConstraints, currentConstraints) => {
  if (!originalConstraints?.length && !currentConstraints?.length) return false;
  if (originalConstraints?.length !== currentConstraints?.length) return true;
  const dirty = anyIndexed((constrain, idx) => {
    const originalConstraint = originalConstraints?.[idx];

    const isNumberOfConditionsDifferent = originalConstraint?.conditions?.length !== constrain.conditions.length;
    if (isNumberOfConditionsDifferent) {
      return true;
    }

    const observedProps = [['name', 'trimmedValue'], ['operator']];
    const constraintHasDirtyProps = hasDirtyProps(observedProps, originalConstraint, constrain);

    const isAnyConditionDirty = anyIndexed((condition, conditionIdx) => {
      const originalCondition = originalConstraint.conditions[conditionIdx];
      const commonConditionPropDirty = any((prop) => !equals(condition[prop], originalCondition[prop]), [
        'conditionTypeId',
        'operator',
      ]);

      if (condition.conditionTypeId === 'Coordinates') {
        if (originalCondition.conditionTypeId !== 'Coordinates') {
          return true;
        }
        const currentValues = omit(['format'], mapObjIndexed(prop('trimmedValue'), condition.value));
        const originalValues = omit(['format'], mapObjIndexed(prop('trimmedValue'), originalCondition.value));

        const isValueDirty =
          !equals(currentValues, originalValues) || condition.value.format !== originalCondition.value.format;

        return commonConditionPropDirty || isValueDirty;
      }

      return commonConditionPropDirty || condition.value?.trimmedValue !== originalCondition.value?.trimmedValue;
    }, constrain.conditions);

    return constraintHasDirtyProps || isAnyConditionDirty;
  }, currentConstraints);

  return dirty;
};

const selectObservedPropsAreDirty = createSelector(
  selectOriginalPolicy,
  selectCurrentPolicy,
  (originalPolicy, currentPolicy) => {
    const observedProps = [
      ['name', 'value'],
      ['threatLevel'],
      ['legacyViolationAllowed'],
      ['policyActionsOverrideAllowed'],
      ['policyNotificationsOverrideAllowed'],
    ];

    // we can't just use `hasDirtyProps` here because the currentPolicy has a default threatLevel of 5, which is
    // not "empty"
    return originalPolicy
      ? hasDirtyProps(observedProps, originalPolicy, currentPolicy)
      : !equals(currentPolicy, initialState.currentPolicy);
  }
);

const selectActionsAreDirty = createSelector(
  selectOriginalPolicy,
  selectCurrentPolicy,
  hasDirtyProps([['actions'], ['policyActionsOverrides']])
);

const selectNotificationsAreDirty = createSelector(
  selectOriginalPolicy,
  selectCurrentPolicy,
  hasDirtyProps([['notifications'], ['policyNotificationsOverrides']])
);

const selectContraintsAreDirty = createSelector(
  selectOriginalPolicy,
  selectCurrentPolicy,
  (originalPolicy, currentPolicy) =>
    isNil(originalPolicy) ? false : isDirtyConstraints(originalPolicy.constraints, currentPolicy?.constraints)
);

const selectIsCategoriesDirty = createSelector(
  selectPolicySlice,
  ({ categories, originalCategories }) => !equals(categories, originalCategories)
);

export const selectIsDirty = createSelector(
  selectPolicySlice,
  selectObservedPropsAreDirty,
  selectActionsAreDirty,
  selectNotificationsAreDirty,
  selectContraintsAreDirty,
  selectIsCategoriesDirty,
  (state, isDirty, isDirtyActions, isDirtyNotifications, isConstraintsDirty, isDirtyCategories) => {
    const {
      isInherited,
      overrideActionsFlag,
      originalOverrideActionsFlag,
      overrideNotificationsFlag,
      originalOverrideNotificationsFlag,
      hasPolicyCategories,
      originalHasPolicyCategories,
    } = state;

    const policyActionOverrideIsDirty = overrideActionsFlag !== originalOverrideActionsFlag;
    const policyNotificationOverrideIsDirty = overrideNotificationsFlag !== originalOverrideNotificationsFlag;
    const isDirtyHasPolicyCategories = hasPolicyCategories !== originalHasPolicyCategories;

    if (overrideActionsFlag && isDirtyActions) {
      return isDirtyActions;
    }

    if (overrideNotificationsFlag && isDirtyNotifications) {
      return isDirtyNotifications;
    }

    if (!policyActionOverrideIsDirty && !policyNotificationOverrideIsDirty && isInherited) {
      return isDirty;
    }

    const isContentDirty = isDirty || isConstraintsDirty || isDirtyHasPolicyCategories || isDirtyCategories;
    return (
      isContentDirty ||
      (policyActionOverrideIsDirty && isInherited) ||
      isDirtyActions ||
      (policyNotificationOverrideIsDirty && isInherited) ||
      isDirtyNotifications
    );
  }
);

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
export const selectCurrentLegacyViolationAllowed = createSelector(selectCurrentPolicy, prop('legacyViolationAllowed'));
export const selectShouldShowQuarantineWarning = createSelector(
  selectCurrentPolicyActions,
  selectOriginalProxyStageAction,
  selectIsRootOrg,
  selectIsRepositoryContainer,
  (actions, originalProxyStageAction, isRootOrg, isRepositoryContainer) =>
    actions?.proxy === 'fail' && originalProxyStageAction !== 'fail' && (isRootOrg || isRepositoryContainer)
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
  selectIsRepositoriesRelated,
  (
    hasEditIqPermission,
    isNotificationsSupported,
    isFirewallSupported,
    isInherited,
    isNotificationOverrideEnabled,
    isRepositoriesRelated
  ) => {
    if (isRepositoriesRelated && !isInherited) {
      return true;
    }
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
  selectIsRepositoriesRelated,
  (isNotificationsInheritOverrideEnabled, isInherited, overrideNotificationsFlag, isRepositoriesRelated) => {
    if (isRepositoriesRelated && !isInherited) {
      return true;
    }
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
  selectIsPolicyWebhooksSupported,
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
export const selectPolicyTileSortingCollapsible = createSelector(selectPolicyTile, prop('collapsibleSorting'));
