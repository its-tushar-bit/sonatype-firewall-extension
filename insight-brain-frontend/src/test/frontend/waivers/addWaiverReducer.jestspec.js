/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import reducer from '../../../main/frontend/waivers/addWaiverReducer';
import { nxDateInputStateHelpers, nxTextInputStateHelpers } from '@sonatype/react-shared-components';

describe('addWaiverReducer', function () {
  describe('unknown action', function () {
    it('returns original state', function () {
      const state = Object.freeze({ foo: 'bar' });
      const action = { type: 'UNKNOWN' };
      const newState = reducer(state, action);
      expect(newState).toBe(state);
    });
  });

  describe('initial state', function () {
    it('is used if no state is provided', function () {
      const action = { type: 'UNKNOWN' };
      const newState = reducer(undefined, action);
      expect(newState).not.toBeUndefined();
    });

    it('has default fields', function () {
      const action = { type: 'UNKNOWN' };
      const newState = reducer(undefined, action);
      expect(newState.loading).toBe(false);
      expect(newState.loadError).toBeNull();
      expect(newState.submitMaskState).toBeNull();
      expect(newState.submitError).toBeNull();
      expect(newState.availableWaiverScopes).toBeNull();
      expect(newState.selectedWaiverScope).toBeNull();
      expect(newState.componentMatcherStrategy).toBe('EXACT_COMPONENT');
      expect(newState.expiryTime).toBeNull();
      expect(newState.fieldsPristineState).toBeNull();
    });

    it('initializes waiverComments with the specified value, isPristine true, and no validation errors', function () {
      const action = { type: 'UNKNOWN' };
      const newState = reducer(undefined, action);

      expect(newState.waiverComments).toEqual({
        value: '',
        trimmedValue: '',
        isPristine: true,
        validationErrors: null,
      });
    });

    it('is immutable', function () {
      const action = { type: 'UNKNOWN' };
      const state = reducer(undefined, action);

      expect(() => {
        state.newProp = 'newProp';
      }).toThrowError(TypeError);

      expect(() => {
        state.violationDetails = [];
      }).toThrowError(TypeError);

      expect(() => {
        state.loading = true;
      }).toThrowError(TypeError);

      expect(() => {
        state.waiverComments.isPristine = false;
      }).toThrowError(TypeError);
    });
  });

  describe('WAIVERS_LOAD_ADD_WAIVER_DATA_REQUESTED action', function () {
    it('sets the loading prop to true', function () {
      const initialState = {
        loading: false,
        loadError: null,
        submitMaskState: null,
        submitError: null,
        waiverComments: {},
        availableWaiverScopes: null,
        selectedWaiverScope: null,
        componentMatcherStrategy: 'EXACT_COMPONENT',
        violationDetails: null,
        otherProp: { prop: 'foo' },
      };

      const newState = reducer(initialState, {
        type: 'WAIVERS_LOAD_ADD_WAIVER_DATA_REQUESTED',
      });

      expect(newState.loading).toBe(true);
      expect(newState.otherProp).toBe(initialState.otherProp);
    });
  });

  describe('WAIVERS_LOAD_ADD_WAIVER_DATA_FULFILLED action', function () {
    const initialState = {
      loading: true,
      loadError: null,
      submitMaskState: null,
      submitError: null,
      waiverComments: {},
      availableWaiverScopes: null,
      selectedWaiverScope: null,
      componentMatcherStrategy: 'EXACT_COMPONENT',
      violationDetails: null,
      otherProp: { prop: 'foo' },
    };
    const payload = { waiverTargets: ['target1', 'target2'] };
    const action = {
      type: 'WAIVERS_LOAD_ADD_WAIVER_DATA_FULFILLED',
      payload,
    };

    it('unsets the loading flag', function () {
      const newState = reducer(initialState, action);
      expect(newState.loading).toBe(false);
      expect(newState.otherProp).toBe(initialState.otherProp);
    });

    it('sets the availableWaiverScopes and chooses the first element as selectedWaiverScope', function () {
      const newState = reducer(initialState, action);
      expect(newState.availableWaiverScopes).toEqual(payload.waiverTargets);
      expect(newState.selectedWaiverScope).toEqual(payload.waiverTargets[0]);
      expect(newState.otherProp).toBe(initialState.otherProp);
    });

    it('creates a snapshot of the pristine fields using the received data', function () {
      const newState = reducer(initialState, action);
      expect(newState.fieldsPristineState).toEqual({
        selectedWaiverScope: 'target1',
        componentMatcherStrategy: 'EXACT_COMPONENT',
        waiverReasonId: null,
        expiryTime: null,
        waiverComments: '',
      });
    });

    it('sets the comments for the waiver if they are passed by the url, decoding them if necessary', () => {
      const payload = { waiverTargets: ['target1', 'target2'], comments: 'preloaded%20Comment' };
      const action = {
        type: 'WAIVERS_LOAD_ADD_WAIVER_DATA_FULFILLED',
        payload,
      };
      const newState = reducer(initialState, action);
      expect(newState.fieldsPristineState).toEqual({
        selectedWaiverScope: 'target1',
        componentMatcherStrategy: 'EXACT_COMPONENT',
        expiryTime: null,
        waiverReasonId: null,
        waiverComments: 'preloaded Comment',
      });
      expect(newState.waiverComments).toEqual({
        value: 'preloaded Comment',
        isPristine: true,
        trimmedValue: 'preloaded Comment',
        validationErrors: null,
      });
    });
  });

  describe('WAIVERS_LOAD_ADD_WAIVER_DATA_FAILED action', function () {
    it('unsets the loading flag and sets the loadError', function () {
      const initialState = {
        loading: true,
        loadError: null,
        submitMaskState: null,
        submitError: null,
        waiverComments: {},
        availableWaiverScopes: null,
        selectedWaiverScope: null,
        componentMatcherStrategy: 'EXACT_COMPONENT',
        violationDetails: null,
        otherProp: { prop: 'foo' },
      };

      const newState = reducer(initialState, {
        type: 'WAIVERS_LOAD_ADD_WAIVER_DATA_FAILED',
        payload: 'Err',
      });

      expect(newState.loading).toBe(false);
      expect(newState.loadError).toBe('Err');
      expect(newState.otherProp).toBe(initialState.otherProp);
    });
  });

  describe('WAIVERS_SAVE_WAIVER_REQUESTED action', function () {
    it('sets submitMaskState to false and unsets submitError', function () {
      const initialState = {
        loading: false,
        loadError: null,
        submitMaskState: null,
        submitError: 'Foo',
        waiverComments: {},
        availableWaiverScopes: null,
        selectedWaiverScope: null,
        componentMatcherStrategy: 'EXACT_COMPONENT',
        violationDetails: null,
        otherProp: { prop: 'foo' },
      };

      const newState = reducer(initialState, {
        type: 'WAIVERS_SAVE_WAIVER_REQUESTED',
      });

      expect(newState.submitMaskState).toBe(false);
      expect(newState.submitError).toBeNull();
      expect(newState.otherProp).toBe(initialState.otherProp);
    });
  });

  describe('WAIVERS_SAVE_WAIVER_FULFILLED action', function () {
    it('sets submitMaskState to true', function () {
      const initialState = {
        isDirty: false,
        loading: false,
        loadError: null,
        submitMaskState: null,
        submitError: null,
        waiverComments: {},
        availableWaiverScopes: null,
        selectedWaiverScope: null,
        componentMatcherStrategy: 'EXACT_COMPONENT',
        violationDetails: null,
        otherProp: { prop: 'foo' },
      };

      const newState = reducer(initialState, {
        type: 'WAIVERS_SAVE_WAIVER_FULFILLED',
      });

      expect(newState.submitMaskState).toBe(true);
      expect(newState.otherProp).toBe(initialState.otherProp);
    });
  });

  describe('WAIVERS_SAVE_WAIVER_FAILED action', function () {
    it('unsets submitMaskState and sets the submitError', function () {
      const initialState = {
        loading: false,
        loadError: null,
        submitMaskState: false,
        submitError: null,
        waiverComments: {},
        availableWaiverScopes: null,
        selectedWaiverScope: null,
        componentMatcherStrategy: 'EXACT_COMPONENT',
        violationDetails: null,
        otherProp: { prop: 'foo' },
      };

      const newState = reducer(initialState, {
        type: 'WAIVERS_SAVE_WAIVER_FAILED',
        payload: 'Err',
      });

      expect(newState.submitMaskState).toBeNull();
      expect(newState.submitError).toBe('Err');
      expect(newState.otherProp).toBe(initialState.otherProp);
    });
  });

  describe('WAIVERS_ADD_WAIVER_SUBMIT_MASK_TIMER_DONE action', function () {
    it('unsets submitMaskState', function () {
      const initialState = {
        loading: false,
        loadError: null,
        submitMaskState: false,
        submitError: null,
        waiverComments: {},
        availableWaiverScopes: null,
        selectedWaiverScope: null,
        componentMatcherStrategy: 'EXACT_COMPONENT',
        violationDetails: null,
        otherProp: { prop: 'foo' },
      };
      const expectedNewState = {
        ...initialState,
        submitMaskState: null,
      };

      let newState = reducer(initialState, {
        type: 'WAIVERS_ADD_WAIVER_SUBMIT_MASK_TIMER_DONE',
      });
      expect(newState).toEqual(expectedNewState);
      expect(newState.otherProp).toBe(initialState.otherProp);

      newState = reducer(
        { ...initialState, submitMaskState: true },
        {
          type: 'WAIVERS_ADD_WAIVER_SUBMIT_MASK_TIMER_DONE',
        }
      );
      expect(newState).toEqual(expectedNewState);
      expect(newState.otherProp).toBe(initialState.otherProp);
    });
  });

  describe('WAIVERS_ADD_WAIVER_SET_WAIVER_COMMENT action', function () {
    it('sets waiverComments and isDiry props', function () {
      const initialState = {
        isDirty: false,
        loading: false,
        loadError: null,
        submitMaskState: false,
        submitError: null,
        waiverComments: {},
        availableWaiverScopes: null,
        selectedWaiverScope: null,
        componentMatcherStrategy: 'EXACT_COMPONENT',
        violationDetails: null,
        otherProp: { prop: 'foo' },
        fieldsPristineState: {
          selectedWaiverScope: null,
          componentMatcherStrategy: 'EXACT_COMPONENT',
          expiryTime: null,
          waiverReasonId: null,
          waiverComments: '',
        },
      };

      const newState = reducer(initialState, {
        type: 'WAIVERS_ADD_WAIVER_SET_WAIVER_COMMENT',
        payload: 'Bar',
      });

      expect(newState.waiverComments).toEqual({
        value: 'Bar',
        trimmedValue: 'Bar',
        isPristine: false,
        validationErrors: null,
      });
      expect(newState.otherProp).toBe(initialState.otherProp);
      expect(newState.isDirty).toBe(true);
    });
  });

  describe('WAIVERS_ADD_WAIVER_SET_WAIVER_SCOPE action', function () {
    it('sets selectedWaiverScope and isDirty props', function () {
      const initialState = {
        isDirty: false,
        loading: false,
        loadError: null,
        submitMaskState: false,
        submitError: null,
        waiverComments: {},
        availableWaiverScopes: null,
        selectedWaiverScope: null,
        componentMatcherStrategy: 'EXACT_COMPONENT',
        violationDetails: null,
        otherProp: { prop: 'foo' },
        fieldsPristineState: {
          selectedWaiverScope: null,
          componentMatcherStrategy: 'EXACT_COMPONENT',
          waiverReasonId: null,
          expiryTime: null,
          waiverComments: '',
        },
      };

      const newState = reducer(initialState, {
        type: 'WAIVERS_ADD_WAIVER_SET_WAIVER_SCOPE',
        payload: 'target',
      });

      expect(newState.selectedWaiverScope).toBe('target');
      expect(newState.otherProp).toBe(initialState.otherProp);
      expect(newState.isDirty).toBe(true);
    });
  });

  describe('WAIVERS_ADD_WAIVER_SET_COMPONENT_MATCHER_STRATEGY action', function () {
    it('sets componentMatcherStrategy and isDirty props', function () {
      const initialState = {
        loading: false,
        loadError: null,
        submitMaskState: false,
        submitError: null,
        waiverComments: {},
        availableWaiverScopes: null,
        selectedWaiverScope: null,
        componentMatcherStrategy: 'EXACT_COMPONENT',
        violationDetails: null,
        otherProp: { prop: 'foo' },
        fieldsPristineState: {
          selectedWaiverScope: null,
          componentMatcherStrategy: 'EXACT_COMPONENT',
          waiverReasonId: null,
          expiryTime: null,
          waiverComments: '',
        },
      };

      let newState = reducer(initialState, {
        type: 'WAIVERS_ADD_WAIVER_SET_COMPONENT_MATCHER_STRATEGY',
        payload: 'ALL_COMPONENTS',
      });

      expect(newState.componentMatcherStrategy).toBe('ALL_COMPONENTS');
      expect(newState.otherProp).toBe(initialState.otherProp);
      expect(newState.isDirty).toBe(true);

      newState = reducer(initialState, {
        type: 'WAIVERS_ADD_WAIVER_SET_COMPONENT_MATCHER_STRATEGY',
        payload: 'ALL_VERSIONS',
      });

      expect(newState.componentMatcherStrategy).toBe('ALL_VERSIONS');
      expect(newState.otherProp).toBe(initialState.otherProp);
      expect(newState.isDirty).toBe(true);

      newState = reducer(initialState, {
        type: 'WAIVERS_ADD_WAIVER_SET_COMPONENT_MATCHER_STRATEGY',
        payload: 'EXACT_COMPONENT',
      });

      expect(newState.componentMatcherStrategy).toBe('EXACT_COMPONENT');
      expect(newState.otherProp).toBe(initialState.otherProp);
      expect(newState.isDirty).toBe(true);
    });
  });

  describe('WAIVERS_ADD_WAIVER_SET_EXPIRY_TIME action', function () {
    it('sets expiryTime and isDirty props in the state', function () {
      const initialState = {
        isDirty: true,
        loading: false,
        loadError: null,
        submitMaskState: false,
        submitError: null,
        waiverComments: {},
        availableWaiverScopes: null,
        selectedWaiverScope: null,
        componentMatcherStrategy: 'EXACT_COMPONENT',
        violationDetails: null,
        expiryTime: null,
        customExpiryTime: nxDateInputStateHelpers.initialState(''),
        otherProp: { prop: 'foo' },
        fieldsPristineState: {
          selectedWaiverScope: null,
          componentMatcherStrategy: 'EXACT_COMPONENT',
          waiverReasonId: null,
          expiryTime: null,
          waiverComments: '',
        },
      };

      let newState = reducer(initialState, {
        type: 'WAIVERS_ADD_WAIVER_SET_EXPIRY_TIME',
        payload: '7',
      });
      expect(newState.isDirty).toBe(true);
      expect(newState.expiryTime).toEqual('7');
      expect(newState.customExpiryTime).toEqual({
        isPristine: true,
        value: '',
        trimmedValue: '',
        validationErrors: null,
      });
      expect(newState.otherProp).toBe(initialState.otherProp);

      newState = reducer(initialState, {
        type: 'WAIVERS_ADD_WAIVER_SET_EXPIRY_TIME',
        payload: '60',
      });

      expect(newState.isDirty).toBe(true);
      expect(newState.expiryTime).toEqual('60');
      expect(newState.customExpiryTime).toEqual({
        isPristine: true,
        value: '',
        trimmedValue: '',
        validationErrors: null,
      });
      expect(newState.otherProp).toBe(initialState.otherProp);

      newState = reducer(initialState, {
        type: 'WAIVERS_ADD_WAIVER_SET_EXPIRY_TIME',
        payload: null,
      });

      expect(newState.isDirty).toBe(true);
      expect(newState.expiryTime).toEqual(null);
      expect(newState.customExpiryTime).toEqual({
        isPristine: true,
        value: '',
        trimmedValue: '',
        validationErrors: null,
      });
      expect(newState.otherProp).toBe(initialState.otherProp);

      newState = reducer(initialState, {
        type: 'WAIVERS_ADD_WAIVER_SET_EXPIRY_TIME',
        payload: 'never',
      });

      expect(newState.isDirty).toBe(true);
      expect(newState.expiryTime).toEqual('never');
      expect(newState.customExpiryTime).toEqual({
        isPristine: true,
        value: '',
        trimmedValue: '',
        validationErrors: null,
      });
      expect(newState.otherProp).toBe(initialState.otherProp);

      newState = reducer(initialState, {
        type: 'WAIVERS_ADD_WAIVER_SET_EXPIRY_TIME',
        payload: 'custom',
      });

      expect(newState.isDirty).toBe(true);
      expect(newState.expiryTime).toEqual('custom');
      expect(newState.customExpiryTime).toEqual({
        isPristine: true,
        value: '',
        trimmedValue: '',
        validationErrors: null,
      });
      expect(newState.otherProp).toBe(initialState.otherProp);
    });
  });

  describe('WAIVERS_ADD_WAIVER_SET_REASON action', function () {
    it('sets waiverReasonId and isDirty props in the state', function () {
      const initialState = {
        isDirty: true,
        loading: false,
        loadError: null,
        submitMaskState: false,
        submitError: null,
        waiverComments: {},
        availableWaiverScopes: null,
        selectedWaiverScope: null,
        componentMatcherStrategy: 'EXACT_COMPONENT',
        violationDetails: null,
        expiryTime: null,
        waiverReasonId: null,
        customExpiryTime: nxDateInputStateHelpers.initialState(''),
        otherProp: { prop: 'foo' },
        fieldsPristineState: {
          selectedWaiverScope: null,
          componentMatcherStrategy: 'EXACT_COMPONENT',
          waiverReasonId: null,
          expiryTime: null,
          waiverComments: '',
        },
      };

      let newState = reducer(initialState, {
        type: 'WAIVERS_ADD_WAIVER_SET_REASON',
        payload: 'waiverReasonId Id',
      });
      expect(newState.isDirty).toBe(true);
      expect(newState.waiverReasonId).toEqual('waiverReasonId Id');
      expect(newState.otherProp).toBe(initialState.otherProp);

      newState = reducer(initialState, {
        type: 'WAIVERS_ADD_WAIVER_SET_REASON',
        payload: null,
      });

      expect(newState.isDirty).toBe(true);
      expect(newState.expiryTime).toEqual(null);
      expect(newState.otherProp).toBe(initialState.otherProp);
    });
  });

  describe('WAIVERS_ADD_WAIVER_SET_CUSTOM_EXPIRY_TIME action', () => {
    let initialState, newState;
    beforeEach(function () {
      initialState = {
        isDirty: false,
        loading: false,
        loadError: null,
        submitMaskState: null,
        submitError: null,
        showUnsavedChangesModal: false,
        waiverComments: {},
        availableWaiverScopes: null,
        selectedWaiverScope: null,
        applyToAllComponents: false,
        expiryTime: null,
        customExpiryTime: nxDateInputStateHelpers.initialState(''),
        fieldsPristineState: null,
      };
    });

    it('sets customExpiryTime prop in the state', () => {
      newState = reducer(initialState, {
        type: 'WAIVERS_ADD_WAIVER_SET_CUSTOM_EXPIRY_TIME',
        payload: '3049-11-11',
      });

      expect(newState.customExpiryTime).toEqual({
        isPristine: false,
        value: '3049-11-11',
        trimmedValue: '3049-11-11',
        validationErrors: null,
      });

      newState = reducer(initialState, {
        type: 'WAIVERS_ADD_WAIVER_SET_CUSTOM_EXPIRY_TIME',
        payload: '1000-11-11',
      });

      expect(newState.customExpiryTime).toEqual({
        isPristine: false,
        value: '1000-11-11',
        trimmedValue: '1000-11-11',
        validationErrors: 'Date must be in the future',
      });
    });

    it('sets customExpiryTime validation error to "Date must be in the future" when payload is a past date', () => {
      newState = reducer(initialState, {
        type: 'WAIVERS_ADD_WAIVER_SET_CUSTOM_EXPIRY_TIME',
        payload: '1000-11-11',
      });

      expect(newState.customExpiryTime).toEqual({
        isPristine: false,
        value: '1000-11-11',
        trimmedValue: '1000-11-11',
        validationErrors: 'Date must be in the future',
      });
    });
  });

  describe('UI_ROUTER_ON_FINISH action', function () {
    it('clears state on onFinish', function () {
      const currentState = {
        loading: true,
        loadError: 'load error',
        submitMaskState: true,
        submitError: 'submit error',
        waiverComments: nxTextInputStateHelpers.initialState('A comment'),
        availableWaiverScopes: 'abc',
        selectedWaiverScope: 'pqr',
        componentMatcherStrategy: 'ALL_COMPONENTS',
        violationDetails: 'xyz',
        otherProp: { prop: 'foo' },
      };

      const newState = reducer(currentState, {
        type: '@@reduxUiRouter/onFinish',
      });

      expect(newState.loading).toBe(false);
      expect(newState.loadError).toBeNull();
      expect(newState.submitMaskState).toBeNull();
      expect(newState.submitError).toBeNull();
      expect(newState.availableWaiverScopes).toBeNull();
      expect(newState.selectedWaiverScope).toBeNull();
      expect(newState.componentMatcherStrategy).toBe('EXACT_COMPONENT');
      expect(newState.waiverComments).toEqual({
        value: '',
        trimmedValue: '',
        isPristine: true,
        validationErrors: null,
      });
      expect(newState.otherProp).toBeUndefined();
    });
  });
});
