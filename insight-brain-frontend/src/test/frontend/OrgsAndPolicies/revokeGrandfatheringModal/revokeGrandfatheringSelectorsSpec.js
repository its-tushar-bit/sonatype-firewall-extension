/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { selectRevokeGrandfatheringSlice } from 'MainRoot/OrgsAndPolicies/revokeGrandfatheringModal/revokeGrandfatheringSelectors.js';

describe('revokeGrandfatheringSelectors', () => {
  let mockState;

  beforeEach(() => {
    mockState = {
      orgsAndPolicies: {
        ownerEditor: {
          revokeGrandfathering: {
            submitError: 'Some error',
            submitMaskState: null,
            isModalOpen: true,
          },
        },
      },
    };
  });

  describe('selectRevokeGrandfatheringModal', () => {
    it('selects revokeGrandfathering modal state', () => {
      expect(selectRevokeGrandfatheringSlice(mockState)).toEqual({
        submitError: 'Some error',
        submitMaskState: null,
        isModalOpen: true,
      });
    });
  });
});
