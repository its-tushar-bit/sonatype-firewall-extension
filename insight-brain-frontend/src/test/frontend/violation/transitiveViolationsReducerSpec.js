/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

import reduce from '../../../main/frontend/violation/transitiveViolationsReducer.js';
import {
  TRANSITIVE_VIOLATION_WAIVERS_LOAD_FAILED,
  TRANSITIVE_VIOLATION_WAIVERS_LOAD_FULFILLED,
  TRANSITIVE_VIOLATION_WAIVERS_LOAD_REQUESTED,
  TRANSITIVE_VIOLATIONS_LOAD_AVAILABLE_SCOPES_FAILED,
  TRANSITIVE_VIOLATIONS_LOAD_AVAILABLE_SCOPES_FULFILLED,
  TRANSITIVE_VIOLATIONS_LOAD_AVAILABLE_SCOPES_REQUESTED,
  TRANSITIVE_VIOLATIONS_LOAD_FAILED,
  TRANSITIVE_VIOLATIONS_LOAD_FULFILLED,
  TRANSITIVE_VIOLATIONS_LOAD_REPORT_METADATA_FAILED,
  TRANSITIVE_VIOLATIONS_LOAD_REPORT_METADATA_FULFILLED,
  TRANSITIVE_VIOLATIONS_LOAD_REPORT_METADATA_REQUESTED,
  TRANSITIVE_VIOLATIONS_LOAD_REQUESTED,
  TRANSITIVE_VIOLATIONS_SET_FILTERING_PARAMETERS,
  TRANSITIVE_VIOLATIONS_SET_SORTING_PARAMETERS,
  TRANSITIVE_VIOLATIONS_TOGGLE_REQUEST_WAIVE,
  TRANSITIVE_VIOLATIONS_TOGGLE_VIEW_WAIVERS,
  TRANSITIVE_VIOLATIONS_TOGGLE_WAIVE,
} from '../../../main/frontend/violation/transitiveViolationsActions';
import { Messages } from '../../../main/frontend/util/CommonServices';

describe('transitiveViolationsReducer', function () {
  describe('initial state', function () {
    it('is used if no state is provided', function () {
      const state = undefined;
      const action = { type: 'UNKNOWN' };
      const newState = reduce(state, action);
      expect(newState).toEqual({
        availableScopes: {
          loading: false,
          error: null,
          data: null,
        },
        reportMetadata: {
          loading: false,
          error: null,
          data: null,
        },
        componentTransitivePolicyViolations: {
          loading: false,
          error: null,
          sortConfiguration: {
            key: 'threatLevel',
            dir: 'desc',
          },
          filterConfiguration: {
            policyName: '',
            displayName: '',
          },
          data: null,
          threatCounts: null,
          threatCountsTotal: null,
          componentCount: null,
        },
        transitiveViolationWaivers: {
          loading: false,
          error: null,
          data: { componentPolicyWaivers: [] },
        },
        isRequestWaiveTransitiveViolationsOpen: false,
        isWaiveTransitiveViolationsOpen: false,
        isViewTransitiveViolationWaiversOpen: false,
      });
    });
  });

  describe('unknown action', function () {
    it('returns original state', function () {
      const state = { foo: 'bar' };
      const action = { type: 'UNKNOWN' };
      const newState = reduce(state, action);
      expect(newState).toBe(state);
    });
  });

  describe('TRANSITIVE_VIOLATIONS_LOAD_AVAILABLE_SCOPES_REQUESTED action', function () {
    it('sets in availableScopes loading to true and error to null', function () {
      const state = {};
      const action = { type: TRANSITIVE_VIOLATIONS_LOAD_AVAILABLE_SCOPES_REQUESTED };
      const newState = reduce(state, action);

      const { availableScopes } = newState;
      expect(availableScopes).toEqual({
        loading: true,
        error: null,
        data: null,
      });
    });
  });

  describe('TRANSITIVE_VIOLATIONS_LOAD_AVAILABLE_SCOPES_FULFILLED action', function () {
    it('sets availableScopes loading to false, error to null, and merges the payload with it', function () {
      const payload = { payload: 'payload' };
      const state = {};
      const action = {
        type: TRANSITIVE_VIOLATIONS_LOAD_AVAILABLE_SCOPES_FULFILLED,
        payload: payload,
      };
      const newState = reduce(state, action);

      const { availableScopes } = newState;
      expect(availableScopes).toEqual({
        loading: false,
        error: null,
        data: payload,
      });
    });
  });

  describe('TRANSITIVE_VIOLATIONS_LOAD_AVAILABLE_SCOPES_FAILED action', function () {
    it('sets availableScopes loading to false and the error message', function () {
      const error = { status: '500', data: 'internal server error' };
      const state = {};
      const action = {
        type: TRANSITIVE_VIOLATIONS_LOAD_AVAILABLE_SCOPES_FAILED,
        payload: error,
      };
      const newState = reduce(state, action);

      const { availableScopes } = newState;
      expect(availableScopes).toEqual({
        loading: false,
        error: Messages.getHttpErrorMessage(error),
      });
    });
  });

  describe('TRANSITIVE_VIOLATIONS_LOAD_REPORT_METADATA_REQUESTED action', function () {
    it('sets in reportMetadata loading to true and error to null', function () {
      const state = {};
      const action = { type: TRANSITIVE_VIOLATIONS_LOAD_REPORT_METADATA_REQUESTED };
      const newState = reduce(state, action);

      const { reportMetadata } = newState;
      expect(reportMetadata).toEqual({
        loading: true,
        error: null,
        data: null,
      });
    });
  });

  describe('TRANSITIVE_VIOLATIONS_LOAD_REPORT_METADATA_FULFILLED action', function () {
    it('sets reportMetadata loading to false, error to null, and merges the payload with it', function () {
      const payload = { payload: 'payload' };
      const state = {};
      const action = {
        type: TRANSITIVE_VIOLATIONS_LOAD_REPORT_METADATA_FULFILLED,
        payload: payload,
      };
      const newState = reduce(state, action);

      const { reportMetadata } = newState;
      expect(reportMetadata).toEqual({
        loading: false,
        error: null,
        data: payload,
      });
    });
  });

  describe('TRANSITIVE_VIOLATIONS_LOAD_REPORT_METADATA_FAILED action', function () {
    it('sets reportMetadata loading to false and the error message', function () {
      const error = { status: '500', data: 'internal server error' };
      const state = {};
      const action = {
        type: TRANSITIVE_VIOLATIONS_LOAD_REPORT_METADATA_FAILED,
        payload: error,
      };
      const newState = reduce(state, action);

      const { reportMetadata } = newState;
      expect(reportMetadata).toEqual({
        loading: false,
        error: Messages.getHttpErrorMessage(error),
      });
    });
  });

  describe('TRANSITIVE_VIOLATIONS_LOAD_REQUESTED action', function () {
    it('sets in componentTransitivePolicyViolations loading to true and error to null', function () {
      const state = {
        componentTransitivePolicyViolations: {
          sortConfiguration: 'someSortConfiguration',
          filterConfiguration: 'someFilterConfiguration',
        },
      };
      const action = { type: TRANSITIVE_VIOLATIONS_LOAD_REQUESTED };
      const newState = reduce(state, action);

      const { componentTransitivePolicyViolations } = newState;
      expect(componentTransitivePolicyViolations).toEqual({
        loading: true,
        error: null,
        sortConfiguration: 'someSortConfiguration',
        filterConfiguration: 'someFilterConfiguration',
        data: null,
        threatCounts: null,
        threatCountsTotal: null,
        componentCount: null,
      });
    });
  });

  describe('TRANSITIVE_VIOLATIONS_LOAD_FULFILLED action', function () {
    it(
      'sets componentTransitivePolicyViolations loading to false, error to null,' +
        ' merges the payload with it' +
        ' and sets the correct counts with no violations',
      function () {
        const payload = { transitivePolicyViolations: [], other: 'other' };
        const state = {
          componentTransitivePolicyViolations: {
            sortConfiguration: 'someSortConfiguration',
            filterConfiguration: 'someFilterConfiguration',
          },
        };
        const action = {
          type: TRANSITIVE_VIOLATIONS_LOAD_FULFILLED,
          payload: payload,
        };
        const newState = reduce(state, action);

        const { componentTransitivePolicyViolations } = newState;
        expect(componentTransitivePolicyViolations).toEqual({
          loading: false,
          error: null,
          sortConfiguration: 'someSortConfiguration',
          filterConfiguration: 'someFilterConfiguration',
          data: {
            violations: payload.transitivePolicyViolations,
            displayedViolations: payload.transitivePolicyViolations,
            other: 'other',
          },
          threatCounts: Object({ critical: 0, severe: 0, moderate: 0, low: 0, none: 0 }),
          threatCountsTotal: 0,
          componentCount: 0,
        });
      }
    );

    it('sets the correct counts with multiple threat threats and components', function () {
      const payload = {
        transitivePolicyViolations: [
          { threatLevel: 10, hash: 'a' },
          { threatLevel: 9, hash: 'b' },
          { threatLevel: 8, hash: 'b' },
          { threatLevel: 7, hash: 'b' },
          { threatLevel: 6, hash: 'c' },
          { threatLevel: 5, hash: 'c' },
          { threatLevel: 4, hash: 'c' },
          { threatLevel: 3, hash: 'c' },
          { threatLevel: 2, hash: 'c' },
          { threatLevel: 1, hash: 'c' },
          { threatLevel: 0, hash: 'd' },
        ],
        other: 'other',
      };
      const state = {
        componentTransitivePolicyViolations: {
          sortConfiguration: 'someSortConfiguration',
          filterConfiguration: 'someFilterConfiguration',
        },
      };
      const action = {
        type: TRANSITIVE_VIOLATIONS_LOAD_FULFILLED,
        payload: payload,
      };
      const newState = reduce(state, action);

      const { componentTransitivePolicyViolations } = newState;
      expect(componentTransitivePolicyViolations).toEqual({
        loading: false,
        error: null,
        sortConfiguration: 'someSortConfiguration',
        filterConfiguration: 'someFilterConfiguration',
        data: {
          violations: payload.transitivePolicyViolations,
          displayedViolations: payload.transitivePolicyViolations,
          other: 'other',
        },
        threatCounts: Object({ critical: 3, severe: 4, moderate: 2, low: 1, none: 1 }),
        threatCountsTotal: 11,
        componentCount: 4,
      });
    });
  });

  describe('TRANSITIVE_VIOLATIONS_LOAD_FAILED action', function () {
    it('sets availableScopes loading to false and the error message', function () {
      const error = { status: '500', data: 'internal server error' };
      const state = {
        componentTransitivePolicyViolations: {
          sortConfiguration: 'someSortConfiguration',
          filterConfiguration: 'someFilterConfiguration',
        },
      };
      const action = {
        type: TRANSITIVE_VIOLATIONS_LOAD_FAILED,
        payload: error,
      };
      const newState = reduce(state, action);

      const { componentTransitivePolicyViolations } = newState;
      expect(componentTransitivePolicyViolations).toEqual({
        loading: false,
        error: Messages.getHttpErrorMessage(error),
        sortConfiguration: 'someSortConfiguration',
        filterConfiguration: 'someFilterConfiguration',
      });
    });
  });

  describe('TRANSITIVE_VIOLATIONS_SET_SORTING_PARAMETERS action', function () {
    it('sorts by threat level descending given other key and payload of threatLevel', function () {
      const payload = 'threatLevel';
      const state = {
        componentTransitivePolicyViolations: {
          sortConfiguration: {
            key: 'policyName',
            dir: 'asc',
          },
          filterConfiguration: {
            policyName: '',
            displayName: '',
          },
          data: {
            violations: [
              { threatLevel: 5, policyName: '', displayName: '' },
              { threatLevel: 0, policyName: '', displayName: '' },
              { threatLevel: 10, policyName: '', displayName: '' },
            ],
          },
        },
      };
      const action = {
        type: TRANSITIVE_VIOLATIONS_SET_SORTING_PARAMETERS,
        payload: payload,
      };
      const newState = reduce(state, action);

      expect(newState).toEqual({
        componentTransitivePolicyViolations: {
          ...state.componentTransitivePolicyViolations,
          sortConfiguration: {
            key: 'threatLevel',
            dir: 'desc',
          },
          data: {
            ...state.componentTransitivePolicyViolations.data,
            displayedViolations: [
              { threatLevel: 10, policyName: '', displayName: '' },
              { threatLevel: 5, policyName: '', displayName: '' },
              { threatLevel: 0, policyName: '', displayName: '' },
            ],
          },
        },
      });
    });

    it('sorts by threat level ascending given threatLevel key descending and payload of threatLevel', function () {
      const payload = 'threatLevel';
      const state = {
        componentTransitivePolicyViolations: {
          sortConfiguration: {
            key: 'threatLevel',
            dir: 'desc',
          },
          filterConfiguration: {
            policyName: '',
            displayName: '',
          },
          data: {
            violations: [
              { threatLevel: 5, policyName: '', displayName: '' },
              { threatLevel: 0, policyName: '', displayName: '' },
              { threatLevel: 10, policyName: '', displayName: '' },
            ],
          },
        },
      };
      const action = {
        type: TRANSITIVE_VIOLATIONS_SET_SORTING_PARAMETERS,
        payload: payload,
      };
      const newState = reduce(state, action);

      expect(newState).toEqual({
        componentTransitivePolicyViolations: {
          ...state.componentTransitivePolicyViolations,
          sortConfiguration: {
            key: 'threatLevel',
            dir: 'asc',
          },
          data: {
            ...state.componentTransitivePolicyViolations.data,
            displayedViolations: [
              { threatLevel: 0, policyName: '', displayName: '' },
              { threatLevel: 5, policyName: '', displayName: '' },
              { threatLevel: 10, policyName: '', displayName: '' },
            ],
          },
        },
      });
    });

    it('sorts by threat level descending given threatLevel key ascending and payload of threatLevel', function () {
      const payload = 'threatLevel';
      const state = {
        componentTransitivePolicyViolations: {
          sortConfiguration: {
            key: 'threatLevel',
            dir: 'asc',
          },
          filterConfiguration: {
            policyName: '',
            displayName: '',
          },
          data: {
            violations: [
              { threatLevel: 5, policyName: '', displayName: '' },
              { threatLevel: 0, policyName: '', displayName: '' },
              { threatLevel: 10, policyName: '', displayName: '' },
            ],
          },
        },
      };
      const action = {
        type: TRANSITIVE_VIOLATIONS_SET_SORTING_PARAMETERS,
        payload: payload,
      };
      const newState = reduce(state, action);

      expect(newState).toEqual({
        componentTransitivePolicyViolations: {
          ...state.componentTransitivePolicyViolations,
          sortConfiguration: {
            key: 'threatLevel',
            dir: 'desc',
          },
          data: {
            ...state.componentTransitivePolicyViolations.data,
            displayedViolations: [
              { threatLevel: 10, policyName: '', displayName: '' },
              { threatLevel: 5, policyName: '', displayName: '' },
              { threatLevel: 0, policyName: '', displayName: '' },
            ],
          },
        },
      });
    });

    it('sorts by policy name descending given other key and payload of policyName', function () {
      const payload = 'policyName';
      const state = {
        componentTransitivePolicyViolations: {
          sortConfiguration: {
            key: 'threatLevel',
            dir: 'asc',
          },
          filterConfiguration: {
            policyName: '',
            displayName: '',
          },
          data: {
            violations: [
              { threatLevel: 0, policyName: 'a', displayName: '' },
              { threatLevel: 0, policyName: 'Z', displayName: '' },
              { threatLevel: 0, policyName: 'b', displayName: '' },
            ],
          },
        },
      };
      const action = {
        type: TRANSITIVE_VIOLATIONS_SET_SORTING_PARAMETERS,
        payload: payload,
      };
      const newState = reduce(state, action);

      expect(newState).toEqual({
        componentTransitivePolicyViolations: {
          ...state.componentTransitivePolicyViolations,
          sortConfiguration: {
            key: 'policyName',
            dir: 'desc',
          },
          data: {
            ...state.componentTransitivePolicyViolations.data,
            displayedViolations: [
              { threatLevel: 0, policyName: 'Z', displayName: '' },
              { threatLevel: 0, policyName: 'b', displayName: '' },
              { threatLevel: 0, policyName: 'a', displayName: '' },
            ],
          },
        },
      });
    });

    it('sorts by policy name ascending given policyName key descending and payload of policyName', function () {
      const payload = 'policyName';
      const state = {
        componentTransitivePolicyViolations: {
          sortConfiguration: {
            key: 'policyName',
            dir: 'desc',
          },
          filterConfiguration: {
            policyName: '',
            displayName: '',
          },
          data: {
            violations: [
              { threatLevel: 0, policyName: 'a', displayName: '' },
              { threatLevel: 0, policyName: 'Z', displayName: '' },
              { threatLevel: 0, policyName: 'b', displayName: '' },
            ],
          },
        },
      };
      const action = {
        type: TRANSITIVE_VIOLATIONS_SET_SORTING_PARAMETERS,
        payload: payload,
      };
      const newState = reduce(state, action);

      expect(newState).toEqual({
        componentTransitivePolicyViolations: {
          ...state.componentTransitivePolicyViolations,
          sortConfiguration: {
            key: 'policyName',
            dir: 'asc',
          },
          data: {
            ...state.componentTransitivePolicyViolations.data,
            displayedViolations: [
              { threatLevel: 0, policyName: 'a', displayName: '' },
              { threatLevel: 0, policyName: 'b', displayName: '' },
              { threatLevel: 0, policyName: 'Z', displayName: '' },
            ],
          },
        },
      });
    });

    it('sorts by policy name descending given policyName key ascending and payload of policyName', function () {
      const payload = 'policyName';
      const state = {
        componentTransitivePolicyViolations: {
          sortConfiguration: {
            key: 'policyName',
            dir: 'asc',
          },
          filterConfiguration: {
            policyName: '',
            displayName: '',
          },
          data: {
            violations: [
              { threatLevel: 0, policyName: 'a', displayName: '' },
              { threatLevel: 0, policyName: 'Z', displayName: '' },
              { threatLevel: 0, policyName: 'b', displayName: '' },
            ],
          },
        },
      };
      const action = {
        type: TRANSITIVE_VIOLATIONS_SET_SORTING_PARAMETERS,
        payload: payload,
      };
      const newState = reduce(state, action);

      expect(newState).toEqual({
        componentTransitivePolicyViolations: {
          ...state.componentTransitivePolicyViolations,
          sortConfiguration: {
            key: 'policyName',
            dir: 'desc',
          },
          data: {
            ...state.componentTransitivePolicyViolations.data,
            displayedViolations: [
              { threatLevel: 0, policyName: 'Z', displayName: '' },
              { threatLevel: 0, policyName: 'b', displayName: '' },
              { threatLevel: 0, policyName: 'a', displayName: '' },
            ],
          },
        },
      });
    });

    it('sorts by display name descending given other key and payload of displayName', function () {
      const payload = 'displayName';
      const state = {
        componentTransitivePolicyViolations: {
          sortConfiguration: {
            key: 'threatLevel',
            dir: 'asc',
          },
          filterConfiguration: {
            policyName: '',
            displayName: '',
          },
          data: {
            violations: [
              { threatLevel: 0, policyName: '', displayName: 'a' },
              { threatLevel: 0, policyName: '', displayName: 'Z' },
              { threatLevel: 0, policyName: '', displayName: 'b' },
            ],
          },
        },
      };
      const action = {
        type: TRANSITIVE_VIOLATIONS_SET_SORTING_PARAMETERS,
        payload: payload,
      };
      const newState = reduce(state, action);

      expect(newState).toEqual({
        componentTransitivePolicyViolations: {
          ...state.componentTransitivePolicyViolations,
          sortConfiguration: {
            key: 'displayName',
            dir: 'desc',
          },
          data: {
            ...state.componentTransitivePolicyViolations.data,
            displayedViolations: [
              { threatLevel: 0, policyName: '', displayName: 'Z' },
              { threatLevel: 0, policyName: '', displayName: 'b' },
              { threatLevel: 0, policyName: '', displayName: 'a' },
            ],
          },
        },
      });
    });

    it('sorts by display name ascending given displayName key descending and payload of displayName', function () {
      const payload = 'displayName';
      const state = {
        componentTransitivePolicyViolations: {
          sortConfiguration: {
            key: 'displayName',
            dir: 'desc',
          },
          filterConfiguration: {
            policyName: '',
            displayName: '',
          },
          data: {
            violations: [
              { threatLevel: 0, policyName: '', displayName: 'a' },
              { threatLevel: 0, policyName: '', displayName: 'Z' },
              { threatLevel: 0, policyName: '', displayName: 'b' },
            ],
          },
        },
      };
      const action = {
        type: TRANSITIVE_VIOLATIONS_SET_SORTING_PARAMETERS,
        payload: payload,
      };
      const newState = reduce(state, action);

      expect(newState).toEqual({
        componentTransitivePolicyViolations: {
          ...state.componentTransitivePolicyViolations,
          sortConfiguration: {
            key: 'displayName',
            dir: 'asc',
          },
          data: {
            ...state.componentTransitivePolicyViolations.data,
            displayedViolations: [
              { threatLevel: 0, policyName: '', displayName: 'a' },
              { threatLevel: 0, policyName: '', displayName: 'b' },
              { threatLevel: 0, policyName: '', displayName: 'Z' },
            ],
          },
        },
      });
    });

    it('sorts by display name descending given displayName key ascending and payload of displayName', function () {
      const payload = 'displayName';
      const state = {
        componentTransitivePolicyViolations: {
          sortConfiguration: {
            key: 'displayName',
            dir: 'asc',
          },
          filterConfiguration: {
            policyName: '',
            displayName: '',
          },
          data: {
            violations: [
              { threatLevel: 0, policyName: '', displayName: 'a' },
              { threatLevel: 0, policyName: '', displayName: 'Z' },
              { threatLevel: 0, policyName: '', displayName: 'b' },
            ],
          },
        },
      };
      const action = {
        type: TRANSITIVE_VIOLATIONS_SET_SORTING_PARAMETERS,
        payload: payload,
      };
      const newState = reduce(state, action);

      expect(newState).toEqual({
        componentTransitivePolicyViolations: {
          ...state.componentTransitivePolicyViolations,
          sortConfiguration: {
            key: 'displayName',
            dir: 'desc',
          },
          data: {
            ...state.componentTransitivePolicyViolations.data,
            displayedViolations: [
              { threatLevel: 0, policyName: '', displayName: 'Z' },
              { threatLevel: 0, policyName: '', displayName: 'b' },
              { threatLevel: 0, policyName: '', displayName: 'a' },
            ],
          },
        },
      });
    });
  });

  describe('TRANSITIVE_VIOLATIONS_SET_FILTERING_PARAMETERS action', function () {
    it('filters by policy name given policyName payload', function () {
      const payload = { policyName: 'z' };
      const state = {
        componentTransitivePolicyViolations: {
          sortConfiguration: {
            key: 'policyName',
            dir: 'asc',
          },
          filterConfiguration: {
            policyName: '',
            displayName: '',
          },
          data: {
            violations: [
              { threatLevel: 0, policyName: 'policy x name', displayName: '' },
              { threatLevel: 0, policyName: 'policy Z name', displayName: '' },
              { threatLevel: 0, policyName: 'policy z name', displayName: '' },
            ],
          },
        },
      };
      const action = {
        type: TRANSITIVE_VIOLATIONS_SET_FILTERING_PARAMETERS,
        payload: payload,
      };
      const newState = reduce(state, action);

      expect(newState).toEqual({
        componentTransitivePolicyViolations: {
          ...state.componentTransitivePolicyViolations,
          filterConfiguration: {
            policyName: 'z',
            displayName: '',
          },
          data: {
            ...state.componentTransitivePolicyViolations.data,
            displayedViolations: [
              { threatLevel: 0, policyName: 'policy Z name', displayName: '' },
              { threatLevel: 0, policyName: 'policy z name', displayName: '' },
            ],
          },
        },
      });
    });

    it('filters by display name given displayName payload', function () {
      const payload = { displayName: 'z' };
      const state = {
        componentTransitivePolicyViolations: {
          sortConfiguration: {
            key: 'policyName',
            dir: 'asc',
          },
          filterConfiguration: {
            policyName: '',
            displayName: 'z',
          },
          data: {
            violations: [
              { threatLevel: 0, policyName: '', displayName: 'display x name' },
              { threatLevel: 0, policyName: '', displayName: 'display Z name' },
              { threatLevel: 0, policyName: '', displayName: 'display z name' },
            ],
          },
        },
      };
      const action = {
        type: TRANSITIVE_VIOLATIONS_SET_FILTERING_PARAMETERS,
        payload: payload,
      };
      const newState = reduce(state, action);

      expect(newState).toEqual({
        componentTransitivePolicyViolations: {
          ...state.componentTransitivePolicyViolations,
          filterConfiguration: {
            policyName: '',
            displayName: 'z',
          },
          data: {
            ...state.componentTransitivePolicyViolations.data,
            displayedViolations: [
              { threatLevel: 0, policyName: '', displayName: 'display Z name' },
              { threatLevel: 0, policyName: '', displayName: 'display z name' },
            ],
          },
        },
      });
    });
  });

  describe('TRANSITIVE_VIOLATIONS_TOGGLE_REQUEST_WAIVE action', function () {
    it('sets isRequestWaiveTransitiveViolationsOpen to true if it is false', function () {
      const state = { isRequestWaiveTransitiveViolationsOpen: false };
      const action = { type: TRANSITIVE_VIOLATIONS_TOGGLE_REQUEST_WAIVE };
      const newState = reduce(state, action);

      expect(newState).toEqual({
        isRequestWaiveTransitiveViolationsOpen: true,
      });
    });

    it('sets isRequestWaiveTransitiveViolationsOpen to false if it is true', function () {
      const state = { isRequestWaiveTransitiveViolationsOpen: true };
      const action = { type: TRANSITIVE_VIOLATIONS_TOGGLE_REQUEST_WAIVE };
      const newState = reduce(state, action);

      expect(newState).toEqual({
        isRequestWaiveTransitiveViolationsOpen: false,
      });
    });
  });

  describe('TRANSITIVE_VIOLATIONS_TOGGLE_WAIVE action', function () {
    it('sets isWaiveTransitiveViolationsOpen to true if it is false', function () {
      const state = { isWaiveTransitiveViolationsOpen: false };
      const action = { type: TRANSITIVE_VIOLATIONS_TOGGLE_WAIVE };
      const newState = reduce(state, action);

      expect(newState).toEqual({
        isWaiveTransitiveViolationsOpen: true,
      });
    });

    it('sets isWaiveTransitiveViolationsOpen to false if it is true', function () {
      const state = { isWaiveTransitiveViolationsOpen: true };
      const action = { type: TRANSITIVE_VIOLATIONS_TOGGLE_WAIVE };
      const newState = reduce(state, action);

      expect(newState).toEqual({
        isWaiveTransitiveViolationsOpen: false,
      });
    });
  });

  describe('TRANSITIVE_VIOLATION_WAIVERS_LOAD_REQUESTED action', function () {
    it('sets in transitiveViolationWaivers loading to true, error to null, and data to the initial value', function () {
      const state = {};
      const action = { type: TRANSITIVE_VIOLATION_WAIVERS_LOAD_REQUESTED };
      const newState = reduce(state, action);

      const { transitiveViolationWaivers } = newState;
      expect(transitiveViolationWaivers).toEqual({
        loading: true,
        error: null,
        data: { componentPolicyWaivers: [] },
      });
    });
  });

  describe('TRANSITIVE_VIOLATION_WAIVERS_LOAD_FULFILLED action', function () {
    it(
      'sets in transitiveViolationWaivers loading to false, error to null, data to the payload,' +
        'and opens the view transitive violation waivers popover',
      function () {
        const state = {};
        const action = { type: TRANSITIVE_VIOLATION_WAIVERS_LOAD_FULFILLED, payload: 'data' };
        const newState = reduce(state, action);

        const { transitiveViolationWaivers } = newState;
        expect(transitiveViolationWaivers).toEqual({
          loading: false,
          error: null,
          data: 'data',
        });
        expect(newState.isViewTransitiveViolationWaiversOpen).toBeTruthy();
      }
    );
  });

  describe('TRANSITIVE_VIOLATION_WAIVERS_LOAD_FAILED action', function () {
    it('sets in transitiveViolationWaivers loading to false and the error to the http error message', function () {
      const state = {};
      const action = { type: TRANSITIVE_VIOLATION_WAIVERS_LOAD_FAILED, payload: 'error' };
      const newState = reduce(state, action);

      const { transitiveViolationWaivers } = newState;
      expect(transitiveViolationWaivers).toEqual({
        loading: false,
        error: 'error',
      });
    });
  });

  describe('TRANSITIVE_VIOLATIONS_TOGGLE_VIEW_WAIVERS action', function () {
    it('sets isViewTransitiveViolationWaiversOpen to true if it is false', function () {
      const state = { isViewTransitiveViolationWaiversOpen: false };
      const action = { type: TRANSITIVE_VIOLATIONS_TOGGLE_VIEW_WAIVERS };
      const newState = reduce(state, action);

      expect(newState).toEqual({
        isViewTransitiveViolationWaiversOpen: true,
      });
    });

    it('sets isViewTransitiveViolationWaiversOpen to false if it is true', function () {
      const state = { isViewTransitiveViolationWaiversOpen: true };
      const action = { type: TRANSITIVE_VIOLATIONS_TOGGLE_VIEW_WAIVERS };
      const newState = reduce(state, action);

      expect(newState).toEqual({
        isViewTransitiveViolationWaiversOpen: false,
      });
    });
  });
});
