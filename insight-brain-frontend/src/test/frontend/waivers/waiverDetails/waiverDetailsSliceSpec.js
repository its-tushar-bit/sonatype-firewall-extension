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
        payload: waiverDetails,
      });

      expect(newState.loading).toBe(false);
      expect(newState.loadError).toBe(null);
      expect(newState.waiverDetails).toBe(waiverDetails);
    });
  });
});
