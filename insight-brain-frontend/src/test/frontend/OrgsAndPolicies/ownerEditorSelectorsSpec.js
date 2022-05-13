/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { selectOwnerEditorSlice, selectDeleteModal } from 'MainRoot/OrgsAndPolicies/ownerEditorSelectors';

describe('ownerEditorSelectors', () => {
  let mockState;

  beforeEach(() => {
    mockState = {
      orgsAndPolicies: {
        ownerEditor: {
          deleteModal: {
            success: true,
            deleting: false,
            errorState: 'someError',
          },
        },
      },
    };
  });

  describe('selectOwnerEditorSlice', () => {
    it('returns slice', () => {
      expect(selectOwnerEditorSlice(mockState)).toEqual(mockState.orgsAndPolicies.ownerEditor);
    });
  });

  describe('selectDeleteModal', () => {
    it('is composed from the following selector', () => {
      expect(selectDeleteModal.dependencies).toEqual([selectOwnerEditorSlice]);
    });

    it('selects deleteModal state', () => {
      expect(selectDeleteModal(mockState)).toEqual({
        success: true,
        deleting: false,
        errorState: 'someError',
      });
    });
  });
});
