/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { selectOwnerModalSlice } from 'MainRoot/OrgsAndPolicies/ownerModal/ownerModalSelectors';

import { nxTextInputStateHelpers, nxFileUploadStateHelpers } from '@sonatype/react-shared-components';
const { initialState: rscInitialState } = nxTextInputStateHelpers;
const { initialState: rscInitialFileUploadState } = nxFileUploadStateHelpers;

describe('ownerModalSelectors - selectOwnerModal', () => {
  const mockState = {
    orgsAndPolicies: {
      ownerActions: {
        ownerModal: {
          submitError: 'Some error',
          submitMaskState: null,
          isModalOpen: true,
          isEditMode: false,
          ownerIconType: 'robot',
          ownerIcon: rscInitialFileUploadState(null),
          robotHash: '1234',
          validationErrors: ['Some error'],
          ownerName: rscInitialState('newOwner'),
          appId: rscInitialState('newId'),
          isDirty: false,
          isUnsavedChangesModalOpen: false,
        },
      },
    },
  };

  it('selects ownerModal state', () => {
    expect(selectOwnerModalSlice(mockState)).toEqual({
      submitError: 'Some error',
      submitMaskState: null,
      isModalOpen: true,
      isEditMode: false,
      ownerIconType: 'robot',
      ownerIcon: rscInitialFileUploadState(null),
      robotHash: '1234',
      validationErrors: ['Some error'],
      ownerName: rscInitialState('newOwner'),
      appId: rscInitialState('newId'),
      isDirty: false,
      isUnsavedChangesModalOpen: false,
    });
  });
});
