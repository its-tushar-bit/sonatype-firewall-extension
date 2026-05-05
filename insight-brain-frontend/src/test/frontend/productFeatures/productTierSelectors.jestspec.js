/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import {
  selectIsPro,
  selectIsEnterprise,
  selectTierLoading,
  TIER_LIFECYCLE_PRO,
  TIER_LIFECYCLE_ENTERPRISE,
} from 'MainRoot/productFeatures/productTierSelectors';

describe('productTierSelectors', () => {
  const createState = (productEdition, loading = false) => ({
    productLicense: {
      loading,
      license: {
        productEdition,
      },
    },
  });

  describe('selectIsPro', () => {
    it('returns true when productEdition is Lifecycle Pro', () => {
      expect(selectIsPro(createState(TIER_LIFECYCLE_PRO))).toBe(true);
    });

    it('returns false when productEdition is Lifecycle Enterprise', () => {
      expect(selectIsPro(createState(TIER_LIFECYCLE_ENTERPRISE))).toBe(false);
    });

    it('returns false when productEdition is Lifecycle', () => {
      expect(selectIsPro(createState('Lifecycle'))).toBe(false);
    });

    it('returns false when productEdition is null', () => {
      expect(selectIsPro(createState(null))).toBe(false);
    });
  });

  describe('selectIsEnterprise', () => {
    it('returns true when productEdition is Lifecycle Enterprise', () => {
      expect(selectIsEnterprise(createState(TIER_LIFECYCLE_ENTERPRISE))).toBe(true);
    });

    it('returns false when productEdition is Lifecycle Pro', () => {
      expect(selectIsEnterprise(createState(TIER_LIFECYCLE_PRO))).toBe(false);
    });

    it('returns false when productEdition is Lifecycle', () => {
      expect(selectIsEnterprise(createState('Lifecycle'))).toBe(false);
    });

    it('returns false when productEdition is null', () => {
      expect(selectIsEnterprise(createState(null))).toBe(false);
    });
  });

  describe('selectTierLoading', () => {
    it('returns true when loading', () => {
      expect(selectTierLoading(createState(null, true))).toBe(true);
    });

    it('returns false when not loading', () => {
      expect(selectTierLoading(createState(null, false))).toBe(false);
    });
  });

  describe('constants', () => {
    it('TIER_LIFECYCLE_PRO matches backend value', () => {
      expect(TIER_LIFECYCLE_PRO).toBe('Lifecycle Pro');
    });

    it('TIER_LIFECYCLE_ENTERPRISE matches backend value', () => {
      expect(TIER_LIFECYCLE_ENTERPRISE).toBe('Lifecycle Enterprise');
    });
  });
});
