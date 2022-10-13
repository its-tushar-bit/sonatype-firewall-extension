/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { selectToastSlice } from 'MainRoot/toastContainer/toastSelectors';

describe('selectToastSlice', () => {
  const mockState = {
    toast: {
      toastIdInc: 1,
      toasts: [
        {
          id: 1,
          type: 'success',
          message: 'Success Toast',
        },
      ],
    },
  };

  it('selects correct state', () => {
    expect(selectToastSlice(mockState)).toEqual({
      toastIdInc: 1,
      toasts: [
        {
          id: 1,
          type: 'success',
          message: 'Success Toast',
        },
      ],
    });
  });
});
