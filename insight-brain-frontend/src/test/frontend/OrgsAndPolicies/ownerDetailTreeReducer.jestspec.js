/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import reducer from 'MainRoot/OrgsAndPolicies/ownerDetailTreeSlice';

describe('ownerDetailTreeSlice reducers', () => {
  describe('setLoading', () => {
    it('sets loading', () => {
      const state = Object.freeze({
        loading: false,
      });

      const { loading } = reducer(state, {
        type: 'ownerDetailTree/setLoading',
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
        type: 'ownerDetailTree/setLoadError',
        payload: 'error',
      });

      expect(loadError).toBe('error');
    });
  });

  describe('loadOwnerDetails', () => {
    it('ownerDetailTree/loadOwnerDetails/pending', () => {
      const state = Object.freeze({
        loadError: 'error',
        loading: false,
      });

      const { loadError, loading } = reducer(state, {
        type: 'ownerDetailTree/loadOwnerDetails/pending',
      });

      expect(loading).toBe(true);
      expect(loadError).toBeNull();
    });

    it('ownerDetailTree/loadOwnerDetails/fulfilled', () => {
      const state = Object.freeze({
        ownerDetails: {},
        loading: true,
      });

      const { ownerDetails, loading } = reducer(state, {
        type: 'ownerDetailTree/loadOwnerDetails/fulfilled',
        payload: 'some payload',
      });

      expect(loading).toBe(false);
      expect(ownerDetails).toBe('some payload');
    });

    it('ownerDetailTree/loadOwnerDetails/rejected', () => {
      const state = Object.freeze({
        loadError: null,
        loading: true,
      });

      const { loadError, loading } = reducer(state, {
        type: 'ownerDetailTree/loadOwnerDetails/rejected',
        payload: 'error',
      });

      expect(loading).toBe(false);
      expect(loadError).toBe('error');
    });
  });
});
