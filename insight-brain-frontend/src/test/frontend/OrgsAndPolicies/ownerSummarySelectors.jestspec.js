/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import {
  selectOwnerSummarySlice,
  selectLoading,
  selectLoadError,
  selectHasEditIqPermission,
} from 'MainRoot/OrgsAndPolicies/ownerSummarySelectors';
import { selectOrgsAndPoliciesSlice } from 'MainRoot/OrgsAndPolicies/orgsAndPoliciesSelectors';

describe('ownerSummarySelectors', () => {
  describe('selectOwnerSummarySlice', () => {
    it('is composed from the following selector', () => {
      expect(selectOwnerSummarySlice.dependencies).toEqual([selectOrgsAndPoliciesSlice]);
    });

    it('selects ownerSummary', () => {
      const orgsAndPoliciesSlice = {
        ownerSummary: null,
      };

      const actualSelection = selectOwnerSummarySlice.resultFunc(orgsAndPoliciesSlice);

      expect(actualSelection).toBe(null);
    });
  });

  describe('selectLoading', () => {
    it('is composed from the following selector', () => {
      expect(selectLoading.dependencies).toEqual([selectOwnerSummarySlice]);
    });

    it('selects loading', () => {
      const ownerSummarySlice = {
        loading: true,
      };

      const actualSelection = selectLoading.resultFunc(ownerSummarySlice);

      expect(actualSelection).toBe(true);
    });
  });

  describe('selectLoadError', () => {
    it('is composed from the following selector', () => {
      expect(selectLoadError.dependencies).toEqual([selectOwnerSummarySlice]);
    });

    it('selects loadError', () => {
      const ownerSummarySlice = {
        loadError: 'error',
      };

      const actualSelection = selectLoadError.resultFunc(ownerSummarySlice);

      expect(actualSelection).toBe('error');
    });
  });

  describe('selectHasEditIqPermission', () => {
    it('is composed from the following selector', () => {
      expect(selectHasEditIqPermission.dependencies).toEqual([selectOwnerSummarySlice]);
    });

    it('selects hasEditIqPermission', () => {
      const ownerSummarySlice = {
        hasEditIqPermission: false,
      };

      const actualSelection = selectHasEditIqPermission.resultFunc(ownerSummarySlice);

      expect(actualSelection).toBe(false);
    });
  });
});
