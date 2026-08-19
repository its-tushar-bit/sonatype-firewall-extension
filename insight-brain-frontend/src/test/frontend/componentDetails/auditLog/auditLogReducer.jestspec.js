/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { map } from 'ramda';
import reducer, { initState } from 'MainRoot/componentDetails/auditLog/auditLogReducer';
import { processAuditRecord } from 'MainRoot/componentDetails/componentDetailsUtils';
import { SELECT_COMPONENT } from 'MainRoot/applicationReport/applicationReportActions';

describe('auditLogReducer', function () {
  describe('initial State', function () {
    it('returns the original immutable state if no state is provided or an unknown action is used', function () {
      const state = reducer(undefined, { type: 'UNKNOWN' });

      expect(state).not.toBeUndefined();
      expect(state.isLoading).toBe(false);
      expect(state.error).toBeNull();
      expect(state.auditRecords).toEqual([]);

      expect(() => {
        state.newProp = 'newProp';
      }).toThrowError(TypeError);

      expect(() => {
        state.isLoading = true;
      }).toThrowError(TypeError);

      expect(() => {
        state.auditRecords = [1, 2, 3];
      }).toThrowError(TypeError);
    });
  });

  describe('AUDIT_LOG_LOAD_AUDIT_LOG_REQUESTED action', function () {
    it('sets the isLoading prop to true', () => {
      const initialState = {
        isLoading: false,
        auditRecords: [],
        error: null,
        otherProp: { prop: 'foo' },
      };

      const newState = reducer(initialState, { type: 'AUDIT_LOG_LOAD_AUDIT_LOG_REQUESTED' });

      expect(newState.isLoading).toBe(true);
      expect(newState.otherProp).toEqual(initialState.otherProp);
    });
  });

  describe('AUDIT_LOG_LOAD_AUDIT_LOG_FULFILLED action', function () {
    it('turns off the isLoading flag, unsets the error and and sets the auditRecords from the payload', () => {
      const initialState = {
        isLoading: true,
        auditRecords: [],
        error: 'error',
        otherProp: { prop: 'foo' },
      };

      const payload = [{ hash: 'hash1' }];
      const transformedPayload = map(processAuditRecord, payload);
      const newState = reducer(initialState, { type: 'AUDIT_LOG_LOAD_AUDIT_LOG_FULFILLED', payload });

      expect(newState.isLoading).toBe(false);
      expect(newState.error).toBe(null);
      expect(newState.auditRecords).toEqual(transformedPayload);
      expect(newState.otherProp).toEqual(initialState.otherProp);
    });
  });

  describe('AUDIT_LOG_LOAD_AUDIT_LOG_FAILED', function () {
    it('turns off the isLoading flag and sets the error prop from the payload', () => {
      const initialState = {
        isLoading: true,
        auditRecords: [],
        error: null,
        otherProp: { prop: 'foo' },
      };

      const payload = 'Some error';
      const newState = reducer(initialState, { type: 'AUDIT_LOG_LOAD_AUDIT_LOG_FAILED', payload });

      expect(newState.isLoading).toBe(false);
      expect(newState.auditRecords).toEqual([]);
      expect(newState.error).toEqual(payload);
      expect(newState.otherProp).toEqual(initialState.otherProp);
    });
  });

  describe('UI_ROUTER_ON_FINISH', function () {
    it('clears the state and returns the initial', () => {
      const currentState = {
        isLoading: true,
        auditRecords: [{ hash: 'hash1' }, { hash: 'hash2' }],
        error: 'Some error',
        otherProp: { prop: 'foo' },
      };

      const newState = reducer(currentState, {
        type: '@@reduxUiRouter/onFinish',
      });

      expect(newState.isLoading).toEqual(false);
      expect(newState.auditRecords).toEqual([]);
      expect(newState.error).toBeNull();
      expect(newState.otherProp).toBeUndefined();
    });
  });

  describe('AUDIT_LOG_SORT_AUDIT_LOG_REQUESTED', function () {
    it('sets the isLoading flag to true and sets the appliedSort prop to payload', () => {
      const currentState = {
        isLoading: false,
        auditRecords: [{ hash: 'hash1' }, { hash: 'hash2' }],
        appliedSort: null,
      };

      const newState = reducer(currentState, {
        type: 'AUDIT_LOG_SORT_AUDIT_LOG_REQUESTED',
        payload: '-time',
      });

      expect(newState.isLoading).toEqual(true);
      expect(newState.appliedSort).toEqual('-time');
    });
  });

  describe('AUDIT_LOG_SORT_AUDIT_LOG_FULFILLED', function () {
    it('sets the isLoading flag to false and sets the auditRecords prop to payload', () => {
      const currentState = {
        isLoading: true,
        auditRecords: [{ hash: 'hash1' }, { hash: 'hash2' }],
        appliedSort: '-time',
      };
      const sortedRecords = [{ hash: 'hash2' }, { hash: 'hash1' }];

      const newState = reducer(currentState, {
        type: 'AUDIT_LOG_SORT_AUDIT_LOG_FULFILLED',
        payload: sortedRecords,
      });

      expect(newState.isLoading).toEqual(false);
      expect(newState.auditRecords).toEqual(sortedRecords);
    });
  });

  describe('SELECT_COMPONENT', () => {
    it('resets current state to initialState', () => {
      const state = Object.freeze({
        isLoading: true,
        auditRecords: [{}],
        error: 'error',
        appliedSort: 'asc',
      });

      const newState = reducer(state, { type: SELECT_COMPONENT });
      expect(newState).toEqual(initState);
    });
  });
});
