/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import reducer from 'MainRoot/sbomManager/features/dashboard/highPriorityVulnerabilitiesTile/highPriorityVulnerabilitiesTileSlice';
import { UI_ROUTER_ON_FINISH } from 'MainRoot/reduxUiRouter/routerActions';

describe('HighPriorityVulnerabilitiesTile', () => {
  const initialState = Object.freeze({
    loading: true,
    loadError: null,
    vulnerabilities: null,
  });

  const response = Object.freeze([
    {
      refId: 'CVE-1234-12345',
      severity: 10,
      severityStatus: 'critical',
      createdAt: '2024-04-29T00:00:00.000+0000',
    },
  ]);

  describe('highPriorityVulnerabilitiesTile/loadHighPriorityVulnerabilities', function () {
    it('/pending', () => {
      const newState = reducer(initialState, {
        type: 'highPriorityVulnerabilitiesTile/loadHighPriorityVulnerabilities/pending',
      });

      expect(newState.loading).toBe(true);
      expect(newState.loadError).toBe(null);
      expect(newState.vulnerabilities).toBe(null);
    });

    it('/failed', () => {
      const payload = {
        response: {
          data: 'payload-error',
        },
      };

      const newState = reducer(initialState, {
        type: 'highPriorityVulnerabilitiesTile/loadHighPriorityVulnerabilities/rejected',
        payload: payload,
      });

      expect(newState.loading).toBe(false);
      expect(newState.loadError).toEqual({ response: { data: 'payload-error' } });
      expect(newState.vulnerabilities).toBe(null);
    });

    it('/fulfilled', () => {
      const newState = reducer(initialState, {
        type: 'highPriorityVulnerabilitiesTile/loadHighPriorityVulnerabilities/fulfilled',
        payload: response,
      });

      expect(newState.loading).toBe(false);
      expect(newState.loadError).toBe(null);
      expect(newState.vulnerabilities).toEqual(response);
    });
  });

  describe('UI_ROUTER_ON_FINISH', () => {
    it('clears state on onFinish', () => {
      const state = Object.freeze({
        loading: false,
        loadError: { response: { data: 'payload-error' } },
        vulnerabilities: response,
      });

      const newState = reducer(state, { type: UI_ROUTER_ON_FINISH });
      expect(newState).toEqual(initialState);
    });
  });
});
