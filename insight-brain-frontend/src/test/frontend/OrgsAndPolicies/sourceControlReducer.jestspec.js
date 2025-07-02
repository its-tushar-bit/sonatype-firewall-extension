/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import reducer from 'MainRoot/OrgsAndPolicies/sourceControlSlice';

describe('sourceControl reducer', () => {
  describe('sourceControl/loadSourceControl/pending', () => {
    it('resets loading, loadError', () => {
      const state = Object.freeze({
        loading: false,
        loadError: 'error',
      });

      const { loading, loadError } = reducer(state, {
        type: 'sourceControl/loadSourceControl/pending',
      });

      expect(loading).toBe(true);
      expect(loadError).toBeNull();
    });
  });

  describe('sourceControl/loadSourceControl/fulfilled', () => {
    it('resets loading, loadError', () => {
      const state = Object.freeze({
        loading: true,
        data: null,
      });

      const { loading, data } = reducer(state, {
        type: 'sourceControl/loadSourceControl/fulfilled',
        payload: 'some data',
      });

      expect(loading).toBe(false);
      expect(data).toBe('some data');
    });
  });

  describe('sourceControl/loadSourceControl/rejected', () => {
    it('sets loading, loadError', () => {
      const state = Object.freeze({
        loading: true,
        loadError: null,
      });

      const { loading, loadError } = reducer(state, {
        type: 'sourceControl/loadSourceControl/rejected',
        payload: 'error',
      });

      expect(loading).toBe(false);
      expect(loadError).toBe('error');
    });
  });
});
