/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import reducer from 'MainRoot/productFeatures/productFeaturesSlice';

describe('productFeatures slice', () => {
  describe('productFeatures/fetchProductFeaturesIfNeeded/fulfilled', () => {
    it('sets payload to state', () => {
      const state = Object.freeze({
        productFeatures: {
          automation: true,
        },
      });

      const newState = reducer(state, {
        type: 'productFeatures/fetchProductFeaturesIfNeeded/fulfilled',
        payload: {
          enforcement: true,
          firewall: true,
        },
      });
      expect(newState.productFeatures.enforcement).toBe(true);
      expect(newState.productFeatures.firewall).toBe(true);
      expect(newState.productFeatures.automation).toBe(true);
    });
  });
});
