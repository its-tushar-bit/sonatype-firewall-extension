/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { selectEvaluateApplicationSlice } from 'MainRoot/OrgsAndPolicies/evaluateApplicationModal/evaluateApplicationSelectors';

describe('selectEvaluateApplicationSelectors', () => {
  const mockState = {
    orgsAndPolicies: {
      ownerActions: {
        evaluateApplication: {
          isEvaluationModalOpen: true,
        },
      },
    },
  };

  describe('selectEvaluateApplication', () => {
    it('selects evaluate application state', () => {
      expect(selectEvaluateApplicationSlice(mockState)).toEqual({
        isEvaluationModalOpen: true,
      });
    });
  });
});
