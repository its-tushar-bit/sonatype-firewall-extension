/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import reducer from 'MainRoot/OrgsAndPolicies/ownerSummarySlice';

describe('ownerSummarySlice reducers', () => {
  describe('setLoading', () => {
    it('sets loading', () => {
      const state = Object.freeze({
        loading: false,
      });

      const { loading } = reducer(state, {
        type: 'ownerSummary/setLoading',
        payload: true,
      });

      expect(loading).toBe(true);
    });
  });

  describe('setLoadError', () => {
    it('sets loadError', () => {
      const state = Object.freeze({
        loadError: null,
      });

      const { loadError } = reducer(state, {
        type: 'ownerSummary/setLoadError',
        payload: 'error',
      });

      expect(loadError).toBe('error');
    });
  });

  describe('ownerSummary/checkEditIqPermission/fulfilled', () => {
    it('sets hasEditIqPermission', () => {
      const state = Object.freeze({ hasEditIqPermission: null });

      const { hasEditIqPermission } = reducer(state, {
        type: 'ownerSummary/checkEditIqPermission/fulfilled',
      });

      expect(hasEditIqPermission).toBe(true);
    });
  });

  describe('ownerSummary/checkEditIqPermission/rejected', () => {
    it('clears hasEditIqPermission', () => {
      const state = Object.freeze({ hasEditIqPermission: null });

      const { hasEditIqPermission } = reducer(state, {
        type: 'ownerSummary/checkEditIqPermission/rejected',
      });

      expect(hasEditIqPermission).toBe(false);
    });
  });
});
