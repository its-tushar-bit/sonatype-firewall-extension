/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

import {
  selectLoadingApplicableCategories,
  selectLoadApplicableCategoriesError,
  selectApplicableCategories,
  selectLoadingAppliedCategories,
  selectLoadAppliedCategoriesError,
  selectAppliedCategories,
  selectAssignAppCategoriesSubmitMaskState,
  selectSubmitApplyCategoriesError,
  selectIsDirty,
  selectCategories,
  selectAreAnyCategoriesDefined,
} from 'MainRoot/OrgsAndPolicies/assignApplicationCategoriesSelectors';

describe('assignApplicationCategoriesSelectors', () => {
  let mockState;

  beforeEach(() => {
    mockState = {
      productFeatures: {
        productFeatures: {},
        isEnterprisePreviewMode: false,
      },
      orgsAndPolicies: {
        root: {
          ownerName: 'alpine test',
        },
        applicationCategories: {
          assign: {
            loadingApplicableCategories: false,
            loadApplicableCategoriesError: 'some tags error',
            applicableCategories: [
              {
                id: 'b41f532e70f14c8e96c1b5932d7296d7',
                name: 'Distributed',
                description: 'Applications that are provided for consumption outside the company',
                organizationId: 'ROOT_ORGANIZATION_ID',
                color: 'yellow',
              },
              {
                id: '0f027a84df8e4d14b736c8eacea6c2ac',
                name: 'Hosted',
                description: 'Applications that are hosted such as services or software as a service.',
                organizationId: 'ROOT_ORGANIZATION_ID',
                color: 'light-purple',
              },
              {
                id: '13dfce231ca24289bec319fddf4bef88',
                name: 'Internal',
                description: 'Applications that are used only by your employees',
                organizationId: 'ROOT_ORGANIZATION_ID',
                color: 'dark-green',
              },
            ],
            loadingAppliedCategories: false,
            loadAppliedCategoriesError: 'some applied tags error',
            appliedCategories: [
              {
                id: 'b41f532e70f14c8e96c1b5932d7296d7',
                name: 'Distributed',
                description: 'Applications that are provided for consumption outside the company',
                organizationId: 'ROOT_ORGANIZATION_ID',
                color: 'yellow',
              },
              {
                id: '0f027a84df8e4d14b736c8eacea6c2ac',
                name: 'Hosted',
                description: 'Applications that are hosted such as services or software as a service.',
                organizationId: 'ROOT_ORGANIZATION_ID',
                color: 'light-purple',
              },
            ],
            originalAppliedCategories: [
              {
                id: 'b41f532e70f14c8e96c1b5932d7296d7',
                name: 'Distributed',
                description: 'Applications that are provided for consumption outside the company',
                organizationId: 'ROOT_ORGANIZATION_ID',
                color: 'yellow',
              },
            ],
            isDirty: false,
            submitMaskState: null,
            submitError: 'some submit error',
          },
        },
      },
    };
  });

  describe('Applicable Categories', () => {
    describe('selectLoadingApplicableCategories', () => {
      it('returns false when applicable categories request is not loading', () => {
        expect(selectLoadingApplicableCategories(mockState)).toBe(false);
      });

      it('returns true when applicable categories request is loading', () => {
        mockState.orgsAndPolicies.applicationCategories.assign.loadingApplicableCategories = true;
        expect(selectLoadingApplicableCategories(mockState)).toBe(true);
      });
    });

    describe('selectLoadApplicableCategoriesError', () => {
      it('returns error when present', () => {
        expect(selectLoadApplicableCategoriesError(mockState)).toBe('some tags error');
      });
    });

    describe('selectApplicableCategories', () => {
      it('returns Applicable tags', () => {
        expect(selectApplicableCategories(mockState)).toEqual(
          mockState.orgsAndPolicies.applicationCategories.assign.applicableCategories
        );
      });
    });
  });

  describe('Applied Categories', () => {
    describe('selectLoadingAppliedCategories', () => {
      it('returns false when applied categories request is not loading', () => {
        expect(selectLoadingAppliedCategories(mockState)).toBe(false);
      });

      it('returns true when applied categories request is loading', () => {
        mockState.orgsAndPolicies.applicationCategories.assign.loadingAppliedCategories = true;
        expect(selectLoadingAppliedCategories(mockState)).toBe(true);
      });
    });

    describe('selectLoadAppliedCategoriesError', () => {
      it('returns error when present', () => {
        expect(selectLoadAppliedCategoriesError(mockState)).toBe('some applied tags error');
      });
    });

    describe('selectAppliedCategories', () => {
      it('returns Applied Categories', () => {
        expect(selectAppliedCategories(mockState)).toEqual(
          mockState.orgsAndPolicies.applicationCategories.assign.appliedCategories
        );
      });
    });
  });

  describe('Submit', () => {
    describe('selectSubmitApplyCategoriesLoading', () => {
      it('returns false when submit request is not loading', () => {
        expect(selectAssignAppCategoriesSubmitMaskState(mockState)).toBeNull();
      });

      it('returns true when submit request is loading', () => {
        mockState.orgsAndPolicies.applicationCategories.assign.submitMaskState = false;
        expect(selectAssignAppCategoriesSubmitMaskState(mockState)).toBe(false);
      });
    });

    describe('selectSubmitApplyCategoriesError', () => {
      it('returns error when present', () => {
        expect(selectSubmitApplyCategoriesError(mockState)).toBe('some submit error');
      });
    });
  });

  describe('selectIsDirty', () => {
    it('returns false when form is not dirty', () => {
      expect(selectIsDirty(mockState)).toBe(false);
    });

    it('returns true when form is dirty', () => {
      mockState.orgsAndPolicies.applicationCategories.assign.isDirty = true;
      expect(selectIsDirty(mockState)).toBe(true);
    });
  });

  describe('selectCategories', () => {
    it('returns categories', () => {
      expect(selectCategories(mockState)).toEqual([
        {
          id: 'b41f532e70f14c8e96c1b5932d7296d7',
          name: 'Distributed',
          description: 'Applications that are provided for consumption outside the company',
          organizationId: 'ROOT_ORGANIZATION_ID',
          color: 'yellow',
          isApplied: true,
        },
        {
          id: '0f027a84df8e4d14b736c8eacea6c2ac',
          name: 'Hosted',
          description: 'Applications that are hosted such as services or software as a service.',
          organizationId: 'ROOT_ORGANIZATION_ID',
          color: 'light-purple',
          isApplied: true,
        },
        {
          id: '13dfce231ca24289bec319fddf4bef88',
          name: 'Internal',
          description: 'Applications that are used only by your employees',
          organizationId: 'ROOT_ORGANIZATION_ID',
          color: 'dark-green',
          isApplied: false,
        },
      ]);
    });
  });
  describe('selectAreAnyCategoriesDefined', () => {
    it('returns if any categories are defined', () => {
      expect(selectAreAnyCategoriesDefined(mockState)).toBe(true);
    });
  });
});
