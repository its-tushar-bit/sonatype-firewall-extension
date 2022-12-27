/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { selectMoveApplicationSlice } from 'MainRoot/OrgsAndPolicies/moveApplicationModal/moveApplicationSelectors.js';

describe('selectMoveApplicationSelectors', () => {
  const mockState = {
    orgsAndPolicies: {
      ownerActions: {
        moveApplication: {
          isMoveAppModalOpen: true,
        },
      },
    },
  };

  describe('selectMoveApplication', () => {
    it('selects move application state', () => {
      expect(selectMoveApplicationSlice(mockState)).toEqual({
        isMoveAppModalOpen: true,
      });
    });
  });
});
