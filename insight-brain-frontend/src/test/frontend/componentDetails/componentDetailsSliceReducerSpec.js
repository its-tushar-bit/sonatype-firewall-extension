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
const ADD_PROPRIETARY_MATCHERS_REQUESTED = 'componentDetails/addProprietaryMatchers/pending';
const ADD_PROPRIETARY_MATCHERS_FULFILLED = 'componentDetails/addProprietaryMatchers/fulfilled';
const ADD_PROPRIETARY_MATCHERS_FAILED = 'componentDetails/addProprietaryMatchers/rejected';
const RESET_SUBMIT_MASK_STATE = 'componentDetails/resetSubmitMaskState';
const RESET_SUBMIT_ERROR = 'componentDetails/resetSubmitError';
const SET_COMPONENT_MATCHERS_DATA = 'componentDetails/setComponentMatchersData';
const TOGGLE_SHOW_MATCHERS_POPOVER = 'componentDetails/toggleShowMatchersPopover';

describe('componentDetailsReducer', () => {
  let mockState;

  beforeEach(() => {
    mockState = {
      pendingLoads: new Set(),
      isVisitingAncestor: false,
      offspring: null,
      labels: [],
      applicableLabels: [],
      loadError: null,
      applicableLabelsLoadError: null,
    };
  });

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
      const action = { type: 'UNKNOWN' };
      const newState = reducer(mockState, action);
      expect(newState).toBe(mockState);
    });
  });

  describe('LOAD_COMPONENT_LABELS_REQUESTED action', function () {
    it('adds "labels" pending load', function () {
      const newState = reducer(mockState, {
        type: LOAD_COMPONENT_LABELS_REQUESTED,
      });
      expect(newState.pendingLoads.has('labels')).toBe(true);
    });
  });

  describe('LOAD_COMPONENT_LABELS_FULFILLED action', function () {
    it('adds labels value and removes "labels" pending load', function () {
      const newState = reducer(mockState, {
        type: LOAD_COMPONENT_LABELS_FULFILLED,
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

  describe('LOAD_COMPONENT_LABELS_FAILED action', function () {
    it('adds loadError value and removes "labels" pending load', function () {
      const newState = reducer(mockState, {
        type: LOAD_COMPONENT_LABELS_FAILED,
        payload: {},
      });

      expect(newState.pendingLoads.size).toEqual(0);
      expect(newState.loadError).toEqual('Error');
    });

    it('clears error state on retry', function () {
      const newState = reducer(mockState, {
        type: LOAD_COMPONENT_LABELS_FAILED,
        payload: {},
      });
      expect(newState.pendingLoads.size).toEqual(0);
      expect(newState.loadError).toEqual('Error');

      const retryState = reducer(newState, {
        type: LOAD_COMPONENT_LABELS_FULFILLED,
        payload: {
          data: {
            labelsByOwner: [],
          },
        },
      });
      expect(retryState.loadError).toBeNull();
    });
  });

  describe('LOAD_APPLICABLE_LABELS_REQUESTED action', function () {
    it('adds "applicableLabels" pending load', function () {
      const newState = reducer(mockState, {
        type: LOAD_APPLICABLE_LABELS_REQUESTED,
      });
      expect(newState.pendingLoads.has('applicableLabels')).toBe(true);
    });
  });

  describe('LOAD_APPLICABLE_LABELS_FULFILLED action', function () {
    it('adds ORDERED applicableLabels value and removes "applicableLabels" pending load', function () {
      const newState = reducer(mockState, {
        type: LOAD_APPLICABLE_LABELS_FULFILLED,
        payload: {
          data: {
            labelsByOwner: [{ labels: [{ label: 'Test z' }, { label: 'Test f' }, { label: 'Test a' }] }],
          },
        },
      });
      expect(newState.pendingLoads.size).toEqual(0);
      expect(newState.applicableLabels).toEqual([{ label: 'Test a' }, { label: 'Test f' }, { label: 'Test z' }]);
      expect(newState.loadError).toBeNull();
    });
  });

  describe('LOAD_APPLICABLE_LABELS_FAILED action', function () {
    it('adds loadError value and removes "applicableLabels" pending load', function () {
      const newState = reducer(mockState, {
        type: LOAD_APPLICABLE_LABELS_FAILED,
        payload: {},
      });

      expect(newState.pendingLoads.size).toEqual(0);
      expect(newState.applicableLabelsLoadError).toEqual('Error');
    });

    it('clears error state on retry', function () {
      const newState = reducer(mockState, {
        type: LOAD_APPLICABLE_LABELS_FAILED,
        payload: {},
      });
      expect(newState.pendingLoads.size).toEqual(0);
      expect(newState.applicableLabelsLoadError).toEqual('Error');

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

  describe('ADD_PROPRIETARY_MATCHERS_REQUESTED action', () => {
    it('sets setProprietaryMatchers.submitMaskState to false and deletes error', () => {
      const otherState = Object.freeze({ foo: 'bar' });
      const state = Object.freeze({
        otherState,
        setProprietaryMatchers: {
          submitMaskState: null,
          submitError: 'someError',
        },
      });
      const newState = reducer(state, {
        type: ADD_PROPRIETARY_MATCHERS_REQUESTED,
      });
      expect(newState.setProprietaryMatchers.submitMaskState).toBe(false);
      expect(newState.setProprietaryMatchers.submitError).toBeNull();
      expect(newState.otherState).toBe(otherState);
    });
  });

  describe('ADD_PROPRIETARY_MATCHERS_FULFILLED action', () => {
    it('sets setProprietaryMatchers.submitMaskState to true and deletes error', () => {
      const otherState = Object.freeze({ foo: 'bar' });
      const state = Object.freeze({
        otherState,
        setProprietaryMatchers: {
          submitMaskState: false,
          submitError: 'someError',
          data: { regex: 'some regex' },
        },
      });
      const newState = reducer(state, {
        type: ADD_PROPRIETARY_MATCHERS_FULFILLED,
      });
      expect(newState.setProprietaryMatchers.submitMaskState).toBe(true);
      expect(newState.setProprietaryMatchers.submitError).toBeNull();
      expect(newState.setProprietaryMatchers.data.regex).toBe('');
      expect(newState.otherState).toBe(otherState);
    });
  });

  describe('ADD_PROPRIETARY_MATCHERS_FAILED action', () => {
    it('sets setProprietaryMatchers.submitMaskState to null and sets the error', () => {
      const otherState = Object.freeze({ foo: 'bar' });
      const state = Object.freeze({
        otherState,
        setProprietaryMatchers: {
          submitMaskState: false,
          submitError: null,
        },
      });
      const newState = reducer(state, {
        type: ADD_PROPRIETARY_MATCHERS_FAILED,
        payload: 'some error',
      });
      expect(newState.setProprietaryMatchers.submitMaskState).toBeNull();
      expect(newState.setProprietaryMatchers.submitError).toBe('some error');
      expect(newState.otherState).toBe(otherState);
    });
  });

  describe('RESET_SUBMIT_MASK_STATE action', () => {
    it('sets setProprietaryMatchers.submitMaskState to null', () => {
      const otherState = Object.freeze({ foo: 'bar' });
      const state = Object.freeze({
        otherState,
        setProprietaryMatchers: {
          submitMaskState: true,
          submitError: null,
        },
      });
      const newState = reducer(state, {
        type: RESET_SUBMIT_MASK_STATE,
      });
      expect(newState.setProprietaryMatchers.submitMaskState).toBeNull();
      expect(newState.otherState).toBe(otherState);
    });
  });

  describe('SET_COMPONENT_MATCHERS_DATA action', () => {
    it('sets setProprietaryMatchers.setProprietaryMatchers', () => {
      const otherState = Object.freeze({ foo: 'bar' });
      const state = Object.freeze({
        otherState,
        setProprietaryMatchers: {
          submitMaskState: null,
          submitError: 'some error',
          data: {},
        },
      });
      const newState = reducer(state, {
        type: SET_COMPONENT_MATCHERS_DATA,
        payload: { data: 'some data' },
      });
      expect(newState.setProprietaryMatchers.data).toEqual({ data: 'some data' });
      expect(newState.otherState).toBe(otherState);
    });
  });

  describe('RESET_SUBMIT_ERROR action', () => {
    it('sets setProprietaryMatchers.submitError to null', () => {
      const otherState = Object.freeze({ foo: 'bar' });
      const state = Object.freeze({
        otherState,
        setProprietaryMatchers: {
          submitMaskState: null,
          submitError: 'some error',
        },
      });
      const newState = reducer(state, {
        type: RESET_SUBMIT_ERROR,
      });
      expect(newState.setProprietaryMatchers.submitError).toBeNull();
      expect(newState.otherState).toBe(otherState);
    });
  });

  describe('TOGGLE_SHOW_MATCHERS_POPOVER action', () => {
    it('toggles showMatchersPopover', () => {
      const otherState = Object.freeze({ foo: 'bar' });
      const state = Object.freeze({
        otherState,
        showMatchersPopover: false,
      });
      let newState = reducer(state, {
        type: TOGGLE_SHOW_MATCHERS_POPOVER,
      });
      expect(newState.showMatchersPopover).toBe(true);
      expect(newState.otherState).toBe(otherState);

      newState = reducer(newState, {
        type: TOGGLE_SHOW_MATCHERS_POPOVER,
      });
      expect(newState.showMatchersPopover).toBe(false);
    });
  });
});
