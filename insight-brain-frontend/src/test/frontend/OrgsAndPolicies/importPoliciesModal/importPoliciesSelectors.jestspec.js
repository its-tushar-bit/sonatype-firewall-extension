/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { selectImportPoliciesSlice } from 'MainRoot/OrgsAndPolicies/importPoliciesModal/importPoliciesSelectors.js';

describe('importPoliciesSelectors', () => {
  describe('selectImportPoliciesSelectors', () => {
    const mockState = {
      orgsAndPolicies: {
        ownerActions: {
          importPolicies: {
            isModalOpen: true,
            submitError: 'Some error',
            submitMaskState: null,
          },
        },
      },
    };

    it('selects import policies modal state', () => {
      expect(selectImportPoliciesSlice(mockState)).toEqual({
        isModalOpen: true,
        submitError: 'Some error',
        submitMaskState: null,
      });
    });
  });
});
