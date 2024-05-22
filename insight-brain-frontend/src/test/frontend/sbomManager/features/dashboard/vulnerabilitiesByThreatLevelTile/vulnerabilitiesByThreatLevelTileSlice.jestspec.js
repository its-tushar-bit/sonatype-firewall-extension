/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import reducer, {
  initialState,
} from 'MainRoot/sbomManager/features/dashboard/vulnerabilitiesByThreatLevelTile/vulnerabilitiesByThreatLevelTileSlice';
import { UI_ROUTER_ON_FINISH } from 'MainRoot/reduxUiRouter/routerActions';

describe('vulnerabilitiesByThreatLevelTile reducers have the correct state when the following reducer is dispatched', function () {
  const vulnerabilitesInitialState = Object.freeze({
    critical: {
      annotated: null,
      unannotated: null,
      total: null,
    },
    high: {
      annotated: null,
      unannotated: null,
      total: null,
    },
    medium: {
      annotated: null,
      unannotated: null,
      total: null,
    },
    low: {
      annotated: null,
      unannotated: null,
      total: null,
    },
  });

  const vulnerabiltiesTotalInitialState = Object.freeze({
    totalVulnerabilities: null,
    totalVulnerabilitiesAnnotated: null,
    totalVulnerabilitiesUnannotated: null,
  });

  const responsePayload = Object.freeze({
    low: 3003,
    lowAnnotated: 1001,
    lowUnannotated: 2002,
    medium: 7003,
    mediumAnnotated: 3001,
    mediumUnannotated: 4002,
    high: 11003,
    highAnnotated: 5001,
    highUnannotated: 6002,
    critical: 15003,
    criticalAnnotated: 7001,
    criticalUnannotated: 8002,
    totalVulnerabilities: 36012,
    totalVulnerabilitiesAnnotated: 20008,
    totalVulnerabilitiesUnannotated: 16004,
  });

  describe('vulnerabilitiesByThreatLevelTile/loadVulnerabilitesByThreatLevel', function () {
    it('/pending', () => {
      const state = { ...vulnerabilitesInitialState };

      const newState = reducer(state, {
        type: 'vulnerabilitiesByThreatLevelTile/loadVulnerabilitesByThreatLevel/pending',
      });

      expect(newState.loading).toBe(true);
      expect(newState.loadError).toBe(null);
      expect(newState.vulnerabilities).toEqual(vulnerabilitesInitialState);
      expect(newState.vulnerabilitiesTotal).toEqual(vulnerabiltiesTotalInitialState);
    });

    it('/failed', () => {
      const state = { ...vulnerabilitesInitialState };

      const payload = {
        response: {
          data: 'payload error',
        },
      };

      const newState = reducer(state, {
        type: 'vulnerabilitiesByThreatLevelTile/loadVulnerabilitesByThreatLevel/rejected',
        payload: payload,
      });

      expect(newState.loading).toBe(false);
      expect(newState.loadError).toEqual({ response: { data: 'payload error' } });
      expect(newState.vulnerabilities).toEqual(vulnerabilitesInitialState);
      expect(newState.vulnerabilitiesTotal).toEqual(vulnerabiltiesTotalInitialState);
    });

    it('/fulfilled', () => {
      const state = { ...vulnerabilitesInitialState };

      const newState = reducer(state, {
        type: 'vulnerabilitiesByThreatLevelTile/loadVulnerabilitesByThreatLevel/fulfilled',
        payload: { ...responsePayload },
      });

      expect(newState.loading).toBe(false);
      expect(newState.loadError).toBe(null);
      expect(newState.vulnerabilities).toEqual({
        critical: {
          annotated: 7001,
          unannotated: 8002,
          total: 15003,
        },
        high: {
          annotated: 5001,
          unannotated: 6002,
          total: 11003,
        },
        medium: {
          annotated: 3001,
          unannotated: 4002,
          total: 7003,
        },
        low: {
          annotated: 1001,
          unannotated: 2002,
          total: 3003,
        },
      });
      expect(newState.vulnerabilitiesTotal).toEqual({
        totalVulnerabilities: 36012,
        totalVulnerabilitiesAnnotated: 20008,
        totalVulnerabilitiesUnannotated: 16004,
      });
    });
  });

  describe('UI_ROUTER_ON_FINISH', () => {
    it('clears state on onFinish', () => {
      const state = Object.freeze({
        loading: false,
        loadError: null,
        vulnerabilities: {
          critical: {
            annotated: 7001,
            unannotated: 8002,
            total: 15003,
          },
          high: {
            annotated: 5001,
            unannotated: 6002,
            total: 11003,
          },
          medium: {
            annotated: 3001,
            unannotated: 4002,
            total: 7003,
          },
          low: {
            annotated: 1001,
            unannotated: 2002,
            total: 3003,
          },
        },
        vulnerabilitiesTotal: {
          totalVulnerabilities: 36012,
          totalVulnerabilitiesAnnotated: 20008,
          totalVulnerabilitiesUnannotated: 16004,
        },
      });

      const newState = reducer(state, { type: UI_ROUTER_ON_FINISH });
      expect(newState).toEqual(initialState);
    });
  });
});
