/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { selectConditionTypesMap } from 'MainRoot/OrgsAndPolicies/constraintSelectors';
import { selectOrgsAndPoliciesSlice, selectPoliciesByOwner } from 'MainRoot/OrgsAndPolicies/orgsAndPoliciesSelectors';
import {
  selectPolicySlice,
  selectIsEditMode,
  selectIsOrgOwner,
  selectIsInherited,
  selectSiblings,
  selectSubmitError,
  selectIsRootOrg,
  selectOriginalProxyStageAction,
  selectDeleteModal,
  selectLoading,
  selectLoadError,
  selectIsDirty,
  selectOriginalCategories,
  selectHasPolicyCategories,
  selectCategories,
  selectOriginalHasPolicyCategories,
  selectIsInheritanceDirty,
  selectCurrentPolicy,
  selectCurrentPolicyActions,
  selectShouldShowQuarantineWarning,
  selectIsCurrentPolicyDirty,
  selectCategoriesForPolicyLoadError,
  selectPolicyLoadError,
  selectIsActionOverrideEnabled,
  selectHasEditIqPermission,
  selectOriginalPolicy,
  selectActionsOverrideNeedsToBeRemoved,
  selectOverrideActionsFlag,
  selectOriginalOverrideActionsFlag,
  selectActionsOverridesForCurrentPolicy,
  selectOriginalPolicyName,
  selectCurrentPolicyName,
  selectCurrentPolicyThreatLevel,
  selectCurrentLegacyViolationAllowed,
  selectNotificationWebhooks,
  selectCrossProductWebhooks,
  selectApplicableWebhooks,
  selectNotificationRecipients,
  selectNotificationsEditorFormState,
  selectRolesForCurrentOwner,
  selectIsJiraEnabled,
  selectJiraIssueTypeNames,
  selectJiraProjectNames,
  selectJiraProjects,
  selectNotificationRecipientTypeOptions,
  selectAvailableJiraProjects,
  selectSelectedJiraProject,
  selectNotificationsEditor,
  selectNotificationsEditorLoading,
  selectNotificationsEditorLoadError,
  selectIfSubmitButtonShouldBeDisabled,
  selectValidationError,
  selectCurrentPolicyConstraints,
  selectPolicyDeleteError,
  selectPolicyTileSortingCollapsible,
  selectCurrentSubmitMaskState,
  selectIsNotificationOverrideEnabled,
  selectOverrideNotificationsFlag,
  selectNotificationsOverridesForCurrentPolicy,
  selectOriginalOverrideNotificationsFlag,
  selectNotifications,
  selectIsNotificationsInheritOverrideEnabled,
  selectActionsOverrideNeedsToBeAdded,
  selectNotificationsOverrideNeedsToBeRemoved,
  selectNotificationsOverrideNeedsToBeAdded,
  selectOverrideNeedsToBeAdded,
  selectOverrideNeedsToBeRemoved,
  selectActionsOverrideNeedsToBeUpdated,
  selectNotificationsOverrideNeedsToBeUpdated,
  selectOverrideNeedsToBeUpdated,
  selectShowActionsOverridesConfirmationModal,
  selectShowNotificationsOverridesConfirmationModal,
  selectActionsOverridesCount,
  selectNotificationsOverridesCount,
  selectIsNotificationsTableEnabled,
  selectIsActionsInheritOverrideEnabled,
  selectIsActionsTableEnabled,
} from 'MainRoot/OrgsAndPolicies/policySelectors';
import {
  selectIsEnforcementSupported,
  selectIsFirewallSupported,
  selectIsNotificationsSupported,
  selectIsPolicyWebhooksSupported,
} from 'MainRoot/productFeatures/productFeaturesSelectors';
import {
  selectIsRepositoriesRelated,
  selectIsRepositoryContainer,
  selectRouterCurrentParams,
} from 'MainRoot/reduxUiRouter/routerSelectors';
import { RECIPIENT_TYPES } from 'MainRoot/OrgsAndPolicies/policySlice';

describe('policySelectors', () => {
  describe('selectPolicySlice', () => {
    it('is composed from the following selector', () => {
      expect(selectPolicySlice.dependencies).toEqual([selectOrgsAndPoliciesSlice]);
    });

    it('selects policy slice', () => {
      const orgsAndPoliciesSlice = {
        policy: 'policy',
      };

      const selected = selectPolicySlice.resultFunc(orgsAndPoliciesSlice);

      expect(selected).toBe('policy');
    });
  });

  describe('selectIsEditMode', () => {
    it('is composed from the following selector', () => {
      expect(selectIsEditMode.dependencies).toEqual([selectRouterCurrentParams]);
    });

    it('returns true if policyId exist in the current param', () => {
      const routerCurrentParams = {
        policyId: 'policyId',
      };

      const selected = selectIsEditMode.resultFunc(routerCurrentParams);

      expect(selected).toBe(true);
    });

    it('returns false if policyId does not exist in the current param', () => {
      const selected = selectIsEditMode.resultFunc({});

      expect(selected).toBe(false);
    });
  });

  describe('selectIsOrgOwner', () => {
    it('is composed from the following selector', () => {
      expect(selectIsOrgOwner.dependencies).toEqual([selectPolicySlice]);
    });

    it('selects isOrgOwner', () => {
      const policySlice = {
        isOrgOwner: true,
      };

      const selected = selectIsOrgOwner.resultFunc(policySlice);

      expect(selected).toBe(true);
    });
  });

  describe('selectHasEditIqPermission', () => {
    it('is composed from the following selector', () => {
      expect(selectHasEditIqPermission.dependencies).toEqual([selectPolicySlice]);
    });

    it('selects hasEditIqPermission', () => {
      const policySlice = {
        hasEditIqPermission: true,
      };

      const selected = selectHasEditIqPermission.resultFunc(policySlice);

      expect(selected).toBe(true);
    });
  });

  describe('selectIsInherited', () => {
    it('is composed from the following selector', () => {
      expect(selectIsInherited.dependencies).toEqual([selectPolicySlice]);
    });

    it('selects isInherited', () => {
      const policySlice = {
        isInherited: true,
      };

      const selected = selectIsInherited.resultFunc(policySlice);

      expect(selected).toBe(true);
    });
  });

  describe('selectSiblings', () => {
    it('is composed from the following selector', () => {
      expect(selectSiblings.dependencies).toEqual([selectPolicySlice]);
    });

    it('selects siblings', () => {
      const siblings = [{ id: '123' }];
      const policySlice = {
        siblings,
      };

      const selected = selectSiblings.resultFunc(policySlice);

      expect(selected).toEqual(siblings);
    });
  });

  describe('selectSubmitError', () => {
    it('is composed from the following selector', () => {
      expect(selectSubmitError.dependencies).toEqual([selectPolicySlice]);
    });

    it('selects submitError', () => {
      const policySlice = {
        submitError: 'someError',
      };

      const selected = selectSubmitError.resultFunc(policySlice);

      expect(selected).toBe('someError');
    });
  });

  describe('selectIsRootOrg', () => {
    it('is composed from the following selector', () => {
      expect(selectIsRootOrg.dependencies).toEqual([selectPolicySlice]);
    });

    it('selects isRootOrg', () => {
      const policySlice = {
        isRootOrg: true,
      };

      const selected = selectIsRootOrg.resultFunc(policySlice);

      expect(selected).toBe(true);
    });
  });

  describe('selectOriginalProxyStageAction', () => {
    it('is composed from the following selector', () => {
      expect(selectOriginalProxyStageAction.dependencies).toEqual([selectPolicySlice]);
    });

    it('selects originalProxyStageAction', () => {
      const policySlice = {
        originalProxyStageAction: 'warn',
      };

      const selected = selectOriginalProxyStageAction.resultFunc(policySlice);

      expect(selected).toEqual('warn');
    });
  });

  describe('selectPolicyLoadError', () => {
    it('is composed from the following selector', () => {
      expect(selectPolicyLoadError.dependencies).toEqual([selectPolicySlice]);
    });

    it('selects policy loadError', () => {
      const policySlice = {
        loadError: 'someError',
      };

      const selected = selectPolicyLoadError.resultFunc(policySlice);

      expect(selected).toBe('someError');
    });
  });

  describe('selectOriginalPolicy', () => {
    it('is composed from the following selector', () => {
      expect(selectOriginalPolicy.dependencies).toEqual([selectPolicySlice]);
    });

    it('selects originalPolicy', () => {
      const policySlice = {
        originalPolicy: {
          ownerId: 'id',
        },
      };

      const selected = selectOriginalPolicy.resultFunc(policySlice);

      expect(selected).toEqual(policySlice.originalPolicy);
    });
  });

  describe('selectOriginalPolicyName', () => {
    it('is composed from the following selector', () => {
      expect(selectOriginalPolicyName.dependencies).toEqual([selectOriginalPolicy]);
    });

    it('selects originalPolicy name', () => {
      const originalPolicy = {
        name: 'Policy name',
      };

      const selected = selectOriginalPolicyName.resultFunc(originalPolicy);

      expect(selected).toBe('Policy name');
    });
  });

  describe('selectOverrideActionsFlag', () => {
    it('is composed from the following selector', () => {
      expect(selectOverrideActionsFlag.dependencies).toEqual([selectPolicySlice]);
    });

    it('selects overrideActionsFlag property', () => {
      const policySlice = {
        overrideActionsFlag: false,
      };

      const selected = selectOverrideActionsFlag.resultFunc(policySlice);

      expect(selected).toBe(false);
    });
  });

  describe('selectOverrideNotificationsFlag', () => {
    it('is composed from the following selector', () => {
      expect(selectOverrideNotificationsFlag.dependencies).toEqual([selectPolicySlice]);
    });

    it('selects overrideNotificationsFlag property', () => {
      const policySlice = {
        overrideNotificationsFlag: false,
      };

      const selected = selectOverrideNotificationsFlag.resultFunc(policySlice);

      expect(selected).toBe(false);
    });
  });

  describe('selectOriginalOverrideActionsFlag', () => {
    it('is composed from the following selector', () => {
      expect(selectOriginalOverrideActionsFlag.dependencies).toEqual([selectPolicySlice]);
    });

    it('selects originalOverrideActionsFlag property', () => {
      const policySlice = {
        originalOverrideActionsFlag: false,
      };

      const selected = selectOriginalOverrideActionsFlag.resultFunc(policySlice);

      expect(selected).toBe(false);
    });
  });

  describe('selectOriginalOverrideNotificationsFlag', () => {
    it('is composed from the following selector', () => {
      expect(selectOriginalOverrideNotificationsFlag.dependencies).toEqual([selectPolicySlice]);
    });

    it('selects originalOverrideNotificationsFlag property', () => {
      const policySlice = {
        originalOverrideNotificationsFlag: false,
      };

      const selected = selectOriginalOverrideNotificationsFlag.resultFunc(policySlice);

      expect(selected).toBe(false);
    });
  });

  describe('selectActionsOverrideNeedsToBeAdded', () => {
    it('is composed from the following selector', () => {
      expect(selectActionsOverrideNeedsToBeAdded.dependencies).toEqual([
        selectOriginalOverrideActionsFlag,
        selectOverrideActionsFlag,
      ]);
    });

    it('selects whether actions override should not be added', () => {
      const overrideActionsFlag = true;
      const originalOverrideActionsFlag = true;

      const selected = selectActionsOverrideNeedsToBeAdded.resultFunc(originalOverrideActionsFlag, overrideActionsFlag);

      expect(selected).toBe(false);
    });

    it('selects whether actions override should be added', () => {
      const overrideActionsFlag = true;
      const originalOverrideActionsFlag = false;

      const selected = selectActionsOverrideNeedsToBeAdded.resultFunc(originalOverrideActionsFlag, overrideActionsFlag);

      expect(selected).toBe(true);
    });
  });

  describe('selectActionsOverrideNeedsToBeRemoved', () => {
    it('is composed from the following selector', () => {
      expect(selectActionsOverrideNeedsToBeRemoved.dependencies).toEqual([
        selectOriginalOverrideActionsFlag,
        selectOverrideActionsFlag,
      ]);
    });

    it('selects whether actions override should not be removed', () => {
      const overrideActionsFlag = true;
      const originalOverrideActionsFlag = true;

      const selected = selectActionsOverrideNeedsToBeRemoved.resultFunc(
        originalOverrideActionsFlag,
        overrideActionsFlag
      );

      expect(selected).toBe(false);
    });

    it('selects whether actions override should be removed', () => {
      const overrideActionsFlag = false;
      const originalOverrideActionsFlag = true;

      const selected = selectActionsOverrideNeedsToBeRemoved.resultFunc(
        originalOverrideActionsFlag,
        overrideActionsFlag
      );

      expect(selected).toBe(true);
    });
  });

  describe('selectActionsOverrideNeedsToBeUpdated', () => {
    it('is composed from the following selector', () => {
      expect(selectActionsOverrideNeedsToBeUpdated.dependencies).toEqual([selectOriginalPolicy, selectCurrentPolicy]);
    });

    it('selects whether actions override should not be updated', () => {
      const originalPolicy = {
        policyActionsOverrides: { currentOwnerId: { build: 'warn' } },
      };
      const currentPolicy = {
        policyActionsOverrides: { currentOwnerId: { build: 'warn' } },
      };

      const selected = selectActionsOverrideNeedsToBeUpdated.resultFunc(originalPolicy, currentPolicy);

      expect(selected).toBe(false);
    });

    it('selects whether actions override should be updated', () => {
      const originalPolicy = {
        policyActionsOverrides: { currentOwnerId: { build: 'warn' } },
      };
      const currentPolicy = {
        policyActionsOverrides: { currentOwnerId: { build: 'fail' } },
      };

      const selected = selectActionsOverrideNeedsToBeUpdated.resultFunc(originalPolicy, currentPolicy);

      expect(selected).toBe(true);
    });
  });

  describe('selectNotificationsOverrideNeedsToBeAdded', () => {
    it('is composed from the following selector', () => {
      expect(selectNotificationsOverrideNeedsToBeAdded.dependencies).toEqual([
        selectOriginalOverrideNotificationsFlag,
        selectOverrideNotificationsFlag,
      ]);
    });

    it('selects whether notifications override should not be added', () => {
      const overrideNotificationsFlag = true;
      const originalOverrideNotificationsFlag = true;

      const selected = selectNotificationsOverrideNeedsToBeAdded.resultFunc(
        originalOverrideNotificationsFlag,
        overrideNotificationsFlag
      );

      expect(selected).toBe(false);
    });

    it('selects whether notifications override should be added', () => {
      const overrideNotificationsFlag = true;
      const originalOverrideNotificationsFlag = false;

      const selected = selectNotificationsOverrideNeedsToBeAdded.resultFunc(
        originalOverrideNotificationsFlag,
        overrideNotificationsFlag
      );

      expect(selected).toBe(true);
    });
  });

  describe('selectNotificationsOverrideNeedsToBeRemoved', () => {
    it('is composed from the following selector', () => {
      expect(selectNotificationsOverrideNeedsToBeRemoved.dependencies).toEqual([
        selectOriginalOverrideNotificationsFlag,
        selectOverrideNotificationsFlag,
      ]);
    });

    it('selects whether notifications override should not be removed', () => {
      const overrideNotificationsFlag = true;
      const originalOverrideNotificationsFlag = true;

      const selected = selectNotificationsOverrideNeedsToBeRemoved.resultFunc(
        originalOverrideNotificationsFlag,
        overrideNotificationsFlag
      );

      expect(selected).toBe(false);
    });

    it('selects whether notifications override should be removed', () => {
      const overrideNotificationsFlag = false;
      const originalOverrideNotificationsFlag = true;

      const selected = selectNotificationsOverrideNeedsToBeRemoved.resultFunc(
        originalOverrideNotificationsFlag,
        overrideNotificationsFlag
      );

      expect(selected).toBe(true);
    });
  });

  describe('selectNotificationsOverrideNeedsToBeUpdated', () => {
    it('is composed from the following selector', () => {
      expect(selectNotificationsOverrideNeedsToBeUpdated.dependencies).toEqual([
        selectOriginalPolicy,
        selectCurrentPolicy,
      ]);
    });

    it('selects whether actions override should not be updated', () => {
      const originalPolicy = {
        policyNotificationsOverrides: {
          currentOwnerId: { userNotifications: [{ emailAddress: 'user@email.com', stageIds: ['proxy', 'develop'] }] },
        },
      };
      const currentPolicy = {
        policyNotificationsOverrides: {
          currentOwnerId: { userNotifications: [{ emailAddress: 'user@email.com', stageIds: ['proxy', 'develop'] }] },
        },
      };

      const selected = selectNotificationsOverrideNeedsToBeUpdated.resultFunc(originalPolicy, currentPolicy);

      expect(selected).toBe(false);
    });

    it('selects whether actions override should be updated', () => {
      const originalPolicy = {
        policyNotificationsOverrides: {
          currentOwnerId: { userNotifications: [{ emailAddress: 'user@email.com', stageIds: ['proxy', 'develop'] }] },
        },
      };
      const currentPolicy = {
        policyNotificationsOverrides: {
          currentOwnerId: { userNotifications: [{ emailAddress: 'user2@email.com', stageIds: ['proxy', 'develop'] }] },
        },
      };

      const selected = selectNotificationsOverrideNeedsToBeUpdated.resultFunc(originalPolicy, currentPolicy);

      expect(selected).toBe(true);
    });
  });

  describe('selectOverrideNeedsToBeAdded', () => {
    it('is composed from the following selector', () => {
      expect(selectOverrideNeedsToBeAdded.dependencies).toEqual([
        selectActionsOverrideNeedsToBeAdded,
        selectNotificationsOverrideNeedsToBeAdded,
      ]);
    });

    it('returns false if neither an action nor a notification override needs adding', () => {
      const actionsOverrideNeedsToBeAdded = false;
      const notificationsOverrideNeedsToBeAdded = false;

      const selected = selectOverrideNeedsToBeAdded.resultFunc(
        actionsOverrideNeedsToBeAdded,
        notificationsOverrideNeedsToBeAdded
      );

      expect(selected).toBe(false);
    });

    it('returns true if an action override needs adding', () => {
      const actionsOverrideNeedsToBeAdded = true;
      const notificationsOverrideNeedsToBeAdded = false;

      const selected = selectOverrideNeedsToBeAdded.resultFunc(
        actionsOverrideNeedsToBeAdded,
        notificationsOverrideNeedsToBeAdded
      );

      expect(selected).toBe(true);
    });

    it('returns true if a notification override needs adding', () => {
      const actionsOverrideNeedsToBeAdded = false;
      const notificationsOverrideNeedsToBeAdded = true;

      const selected = selectOverrideNeedsToBeAdded.resultFunc(
        actionsOverrideNeedsToBeAdded,
        notificationsOverrideNeedsToBeAdded
      );

      expect(selected).toBe(true);
    });

    it('returns true if both an action override and a notification override need adding', () => {
      const actionsOverrideNeedsToBeAdded = true;
      const notificationsOverrideNeedsToBeAdded = true;

      const selected = selectOverrideNeedsToBeAdded.resultFunc(
        actionsOverrideNeedsToBeAdded,
        notificationsOverrideNeedsToBeAdded
      );

      expect(selected).toBe(true);
    });
  });

  describe('selectOverrideNeedsToBeRemoved', () => {
    it('is composed from the following selector', () => {
      expect(selectOverrideNeedsToBeRemoved.dependencies).toEqual([
        selectActionsOverrideNeedsToBeRemoved,
        selectNotificationsOverrideNeedsToBeRemoved,
      ]);
    });

    it('returns false if neither an action nor a notification override needs removing', () => {
      const actionsOverrideNeedsToBeRemoved = false;
      const notificationsOverrideNeedsToBeRemoved = false;

      const selected = selectOverrideNeedsToBeRemoved.resultFunc(
        actionsOverrideNeedsToBeRemoved,
        notificationsOverrideNeedsToBeRemoved
      );

      expect(selected).toBe(false);
    });

    it('returns true if an action override needs removing', () => {
      const actionsOverrideNeedsToBeRemoved = true;
      const notificationsOverrideNeedsToBeRemoved = false;

      const selected = selectOverrideNeedsToBeRemoved.resultFunc(
        actionsOverrideNeedsToBeRemoved,
        notificationsOverrideNeedsToBeRemoved
      );

      expect(selected).toBe(true);
    });

    it('returns true if a notification override needs removing', () => {
      const actionsOverrideNeedsToBeRemoved = false;
      const notificationsOverrideNeedsToBeRemoved = true;

      const selected = selectOverrideNeedsToBeRemoved.resultFunc(
        actionsOverrideNeedsToBeRemoved,
        notificationsOverrideNeedsToBeRemoved
      );

      expect(selected).toBe(true);
    });

    it('returns true if both an action override and a notification override need removing', () => {
      const actionsOverrideNeedsToBeRemoved = true;
      const notificationsOverrideNeedsToBeRemoved = true;

      const selected = selectOverrideNeedsToBeRemoved.resultFunc(
        actionsOverrideNeedsToBeRemoved,
        notificationsOverrideNeedsToBeRemoved
      );

      expect(selected).toBe(true);
    });
  });

  describe('selectOverrideNeedsToBeUpdated', () => {
    it('is composed from the following selector', () => {
      expect(selectOverrideNeedsToBeUpdated.dependencies).toEqual([
        selectActionsOverrideNeedsToBeUpdated,
        selectNotificationsOverrideNeedsToBeUpdated,
      ]);
    });

    it('returns false if neither an action nor a notification override needs updating', () => {
      const actionsOverrideNeedsToBeUpdated = false;
      const notificationsOverrideNeedsToBeUpdated = false;

      const selected = selectOverrideNeedsToBeUpdated.resultFunc(
        actionsOverrideNeedsToBeUpdated,
        notificationsOverrideNeedsToBeUpdated
      );

      expect(selected).toBe(false);
    });

    it('returns true if an action override needs updating', () => {
      const actionsOverrideNeedsToBeUpdated = true;
      const notificationsOverrideNeedsToBeUpdated = false;

      const selected = selectOverrideNeedsToBeUpdated.resultFunc(
        actionsOverrideNeedsToBeUpdated,
        notificationsOverrideNeedsToBeUpdated
      );

      expect(selected).toBe(true);
    });

    it('returns true if a notification override needs updating', () => {
      const actionsOverrideNeedsToBeUpdated = false;
      const notificationsOverrideNeedsToBeUpdated = true;

      const selected = selectOverrideNeedsToBeUpdated.resultFunc(
        actionsOverrideNeedsToBeUpdated,
        notificationsOverrideNeedsToBeUpdated
      );

      expect(selected).toBe(true);
    });

    it('returns true if both an action override and a notification override need updating', () => {
      const actionsOverrideNeedsToBeUpdated = true;
      const notificationsOverrideNeedsToBeUpdated = true;

      const selected = selectOverrideNeedsToBeUpdated.resultFunc(
        actionsOverrideNeedsToBeUpdated,
        notificationsOverrideNeedsToBeUpdated
      );

      expect(selected).toBe(true);
    });
  });

  describe('selectCategoriesForPolicyLoadError', () => {
    it('is composed from the following selector', () => {
      expect(selectCategoriesForPolicyLoadError.dependencies).toEqual([selectPolicySlice]);
    });

    it('selects categories for policy loadError', () => {
      const policySlice = {
        categoriesForPolicyLoadError: 'someError',
      };

      const selected = selectCategoriesForPolicyLoadError.resultFunc(policySlice);

      expect(selected).toBe('someError');
    });
  });

  describe('selectLoadError', () => {
    it('is composed from the following selector', () => {
      expect(selectLoadError.dependencies).toEqual([selectPolicyLoadError, selectCategoriesForPolicyLoadError]);
    });

    it('selects policy load error when its defined', () => {
      const loadError = 'someError';
      const categoriesForPolicyLoadError = 'ignoredError';

      const selected = selectLoadError.resultFunc(loadError, categoriesForPolicyLoadError);

      expect(selected).toBe('someError');
    });

    it('selects categories for policy load error when its defined and policy load error is not defined', () => {
      const loadError = null;
      const categoriesForPolicyLoadError = 'foundError';

      const selected = selectLoadError.resultFunc(loadError, categoriesForPolicyLoadError);

      expect(selected).toBe('foundError');
    });
  });

  describe('selectLoading', () => {
    it('is composed from the following selector', () => {
      expect(selectLoading.dependencies).toEqual([selectPolicySlice]);
    });

    it('selects loading', () => {
      const policySlice = {
        loadingSavePolicy: false,
        loadingCategories: true,
        loadingPolicyEditor: false,
      };

      const selected = selectLoading.resultFunc(policySlice);

      expect(selected).toBe(true);
    });
  });

  describe('selectDeleteModal', () => {
    it('is composed from the following selector', () => {
      expect(selectDeleteModal.dependencies).toEqual([selectPolicySlice]);
    });

    it('selects deleteModal', () => {
      const deleteModal = { success: true };
      const policySlice = {
        deleteModal,
      };

      const selected = selectDeleteModal.resultFunc(policySlice);

      expect(selected).toEqual(deleteModal);
    });
  });

  describe('selectDeleteModal', () => {
    it('is composed from the following selector', () => {
      expect(selectDeleteModal.dependencies).toEqual([selectPolicySlice]);
    });

    it('selects deleteModal', () => {
      const deleteModal = { success: true };
      const policySlice = {
        deleteModal,
      };

      const selected = selectDeleteModal.resultFunc(policySlice);

      expect(selected).toEqual(deleteModal);
    });
  });

  describe('selectHasPolicyCategories', () => {
    it('is composed from the following selector', () => {
      expect(selectHasPolicyCategories.dependencies).toEqual([selectPolicySlice]);
    });

    it('selects hasPolicyCategories', () => {
      const policySlice = {
        hasPolicyCategories: true,
      };

      const selected = selectHasPolicyCategories.resultFunc(policySlice);

      expect(selected).toBe(true);
    });
  });

  describe('selectOriginalCategories', () => {
    it('is composed from the following selector', () => {
      expect(selectOriginalCategories.dependencies).toEqual([selectPolicySlice]);
    });

    it('selects originalCategories', () => {
      const originalCategories = [{ id: 'categoryId' }];
      const policySlice = {
        originalCategories,
      };

      const selected = selectOriginalCategories.resultFunc(policySlice);

      expect(selected).toEqual(originalCategories);
    });
  });

  describe('selectCategories', () => {
    it('is composed from the following selector', () => {
      expect(selectCategories.dependencies).toEqual([selectPolicySlice]);
    });

    it('selects originalCategories', () => {
      const categories = [{ id: 'categoryId' }];
      const policySlice = {
        categories,
      };

      const selected = selectCategories.resultFunc(policySlice);

      expect(selected).toEqual(categories);
    });
  });

  describe('selectOriginalHasPolicyCategories', () => {
    it('is composed from the following selector', () => {
      expect(selectOriginalHasPolicyCategories.dependencies).toEqual([selectPolicySlice]);
    });

    it('selects originalHasPolicyCategories', () => {
      const policySlice = {
        originalHasPolicyCategories: true,
      };

      const selected = selectOriginalHasPolicyCategories.resultFunc(policySlice);

      expect(selected).toBe(true);
    });
  });

  describe('selectIsInheritanceDirty', () => {
    it('is composed from the following selectors', () => {
      expect(selectIsInheritanceDirty.dependencies).toEqual([
        selectIsOrgOwner,
        selectHasPolicyCategories,
        selectOriginalHasPolicyCategories,
        selectCategories,
        selectOriginalCategories,
      ]);
    });

    const testCases = [
      {
        isOrgOwner: false,
        categories: [],
        originalCategories: [],
        result: false,
      },
      {
        isOrgOwner: true,
        hasPolicyCategories: true,
        originalHasPolicyCategories: false,
        categories: [{ id: '123' }],
        originalCategories: [],
        result: true,
      },
      {
        isOrgOwner: true,
        hasPolicyCategories: true,
        originalHasPolicyCategories: true,
        categories: [{ id: '123' }],
        originalCategories: [{ id: '123' }],
        result: false,
      },
      {
        isOrgOwner: true,
        hasPolicyCategories: true,
        originalHasPolicyCategories: false,
        categories: [{ id: '123' }],
        originalCategories: [{ id: '123' }],
        result: true,
      },
    ];

    testCases.forEach(
      ({ isOrgOwner, hasPolicyCategories, originalHasPolicyCategories, categories, originalCategories, result }) => {
        it(`selects isInheritanceDirty with the following params: ${JSON.stringify({
          isOrgOwner,
          hasPolicyCategories,
          originalHasPolicyCategories,
          categories,
          originalCategories,
        })}`, () => {
          const selected = selectIsInheritanceDirty.resultFunc(
            isOrgOwner,
            hasPolicyCategories,
            originalHasPolicyCategories,
            categories,
            originalCategories
          );

          expect(selected).toBe(result);
        });
      }
    );
  });

  describe('selectIfSubmitButtonShouldBeDisabled', () => {
    it('is composed from the following selectors', () => {
      expect(selectIfSubmitButtonShouldBeDisabled.dependencies).toEqual([
        selectValidationError,
        selectCurrentPolicyConstraints,
        selectConditionTypesMap,
        selectIsCurrentPolicyDirty,
        selectCurrentPolicyName,
        selectIsInherited,
        selectIsActionOverrideEnabled,
        selectIsNotificationOverrideEnabled,
      ]);
    });

    const currentConstraints = [
      {
        id: '1661470698102',
        name: {
          isPristine: false,
          value: 'Custom',
          trimmedValue: 'Custom',
          validationErrors: [],
        },
        conditions: [
          {
            conditionTypeId: 'AgeInDays',
            operator: 'older than',
            value: {
              isPristine: false,
              value: '1825',
              trimmedValue: '1825',
              validationErrors: [],
            },
          },
        ],
        operator: 'OR',
      },
    ];
    const conditionTypesMap = {
      AgeInDays: {
        enabled: true,
        autoUnquarantineSupported: false,
        threatCategory: 'QUALITY',
        valueTypeId: 'AgeInDaysValueType',
        supportedOperators: ['older than', 'younger than'],
        valueHint: 'Enter term',
        name: 'Age',
        id: 'AgeInDays',
        valueType: {
          dataType: 'Integer',
          allowMultiple: false,
          availableValues: null,
          id: 'AgeInDaysValueType',
        },
      },
    };
    const unsupportedConditionTypesMap = {
      AgeInDays: {
        enabled: false,
        autoUnquarantineSupported: false,
        threatCategory: 'QUALITY',
        valueTypeId: 'AgeInDaysValueType',
        supportedOperators: ['older than', 'younger than'],
        valueHint: 'Enter term',
        name: 'Age',
        id: 'AgeInDays',
        valueType: {
          dataType: 'Integer',
          allowMultiple: false,
          availableValues: null,
          id: 'AgeInDaysValueType',
        },
      },
    };

    const testCases = [
      {
        validationError: null,
        currentConstraints,
        conditionTypesMap,
        isPolicyDirty: true,
        policyName: { validationErrors: [], isPristine: false },
        isInherited: true,
        isActionOverrideEnabled: true,
        isNotificationOverrideEnabled: true,
        result: null,
      },
      {
        validationError: null,
        currentConstraints,
        conditionTypesMap,
        isPolicyDirty: true,
        policyName: { validationErrors: [], isPristine: false },
        isInherited: true,
        isActionOverrideEnabled: true,
        isNotificationOverrideEnabled: false,
        result: null,
      },
      {
        validationError: null,
        currentConstraints,
        conditionTypesMap,
        isPolicyDirty: true,
        policyName: { validationErrors: [], isPristine: false },
        isInherited: true,
        isActionOverrideEnabled: false,
        isNotificationOverrideEnabled: true,
        result: null,
      },
      {
        validationError: null,
        currentConstraints,
        conditionTypesMap,
        isPolicyDirty: true,
        policyName: { validationErrors: [], isPristine: false },
        isInherited: false,
        isActionOverrideEnabled: true,
        isNotificationOverrideEnabled: true,
        result: null,
      },
      {
        validationError: null,
        currentConstraints,
        conditionTypesMap,
        isPolicyDirty: true,
        policyName: { validationErrors: [], isPristine: false },
        isInherited: true,
        isActionOverrideEnabled: false,
        isNotificationOverrideEnabled: false,
        result: true,
      },
      {
        validationError: null,
        currentConstraints,
        conditionTypesMap,
        isPolicyDirty: true,
        policyName: { validationErrors: [], isPristine: false },
        isInherited: false,
        isActionOverrideEnabled: true,
        isNotificationOverrideEnabled: false,
        result: null,
      },
      {
        validationError: null,
        currentConstraints,
        conditionTypesMap,
        isPolicyDirty: true,
        policyName: { validationErrors: [], isPristine: false },
        isInherited: false,
        isActionOverrideEnabled: false,
        isNotificationOverrideEnabled: true,
        result: null,
      },
      {
        validationError: null,
        currentConstraints,
        conditionTypesMap,
        isPolicyDirty: true,
        policyName: { validationErrors: [], isPristine: false },
        isInherited: false,
        isActionOverrideEnabled: false,
        isNotificationOverrideEnabled: false,
        result: null,
      },
      {
        validationError: 'Some validation error',
        currentConstraints,
        conditionTypesMap,
        isPolicyDirty: true,
        policyName: { validationErrors: [], isPristine: false },
        isInherited: false,
        isActionOverrideEnabled: true,
        isNotificationOverrideEnabled: false,
        result: 'Some validation error',
      },
      {
        validationError: null,
        currentConstraints,
        conditionTypesMap: unsupportedConditionTypesMap,
        isPolicyDirty: true,
        policyName: { validationErrors: [], isPristine: false },
        isInherited: false,
        isActionOverrideEnabled: true,
        isNotificationOverrideEnabled: false,
        result: 'Unable to save: unsupported conditions added',
      },
      {
        validationError: null,
        currentConstraints,
        conditionTypesMap,
        isPolicyDirty: true,
        policyName: { validationErrors: ['some error'], isPristine: true },
        isInherited: false,
        isActionOverrideEnabled: true,
        isNotificationOverrideEnabled: false,
        result: null,
      },
      {
        validationError: null,
        currentConstraints,
        conditionTypesMap,
        isPolicyDirty: true,
        policyName: { validationErrors: ['some error'], isPristine: false },
        isInherited: false,
        isActionOverrideEnabled: true,
        isNotificationOverrideEnabled: false,
        result: 'Unable to save: fields with invalid or missing data',
      },
    ];

    testCases.forEach(
      ({
        validationError,
        currentConstraints,
        conditionTypesMap,
        isPolicyDirty,
        policyName,
        isInherited,
        isActionOverrideEnabled,
        isNotificationOverrideEnabled,
        result,
      }) => {
        it(`selects ifSubmitButtonShouldBeDisabled with the following params: ${JSON.stringify(
          {
            validationError,
            currentConstraints,
            conditionTypesMap,
            isPolicyDirty,
            policyName,
            isInherited,
            isActionOverrideEnabled,
            isNotificationOverrideEnabled,
            result,
          },
          null,
          2
        )}`, () => {
          const selected = selectIfSubmitButtonShouldBeDisabled.resultFunc(
            validationError,
            currentConstraints,
            conditionTypesMap,
            isPolicyDirty,
            policyName,
            isInherited,
            isActionOverrideEnabled,
            isNotificationOverrideEnabled
          );

          expect(selected).toBe(result);
        });
      }
    );
  });

  describe('selectCurrentPolicyActions', () => {
    it('is composed from the following selector', () => {
      expect(selectCurrentPolicyActions.dependencies).toEqual([selectCurrentPolicy]);
    });

    it('selects originalCategories', () => {
      const actions = ['proxy'];
      const currentPolicy = {
        actions,
      };

      const selected = selectCurrentPolicyActions.resultFunc(currentPolicy);

      expect(selected).toEqual(actions);
    });
  });

  describe('selectCurrentPolicyName', () => {
    it('is composed from the following selector', () => {
      expect(selectCurrentPolicyName.dependencies).toEqual([selectCurrentPolicy]);
    });

    it('selects CurrentPolicy Name', () => {
      const currentPolicy = {
        name: 'Policy name',
      };

      const selected = selectCurrentPolicyName.resultFunc(currentPolicy);

      expect(selected).toBe('Policy name');
    });
  });

  describe('selectCurrentPolicyThreatLevel', () => {
    it('is composed from the following selector', () => {
      expect(selectCurrentPolicyThreatLevel.dependencies).toEqual([selectCurrentPolicy]);
    });

    it('selects CurrentPolicy threatLevel', () => {
      const currentPolicy = {
        threatLevel: 2,
      };

      const selected = selectCurrentPolicyThreatLevel.resultFunc(currentPolicy);

      expect(selected).toBe(2);
    });
  });

  describe('selectCurrentLegacyViolationAllowed', () => {
    it('is composed from the following selector', () => {
      expect(selectCurrentLegacyViolationAllowed.dependencies).toEqual([selectCurrentPolicy]);
    });

    it('selects CurrentPolicy legacyViolationAllowed', () => {
      const currentPolicy = {
        legacyViolationAllowed: true,
      };

      const selected = selectCurrentLegacyViolationAllowed.resultFunc(currentPolicy);

      expect(selected).toBe(true);
    });
  });

  describe('selectShouldShowQuarantineWarning', () => {
    it('is composed from the following selectors', () => {
      expect(selectShouldShowQuarantineWarning.dependencies).toEqual([
        selectCurrentPolicyActions,
        selectOriginalProxyStageAction,
        selectIsRootOrg,
        selectIsRepositoryContainer,
      ]);
    });

    const testCases = [
      {
        actions: {},
        originalProxyStageAction: 'fail',
        isRootOrg: true,
        result: false,
        isRepositoryContainer: false,
      },
      {
        actions: { proxy: 'fail' },
        originalProxyStageAction: 'fail',
        isRootOrg: true,
        result: false,
        isRepositoryContainer: false,
      },
      {
        actions: { proxy: 'fail' },
        originalProxyStageAction: 'warn',
        isRootOrg: false,
        result: false,
        isRepositoryContainer: false,
      },
      {
        actions: { proxy: 'fail' },
        originalProxyStageAction: 'warn',
        isRootOrg: true,
        result: true,
        isRepositoryContainer: false,
      },
    ];

    testCases.forEach(({ actions, originalProxyStageAction, isRootOrg, result, isRepositoryContainer }) => {
      it(`selects shouldShowQuarantineWarning with the following params: ${JSON.stringify({
        actions,
        originalProxyStageAction,
        isRootOrg,
        isRepositoryContainer,
      })}`, () => {
        const selected = selectShouldShowQuarantineWarning.resultFunc(
          actions,
          originalProxyStageAction,
          isRootOrg,
          isRepositoryContainer
        );

        expect(selected).toBe(result);
      });
    });
  });

  describe('selectIsCurrentPolicyDirty', () => {
    it('is composed from the following selectors', () => {
      expect(selectIsCurrentPolicyDirty.dependencies).toEqual([selectIsDirty, selectIsInheritanceDirty]);
    });

    const testCases = [
      {
        isDirty: false,
        selectIsInheritanceDirty: false,
        result: false,
      },
      {
        isDirty: true,
        selectIsInheritanceDirty: false,
        result: true,
      },
      {
        isDirty: false,
        selectIsInheritanceDirty: true,
        result: true,
      },
      {
        isDirty: true,
        selectIsInheritanceDirty: true,
        result: true,
      },
    ];

    testCases.forEach(({ isDirty, selectIsInheritanceDirty, result }) => {
      it(`selects isCurrentPolicyDirty with the following params: ${JSON.stringify({
        isDirty,
        selectIsInheritanceDirty,
      })}`, () => {
        const selected = selectIsCurrentPolicyDirty.resultFunc(isDirty, selectIsInheritanceDirty);

        expect(selected).toBe(result);
      });
    });
  });

  describe('selectIsActionOverrideEnabled', () => {
    let currentPolicy;

    beforeEach(() => {
      currentPolicy = {
        policyActionsOverrideAllowed: true,
      };
    });

    it('is composed from the following selectors', () => {
      expect(selectIsActionOverrideEnabled.dependencies).toEqual([selectIsInherited, selectCurrentPolicy]);
    });

    it('returns false if policy is not inherited', () => {
      expect(selectIsActionOverrideEnabled.resultFunc(false, currentPolicy)).toBe(false);
    });

    it('returns false if policy is inherited but policyActionsOverrideAllowed is false', () => {
      currentPolicy.policyActionsOverrideAllowed = false;
      expect(selectIsActionOverrideEnabled.resultFunc(true, currentPolicy)).toBe(false);
    });

    it('returns true if policy is inherited and policyActionsOverrideAllowed is true', () => {
      expect(selectIsActionOverrideEnabled.resultFunc(true, currentPolicy)).toBe(true);
    });
  });

  describe('selectIsNotificationOverrideEnabled', () => {
    let currentPolicy;

    beforeEach(() => {
      currentPolicy = {
        policyNotificationsOverrideAllowed: true,
      };
    });

    it('is composed from the following selectors', () => {
      expect(selectIsNotificationOverrideEnabled.dependencies).toEqual([selectIsInherited, selectCurrentPolicy]);
    });

    it('returns false if policy is not inherited', () => {
      expect(selectIsNotificationOverrideEnabled.resultFunc(false, currentPolicy)).toBe(false);
    });

    it('returns false if policy is inherited but policyNotificationsOverrideAllowed is false', () => {
      currentPolicy.policyNotificationsOverrideAllowed = false;
      expect(selectIsNotificationOverrideEnabled.resultFunc(true, currentPolicy)).toBe(false);
    });

    it('returns true if policy is inherited and policyNotificationsOverrideAllowed is true', () => {
      expect(selectIsNotificationOverrideEnabled.resultFunc(true, currentPolicy)).toBe(true);
    });
  });

  describe('selectActionsOverridesForCurrentPolicy', () => {
    it('is composed from the following selector', () => {
      expect(selectActionsOverridesForCurrentPolicy.dependencies).toEqual([selectPoliciesByOwner, selectCurrentPolicy]);
    });

    it('selects actions overrides for current policy', () => {
      const currentPolicy = {
        ownerId: 'ownerId',
        policyActionsOverrideAllowed: true,
        policyActionsOverrides: { rootOwnerId: { build: 'fail' } },
      };
      const policiesByOwners = [{ ownerId: 'rootOwnerId' }, { ownerId: 'ownerId' }];
      const actualSelection = selectActionsOverridesForCurrentPolicy.resultFunc(policiesByOwners, currentPolicy);

      expect(actualSelection).toEqual({ build: 'fail' });
    });

    it('returns undefined when there are no actions overrides for current policy', () => {
      const currentPolicy = {
        ownerId: 'ownerId',
        policyActionsOverrideAllowed: true,
      };
      const policiesByOwners = [{ ownerId: 'rootOwnerId' }, { ownerId: 'ownerId' }];
      const actualSelection = selectActionsOverridesForCurrentPolicy.resultFunc(policiesByOwners, currentPolicy);

      expect(actualSelection).toBe(undefined);
    });

    it('returns undefined when policy actions overrides are not allowed', () => {
      const currentPolicy = {
        ownerId: 'ownerId',
        policyActionsOverrideAllowed: false,
        policyActionsOverrides: { rootOwnerId: { build: 'fail' } },
      };
      const policiesByOwners = [{ ownerId: 'rootOwnerId' }, { ownerId: 'ownerId' }];
      const actualSelection = selectActionsOverridesForCurrentPolicy.resultFunc(policiesByOwners, currentPolicy);

      expect(actualSelection).toBe(undefined);
    });
  });

  describe('selectNotificationsOverridesForCurrentPolicy', () => {
    it('is composed from the following selector', () => {
      expect(selectNotificationsOverridesForCurrentPolicy.dependencies).toEqual([
        selectPoliciesByOwner,
        selectCurrentPolicy,
      ]);
    });

    it('selects notifications overrides for current policy', () => {
      const currentPolicy = {
        ownerId: 'ownerId',
        policyNotificationsOverrideAllowed: true,
        policyNotificationsOverrides: {
          rootOwnerId: {
            userNotifications: [{ emailAddress: 'email@email.com', stageIds: ['build', 'release'] }],
          },
        },
      };
      const policiesByOwners = [{ ownerId: 'rootOwnerId' }, { ownerId: 'ownerId' }];
      const actualSelection = selectNotificationsOverridesForCurrentPolicy.resultFunc(policiesByOwners, currentPolicy);

      expect(actualSelection).toEqual({
        userNotifications: [{ emailAddress: 'email@email.com', stageIds: ['build', 'release'] }],
      });
    });

    it('returns undefined when there are no notifications overrides for current policy', () => {
      const currentPolicy = {
        ownerId: 'ownerId',
        policyNotificationsOverrideAllowed: true,
      };
      const policiesByOwners = [{ ownerId: 'rootOwnerId' }, { ownerId: 'ownerId' }];
      const actualSelection = selectNotificationsOverridesForCurrentPolicy.resultFunc(policiesByOwners, currentPolicy);

      expect(actualSelection).toBe(undefined);
    });

    it('returns undefined when policy notifications overrides are not allowed', () => {
      const currentPolicy = {
        ownerId: 'ownerId',
        policyNotificationsOverrideAllowed: false,
        policyNotificationsOverrides: {
          rootOwnerId: {
            userNotifications: [{ emailAddress: 'email@email.com', stageIds: ['build', 'release'] }],
          },
        },
      };
      const policiesByOwners = [{ ownerId: 'rootOwnerId' }, { ownerId: 'ownerId' }];
      const actualSelection = selectNotificationsOverridesForCurrentPolicy.resultFunc(policiesByOwners, currentPolicy);

      expect(actualSelection).toBe(undefined);
    });
  });

  describe('selectNotifications', () => {
    it('is composed from the following selectors', () => {
      expect(selectNotifications.dependencies).toEqual([
        selectIsNotificationOverrideEnabled,
        selectOverrideNotificationsFlag,
        selectNotificationsOverridesForCurrentPolicy,
        selectCurrentPolicy,
      ]);
    });

    it('returns current policy notifications if notificationOverrideEnabled is false', () => {
      const notifications = selectNotifications.resultFunc(false, true, 'overrides', {
        notifications: 'notifications',
      });
      expect(notifications).toBe('notifications');
    });

    it('returns current policy notifications if overrideNotificationsFlag is false', () => {
      const notifications = selectNotifications.resultFunc(true, false, 'overrides', {
        notifications: 'notifications',
      });
      expect(notifications).toBe('notifications');
    });

    it('returns current policy notifications if notificationOverrideEnabled and overrideNotificationsFlag are false', () => {
      const notifications = selectNotifications.resultFunc(false, false, 'overrides', {
        notifications: 'notifications',
      });
      expect(notifications).toBe('notifications');
    });

    it('returns notifications overrides if notificationOverrideEnabled and overrideNotificationsFlag are true', () => {
      const notifications = selectNotifications.resultFunc(true, true, 'overrides', {
        notifications: 'notifications',
      });
      expect(notifications).toBe('overrides');
    });

    it('returns an empty object if overrides would be returned but they do not exist', () => {
      const notifications = selectNotifications.resultFunc(true, true, null, {
        notifications: 'notifications',
      });
      expect(notifications).toEqual({});
    });

    it('returns an empty object if notifications would be returned but they do not exist', () => {
      const notifications = selectNotifications.resultFunc(false, false, 'overrides', {
        notifications: null,
      });
      expect(notifications).toEqual({});
    });

    it('returns an empty object if notifications would be returned but the current policy does not exist', () => {
      const notifications = selectNotifications.resultFunc(false, false, 'overrides', null);
      expect(notifications).toEqual({});
    });
  });

  describe('selectIsNotificationsInheritOverrideEnabled', () => {
    it('is composed from the following selectors', () => {
      expect(selectIsNotificationsInheritOverrideEnabled.dependencies).toEqual([
        selectHasEditIqPermission,
        selectIsNotificationsSupported,
        selectIsFirewallSupported,
        selectIsInherited,
        selectIsNotificationOverrideEnabled,
        selectIsRepositoriesRelated,
      ]);
    });

    it('returns true under the right conditions if not inherited', () => {
      // Has edit permission, notifications supported
      expect(selectIsNotificationsInheritOverrideEnabled.resultFunc(true, true, false, false, false)).toBeTruthy();
      // Has edit permission, firewall supported
      expect(selectIsNotificationsInheritOverrideEnabled.resultFunc(true, false, true, false, false)).toBeTruthy();
    });

    it('returns true under the right conditions if inherited', () => {
      // Has edit permission, notifications supported, notification override enabled
      expect(selectIsNotificationsInheritOverrideEnabled.resultFunc(true, true, false, true, true)).toBeTruthy();
      // Has edit permission, firewall supported, notification override enabled
      expect(selectIsNotificationsInheritOverrideEnabled.resultFunc(true, false, true, true, true)).toBeTruthy();
    });

    it('returns false if without edit iq permission', () => {
      // Not inherited
      expect(selectIsNotificationsInheritOverrideEnabled.resultFunc(false, true, false, false, false)).toBeFalsy();
      // Inherited
      expect(selectIsNotificationsInheritOverrideEnabled.resultFunc(false, true, false, true, true)).toBeFalsy();
    });

    it('returns false if notifications and firewall are not supported', () => {
      // Not inherited
      expect(selectIsNotificationsInheritOverrideEnabled.resultFunc(true, false, false, false, false)).toBeFalsy();
      // Inherited
      expect(selectIsNotificationsInheritOverrideEnabled.resultFunc(true, false, false, true, true)).toBeFalsy();
    });

    it('returns false if inherited without notifications override enabled', () => {
      expect(selectIsNotificationsInheritOverrideEnabled.resultFunc(true, true, false, true, false)).toBeFalsy();
    });
  });

  describe('selectIsNotificationsTableEnabled', () => {
    it('is composed from the following selectors', () => {
      expect(selectIsNotificationsTableEnabled.dependencies).toEqual([
        selectIsNotificationsInheritOverrideEnabled,
        selectIsInherited,
        selectOverrideNotificationsFlag,
        selectIsRepositoriesRelated,
      ]);
    });

    it('returns true under the right conditions if not inherited', () => {
      expect(selectIsNotificationsTableEnabled.resultFunc(true, false, false)).toBeTruthy();
      expect(selectIsNotificationsTableEnabled.resultFunc(true, false, true)).toBeTruthy();
    });

    it('returns true under the right conditions if inherited', () => {
      expect(selectIsNotificationsTableEnabled.resultFunc(true, true, true)).toBeTruthy();
    });

    it('returns false if inherited without notifications override enabled', () => {
      expect(selectIsNotificationsTableEnabled.resultFunc(true, true, false)).toBeFalsy();
    });

    it('returns false if inherit/override is disabled', () => {
      expect(selectIsNotificationsTableEnabled.resultFunc(false, true, true)).toBeFalsy();
      expect(selectIsNotificationsTableEnabled.resultFunc(false, true, false)).toBeFalsy();
      expect(selectIsNotificationsTableEnabled.resultFunc(false, false, true)).toBeFalsy();
      expect(selectIsNotificationsTableEnabled.resultFunc(false, false, false)).toBeFalsy();
    });
  });

  describe('selectIsActionsInheritOverrideEnabled', () => {
    it('is composed from the following selectors', () => {
      expect(selectIsActionsInheritOverrideEnabled.dependencies).toEqual([
        selectHasEditIqPermission,
        selectIsEnforcementSupported,
        selectIsFirewallSupported,
        selectIsInherited,
        selectIsActionOverrideEnabled,
      ]);
    });

    it('returns true under the right conditions if not inherited', () => {
      // Has edit permission, enforcement supported
      expect(selectIsActionsInheritOverrideEnabled.resultFunc(true, true, false, false, false)).toBeTruthy();
      // Has edit permission, firewall supported
      expect(selectIsActionsInheritOverrideEnabled.resultFunc(true, false, true, false, false)).toBeTruthy();
    });

    it('returns true under the right conditions if inherited', () => {
      // Has edit permission, enforcement supported, action override enabled
      expect(selectIsActionsInheritOverrideEnabled.resultFunc(true, true, false, true, true)).toBeTruthy();
      // Has edit permission, firewall supported, action override enabled
      expect(selectIsActionsInheritOverrideEnabled.resultFunc(true, false, true, true, true)).toBeTruthy();
    });

    it('returns false if without edit iq permission', () => {
      // Not inherited
      expect(selectIsActionsInheritOverrideEnabled.resultFunc(false, true, false, false, false)).toBeFalsy();
      // Inherited
      expect(selectIsActionsInheritOverrideEnabled.resultFunc(false, true, false, true, true)).toBeFalsy();
    });

    it('returns false if enforcement and firewall are not supported', () => {
      // Not inherited
      expect(selectIsActionsInheritOverrideEnabled.resultFunc(true, false, false, false, false)).toBeFalsy();
      // Inherited
      expect(selectIsActionsInheritOverrideEnabled.resultFunc(true, false, false, true, true)).toBeFalsy();
    });

    it('returns false if inherited without actions override enabled', () => {
      expect(selectIsActionsInheritOverrideEnabled.resultFunc(true, true, false, true, false)).toBeFalsy();
    });
  });

  describe('selectIsActionsTableEnabled', () => {
    it('is composed from the following selectors', () => {
      expect(selectIsActionsTableEnabled.dependencies).toEqual([
        selectIsActionsInheritOverrideEnabled,
        selectIsInherited,
        selectOverrideActionsFlag,
      ]);
    });

    it('returns true under the right conditions if not inherited', () => {
      expect(selectIsActionsTableEnabled.resultFunc(true, false, false)).toBeTruthy();
      expect(selectIsActionsTableEnabled.resultFunc(true, false, true)).toBeTruthy();
    });

    it('returns true under the right conditions if inherited', () => {
      expect(selectIsActionsTableEnabled.resultFunc(true, true, true)).toBeTruthy();
    });

    it('returns false if inherited without actions override enabled', () => {
      expect(selectIsActionsTableEnabled.resultFunc(true, true, false)).toBeFalsy();
    });

    it('returns false if inherit/override is disabled', () => {
      expect(selectIsActionsTableEnabled.resultFunc(false, true, true)).toBeFalsy();
      expect(selectIsActionsTableEnabled.resultFunc(false, true, false)).toBeFalsy();
      expect(selectIsActionsTableEnabled.resultFunc(false, false, true)).toBeFalsy();
      expect(selectIsActionsTableEnabled.resultFunc(false, false, false)).toBeFalsy();
    });
  });

  describe('selectNotificationsEditor', () => {
    it('is composed from the following selector', () => {
      expect(selectNotificationsEditor.dependencies).toEqual([selectPolicySlice]);
    });

    it('returns notificationsEditor', () => {
      const policySlice = { notificationsEditor: null };

      expect(selectNotificationsEditor.resultFunc(policySlice)).toBe(null);
    });
  });

  describe('selectNotificationsEditorLoading', () => {
    it('is composed from the following selector', () => {
      expect(selectNotificationsEditorLoading.dependencies).toEqual([selectNotificationsEditor]);
    });

    it('returns loading', () => {
      expect(selectNotificationsEditorLoading.resultFunc({ loading: true })).toBe(true);
    });
  });

  describe('selectNotificationsEditorLoadError', () => {
    it('is composed from the following selector', () => {
      expect(selectNotificationsEditorLoadError.dependencies).toEqual([selectNotificationsEditor]);
    });

    it('returns loadError', () => {
      expect(selectNotificationsEditorLoadError.resultFunc({ loadError: null })).toBe(null);
    });
  });

  describe('selectNotificationWebhooks', () => {
    it('is composed from the following selector', () => {
      expect(selectNotificationWebhooks.dependencies).toEqual([selectNotificationsEditor]);
    });

    it('returns recipients with proper display names', () => {
      const notificationsEditor = { notificationWebhooks: [] };

      expect(selectNotificationWebhooks.resultFunc(notificationsEditor)).toEqual([]);
    });
  });

  describe('selectApplicableWebhooks', () => {
    const notifications = {
      userNotifications: [{ emailAddress: 'user@email.com', stageIds: [] }],
      roleNotifications: [{ roleId: '1', stageIds: [] }],
      webhookNotifications: [{ webhookId: '1', stageIds: [] }],
    };
    const webhooks = [
      { id: '1', description: 'webhook', url: 'url' },
      { id: '2', description: 'webhook', url: 'url' },
    ];

    it('is composed from the following selectors', () => {
      expect(selectApplicableWebhooks.dependencies).toEqual([selectNotifications, selectNotificationWebhooks]);
    });

    it('returns only available webhooks', () => {
      expect(selectApplicableWebhooks.resultFunc(notifications, webhooks)).toEqual([
        { id: '2', description: 'webhook', url: 'url', displayName: 'webhook' },
      ]);
    });
  });

  describe('selectNotificationRecipients', () => {
    it('is composed from the following selectors', () => {
      expect(selectNotificationRecipients.dependencies).toEqual([
        selectNotifications,
        selectNotificationWebhooks,
        selectCrossProductWebhooks,
        selectRolesForCurrentOwner,
        selectJiraProjectNames,
        selectJiraIssueTypeNames,
      ]);
    });

    it('returns recipients with proper display names', () => {
      const notifications = {
        userNotifications: [{ emailAddress: 'user@email.com', stageIds: [] }],
        roleNotifications: [{ roleId: '1', stageIds: [] }],
        webhookNotifications: [{ webhookId: '1', stageIds: [] }],
        jiraNotifications: [{ projectKey: 1, issueTypeId: 1, stageIds: [] }],
      };
      const webhooks = [{ id: '1', description: 'webhook', url: 'url' }];
      const roles = [{ roleId: '1', roleName: 'developer' }];
      const jiraProjectNames = { 1: 'Project 1' };
      const jiraIssueTypeNames = { 1: 'Issue 1' };

      expect(
        selectNotificationRecipients.resultFunc(notifications, webhooks, [], roles, jiraProjectNames, jiraIssueTypeNames)
      ).toEqual([
        { roleId: '1', displayName: 'developer', stageIds: [] },
        { projectKey: 1, issueTypeId: 1, displayName: 'Project 1 (Issue 1)', stageIds: [] },
        { emailAddress: 'user@email.com', displayName: 'user@email.com', stageIds: [] },
        { webhookId: '1', displayName: 'Webhook: webhook', stageIds: [], webhookEventTypes: [], isCrossProductWebhook: false },
      ]);
    });
  });

  describe('selectNotificationsEditorFormState', () => {
    it('is composed from the following selector', () => {
      expect(selectNotificationsEditorFormState.dependencies).toEqual([selectNotificationsEditor]);
    });

    it('returns policy editor form state', () => {
      const notificationsEditor = {
        formState: {
          recipientType: { value: 'type' },
          recipientEmail: { value: 'email' },
          recipientRoleId: { value: 'roleId' },
          recipientWebhookId: { value: 'webhookId' },
        },
      };

      expect(selectNotificationsEditorFormState.resultFunc(notificationsEditor)).toEqual({
        recipientType: { value: 'type' },
        recipientEmail: { value: 'email' },
        recipientRoleId: { value: 'roleId' },
        recipientWebhookId: { value: 'webhookId' },
      });
    });
  });

  describe('selectRolesForCurrentOwner', () => {
    it('is composed from the following selector', () => {
      expect(selectRolesForCurrentOwner.dependencies).toEqual([selectNotificationsEditor]);
    });

    it('selects roles for current owner', () => {
      const actual = selectRolesForCurrentOwner.resultFunc({
        roles: [],
      });
      expect(actual).toEqual([]);
    });
  });

  describe('selectIsJiraEnabled', () => {
    it('is composed from the following selector', () => {
      expect(selectIsJiraEnabled.dependencies).toEqual([selectNotificationsEditor]);
    });

    it('selects isJiraEnabled flag', () => {
      const actual = selectIsJiraEnabled.resultFunc({ isJiraEnabled: true });
      expect(actual).toBe(true);
    });
  });

  describe('selectJiraProjects', () => {
    it('is composed from the following selector', () => {
      expect(selectJiraProjects.dependencies).toEqual([selectNotificationsEditor]);
    });

    it('selects jira projects', () => {
      const actual = selectJiraProjects.resultFunc({ jiraProjects: [] });
      expect(actual).toEqual([]);
    });
  });

  describe('selectJiraProjectNames', () => {
    it('is composed from the following selectors', () => {
      expect(selectJiraProjectNames.dependencies).toEqual([selectJiraProjects]);
    });

    it('selects jira project names', () => {
      const actual = selectJiraProjectNames.resultFunc([
        { key: 1, name: 'Name 1' },
        { key: 2, name: 'Name 2' },
      ]);
      expect(actual).toEqual({ 1: 'Name 1', 2: 'Name 2' });
    });
  });

  describe('selectJiraIssueTypeNames', () => {
    it('is composed from the following selectors', () => {
      expect(selectJiraIssueTypeNames.dependencies).toEqual([selectJiraProjects]);
    });

    it('selects jira issue type names', () => {
      const actual = selectJiraIssueTypeNames.resultFunc([
        { key: 1, name: 'Name 1', issueTypes: [{ id: 1, name: 'Name 1' }] },
        { key: 2, name: 'Name 2', issueTypes: [{ id: 2, name: 'Name 2' }] },
      ]);
      expect(actual).toEqual({ 1: 'Name 1', 2: 'Name 2' });
    });
  });

  describe('selectNotificationRecipientTypeOptions', () => {
    it('is composed from the following selectors', () => {
      expect(selectNotificationRecipientTypeOptions.dependencies).toEqual([
        selectIsJiraEnabled,
        selectIsPolicyWebhooksSupported,
      ]);
    });

    it('selects recipient type options', () => {
      const actual = selectNotificationRecipientTypeOptions.resultFunc(true, true);
      expect(actual).toEqual([
        RECIPIENT_TYPES.EMAIL,
        RECIPIENT_TYPES.ROLE,
        RECIPIENT_TYPES.WEBHOOK,
        RECIPIENT_TYPES.JIRA,
      ]);
    });

    it('selects recipient type options without jira type when jira is not enabled', () => {
      const actual = selectNotificationRecipientTypeOptions.resultFunc(false, true);
      expect(actual).toEqual([RECIPIENT_TYPES.EMAIL, RECIPIENT_TYPES.ROLE, RECIPIENT_TYPES.WEBHOOK]);
    });

    it('selects recipient type options without webhook type when webhooks are not supported', () => {
      const actual = selectNotificationRecipientTypeOptions.resultFunc(true, false);
      expect(actual).toEqual([RECIPIENT_TYPES.EMAIL, RECIPIENT_TYPES.ROLE, RECIPIENT_TYPES.JIRA]);
    });
  });

  describe('selectAvailableJiraProjects', () => {
    it('is composed from the following selectors', () => {
      expect(selectAvailableJiraProjects.dependencies).toEqual([selectNotifications, selectJiraProjects]);
    });

    it('selects available jira projects', () => {
      const actual = selectAvailableJiraProjects.resultFunc({ jiraNotifications: [{ projectKey: 1 }] }, [
        { key: 1, name: 'Name 1', issueTypes: [{ id: 1, name: 'Name 1' }] },
        { key: 2, name: 'Name 2', issueTypes: [{ id: 2, name: 'Name 2' }] },
      ]);
      expect(actual).toEqual([{ key: 2, name: 'Name 2', issueTypes: [{ id: 2, name: 'Name 2' }] }]);
    });
  });

  describe('selectSelectedJiraProject', () => {
    it('is composed from the following selectors', () => {
      expect(selectSelectedJiraProject.dependencies).toEqual([
        selectAvailableJiraProjects,
        selectNotificationsEditorFormState,
      ]);
    });

    it('selects selected jira project', () => {
      const actual = selectSelectedJiraProject.resultFunc(
        [
          { key: 1, name: 'Name 1', issueTypes: [{ id: 1, name: 'Name 1' }] },
          { key: 2, name: 'Name 2', issueTypes: [{ id: 2, name: 'Name 2' }] },
        ],
        {
          recipientProjectKey: { value: 2 },
        }
      );
      expect(actual).toEqual({ key: 2, name: 'Name 2', issueTypes: [{ id: 2, name: 'Name 2' }] });
    });
  });

  describe('selectPolicyDeleteError', () => {
    it('is composed from the following selectors', () => {
      expect(selectPolicyDeleteError.dependencies).toEqual([selectPolicySlice]);
    });

    it('selects deleteError', () => {
      const actual = selectPolicyDeleteError.resultFunc({ deleteError: 'error' });
      expect(actual).toEqual('error');
    });
  });

  describe('selectCurrentSubmitMaskState', () => {
    it('is composed from the following selectors', () => {
      expect(selectCurrentSubmitMaskState.dependencies).toEqual([selectPolicySlice]);
    });

    it('selects selected jira project', () => {
      const actual = selectCurrentSubmitMaskState.resultFunc({ submitMaskState: 'submitMaskState' });
      expect(actual).toEqual('submitMaskState');
    });
  });

  describe('selectShowActionsOverridesConfirmationModal', () => {
    it('is composed from the following selectors', () => {
      expect(selectShowActionsOverridesConfirmationModal.dependencies).toEqual([selectPolicySlice]);
    });

    it('selects showActionsOverridesConfirmationModal', () => {
      const actual = selectShowActionsOverridesConfirmationModal.resultFunc({
        showActionsOverridesConfirmationModal: true,
      });
      expect(actual).toBeTruthy();
    });
  });

  describe('selectShowNotificationsOverridesConfirmationModal', () => {
    it('is composed from the following selectors', () => {
      expect(selectShowNotificationsOverridesConfirmationModal.dependencies).toEqual([selectPolicySlice]);
    });

    it('selects showNotificationsOverridesConfirmationModal', () => {
      const actual = selectShowNotificationsOverridesConfirmationModal.resultFunc({
        showNotificationsOverridesConfirmationModal: true,
      });
      expect(actual).toBeTruthy();
    });
  });

  describe('selectActionsOverridesCount', () => {
    it('is composed from the following selectors', () => {
      expect(selectActionsOverridesCount.dependencies).toEqual([selectOriginalPolicy]);
    });

    it('selects 0 if policyActionsOverrides is null', () => {
      const actual = selectActionsOverridesCount.resultFunc({
        policyActionsOverrides: null,
      });
      expect(actual).toEqual(0);
    });

    it('selects the number of policyActionsOverrides entries', () => {
      const actual = selectActionsOverridesCount.resultFunc({
        policyActionsOverrides: { a: {}, b: {}, c: {} },
      });
      expect(actual).toEqual(3);
    });
  });

  describe('selectNotificationsOverridesCount', () => {
    it('is composed from the following selectors', () => {
      expect(selectNotificationsOverridesCount.dependencies).toEqual([selectOriginalPolicy]);
    });

    it('selects 0 if policyNotificationsOverrides is null', () => {
      const actual = selectNotificationsOverridesCount.resultFunc({
        policyActionsOverrides: null,
      });
      expect(actual).toEqual(0);
    });

    it('selects the number of policyNotificationsOverrides entries', () => {
      const actual = selectNotificationsOverridesCount.resultFunc({
        policyNotificationsOverrides: { a: {}, b: {}, c: {} },
      });
      expect(actual).toEqual(3);
    });
  });

  describe('selectPolicyTileSortingCollapsible', () => {
    it('selects sorting for policy tile', () => {
      const actual = selectPolicyTileSortingCollapsible.resultFunc({
        collapsibleSorting: { key: 'mykey', dir: 'asc' },
      });
      expect(actual).toEqual({ key: 'mykey', dir: 'asc' });
    });
  });
});
