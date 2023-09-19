/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { legacyViolationModalSlice } from 'MainRoot/OrgsAndPolicies/legacyViolationModal/legacyViolationModalSelectors.js';

describe('legacyViolationsSelectors', () => {
  let mockState;

  beforeEach(() => {
    mockState = {
      orgsAndPolicies: {
        ownerActions: {
          legacyViolations: {
            submitError: 'Some error',
            submitMaskState: null,
            isModalOpen: true,
          },
        },
      },
    };
  });

  describe('selectLegacyViolationsModal', () => {
    it('selects legacyViolations modal state', () => {
      expect(legacyViolationModalSlice(mockState)).toEqual({
        submitError: 'Some error',
        submitMaskState: null,
        isModalOpen: true,
      });
    });
  });
});
