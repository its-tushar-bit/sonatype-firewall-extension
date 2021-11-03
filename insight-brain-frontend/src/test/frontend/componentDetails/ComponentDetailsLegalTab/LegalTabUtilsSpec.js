/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { isOverriddenOrSelected } from 'MainRoot/componentDetails/ComponentDetailsLegalTab/LegalTabUtils';

describe('LegalTabUtils', () => {
  describe('isOverriddenOrSelected', () => {
    it('returns false if status is not Overridden or Selected', () => {
      expect(isOverriddenOrSelected('OPEN')).toBeFalse();
      expect(isOverriddenOrSelected('ACKNOWLEDGED')).toBeFalse();
      expect(isOverriddenOrSelected('CONFIRMED')).toBeFalse();
      expect(isOverriddenOrSelected(null)).toBeFalse();
    });

    it('returns true if status is `Selected`', () => {
      expect(isOverriddenOrSelected('SELECTED')).toBeTrue();
    });

    it('returns true if status is `Overridden`', () => {
      expect(isOverriddenOrSelected('OVERRIDDEN')).toBeTrue();
    });
  });
});
