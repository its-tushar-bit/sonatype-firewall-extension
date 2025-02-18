/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import reducer from 'MainRoot/firewall/roiMetrics/roiFirewallMetricsSlice';

describe('roiFirewallMetricsSlice reducers have the correct state when the following reducer is dispatched', function () {
  const initState = Object.freeze({
    loading: true,
    error: null,
    hasConfigureSystemPermission: false,
    total: 0,
    supplyChainAttacksBlocked: 0,
    namespaceAttacksBlocked: 0,
    safeVersionsSelected: 0,
  });

  describe('roiFirewallMetrics/loadMetrics', function () {
    it('/pending', () => {
      const state = initState;

      const newState = reducer(state, {
        type: 'roiFirwallMetrics/loadMetrics/pending',
      });

      expect(newState.loading).toBe(true);
      expect(newState.error).toBe(null);
      expect(newState.hasConfigureSystemPermission).toBe(false);
      expect(newState.total).toBe(0);
      expect(newState.supplyChainAttacksBlocked).toBe(0);
      expect(newState.namespaceAttacksBlocked).toBe(0);
      expect(newState.safeVersionsSelected).toBe(0);
    });

    it('/failed', () => {
      const state = initState;

      const payload = {
        response: {
          data: 'payload error',
        },
      };

      const newState = reducer(state, {
        type: 'roiFirewallMetrics/loadMetrics/rejected',
        payload: payload,
      });

      expect(newState.error).toEqual('payload error');
      expect(newState.loading).toBe(false);
      expect(newState.hasConfigureSystemPermission).toBe(false);
      expect(newState.total).toBe(0);
      expect(newState.supplyChainAttacksBlocked).toBe(0);
      expect(newState.namespaceAttacksBlocked).toBe(0);
      expect(newState.safeVersionsSelected).toBe(0);
    });

    it('/fulfilled', () => {
      const state = initState;

      const newState = reducer(state, {
        type: 'roiFirewallMetrics/loadMetrics/fulfilled',
        payload: {
          hasConfigureSystemPermission: true,
          total: 6000,
          supplyChainAttacksBlocked: 1000,
          namespaceAttacksBlocked: 2000,
          safeVersionsSelected: 3000,
        },
      });

      expect(newState.loading).toBe(false);
      expect(newState.error).toBe(null);
      expect(newState.hasConfigureSystemPermission).toBe(true);
      expect(newState.total).toBe(6000);
      expect(newState.supplyChainAttacksBlocked).toBe(1000);
      expect(newState.namespaceAttacksBlocked).toBe(2000);
      expect(newState.safeVersionsSelected).toBe(3000);
    });
  });
});
