/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import reducer, { initialState } from 'MainRoot/OrgsAndPolicies/ownerSummarySlice';

describe('ownerSummarySlice', () => {
  describe('initialState', () => {
    it('has hasViewIqPermission as false', () => {
      expect(initialState.hasViewIqPermission).toBe(false);
    });
  });

  describe('checkViewIqPermission', () => {
    it('sets hasViewIqPermission to true when fulfilled', () => {
      const state = reducer(initialState, { type: 'ownerSummary/checkViewIqPermission/fulfilled' });
      expect(state.hasViewIqPermission).toBe(true);
    });

    it('sets hasViewIqPermission to false when rejected', () => {
      const stateWithTrue = { ...initialState, hasViewIqPermission: true };
      const state = reducer(stateWithTrue, { type: 'ownerSummary/checkViewIqPermission/rejected' });
      expect(state.hasViewIqPermission).toBe(false);
    });
  });
});
