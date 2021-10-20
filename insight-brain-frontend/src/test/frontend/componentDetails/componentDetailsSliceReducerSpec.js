/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

import reducer from '../../../main/frontend/componentDetails/componentDetailsSlice';
import {
  VISIT_ANCESTOR_ACTION,
  RETURN_TO_OFFSPRING,
} from '../../../main/frontend/componentDetails/componentDetailsSlice';

const LOAD_COMPONENT_LABELS_REQUESTED = 'componentDetails/loadComponentDetails/pending';
const LOAD_COMPONENT_LABELS_FULFILLED = 'componentDetails/loadComponentDetails/fulfilled';
const LOAD_COMPONENT_LABELS_FAILED = 'componentDetails/loadComponentDetails/rejected';
const LOAD_APPLICABLE_LABELS_REQUESTED = 'componentDetails/loadApplicableLabels/pending';
const LOAD_APPLICABLE_LABELS_FULFILLED = 'componentDetails/loadApplicableLabels/fulfilled';
const LOAD_APPLICABLE_LABELS_FAILED = 'componentDetails/loadApplicableLabels/rejected';

describe('componentDetailsReducer', () => {
  describe('VISIT_ANCESTOR_ACTION action', () => {
    it('adds "offspring" information', () => {
      const state = {
        isVisitingAncestor: false,
        offspring: null,
      };
      const offspring = {
        derivedComponentName: 'org.springframework : spring-web : 5.3.9',
        hash: '88c920ec1bda67fea04d',
      };
      const newState = reducer(state, {
        type: VISIT_ANCESTOR_ACTION,
        payload: {
          offspring: offspring,
        },
      });
      expect(newState.offspring).not.toBeNull();
      expect(newState.offspring).toBe(offspring);
      expect(newState.isVisitingAncestor).toBe(true);
    });
  });

  describe('RETURN_TO_OFFSPRING action', () => {
    it('removes "offspring" information', () => {
      const state = {
        isVisitingAncestor: true,
        offspring: {
          derivedComponentName: 'org.springframework : spring-web : 5.3.9',
          hash: '88c920ec1bda67fea04d',
        },
      };
      const newState = reducer(state, {
        type: RETURN_TO_OFFSPRING,
      });
      expect(newState.offspring).toBeNull();
      expect(newState.isVisitingAncestor).toBe(false);
    });
  });

  describe('unknown action', () => {
    it('returns original state', () => {
      const state = {
        pendingLoads: new Set(),
        isVisitingAncestor: false,
        offspring: null,
        labels: [],
        loadError: null,
      };
      const action = { type: 'UNKNOWN' };
      const newState = reducer(state, action);
      expect(newState).toBe(state);
    });
  });

  describe('LOAD_COMPONENT_LABELS_REQUESTED action', function () {
    it('adds "labels" pending load', function () {
      const state = {
        pendingLoads: new Set(),
        isVisitingAncestor: false,
        offspring: null,
        labels: [],
        loadError: null,
      };
      const newState = reducer(state, {
        type: LOAD_COMPONENT_LABELS_REQUESTED,
      });
      expect(newState.pendingLoads.has('labels')).toBe(true);
    });
  });

  describe('LOAD_COMPONENT_LABELS_FULFILLED action', function () {
    it('adds labels value and removes "labels" pending load', function () {
      const state = {
        pendingLoads: new Set(),
        isVisitingAncestor: false,
        offspring: null,
        labels: [],
        loadError: null,
      };
      const newState = reducer(state, {
        type: LOAD_COMPONENT_LABELS_FULFILLED,
        payload: [
          {
            data: {
              labelsByOwner: [],
            },
          },
          {},
        ],
      });
      expect(newState.pendingLoads.size).toEqual(0);
      expect(newState.labels).toEqual([]);
      expect(newState.loadError).toBeNull();
    });
  });

  describe('LOAD_COMPONENT_LABELS_FAILED action', function () {
    it('adds loadError value and removes "labels" pending load', function () {
      const state = {
        pendingLoads: new Set(),
        isVisitingAncestor: false,
        offspring: null,
        labels: [],
        loadError: null,
      };
      const newState = reducer(state, {
        type: LOAD_COMPONENT_LABELS_FAILED,
        payload: {},
      });

      expect(newState.pendingLoads.size).toEqual(0);
      expect(newState.loadError).toEqual('Error');
    });

    it('clears error state on retry', function () {
      const state = { pendingLoads: new Set(), labels: [], loadError: null };
      const newState = reducer(state, {
        type: LOAD_COMPONENT_LABELS_FAILED,
        payload: {},
      });
      expect(newState.pendingLoads.size).toEqual(0);
      expect(newState.loadError).toEqual('Error');

      const retryState = reducer(newState, {
        type: LOAD_COMPONENT_LABELS_FULFILLED,
        payload: [
          {
            data: {
              labelsByOwner: [],
            },
          },
          {},
        ],
      });
      expect(retryState.loadError).toBeNull();
    });
  });

  describe('LOAD_APPLICABLE_LABELS_REQUESTED action', function () {
    it('adds "applicableLabels" pending load', function () {
      const state = {
        pendingLoads: new Set(),
        isVisitingAncestor: false,
        offspring: null,
        labels: [],
        applicableLabels: [],
        loadError: null,
      };
      const newState = reducer(state, {
        type: LOAD_APPLICABLE_LABELS_REQUESTED,
      });
      expect(newState.pendingLoads.has('applicableLabels')).toBe(true);
    });
  });

  describe('LOAD_APPLICABLE_LABELS_FULFILLED action', function () {
    it('adds applicableLabels value and removes "applicableLabels" pending load', function () {
      const state = {
        pendingLoads: new Set(),
        isVisitingAncestor: false,
        offspring: null,
        labels: [],
        applicableLabels: [],
        loadError: null,
      };
      const newState = reducer(state, {
        type: LOAD_APPLICABLE_LABELS_FULFILLED,
        payload: {
          data: {
            labelsByOwner: [],
          },
        },
      });
      expect(newState.pendingLoads.size).toEqual(0);
      expect(newState.labels).toEqual([]);
      expect(newState.loadError).toBeNull();
    });
  });

  describe('LOAD_APPLICABLE_LABELS_FAILED action', function () {
    it('adds loadError value and removes "applicableLabels" pending load', function () {
      const state = {
        pendingLoads: new Set(),
        isVisitingAncestor: false,
        offspring: null,
        labels: [],
        applicableLabels: [],
        loadError: null,
      };
      const newState = reducer(state, {
        type: LOAD_APPLICABLE_LABELS_FAILED,
        payload: {},
      });

      expect(newState.pendingLoads.size).toEqual(0);
      expect(newState.loadError).toEqual('Error');
    });

    it('clears error state on retry', function () {
      const state = { pendingLoads: new Set(), labels: [], applicableLabels: [], loadError: null };
      const newState = reducer(state, {
        type: LOAD_APPLICABLE_LABELS_FAILED,
        payload: {},
      });
      expect(newState.pendingLoads.size).toEqual(0);
      expect(newState.loadError).toEqual('Error');

      const retryState = reducer(newState, {
        type: LOAD_APPLICABLE_LABELS_FULFILLED,
        payload: {
          data: {
            labelsByOwner: [],
          },
        },
      });
      expect(retryState.loadError).toBeNull();
    });
  });
});
