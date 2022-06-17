/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import {
  selectOwnerDetailTreeSlice,
  selectLoading,
  selectLoadError,
} from 'MainRoot/OrgsAndPolicies/ownerDetailTreeSelectors';
import { selectOrgsAndPoliciesSlice } from 'MainRoot/OrgsAndPolicies/orgsAndPoliciesSelectors';

describe('ownerDetailTreeSelectors', () => {
  describe('selectOwnerDetailTreeSlice', () => {
    it('is composed from the following selector', () => {
      expect(selectOwnerDetailTreeSlice.dependencies).toEqual([selectOrgsAndPoliciesSlice]);
    });

    it('selects OwnerDetailTree', () => {
      const orgsAndPoliciesSlice = {
        ownerDetailTree: null,
      };

      const actualSelection = selectOwnerDetailTreeSlice.resultFunc(orgsAndPoliciesSlice);

      expect(actualSelection).toBe(null);
    });
  });

  describe('selectLoading', () => {
    it('is composed from the following selector', () => {
      expect(selectLoading.dependencies).toEqual([selectOwnerDetailTreeSlice]);
    });

    it('selects loading', () => {
      const OwnerDetailTreeSlice = {
        loading: true,
      };

      const actualSelection = selectLoading.resultFunc(OwnerDetailTreeSlice);

      expect(actualSelection).toBe(true);
    });
  });

  describe('selectLoadError', () => {
    it('is composed from the following selector', () => {
      expect(selectLoadError.dependencies).toEqual([selectOwnerDetailTreeSlice]);
    });

    it('selects loadError', () => {
      const OwnerDetailTreeSlice = {
        loadError: 'error',
      };

      const actualSelection = selectLoadError.resultFunc(OwnerDetailTreeSlice);

      expect(actualSelection).toBe('error');
    });
  });
});
