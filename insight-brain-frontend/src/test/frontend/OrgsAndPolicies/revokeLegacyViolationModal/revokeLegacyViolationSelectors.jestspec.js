/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { selectRevokeLegacyViolationModalSlice } from 'MainRoot/OrgsAndPolicies/revokeLegacyViolationModal/revokeLegacyViolationModalSelectors.js';

describe('revokeLegacyViolationSelectors', () => {
  let mockState;

  beforeEach(() => {
    mockState = {
      orgsAndPolicies: {
        ownerActions: {
          revokeLegacyViolations: {
            submitError: 'Some error',
            submitMaskState: null,
            isModalOpen: true,
          },
        },
      },
    };
  });

  describe('selectRevokeLegacyViolationModal', () => {
    it('selects selectRevokeLegacyViolation modal state', () => {
      expect(selectRevokeLegacyViolationModalSlice(mockState)).toEqual({
        submitError: 'Some error',
        submitMaskState: null,
        isModalOpen: true,
      });
    });
  });
});
