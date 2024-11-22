/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { selectMoveOwnerSlice, selectMoveOwnerWarnings } from 'MainRoot/OrgsAndPolicies/moveOwner/moveOwnerSelectors';

describe('selectMoveApplicationSelectors', () => {
  const mockState = {
    orgsAndPolicies: {
      ownerActions: {
        moveOwner: {
          isMoveAppModalOpen: true,
        },
      },
    },
  };

  describe('selectMoveOwnerSlice', () => {
    it('selects move owner state', () => {
      expect(selectMoveOwnerSlice(mockState)).toEqual({
        isMoveAppModalOpen: true,
      });
    });

    describe('selectMoveOwnerWarnings', () => {
      it('is composed from the following selector', () => {
        expect(selectMoveOwnerWarnings.dependencies).toEqual([selectMoveOwnerSlice]);
      });

      it('selects owner sidenav slice', () => {
        const state = { warnings: ['some warnings', 'other warning'] };
        const result = selectMoveOwnerWarnings.resultFunc(state);

        expect(result).toEqual(['some warnings', 'other warning']);
      });
    });
  });
});
