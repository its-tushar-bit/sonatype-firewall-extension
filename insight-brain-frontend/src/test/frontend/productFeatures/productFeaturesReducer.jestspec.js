/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import reducer, { actions } from 'MainRoot/productFeatures/productFeaturesSlice';

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

  // CLM-39601: Enterprise preview mode actions
  describe('setEnterprisePreviewMode', () => {
    it('sets isEnterprisePreviewMode to true', () => {
      const state = Object.freeze({
        loading: false,
        loadError: null,
        productFeatures: {},
        isEnterprisePreviewMode: false,
      });

      const newState = reducer(state, actions.setEnterprisePreviewMode(true));
      expect(newState.isEnterprisePreviewMode).toBe(true);
    });

    it('sets isEnterprisePreviewMode to false', () => {
      const state = Object.freeze({
        loading: false,
        loadError: null,
        productFeatures: {},
        isEnterprisePreviewMode: true,
      });

      const newState = reducer(state, actions.setEnterprisePreviewMode(false));
      expect(newState.isEnterprisePreviewMode).toBe(false);
    });
  });

  describe('dismissPopover', () => {
    it('sets dismissedPopovers[featureId] to true', () => {
      const state = {
        loading: false,
        loadError: null,
        productFeatures: {},
        isEnterprisePreviewMode: false,
        dismissedPopovers: {},
      };

      const newState = reducer(state, actions.dismissPopover('constraints'));
      expect(newState.dismissedPopovers.constraints).toBe(true);
    });

    it('preserves existing dismissals when a new featureId is dismissed', () => {
      const state = {
        loading: false,
        loadError: null,
        productFeatures: {},
        isEnterprisePreviewMode: false,
        dismissedPopovers: { constraints: true },
      };

      const newState = reducer(state, actions.dismissPopover('labels'));
      expect(newState.dismissedPopovers.constraints).toBe(true);
      expect(newState.dismissedPopovers.labels).toBe(true);
    });
  });
});
