/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import {
  selectImportSbomModalSlice,
  selectIsModalOpen,
} from 'MainRoot/OrgsAndPolicies/importSbomModal/importSbomModalSelectors';

describe('selectImportSbomModalSelectors', () => {
  const mockState = {
    orgsAndPolicies: {
      ownerActions: {
        importSbomModal: {
          isModalOpen: true,
        },
      },
    },
  };

  describe('selectImportSbomModalSlice', () => {
    it('selects importSbomModal state', () => {
      expect(selectImportSbomModalSlice(mockState)).toEqual({
        isModalOpen: true,
      });
    });
  });

  describe('selectIsModalOpen', () => {
    it('selects isModalOpen state', () => {
      expect(selectIsModalOpen(mockState)).toEqual(true);
    });
  });
});
