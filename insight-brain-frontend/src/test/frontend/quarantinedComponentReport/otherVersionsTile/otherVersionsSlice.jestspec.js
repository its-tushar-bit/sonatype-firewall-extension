/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

import reducer from 'MainRoot/quarantinedComponentReport/otherVersionsTile/otherVersionsSlice';

describe('otherVersionsSliceSpec', () => {
  const stateConstantObject = { value: 'test value' };

  describe('unknown action', () => {
    it('returns original state', () => {
      const state = Object.freeze({
        loading: false,
        loadError: null,
        otherVersions: [],
        pageCount: 0,
        pageSize: 5,
        currentPage: null,
        sortAsc: true,
      });
      const action = {
        type: 'UNKNOWN',
      };
      const newState = reducer(state, action);
      expect(newState).toBe(state);
    });
  });

  describe('otherVersions/loadOtherVersions/pending action', () => {
    it('sets the loading flag to true', () => {
      const state = Object.freeze({
        other: stateConstantObject,
        loading: false,
      });

      const newState = reducer(state, {
        type: 'otherVersions/loadOtherVersions/pending',
      });

      expect(newState.loading).toBe(true);
      expect(newState.other).toBe(stateConstantObject);
    });
  });

  describe('otherVersions/loadOtherVersions/fulfilled action', () => {
    it('sets loading flag to false, unsets the loadError and fills the data', () => {
      const state = Object.freeze({
        other: stateConstantObject,
        loading: false,
        loadError: 'error',
        otherVersions: [],
        pageCount: 0,
        currentPage: null,
      });
      const otherVersions = ['list'];
      const payload = {
        total: 49,
        page: 1,
        pageSize: 5,
        pageCount: 10,
        results: otherVersions,
      };

      const newState = reducer(state, {
        type: 'otherVersions/loadOtherVersions/fulfilled',
        payload,
      });

      expect(newState.loading).toBe(false);
      expect(newState.loadError).toBe(null);
      expect(newState.otherVersions).toBe(otherVersions);
      expect(newState.pageCount).toBe(10);
      expect(newState.currentPage).toBe(0);
      expect(newState.other).toBe(stateConstantObject);
    });
  });

  describe('otherVersions/loadOtherVersions/rejected action', () => {
    it('sets the loadError to the payload and the loading flag to false', () => {
      const state = Object.freeze({
        other: stateConstantObject,
        loading: true,
        loadError: 'error',
      });

      const newState = reducer(state, {
        type: 'otherVersions/loadOtherVersions/rejected',
        payload: 'loadError',
      });

      expect(newState.loading).toBe(false);
      expect(newState.loadError).toBe('loadError');
      expect(newState.other).toBe(stateConstantObject);
    });
  });

  describe('otherVersions/setCurrentPage', () => {
    it('sets the currentPage to the payload', () => {
      const state = Object.freeze({
        other: stateConstantObject,
        currentPage: 1,
      });

      const newState = reducer(state, {
        type: 'otherVersions/setCurrentPage',
        payload: { currentPage: 2 },
      });

      expect(newState.currentPage).toBe(2);
    });
  });
});
