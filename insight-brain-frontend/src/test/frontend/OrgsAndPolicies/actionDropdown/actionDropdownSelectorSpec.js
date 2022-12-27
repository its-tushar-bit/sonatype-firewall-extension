/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { selectActionDropdownSlice } from 'MainRoot/OrgsAndPolicies/actionDropdown/actionDropdownSelectors.js';

describe('actionDropdownSelector', () => {
  const mockState = {
    orgsAndPolicies: {
      ownerActions: {
        actionDropdown: {
          loading: false,
          loadError: null,
          applicationSummary: null,
        },
      },
    },
  };

  it('action dropdown state', () => {
    expect(selectActionDropdownSlice(mockState)).toEqual({
      loading: false,
      loadError: null,
      applicationSummary: null,
    });
  });
});
