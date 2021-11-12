/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

import reducer, { initialState } from 'MainRoot/componentDetails/componentDetailsSlice';
import { VISIT_ANCESTOR_ACTION, RETURN_TO_OFFSPRING } from 'MainRoot/componentDetails/componentDetailsSlice';
import { SELECT_COMPONENT } from 'MainRoot/applicationReport/applicationReportActions';

const LOAD_COMPONENT_LABELS_REQUESTED = 'componentDetails/loadComponentDetails/pending';
const LOAD_COMPONENT_LABELS_FULFILLED = 'componentDetails/loadComponentDetails/fulfilled';
const LOAD_COMPONENT_LABELS_FAILED = 'componentDetails/loadComponentDetails/rejected';
const LOAD_APPLICABLE_LABELS_REQUESTED = 'componentDetails/loadApplicableLabels/pending';
const LOAD_APPLICABLE_LABELS_FULFILLED = 'componentDetails/loadApplicableLabels/fulfilled';
const LOAD_APPLICABLE_LABELS_FAILED = 'componentDetails/loadApplicableLabels/rejected';
const REMOVE_APPLIED_LABEL_REQUESTED = 'componentDetails/removeLabel/pending';
const REMOVE_APPLIED_LABEL_FULFILLED = 'componentDetails/removeLabel/fulfilled';
const REMOVE_APPLIED_LABEL_FAILED = 'componentDetails/removeLabel/rejected';
const LOAD_APPLICABLE_LABEL_SCOPES_REQUESTED = 'componentDetails/loadApplicableLabelScopes/pending';
const LOAD_APPLICABLE_LABEL_SCOPES_FULFILLED = 'componentDetails/loadApplicableLabelScopes/fulfilled';
const LOAD_APPLICABLE_LABEL_SCOPES_FAILED = 'componentDetails/loadApplicableLabelScopes/rejected';
const SAVE_LABEL_SCOPE_REQUESTED = 'componentDetails/saveApplyLabelScope/pending';
const SAVE_LABEL_SCOPE_FULFILLED = 'componentDetails/saveApplyLabelScope/fulfilled';
const SAVE_LABEL_SCOPE_FAILED = 'componentDetails/saveApplyLabelScope/rejected';
const ADD_PROPRIETARY_MATCHERS_REQUESTED = 'componentDetails/addProprietaryMatchers/pending';
const ADD_PROPRIETARY_MATCHERS_FULFILLED = 'componentDetails/addProprietaryMatchers/fulfilled';
const ADD_PROPRIETARY_MATCHERS_FAILED = 'componentDetails/addProprietaryMatchers/rejected';
const RESET_SUBMIT_MASK_STATE = 'componentDetails/resetSubmitMaskState';
const RESET_SUBMIT_ERROR = 'componentDetails/resetSubmitError';
const SET_COMPONENT_MATCHERS_DATA = 'componentDetails/setComponentMatchersData';
const TOGGLE_SHOW_MATCHERS_POPOVER = 'componentDetails/toggleShowMatchersPopover';
const TOGGLE_SHOW_REMOVE_LABEL_MODAL = 'componentDetails/toggleShowRemoveLabelModal';
const SET_SELECTED_LABEL_DETAILS = 'componentDetails/setSelectedLabelDetails';

describe('componentDetailsReducer', () => {
  let mockState;

  beforeEach(() => {
    mockState = {
      pendingLoads: new Set(),
      isVisitingAncestor: false,
      offspring: null,
      labels: [],
      applicableLabels: [],
      applicableLabelScopes: [],
      loadError: null,
      applicableLabelsLoadError: null,
      removeAppliedLabelError: null,
      selectedLabelDetails: {},
      showRemoveLabelModal: false,
      applicableLabelScopesLoadError: null,
      saveLabelScopeError: null,
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
            labelsByOwner: [
              {
                labels: [{ label: 'Test z' }, { label: 'Test f' }, { label: 'Test a' }],
                ownerType: 'testOwner',
                ownerId: 'testId',
              },
            ],
          },
        },
      });
      expect(newState.pendingLoads.size).toEqual(0);
      expect(newState.applicableLabels).toEqual([
        { label: 'Test a', ownerType: 'testOwner', ownerId: 'testId' },
        { label: 'Test f', ownerType: 'testOwner', ownerId: 'testId' },
        { label: 'Test z', ownerType: 'testOwner', ownerId: 'testId' },
      ]);
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

  describe('REMOVE_APPLIED_LABEL_REQUESTED action', function () {
    it('adds "removeAppliedLabel" pending load', function () {
      const newState = reducer(mockState, {
        type: REMOVE_APPLIED_LABEL_REQUESTED,
      });
      expect(newState.pendingLoads.has('removeAppliedLabel')).toBe(true);
    });
    it('removes any `removeAppliedLabelError` prop that may be on state', function () {
      const state = {
        ...mockState,
        removeAppliedLabelError: 'Some Error',
      };
      const newState = reducer(state, {
        type: REMOVE_APPLIED_LABEL_REQUESTED,
      });
      expect(newState.removeAppliedLabelError).toBeNull();
    });
  });

  describe('REMOVE_APPLIED_LABEL_FULFILLED action', function () {
    it('asign an empty object to selectedLabelDetails and removes "removeAppliedLabel" pending load', function () {
      const newState = reducer(mockState, {
        type: REMOVE_APPLIED_LABEL_FULFILLED,
      });
      expect(newState.pendingLoads.size).toEqual(0);
      expect(newState.selectedLabelDetails).toEqual({});
      expect(newState.removeAppliedLabelError).toBeNull();
    });
  });

  describe('REMOVE_APPLIED_LABEL_FAILED action', function () {
    it('adds removeAppliedLabelError value and removes "removeAppliedLabel" pending load', function () {
      const newState = reducer(mockState, {
        type: REMOVE_APPLIED_LABEL_FAILED,
        payload: {},
      });
      expect(newState.pendingLoads.size).toEqual(0);
      expect(newState.removeAppliedLabelError).toEqual('Error');
    });
  });
  describe('LOAD_APPLICABLE_LABEL_SCOPES_REQUESTED action', function () {
    it('adds "applicableLabelScopes" pending load', function () {
      const newState = reducer(mockState, {
        type: LOAD_APPLICABLE_LABEL_SCOPES_REQUESTED,
      });
      expect(newState.pendingLoads.has('applicableLabelScopes')).toBe(true);
    });
  });

  describe('LOAD_APPLICABLE_LABEL_SCOPES_FULFILLED action', function () {
    it('adds applicableLabelScopes value and removes "applicableLabelScopes" pending load', function () {
      const newState = reducer(mockState, {
        type: LOAD_APPLICABLE_LABEL_SCOPES_FULFILLED,
        payload: {
          data: {
            children: [{ children: null, id: 'testScopeId', name: 'testScopeName', type: 'testAppLevel' }],
            id: 'testScopeId',
            name: 'testScopeName',
            type: 'testOrgLevel',
          },
        },
      });

      expect(newState.pendingLoads.size).toEqual(0);
      expect(newState.applicableLabelScopes).toEqual([
        {
          children: [{ children: null, id: 'testScopeId', name: 'testScopeName', type: 'testAppLevel' }],
          id: 'testScopeId',
          name: 'testScopeName',
          type: 'testOrgLevel',
        },
        { children: null, id: 'testScopeId', name: 'testScopeName', type: 'testAppLevel' },
      ]);
      expect(newState.applicableLabelScopesLoadError).toBeNull();
    });
  });

  describe('LOAD_APPLICABLE_LABEL_SCOPES_FAILED action', function () {
    it('adds applicableLabelScopesLoadError value and removes "applicableLabelScopes" pending load', function () {
      const newState = reducer(mockState, {
        type: LOAD_APPLICABLE_LABEL_SCOPES_FAILED,
        payload: {},
      });

      expect(newState.pendingLoads.size).toEqual(0);
      expect(newState.applicableLabelScopesLoadError).toEqual('Error');
    });

    it('clears error state on retry', function () {
      const newState = reducer(mockState, {
        type: LOAD_APPLICABLE_LABEL_SCOPES_FAILED,
        payload: {},
      });
      expect(newState.pendingLoads.size).toEqual(0);
      expect(newState.applicableLabelScopesLoadError).toEqual('Error');

      const retryState = reducer(newState, {
        type: LOAD_APPLICABLE_LABEL_SCOPES_FULFILLED,
        payload: {
          data: {
            children: [{ children: null, id: 'testScopeId', name: 'testScopeName', type: 'testAppLevel' }],
            id: 'testScopeId',
            name: 'testScopeName',
            type: 'testOrgLevel',
          },
        },
      });
      expect(retryState.applicableLabelScopesLoadError).toBeNull();
    });
  });

  describe('SAVE_LABEL_SCOPE_REQUESTED action', function () {
    it('adds "isSavingLabelScope" pending load', function () {
      const newState = reducer(mockState, {
        type: SAVE_LABEL_SCOPE_REQUESTED,
      });
      expect(newState.pendingLoads.has('isSavingLabelScope')).toBe(true);
    });
  });

  describe('SAVE_LABEL_SCOPE_FULFILLED action', function () {
    it('removes "isSavingLabelScope" pending load', function () {
      const newState = reducer(mockState, {
        type: SAVE_LABEL_SCOPE_FULFILLED,
      });

      expect(newState.pendingLoads.size).toEqual(0);
      expect(newState.saveLabelScopeError).toBeNull();
    });
  });

  describe('SAVE_LABEL_SCOPE_FAILED action', function () {
    it('adds removes "isSavingLabelScope" pending load', function () {
      const newState = reducer(mockState, {
        type: SAVE_LABEL_SCOPE_FAILED,
        payload: {},
      });

      expect(newState.pendingLoads.size).toEqual(0);
      expect(newState.saveLabelScopeError).toEqual('Error');
    });

    it('clears error state on retry', function () {
      const newState = reducer(mockState, {
        type: SAVE_LABEL_SCOPE_FAILED,
        payload: {
          saveLabelScopeError: {},
        },
      });
      expect(newState.pendingLoads.size).toEqual(0);
      expect(newState.saveLabelScopeError).toEqual('Error');

      const retryState = reducer(newState, {
        type: SAVE_LABEL_SCOPE_FULFILLED,
      });
      expect(retryState.saveLabelScopeError).toBeNull();
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
        },
      });
      const newState = reducer(state, {
        type: ADD_PROPRIETARY_MATCHERS_FULFILLED,
      });
      expect(newState.setProprietaryMatchers.submitMaskState).toBe(true);
      expect(newState.setProprietaryMatchers.submitError).toBeNull();
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

  describe('TOGGLE_SHOW_REMOVE_LABEL_MODAL action', () => {
    it('toggles showRemoveLabelModal', () => {
      let newState = reducer(mockState, {
        type: TOGGLE_SHOW_REMOVE_LABEL_MODAL,
      });
      expect(newState.showRemoveLabelModal).toBe(true);

      newState = reducer(newState, {
        type: TOGGLE_SHOW_REMOVE_LABEL_MODAL,
      });
      expect(newState.showRemoveLabelModal).toBe(false);
    });
    it('clears the `removeAppliedLabelError` prop from state', () => {
      const state = {
        ...mockState,
        removeAppliedLabelError: 'Some Error',
      };
      const newState = reducer(state, {
        type: TOGGLE_SHOW_REMOVE_LABEL_MODAL,
      });
      expect(newState.removeAppliedLabelError).toBeNull();
    });
  });

  describe('SET_SELECTED_LABEL_DETAILS action', () => {
    it('set selectedLabelDetails', () => {
      let newState = reducer(mockState, {
        type: SET_SELECTED_LABEL_DETAILS,
        payload: { test: 'test' },
      });
      expect(newState.selectedLabelDetails).toEqual({ test: 'test' });
    });
  });

  describe('SELECT_COMPONENT', () => {
    it('resets current state to initialState', () => {
      const state = Object.freeze({
        isVisitingAncestor: true,
        isSavingLabelScope: true,
        offspring: {},
        labels: ['label'],
        applicableLabels: ['applicable'],
        applicableLabelScopes: ['scope'],
        loadError: 'error',
        showApplyLabelModal: true,
        selectedLabelDetails: {},
        labelScopeToSave: {},
        applicableLabelsLoadError: 'error',
        removeAppliedLabelError: 'error',
        showRemoveLabelModal: true,
        applicableLabelScopesLoadError: 'error',
        saveLabelScopeError: 'error',
        showMatchersPopover: true,
        setProprietaryMatchers: {
          submitMaskState: false,
          submitError: 'error',
          data: { pathnames: [], regex: '' },
        },
      });

      const newState = reducer(state, { type: SELECT_COMPONENT });
      expect(newState).toEqual(initialState);
    });
  });
});
