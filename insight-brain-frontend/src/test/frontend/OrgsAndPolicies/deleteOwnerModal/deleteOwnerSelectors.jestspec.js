/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { selectDeleteOwnerSlice } from 'MainRoot/OrgsAndPolicies/deleteOwnerModal/deleteOwnerSelectors.js';

describe('deleteOwnerSelectors', () => {
  let mockState;

  beforeEach(() => {
    mockState = {
      orgsAndPolicies: {
        ownerActions: {
          deleteOwner: {
            submitError: 'Some error',
            submitMaskState: null,
            isModalOpen: true,
          },
        },
      },
    };
  });

  describe('selectDeleteOwnerModal', () => {
    it('selects deleteModal state', () => {
      expect(selectDeleteOwnerSlice(mockState)).toEqual({
        submitError: 'Some error',
        submitMaskState: null,
        isModalOpen: true,
      });
    });
  });
});
