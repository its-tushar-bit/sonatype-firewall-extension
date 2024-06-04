/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import reducer from 'MainRoot/mainHeader/MenuBar/SolutionSwitcherContainer/solutionSwitcherSlice';

describe('solutionSwitcher reducer', () => {
  describe('solutionSwitcher/fetchLicensedSolutions/pending', () => {
    it('resets loading, loadError', () => {
      const state = Object.freeze({
        loading: false,
        loadError: 'error',
      });

      const { loading, loadError } = reducer(state, {
        type: 'solutionSwitcher/fetchLicensedSolutions/pending',
      });

      expect(loading).toBe(true);
      expect(loadError).toBeNull();
    });
  });

  describe('solutionSwitcher/fetchLicensedSolutions/fulfilled', () => {
    it('resets loading, loadError', () => {
      const state = Object.freeze({
        loading: true,
        licensedSolutions: [],
      });

      const mockFetchResult = [{ id: 'solution', url: 'solutionUrl' }];

      const { loading, licensedSolutions } = reducer(state, {
        type: 'solutionSwitcher/fetchLicensedSolutions/fulfilled',
        payload: mockFetchResult,
      });

      expect(loading).toBe(false);
      expect(licensedSolutions).toBe(mockFetchResult);
    });
  });

  describe('solutionSwitcher/fetchLicensedSolutions/rejected', () => {
    it('sets loading, loadError', () => {
      const state = Object.freeze({
        loading: true,
        loadError: null,
      });

      const { loading, loadError } = reducer(state, {
        type: 'solutionSwitcher/fetchLicensedSolutions/rejected',
        payload: 'error',
      });
      expect(loading).toBe(false);
      expect(loadError).toBe('error');
    });
  });
});
