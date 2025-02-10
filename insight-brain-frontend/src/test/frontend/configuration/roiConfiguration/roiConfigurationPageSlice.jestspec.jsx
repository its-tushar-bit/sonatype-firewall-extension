/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import reducer, { defaultConfiguration } from 'MainRoot/configuration/roiConfiguration/roiConfigurationPageSlice';

describe('roiConfigurationPageSlice', () => {
  describe('roiConfigurationPage/loadConfiguration', () => {
    it('/pending', () => {
      const state = Object.freeze({
        loading: true,
        error: null,
        configuration: { ...defaultConfiguration },
      });

      const newState = reducer(state, {
        type: 'roiConfigurationPage/loadConfiguration/pending',
      });

      expect(newState.loading).toBe(true);
      expect(newState.error).toBe(null);
    });

    it('/rejected', () => {
      const state = Object.freeze({
        loading: true,
        error: null,
        configuration: { ...defaultConfiguration },
      });

      const newState = reducer(state, {
        type: 'roiConfigurationPage/loadConfiguration/rejected',
        payload: 'Something went wrong.',
      });

      expect(newState.loading).toBe(false);
      expect(newState.error).toBe('Something went wrong.');
    });

    it('/fulfilled', () => {
      const state = Object.freeze({
        loading: true,
        error: null,
        configuration: { ...defaultConfiguration },
      });

      const newState = reducer(state, {
        type: 'roiConfigurationPage/loadConfiguration/fulfilled',
        payload: {},
      });

      expect(newState.loading).toBe(false);
      expect(newState.error).toBe(null);
      // TODO: test mapping of payload to configuration state.
    });
  });
});
