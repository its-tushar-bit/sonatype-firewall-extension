/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { selectChangeApplicationIdSlice } from 'MainRoot/OrgsAndPolicies/changeApplicationIdModal/changeApplicationIdSelectors.js';

import { nxTextInputStateHelpers } from '@sonatype/react-shared-components';
const { initialState: rscInitialState } = nxTextInputStateHelpers;

describe('changeApplicationIdSelectors', () => {
  let mockState;

  beforeEach(() => {
    mockState = {
      orgsAndPolicies: {
        ownerActions: {
          changeAppId: {
            submitError: 'Some error',
            submitMaskState: null,
            isModalOpen: true,
            newPublicId: rscInitialState('initialId'),
          },
        },
      },
    };
  });

  describe('selectChangeApplicationIdModal', () => {
    it('selects changeAppId state', () => {
      expect(selectChangeApplicationIdSlice(mockState)).toEqual({
        submitError: 'Some error',
        submitMaskState: null,
        isModalOpen: true,
        newPublicId: rscInitialState('initialId'),
      });
    });
  });
});
