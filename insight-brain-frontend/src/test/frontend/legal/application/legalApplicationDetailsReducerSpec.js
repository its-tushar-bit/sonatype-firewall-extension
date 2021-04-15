/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

import legalApplicationDetailsReducer from '../../../../main/frontend/legal/application/legalApplicationDetailsReducer';
import {
  LEGAL_APPLICATION_DETAILS_LOAD_APP_FAILED,
  LEGAL_APPLICATION_DETAILS_LOAD_APP_FULFILLED,
  LEGAL_APPLICATION_DETAILS_LOAD_APP_REQUESTED,
  LEGAL_APPLICATION_DETAILS_LOAD_COMPONENTS_FAILED,
  LEGAL_APPLICATION_DETAILS_LOAD_COMPONENTS_FULFILLED,
  LEGAL_APPLICATION_DETAILS_LOAD_COMPONENTS_REQUESTED,
  LEGAL_APPLICATION_DETAILS_LOAD_STAGE_FAILED,
  LEGAL_APPLICATION_DETAILS_LOAD_STAGE_FULFILLED,
  LEGAL_APPLICATION_DETAILS_LOAD_STAGE_REQUESTED,
} from '../../../../main/frontend/legal/application/legalApplicationDetailsActions';

const otherObject = { value: 'test value' };

describe('legalApplicationDetailsReducer', function () {
  describe('initial state', function () {
    it('has default fields', function () {
      const action = { type: 'UNKNOWN' };
      const newState = legalApplicationDetailsReducer(undefined, action);

      expect(newState.application).toEqual({
        name: null,
        error: null,
        loading: false,
      });
      expect(newState.stageType).toEqual({
        name: null,
        error: null,
        loading: false,
      });
      expect(newState.components).toEqual({
        results: [],
        error: null,
        loading: false,
      });
    });
  });

  describe('unknown action', function () {
    it('returns original state', function () {
      const state = { foo: 'bar' };
      const action = {
        type: 'UNKNOWN',
      };
      const newState = legalApplicationDetailsReducer(state, action);
      expect(newState).toBe(state);
    });
  });

  describe('LEGAL_APPLICATION_DETAILS_LOAD_APP_REQUESTED action', function () {
    it('resets state when fetching an application', function () {
      const state = Object.freeze({
        application: {
          name: 'some-app',
          error: null,
          loading: false,
        },
        stageType: {
          name: 'some-stage',
          error: null,
          loading: true,
        },
        components: {
          results: [],
          error: 'some error',
          loading: true,
        },
      });
      const action = { type: LEGAL_APPLICATION_DETAILS_LOAD_APP_REQUESTED };
      const newState = legalApplicationDetailsReducer(state, action);
      expect(newState.application).toEqual({
        name: null,
        error: null,
        loading: true,
      });
      expect(newState.stageType).toEqual({
        name: null,
        error: null,
        loading: false,
      });
      expect(newState.components).toEqual({
        results: [],
        error: null,
        loading: false,
      });
    });
  });

  describe('LEGAL_APPLICATION_DETAILS_LOAD_APP_FULFILLED action', function () {
    it('updates application state', function () {
      const state = Object.freeze({
        application: {
          name: null,
          error: null,
          loading: true,
        },
      });
      const action = {
        type: LEGAL_APPLICATION_DETAILS_LOAD_APP_FULFILLED,
        payload: { name: 'some app' },
      };
      const newState = legalApplicationDetailsReducer(state, action);
      expect(newState.application).toEqual({
        name: 'some app',
        error: null,
        loading: false,
      });
    });
  });

  describe('LEGAL_APPLICATION_DETAILS_LOAD_APP_FAILED action', function () {
    it('updates application state with an error', function () {
      const state = Object.freeze({
        application: {
          name: null,
          error: null,
          loading: true,
        },
      });
      const action = {
        type: LEGAL_APPLICATION_DETAILS_LOAD_APP_FAILED,
        payload: 'some app error',
      };
      const newState = legalApplicationDetailsReducer(state, action);
      expect(newState.application).toEqual({
        name: null,
        error: 'some app error',
        loading: false,
      });
    });
  });

  describe('LEGAL_APPLICATION_DETAILS_LOAD_STAGE_REQUESTED action', function () {
    it('updates stageType state to loading', function () {
      const state = Object.freeze({
        stageType: {
          name: null,
          error: null,
          loading: false,
        },
        other: otherObject,
      });
      const action = { type: LEGAL_APPLICATION_DETAILS_LOAD_STAGE_REQUESTED };
      const newState = legalApplicationDetailsReducer(state, action);
      expect(newState.stageType).toEqual({
        name: null,
        error: null,
        loading: true,
      });
      expect(newState.other).toBe(otherObject); // other properties are not modified
    });
  });

  describe('LEGAL_APPLICATION_DETAILS_LOAD_APP_FULFILLED action', function () {
    it('updates stageType state with data', function () {
      const state = Object.freeze({
        stageType: {
          name: null,
          error: null,
          loading: true,
        },
        other: otherObject,
      });
      const action = {
        type: LEGAL_APPLICATION_DETAILS_LOAD_STAGE_FULFILLED,
        payload: 'some stage',
      };
      const newState = legalApplicationDetailsReducer(state, action);
      expect(newState.stageType).toEqual({
        name: 'some stage',
        error: null,
        loading: false,
      });
      expect(newState.other).toBe(otherObject); // other properties are not modified
    });
  });

  describe('LEGAL_APPLICATION_DETAILS_LOAD_STAGE_FAILED action', function () {
    it('updates stageType state with an error', function () {
      const state = Object.freeze({
        stageType: {
          name: null,
          error: null,
          loading: true,
        },
        other: otherObject,
      });
      const action = {
        type: LEGAL_APPLICATION_DETAILS_LOAD_STAGE_FAILED,
        payload: 'some stageType error',
      };
      const newState = legalApplicationDetailsReducer(state, action);
      expect(newState.stageType).toEqual({
        name: null,
        error: 'some stageType error',
        loading: false,
      });
      expect(newState.other).toBe(otherObject); // other properties are not modified
    });
  });

  describe('LEGAL_APPLICATION_DETAILS_LOAD_COMPONENTS_REQUESTED action', function () {
    it('updates components state to loading', function () {
      const state = Object.freeze({
        components: {
          results: [],
          error: null,
          loading: false,
        },
        other: otherObject,
      });
      const action = {
        type: LEGAL_APPLICATION_DETAILS_LOAD_COMPONENTS_REQUESTED,
      };
      const newState = legalApplicationDetailsReducer(state, action);
      expect(newState.components).toEqual({
        results: [],
        error: null,
        loading: true,
      });
      expect(newState.other).toBe(otherObject); // other properties are not modified
    });
  });

  describe('LEGAL_APPLICATION_DETAILS_LOAD_COMPONENTS_FULFILLED action', function () {
    it('updates components state with data', function () {
      const state = Object.freeze({
        components: {
          results: [],
          error: null,
          loading: true,
        },
        other: otherObject,
      });
      const action = {
        type: LEGAL_APPLICATION_DETAILS_LOAD_COMPONENTS_FULFILLED,
        payload: [1, 2, 3],
      };
      const newState = legalApplicationDetailsReducer(state, action);
      expect(newState.components).toEqual({
        results: [1, 2, 3],
        error: null,
        loading: false,
      });
      expect(newState.other).toBe(otherObject); // other properties are not modified
    });
  });

  describe('LEGAL_APPLICATION_DETAILS_LOAD_COMPONENTS_FAILED action', function () {
    it('updates components state with an error', function () {
      const state = Object.freeze({
        components: {
          results: [],
          error: null,
          loading: true,
        },
        other: otherObject,
      });
      const action = {
        type: LEGAL_APPLICATION_DETAILS_LOAD_COMPONENTS_FAILED,
        payload: 'some components error',
      };
      const newState = legalApplicationDetailsReducer(state, action);
      expect(newState.components).toEqual({
        results: [],
        error: 'some components error',
        loading: false,
      });
      expect(newState.other).toBe(otherObject); // other properties are not modified
    });
  });
});
