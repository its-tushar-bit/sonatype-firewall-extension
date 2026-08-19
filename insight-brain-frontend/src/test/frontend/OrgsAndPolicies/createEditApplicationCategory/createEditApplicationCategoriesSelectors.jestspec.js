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
  selectApplicationTags,
} from 'MainRoot/OrgsAndPolicies/createEditApplicationCategory/createEditApplicationCategoriesSelectors';
import { selectOrgsAndPoliciesSlice } from 'MainRoot/OrgsAndPolicies/orgsAndPoliciesSelectors';
import { selectOwnersFlattenEntries } from 'MainRoot/OrgsAndPolicies/ownerSideNav/ownerSideNavSelectors';
import { selectRouterCurrentParams } from 'MainRoot/reduxUiRouter/routerSelectors';

describe('applicationCategoriesSelectors', () => {
  describe('selectApplicationCategoriesSlice', () => {
    it('is composed from the following selector', () => {
      expect(selectApplicationCategoriesSlice.dependencies).toEqual([selectOrgsAndPoliciesSlice]);
    });

    it('selects applicationCategoriesSlice', () => {
      const orgsAndPoliciesSlice = {
        applicationCategories: { createEdit: 'applicationCategories' },
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

      expect(selected).toBe(true);
    });

    it('returns false if categoryId does not exist in the current param', () => {
      const selected = selectIsEditMode.resultFunc({});

      expect(selected).toBe(false);
    });
  });

  describe('selectAppCategoryOwners', () => {
    it('is composed from the following selector', () => {
      expect(selectAppCategoryOwners.dependencies).toEqual([selectApplicationCategoriesSlice]);
    });

    it('selects appCategoryOwners', () => {
      const applicationCategoriesSlice = {
        appCategoryOwners: ['owner'],
      };

      const selected = selectAppCategoryOwners.resultFunc(applicationCategoriesSlice);

      expect(selected).toEqual(['owner']);
    });
  });

  describe('selectIsLoading', () => {
    it('is composed from the following selector', () => {
      expect(selectIsLoading.dependencies).toEqual([selectApplicationCategoriesSlice]);
    });

    it('selects loading', () => {
      const applicationCategoriesSlice = {
        loading: true,
      };

      const selected = selectIsLoading.resultFunc(applicationCategoriesSlice);

      expect(selected).toBe(true);
    });
  });

  describe('selectLoadError', () => {
    it('is composed from the following selector', () => {
      expect(selectLoadError.dependencies).toEqual([selectApplicationCategoriesSlice]);
    });

    it('selects loadError', () => {
      const applicationCategoriesSlice = {
        loadError: 'someError',
      };

      const selected = selectLoadError.resultFunc(applicationCategoriesSlice);

      expect(selected).toBe('someError');
    });
  });

  describe('selectIsDirty', () => {
    it('returns true when form is dirty and feature is entitled', () => {
      // hasFeature=true, isDirty=true
      const selected = selectIsDirty.resultFunc(true, true);

      expect(selected).toBe(true);
    });

    it('returns false when form is not dirty', () => {
      const selected = selectIsDirty.resultFunc(true, false);

      expect(selected).toBe(false);
    });

    it('returns false when feature is gated, even if raw form is dirty', () => {
      // hasFeature=false, isDirty=true — Pro user on a non-saveable preview form
      const selected = selectIsDirty.resultFunc(false, true);

      expect(selected).toBe(false);
    });
  });

  describe('selectCurrentCategory', () => {
    it('is composed from the following selector', () => {
      expect(selectCurrentCategory.dependencies).toEqual([selectApplicationCategoriesSlice]);
    });

    it('selects currentCategory', () => {
      const applicationCategoriesSlice = {
        currentCategory: { id: 'someId' },
      };

      const selected = selectCurrentCategory.resultFunc(applicationCategoriesSlice);

      expect(selected).toEqual({ id: 'someId' });
    });
  });

  describe('selectDeleteModal', () => {
    it('is composed from the following selector', () => {
      expect(selectDeleteModal.dependencies).toEqual([selectApplicationCategoriesSlice]);
    });

    it('selects deleteModal', () => {
      const applicationCategoriesSlice = {
        deleteModal: { deleting: false },
      };

      const selected = selectDeleteModal.resultFunc(applicationCategoriesSlice);

      expect(selected).toEqual({ deleting: false });
    });
  });

  describe('selectAssociatedApplicationNames', () => {
    it('is composed from the following selector', () => {
      expect(selectAssociatedApplicationNames.dependencies).toEqual([
        selectApplicationTags,
        selectOwnersFlattenEntries,
        selectRouterCurrentParams,
      ]);
    });

    it('selects associatedApplicationNames', () => {
      const selected = selectAssociatedApplicationNames.resultFunc(
        [
          {
            id: 'ce37471280114bb7b3d64b30c65e6e86',
            applicationId: 'applicationId',
            tagId: 'categoryId',
          },
          {
            id: 'f47f64880c5d44b882045b564ecf3dd7',
            applicationId: 'd0fbe038085941c9a25f62bb9eb7f085',
            tagId: '34f1f000d7a54abaa2b12731cdff780f',
          },
        ],
        { applications: [{ id: 'applicationId', name: 'someAssociatedApplicationName' }] },
        { categoryId: 'categoryId' }
      );

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
      const applicationCategoriesSlice = {
        siblings: [{ id: 'someCategoryId' }],
      };

      const selected = selectSiblings.resultFunc(applicationCategoriesSlice);

      expect(selected).toEqual([{ id: 'someCategoryId' }]);
    });
  });
});
