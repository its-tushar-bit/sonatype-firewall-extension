/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import reducer from 'MainRoot/OrgsAndPolicies/orgsAndPoliciesRootSlice';

describe('orgsAndPolicies reducer', () => {
  describe('orgsAndPolicies/updatedOwnerHandler', () => {
    it('sets ownerName property', () => {
      const state = Object.freeze({
        ownerName: null,
      });

      const newState = reducer(state, {
        type: 'orgsAndPolicies/updatedOwnerHandler',
        payload: 'name',
      });

      expect(newState.ownerName).toBe('name');
    });
  });
});
