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
  selectOverrideNeedsToBeRemoved,
  selectOverrideActionsFlag,
  selectOriginalOverrideActionsFlag,
  selectActionsOverridesForCurrentPolicy,
  selectOriginalPolicyName,
  selectCurrentPolicyName,
  selectCurrentPolicyThreatLevel,
  selectCurrentPolicyViolationGrandfatheringAllowed,
  selectNotificationWebhooks,
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
  selectCurrentSubmitMaskState,
} from 'MainRoot/OrgsAndPolicies/policySelectors';
import { selectIsWebhooksSupported } from 'MainRoot/productFeatures/productFeaturesSelectors';
import { selectRouterCurrentParams } from 'MainRoot/reduxUiRouter/routerSelectors';
import { RECIPIENT_TYPES } from 'MainRoot/OrgsAndPolicies/policySlice';
import { MSG_NO_CHANGES_TO_SAVE } from 'MainRoot/util/constants';

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

      expect(selected).toBeTrue();
    });

    it('returns false if policyId does not exist in the current param', () => {
      const selected = selectIsEditMode.resultFunc({});

      expect(selected).toBeFalse();
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

      expect(selected).toBeTrue();
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

      expect(selected).toBeTrue();
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

      expect(selected).toBeTrue();
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

      expect(selected).toBeTrue();
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

      expect(selected).toBeFalse();
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

      expect(selected).toBeFalse();
    });
  });

  describe('selectOverrideNeedsToBeRemoved', () => {
    it('is composed from the following selector', () => {
      expect(selectOverrideNeedsToBeRemoved.dependencies).toEqual([
        selectOriginalOverrideActionsFlag,
        selectOverrideActionsFlag,
      ]);
    });

    it('selects whether actions override should not be removed', () => {
      const overrideActionsFlag = true;
      const originalOverrideActionsFlag = true;

      const selected = selectOverrideNeedsToBeRemoved.resultFunc(originalOverrideActionsFlag, overrideActionsFlag);

      expect(selected).toBeFalse();
    });

    it('selects whether actions override should be removed', () => {
      const overrideActionsFlag = false;
      const originalOverrideActionsFlag = true;

      const selected = selectOverrideNeedsToBeRemoved.resultFunc(originalOverrideActionsFlag, overrideActionsFlag);

      expect(selected).toBeTrue();
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

      expect(selected).toBeTrue();
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

  describe('selectIsDirty', () => {
    it('is composed from the following selector', () => {
      expect(selectIsDirty.dependencies).toEqual([selectPolicySlice]);
    });

    it('selects isDirty', () => {
      const policySlice = {
        isDirty: true,
      };

      const selected = selectIsDirty.resultFunc(policySlice);

      expect(selected).toBeTrue();
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

      expect(selected).toBeTrue();
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

      expect(selected).toBeTrue();
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
        isInherited: false,
        isActionOverrideEnabled: true,
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
        result: true,
      },
      {
        validationError: null,
        currentConstraints,
        conditionTypesMap,
        isPolicyDirty: false,
        policyName: { validationErrors: [], isPristine: false },
        isInherited: false,
        isActionOverrideEnabled: true,
        result: MSG_NO_CHANGES_TO_SAVE,
      },
      {
        validationError: 'Some validation error',
        currentConstraints,
        conditionTypesMap,
        isPolicyDirty: true,
        policyName: { validationErrors: [], isPristine: false },
        isInherited: false,
        isActionOverrideEnabled: true,
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
            isActionOverrideEnabled
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

  describe('selectCurrentPolicyViolationGrandfatheringAllowed', () => {
    it('is composed from the following selector', () => {
      expect(selectCurrentPolicyViolationGrandfatheringAllowed.dependencies).toEqual([selectCurrentPolicy]);
    });

    it('selects CurrentPolicy isViolationGrandfatheringAllowed', () => {
      const currentPolicy = {
        policyViolationGrandfatheringAllowed: true,
      };

      const selected = selectCurrentPolicyViolationGrandfatheringAllowed.resultFunc(currentPolicy);

      expect(selected).toBeTrue();
    });
  });

  describe('selectShouldShowQuarantineWarning', () => {
    it('is composed from the following selectors', () => {
      expect(selectShouldShowQuarantineWarning.dependencies).toEqual([
        selectCurrentPolicyActions,
        selectOriginalProxyStageAction,
        selectIsRootOrg,
      ]);
    });

    const testCases = [
      {
        actions: {},
        originalProxyStageAction: 'fail',
        isRootOrg: true,
        result: false,
      },
      {
        actions: { proxy: 'fail' },
        originalProxyStageAction: 'fail',
        isRootOrg: true,
        result: false,
      },
      {
        actions: { proxy: 'fail' },
        originalProxyStageAction: 'warn',
        isRootOrg: false,
        result: false,
      },
      {
        actions: { proxy: 'fail' },
        originalProxyStageAction: 'warn',
        isRootOrg: true,
        result: true,
      },
    ];

    testCases.forEach(({ actions, originalProxyStageAction, isRootOrg, result }) => {
      it(`selects shouldShowQuarantineWarning with the following params: ${JSON.stringify({
        actions,
        originalProxyStageAction,
        isRootOrg,
      })}`, () => {
        const selected = selectShouldShowQuarantineWarning.resultFunc(actions, originalProxyStageAction, isRootOrg);

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
    const webhooks = [
      { id: '1', description: 'webhook', url: 'url' },
      { id: '2', description: 'webhook', url: 'url' },
    ];
    const currentPolicy = {
      notifications: {
        userNotifications: [{ emailAddress: 'user@email.com', stageIds: [] }],
        roleNotifications: [{ roleId: '1', stageIds: [] }],
        webhookNotifications: [{ webhookId: '1', stageIds: [] }],
      },
    };

    it('is composed from the following selectors', () => {
      expect(selectApplicableWebhooks.dependencies).toEqual([selectCurrentPolicy, selectNotificationWebhooks]);
    });

    it('returns only available webhooks', () => {
      expect(selectApplicableWebhooks.resultFunc(currentPolicy, webhooks)).toEqual([
        { id: '2', description: 'webhook', url: 'url', displayName: 'webhook' },
      ]);
    });
  });

  describe('selectNotificationRecipients', () => {
    it('is composed from the following selectors', () => {
      expect(selectNotificationRecipients.dependencies).toEqual([
        selectCurrentPolicy,
        selectNotificationWebhooks,
        selectRolesForCurrentOwner,
        selectJiraProjectNames,
        selectJiraIssueTypeNames,
      ]);
    });

    it('returns recipients with proper display names', () => {
      const currentPolicy = {
        notifications: {
          userNotifications: [{ emailAddress: 'user@email.com', stageIds: [] }],
          roleNotifications: [{ roleId: '1', stageIds: [] }],
          webhookNotifications: [{ webhookId: '1', stageIds: [] }],
          jiraNotifications: [{ projectKey: 1, issueTypeId: 1, stageIds: [] }],
        },
      };
      const roles = [{ roleId: '1', roleName: 'developer' }];
      const webhooks = [{ id: '1', description: 'webhook', url: 'url' }];
      const jiraProjectNames = { 1: 'Project 1' };
      const jiraIssueTypeNames = { 1: 'Issue 1' };

      expect(
        selectNotificationRecipients.resultFunc(currentPolicy, webhooks, roles, jiraProjectNames, jiraIssueTypeNames)
      ).toEqual([
        { roleId: '1', displayName: 'developer', stageIds: [] },
        { projectKey: 1, issueTypeId: 1, displayName: 'Project 1 (Issue 1)', stageIds: [] },
        { emailAddress: 'user@email.com', displayName: 'user@email.com', stageIds: [] },
        { webhookId: '1', displayName: 'Webhook: webhook', stageIds: [] },
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
        selectIsWebhooksSupported,
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
      expect(selectAvailableJiraProjects.dependencies).toEqual([selectCurrentPolicy, selectJiraProjects]);
    });

    it('selects available jira projects', () => {
      const actual = selectAvailableJiraProjects.resultFunc(
        { notifications: { jiraNotifications: [{ projectKey: 1 }] } },
        [
          { key: 1, name: 'Name 1', issueTypes: [{ id: 1, name: 'Name 1' }] },
          { key: 2, name: 'Name 2', issueTypes: [{ id: 2, name: 'Name 2' }] },
        ]
      );
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
});
