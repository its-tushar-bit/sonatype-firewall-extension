/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { createTierGatedDirtySelector } from 'MainRoot/productFeatures/tierGateUtils';

describe('tierGateUtils', () => {
  describe('createTierGatedDirtySelector', () => {
    const selectIsDirtyMock = (state) => state.isDirty;
    const selectHasFeatureMock = (state) => state.hasFeature;
    const selectGatedIsDirty = createTierGatedDirtySelector(selectIsDirtyMock, selectHasFeatureMock);

    it('returns dirty state when feature is entitled', () => {
      expect(selectGatedIsDirty({ hasFeature: true, isDirty: true })).toBe(true);
    });

    it('returns false when feature is entitled but not dirty', () => {
      expect(selectGatedIsDirty({ hasFeature: true, isDirty: false })).toBe(false);
    });

    it('suppresses dirty when feature is not entitled, even when raw isDirty is true', () => {
      // When a Pro user has no entitlement, no form on the tier-gated pages is
      // saveable — the underlying dirty flag must never leak through to the
      // router guard. This covers both Custom-preview edits and "Preview Add"
      // new-entity flows where isEnterprisePreviewMode stays false.
      expect(selectGatedIsDirty({ hasFeature: false, isDirty: true })).toBe(false);
    });

    it('returns false when feature is not entitled and not dirty', () => {
      expect(selectGatedIsDirty({ hasFeature: false, isDirty: false })).toBe(false);
    });

    describe('memoization', () => {
      it('returns cached result when dependencies have not changed', () => {
        const state = { hasFeature: false, isDirty: true };
        const result1 = selectGatedIsDirty(state);
        const result2 = selectGatedIsDirty(state);
        expect(result1).toBe(false);
        expect(result2).toBe(false);
        expect(result1).toBe(result2);
      });
    });
  });
});
