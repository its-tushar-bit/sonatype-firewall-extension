/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

import {
  selectAppCategoryOwners,
  selectIsEditMode,
  selectApplicationCategoriesSlice,
  selectIsLoading,
  selectLoadError,
  selectIsDirty,
  selectCurrentCategory,
  selectAssociatedApplicationNames,
  selectDeleteModal,
  selectSiblings,
  selectTagPolicyList,
} from 'MainRoot/OrgsAndPolicies/orgsAndPoliciesApplicationCategoriesSelectors';
import { selectOrgsAndPoliciesSlice } from 'MainRoot/OrgsAndPolicies/orgsAndPoliciesSelectors';
import { selectRouterCurrentParams } from 'MainRoot/reduxUiRouter/routerSelectors';

describe('orgsAndPoliciesSelectors', () => {
  describe('selectApplicationCategoriesSlice', () => {
    it('is composed from the following selector', () => {
      expect(selectApplicationCategoriesSlice.dependencies).toEqual([selectOrgsAndPoliciesSlice]);
    });

    it('selects applicationCategoriesSlice', () => {
      const orgsAndPoliciesSlice = {
        applicationCategories: 'applicationCategories',
      };

      const selected = selectApplicationCategoriesSlice.resultFunc(orgsAndPoliciesSlice);

      expect(selected).toBe('applicationCategories');
    });
  });

  describe('selectIsEditMode', () => {
    it('is composed from the following selector', () => {
      expect(selectIsEditMode.dependencies).toEqual([selectRouterCurrentParams]);
    });

    it('returns true if categoryId exist in the current param', () => {
      const routerCurrentParams = {
        categoryId: 'categoryId',
      };

      const selected = selectIsEditMode.resultFunc(routerCurrentParams);

      expect(selected).toBeTrue();
    });

    it('returns false if categoryId does not exist in the current param', () => {
      const selected = selectIsEditMode.resultFunc({});

      expect(selected).toBeFalse();
    });
  });

  describe('selectAppCategoryOwners', () => {
    it('is composed from the following selector', () => {
      expect(selectAppCategoryOwners.dependencies).toEqual([selectApplicationCategoriesSlice]);
    });

    it('selects appCategoryOwners', () => {
      const orgsAndPoliciesApplicationCategoriesSlice = {
        appCategoryOwners: ['owner'],
      };

      const selected = selectAppCategoryOwners.resultFunc(orgsAndPoliciesApplicationCategoriesSlice);

      expect(selected).toEqual(['owner']);
    });
  });

  describe('selectIsLoading', () => {
    it('is composed from the following selector', () => {
      expect(selectIsLoading.dependencies).toEqual([selectApplicationCategoriesSlice]);
    });

    it('selects loading', () => {
      const orgsAndPoliciesApplicationCategoriesSlice = {
        loading: true,
      };

      const selected = selectIsLoading.resultFunc(orgsAndPoliciesApplicationCategoriesSlice);

      expect(selected).toBeTrue();
    });
  });

  describe('selectLoadError', () => {
    it('is composed from the following selector', () => {
      expect(selectLoadError.dependencies).toEqual([selectApplicationCategoriesSlice]);
    });

    it('selects loadError', () => {
      const orgsAndPoliciesApplicationCategoriesSlice = {
        loadError: 'someError',
      };

      const selected = selectLoadError.resultFunc(orgsAndPoliciesApplicationCategoriesSlice);

      expect(selected).toBe('someError');
    });
  });

  describe('selectIsDirty', () => {
    it('is composed from the following selector', () => {
      expect(selectIsDirty.dependencies).toEqual([selectApplicationCategoriesSlice]);
    });

    it('selects isDirty', () => {
      const orgsAndPoliciesApplicationCategoriesSlice = {
        isDirty: true,
      };

      const selected = selectIsDirty.resultFunc(orgsAndPoliciesApplicationCategoriesSlice);

      expect(selected).toBeTrue();
    });
  });

  describe('selectCurrentCategory', () => {
    it('is composed from the following selector', () => {
      expect(selectCurrentCategory.dependencies).toEqual([selectApplicationCategoriesSlice]);
    });

    it('selects currentCategory', () => {
      const orgsAndPoliciesApplicationCategoriesSlice = {
        currentCategory: { id: 'someId' },
      };

      const selected = selectCurrentCategory.resultFunc(orgsAndPoliciesApplicationCategoriesSlice);

      expect(selected).toEqual({ id: 'someId' });
    });
  });

  describe('selectDeleteModal', () => {
    it('is composed from the following selector', () => {
      expect(selectDeleteModal.dependencies).toEqual([selectApplicationCategoriesSlice]);
    });

    it('selects deleteModal', () => {
      const orgsAndPoliciesApplicationCategoriesSlice = {
        deleteModal: { deleting: false },
      };

      const selected = selectDeleteModal.resultFunc(orgsAndPoliciesApplicationCategoriesSlice);

      expect(selected).toEqual({ deleting: false });
    });
  });

  describe('selectAssociatedApplicationNames', () => {
    it('is composed from the following selector', () => {
      expect(selectAssociatedApplicationNames.dependencies).toEqual([selectDeleteModal]);
    });

    it('selects associatedApplicationNames', () => {
      const deleteModal = {
        associatedApplicationNames: ['someAssociatedApplicationName'],
      };

      const selected = selectAssociatedApplicationNames.resultFunc(deleteModal);

      expect(selected).toEqual(['someAssociatedApplicationName']);
    });
  });

  describe('selectTagPolicyList', () => {
    it('is composed from the following selector', () => {
      expect(selectTagPolicyList.dependencies).toEqual([selectDeleteModal]);
    });

    it('selects tagPolicyList', () => {
      const deleteModal = {
        tagPolicyList: ['someAssociatedPolicyName'],
      };

      const selected = selectTagPolicyList.resultFunc(deleteModal);

      expect(selected).toEqual(['someAssociatedPolicyName']);
    });
  });

  describe('selectSiblings', () => {
    it('is composed from the following selector', () => {
      expect(selectSiblings.dependencies).toEqual([selectApplicationCategoriesSlice]);
    });

    it('selects siblings', () => {
      const orgsAndPoliciesApplicationCategoriesSlice = {
        siblings: [{ id: 'someCategoryId' }],
      };

      const selected = selectSiblings.resultFunc(orgsAndPoliciesApplicationCategoriesSlice);

      expect(selected).toEqual([{ id: 'someCategoryId' }]);
    });
  });
});
