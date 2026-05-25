/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import reducer from 'MainRoot/waivers/waiverDetails/waiverDetailsSlice';

const LOAD_WAIVER_DETAILS_REQUESTED = 'waiverDetails/loadWaiver/pending';
const LOAD_WAIVER_DETAILS_FULFILLED = 'waiverDetails/loadWaiver/fulfilled';
const LOAD_WAIVER_DETAILS_FAILED = 'waiverDetails/loadWaiver/rejected';

describe('waiverDetailsSlice reducers', function () {
  describe(LOAD_WAIVER_DETAILS_REQUESTED, () => {
    it('sets loading to true and clears error', () => {
      const state = {
        waiverDetails: null,
        loading: false,
        loadError: 'previous error',
      };

      const newState = reducer(state, {
        type: LOAD_WAIVER_DETAILS_REQUESTED,
      });

      expect(newState.loading).toBe(true);
      expect(newState.loadError).toBe(null);
    });
  });

  describe(LOAD_WAIVER_DETAILS_FAILED, () => {
    it('clears loading state and sets a load error', () => {
      const state = {
        waiverDetails: null,
        loading: true,
        loadError: null,
      };

      const newState = reducer(state, {
        type: LOAD_WAIVER_DETAILS_FAILED,
        payload: 'test error',
      });

      expect(newState.loading).toBe(false);
      expect(newState.loadError).toBe('test error');
    });
  });

  describe(LOAD_WAIVER_DETAILS_FULFILLED, () => {
    it('sets loading to false, clears load error, and sets waiverDetails', () => {
      const state = {
        waiverDetails: null,
        loading: true,
        loadError: 'previous error',
      };

      const waiverDetails = { testDetails: 'test details' };

      const newState = reducer(state, {
        type: LOAD_WAIVER_DETAILS_FULFILLED,
        payload: { waiverDetails, hasWaivePermission: true },
      });

      expect(newState.loading).toBe(false);
      expect(newState.loadError).toBe(null);
      expect(newState.waiverDetails).toBe(waiverDetails);
    });

    it('stores waiverDetails with ALL_VERSIONS matcherStrategy', () => {
      const state = {
        waiverDetails: null,
        loading: true,
        loadError: null,
      };

      const waiverDetails = {
        policyWaiverId: 'test-id',
        matcherStrategy: 'ALL_VERSIONS',
        hash: null, // ALL_VERSIONS waivers have null hash in DB
        componentIdentifier: { coordinates: { group: 'com.test', name: 'artifact' } },
        displayName: { parts: [{ field: 'Group', value: 'com.test' }] },
      };

      const newState = reducer(state, {
        type: LOAD_WAIVER_DETAILS_FULFILLED,
        payload: { waiverDetails, hasWaivePermission: true },
      });

      expect(newState.waiverDetails).toBe(waiverDetails);
      expect(newState.waiverDetails.matcherStrategy).toBe('ALL_VERSIONS');
      expect(newState.waiverDetails.hash).toBe(null);
    });

    it('stores waiverDetails with ALL_COMPONENTS matcherStrategy', () => {
      const state = {
        waiverDetails: null,
        loading: true,
        loadError: null,
      };

      const waiverDetails = {
        policyWaiverId: 'test-id',
        matcherStrategy: 'ALL_COMPONENTS',
        hash: null, // ALL_COMPONENTS waivers have null hash in DB
        componentIdentifier: null,
        displayName: null,
      };

      const newState = reducer(state, {
        type: LOAD_WAIVER_DETAILS_FULFILLED,
        payload: { waiverDetails, hasWaivePermission: true },
      });

      expect(newState.waiverDetails).toBe(waiverDetails);
      expect(newState.waiverDetails.matcherStrategy).toBe('ALL_COMPONENTS');
      expect(newState.waiverDetails.hash).toBe(null);
    });

    it('stores waiverDetails with EXACT_COMPONENT matcherStrategy and hash', () => {
      const state = {
        waiverDetails: null,
        loading: true,
        loadError: null,
      };

      const waiverDetails = {
        policyWaiverId: 'test-id',
        matcherStrategy: 'EXACT_COMPONENT',
        hash: 'abc123def456', // EXACT_COMPONENT waivers have hash in DB
        componentIdentifier: { coordinates: { group: 'com.test', name: 'artifact', version: '1.0.0' } },
        displayName: {
          parts: [
            { field: 'Group', value: 'com.test' },
            { value: ':' },
            { field: 'Artifact', value: 'artifact' },
            { value: ':' },
            { field: 'Version', value: '1.0.0' },
          ],
        },
      };

      const newState = reducer(state, {
        type: LOAD_WAIVER_DETAILS_FULFILLED,
        payload: { waiverDetails, hasWaivePermission: true },
      });

      expect(newState.waiverDetails).toBe(waiverDetails);
      expect(newState.waiverDetails.matcherStrategy).toBe('EXACT_COMPONENT');
      expect(newState.waiverDetails.hash).toBe('abc123def456');
    });

    it('stores waiverDetails when matcherStrategy is null (legacy/default case)', () => {
      const state = {
        waiverDetails: null,
        loading: true,
        loadError: null,
      };

      const waiverDetails = {
        policyWaiverId: 'test-id',
        matcherStrategy: null, // null matcherStrategy (legacy waiver)
        hash: null,
        componentIdentifier: { coordinates: { group: 'com.test', name: 'artifact' } },
      };

      const newState = reducer(state, {
        type: LOAD_WAIVER_DETAILS_FULFILLED,
        payload: { waiverDetails, hasWaivePermission: true },
      });

      expect(newState.waiverDetails).toBe(waiverDetails);
      expect(newState.waiverDetails.matcherStrategy).toBe(null);
    });

    it('preserves hash field even when not used for display in WaiverDetails', () => {
      const state = {
        waiverDetails: null,
        loading: true,
        loadError: null,
      };

      // This test verifies that hash is stored in state even though
      // WaiverDetails.jsx does not use it for display purposes.
      // Hash IS returned in the API response (ApiPolicyWaiverDTO)
      // and IS used by other components (e.g., waiver list pages).
      const waiverDetails = {
        policyWaiverId: 'test-id',
        matcherStrategy: 'EXACT_COMPONENT',
        hash: 'secure-hash-value-123',
        componentIdentifier: { coordinates: { version: '2.5.0' } },
      };

      const newState = reducer(state, {
        type: LOAD_WAIVER_DETAILS_FULFILLED,
        payload: { waiverDetails, hasWaivePermission: true },
      });

      expect(newState.waiverDetails.hash).toBe('secure-hash-value-123');
    });
  });
});
