/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { grandfatheringSlice } from 'MainRoot/OrgsAndPolicies/grandfatheringModal/grandfatheringSelectors.js';

describe('grandfatheringSelectors', () => {
  let mockState;

  beforeEach(() => {
    mockState = {
      orgsAndPolicies: {
        ownerActions: {
          grandfathering: {
            submitError: 'Some error',
            submitMaskState: null,
            isModalOpen: true,
          },
        },
      },
    };
  });

  describe('selectGrandfathering', () => {
    it('selects grandfathering modal state', () => {
      expect(grandfatheringSlice(mockState)).toEqual({
        submitError: 'Some error',
        submitMaskState: null,
        isModalOpen: true,
      });
    });
  });
});
