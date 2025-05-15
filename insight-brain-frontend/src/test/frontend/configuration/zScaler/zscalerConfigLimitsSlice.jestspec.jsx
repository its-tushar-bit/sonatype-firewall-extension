/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import reducer from 'MainRoot/configuration/zscaler/zscalerConfigLimitsSlice';

describe('zscalerConfigLimitsSlice', () => {
  describe('zscalerConfigLimits/load', () => {
    it('pending', () => {
      const state = Object.freeze({
        loading: false,
      });

      const newState = reducer(state, {
        type: 'zscalerConfigLimits/load/pending',
      });

      expect(newState.loading).toBe(true);
    });

    it('rejected', () => {
      const state = Object.freeze({
        loading: true,
        error: null,
      });

      const newState = reducer(state, {
        type: 'zscalerConfigLimits/load/rejected',
        payload: 'Some error message',
      });

      expect(newState.loading).toBe(false);
      expect(newState.error).toBe('Some error message');
    });

    it('fulfilled', () => {
      const payload = {
        totalAllowedUrls: 100,
        remainingUrls: 50,
        status: 'under',
      };
      const state = Object.freeze({
        loading: true,
        limits: null,
      });

      const newState = reducer(state, {
        type: 'zscalerConfigLimits/load/fulfilled',
        payload: payload,
      });

      expect(newState.loading).toBe(false);
      expect(newState.limits).toEqual(payload);
    });
  });
});
