/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { selectOrgsAndPoliciesSlice } from 'MainRoot/OrgsAndPolicies/orgsAndPoliciesSelectors';
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
} from 'MainRoot/OrgsAndPolicies/policySelectors';
import { selectRouterCurrentParams } from 'MainRoot/reduxUiRouter/routerSelectors';

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
        loading: true,
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
});
