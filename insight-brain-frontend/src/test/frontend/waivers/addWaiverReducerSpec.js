/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import reducer from '../../../main/frontend/waivers/addWaiverReducer';
import {initialState as initState} from '@sonatype/react-shared-components/components/NxTextInput/stateHelpers';

describe('addWaiverReducer', function() {
  describe('unknown action', function() {
    it('returns original state', function() {
      const state = Object.freeze({ foo: 'bar' });
      const action = { type: 'UNKNOWN' };
      const newState = reducer(state, action);
      expect(newState).toBe(state);
    });
  });

  describe('initial state', function() {
    it('is used if no state is provided', function() {
      const action = { type: 'UNKNOWN' };
      const newState = reducer(undefined, action);
      expect(newState).not.toBeUndefined();
    });

    it('has default fields', function() {
      const action = { type: 'UNKNOWN' };
      const newState = reducer(undefined, action);
      expect(newState.loading).toBe(false);
      expect(newState.loadError).toBeNull();
      expect(newState.submitMaskState).toBeNull();
      expect(newState.submitError).toBeNull();
      expect(newState.availableWaiverScopes).toBeNull();
      expect(newState.selectedWaiverScope).toBeNull();
      expect(newState.applyToAllComponents).toBe(false);
      expect(newState.expiryTime).toBeNull();
    });

    it('calls textHelpers.initialState for waiverComments', function() {
      const textHelperInitialStateSpy = jasmine.createSpy('initialState').and.callFake((val) => ({ value: val }));
      const reducerWithMockDeps = require('inject-loader!../../../main/frontend/waivers/addWaiverReducer')({
        '@sonatype/react-shared-components/components/NxTextInput/stateHelpers': {
          initialState: textHelperInitialStateSpy
        }
      }).default;
      const action = { type: 'UNKNOWN' };
      const newState = reducerWithMockDeps(undefined, action);

      expect(textHelperInitialStateSpy).toHaveBeenCalledWith('');
      expect(newState.waiverComments).toEqual({ value: '' });
    });

    it('is immutable', function() {
      const action = {type: 'UNKNOWN'};
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

  describe('ADD_WAIVER_LOAD_DATA_REQUESTED action', function() {
    it('sets the loading prop to true', function() {
      const initialState = {
        loading: false,
        loadError: null,
        submitMaskState: null,
        submitError: null,
        waiverComments: {},
        availableWaiverScopes: null,
        selectedWaiverScope: null,
        applyToAllComponents: false,
        violationDetails: null,
        otherProp: { prop: 'foo' }
      };

      const newState = reducer(initialState, { type: 'ADD_WAIVER_LOAD_DATA_REQUESTED' });

      expect(newState.loading).toBe(true);
      expect(newState.otherProp).toBe(initialState.otherProp);
    });
  });

  describe('ADD_WAIVER_LOAD_DATA_FULFILLED action', function() {
    const initialState = {
      loading: true,
      loadError: null,
      submitMaskState: null,
      submitError: null,
      waiverComments: {},
      availableWaiverScopes: null,
      selectedWaiverScope: null,
      applyToAllComponents: false,
      violationDetails: null,
      otherProp: { prop: 'foo' }
    };
    const action = {
      type: 'ADD_WAIVER_LOAD_DATA_FULFILLED',
      payload: ['target1', 'target2']
    };
    const payload = ['target1', 'target2'];

    it('unsets the loading flag', function() {
      const newState = reducer(initialState, action);
      expect(newState.loading).toBe(false);
      expect(newState.otherProp).toBe(initialState.otherProp);
    });

    it('sets the availableWaiverScopes and chooses the first element as selectedWaiverScope', function() {
      const newState = reducer(initialState, action);
      expect(newState.availableWaiverScopes).toEqual(payload);
      expect(newState.selectedWaiverScope).toEqual(payload[0]);
      expect(newState.otherProp).toBe(initialState.otherProp);
    });
  });

  describe('ADD_WAIVER_LOAD_DATA_FAILED action', function() {
    it('unsets the loading flag and sets the loadError', function() {
      const initialState = {
        loading: true,
        loadError: null,
        submitMaskState: null,
        submitError: null,
        waiverComments: {},
        availableWaiverScopes: null,
        selectedWaiverScope: null,
        applyToAllComponents: false,
        violationDetails: null,
        otherProp: { prop: 'foo' }
      };

      const newState = reducer(initialState, {
        type: 'ADD_WAIVER_LOAD_DATA_FAILED',
        payload: 'Err'
      });

      expect(newState.loading).toBe(false);
      expect(newState.loadError).toBe('Err');
      expect(newState.otherProp).toBe(initialState.otherProp);
    });
  });

  describe('ADD_WAIVER_SAVE_REQUESTED action', function() {
    it('sets submitMaskState to false and unsets submitError', function() {
      const initialState = {
        loading: false,
        loadError: null,
        submitMaskState: null,
        submitError: 'Foo',
        waiverComments: {},
        availableWaiverScopes: null,
        selectedWaiverScope: null,
        applyToAllComponents: false,
        violationDetails: null,
        otherProp: { prop: 'foo' }
      };

      const newState = reducer(initialState, { type: 'ADD_WAIVER_SAVE_REQUESTED' });

      expect(newState.submitMaskState).toBe(false);
      expect(newState.submitError).toBeNull();
      expect(newState.otherProp).toBe(initialState.otherProp);
    });
  });

  describe('ADD_WAIVER_SAVE_FULFILLED action', function() {
    it('sets submitMaskState to true', function() {
      const initialState = {
        loading: false,
        loadError: null,
        submitMaskState: null,
        submitError: null,
        waiverComments: {},
        availableWaiverScopes: null,
        selectedWaiverScope: null,
        applyToAllComponents: false,
        violationDetails: null,
        otherProp: { prop: 'foo' }
      };

      const newState = reducer(initialState, { type: 'ADD_WAIVER_SAVE_FULFILLED' });

      expect(newState.submitMaskState).toBe(true);
      expect(newState.otherProp).toBe(initialState.otherProp);
    });
  });

  describe('ADD_WAIVER_SAVE_FAILED action', function() {
    it('unsets submitMaskState and sets the submitError', function() {
      const initialState = {
        loading: false,
        loadError: null,
        submitMaskState: false,
        submitError: null,
        waiverComments: {},
        availableWaiverScopes: null,
        selectedWaiverScope: null,
        applyToAllComponents: false,
        violationDetails: null,
        otherProp: { prop: 'foo' }
      };

      const newState = reducer(initialState, {
        type: 'ADD_WAIVER_SAVE_FAILED',
        payload: 'Err'
      });

      expect(newState.submitMaskState).toBeNull();
      expect(newState.submitError).toBe('Err');
      expect(newState.otherProp).toBe(initialState.otherProp);
    });
  });

  describe('ADD_WAIVER_SUBMIT_MASK_TIMER_DONE action', function() {
    it('unsets submitMaskState', function() {
      const initialState = {
        loading: false,
        loadError: null,
        submitMaskState: false,
        submitError: null,
        waiverComments: {},
        availableWaiverScopes: null,
        selectedWaiverScope: null,
        applyToAllComponents: false,
        violationDetails: null,
        otherProp: { prop: 'foo' }
      };
      const expectedNewState = {
        ...initialState,
        submitMaskState: null
      };

      let newState = reducer(initialState, { type: 'ADD_WAIVER_SUBMIT_MASK_TIMER_DONE' });
      expect(newState).toEqual(expectedNewState);
      expect(newState.otherProp).toBe(initialState.otherProp);

      newState = reducer({ ...initialState, submitMaskState: true }, {
        type: 'ADD_WAIVER_SUBMIT_MASK_TIMER_DONE'
      });
      expect(newState).toEqual(expectedNewState);
      expect(newState.otherProp).toBe(initialState.otherProp);
    });
  });

  describe('ADD_WAIVER_SET_WAIVER_COMMENT action', function() {
    it('sets the waiverComments prop', function() {
      const textHelperUserInputSpy = jasmine.createSpy('userInput').and.callFake((validator, val) => ({ value: val }));
      const reducerWithMockDeps = require('inject-loader!../../../main/frontend/waivers/addWaiverReducer')({
        '@sonatype/react-shared-components/components/NxTextInput/stateHelpers': {
          userInput: textHelperUserInputSpy,
          initialState: () => {}
        }
      }).default;
      const initialState = {
        loading: false,
        loadError: null,
        submitMaskState: false,
        submitError: null,
        waiverComments: {},
        availableWaiverScopes: null,
        selectedWaiverScope: null,
        applyToAllComponents: false,
        violationDetails: null,
        otherProp: { prop: 'foo' }
      };

      const newState = reducerWithMockDeps(initialState, {
        type: 'ADD_WAIVER_SET_WAIVER_COMMENT',
        payload: 'Bar'
      });

      expect(textHelperUserInputSpy).toHaveBeenCalledWith(null, 'Bar');
      expect(newState.waiverComments).toEqual({ value: 'Bar' });
      expect(newState.otherProp).toBe(initialState.otherProp);
    });
  });

  describe('ADD_WAIVER_SET_WAIVER_SCOPE action', function() {
    it('sets the selectedWaiverScope', function() {
      const initialState = {
        loading: false,
        loadError: null,
        submitMaskState: false,
        submitError: null,
        waiverComments: {},
        availableWaiverScopes: null,
        selectedWaiverScope: null,
        applyToAllComponents: false,
        violationDetails: null,
        otherProp: { prop: 'foo' }
      };

      const newState = reducer(initialState, {
        type: 'ADD_WAIVER_SET_WAIVER_SCOPE',
        payload: 'target'
      });

      expect(newState.selectedWaiverScope).toBe('target');
      expect(newState.otherProp).toBe(initialState.otherProp);
    });
  });

  describe('ADD_WAIVER_SET_APPLY_TO_ALL_COMPONENTS action', function() {
    it('sets applyToAllComponents prop', function() {
      const initialState = {
        loading: false,
        loadError: null,
        submitMaskState: false,
        submitError: null,
        waiverComments: {},
        availableWaiverScopes: null,
        selectedWaiverScope: null,
        applyToAllComponents: null,
        violationDetails: null,
        otherProp: { prop: 'foo' }
      };

      let newState = reducer(initialState, {
        type: 'ADD_WAIVER_SET_APPLY_TO_ALL_COMPONENTS',
        payload: true
      });

      expect(newState.applyToAllComponents).toBe(true);
      expect(newState.otherProp).toBe(initialState.otherProp);

      newState = reducer(initialState, {
        type: 'ADD_WAIVER_SET_APPLY_TO_ALL_COMPONENTS',
        payload: false
      });

      expect(newState.applyToAllComponents).toBe(false);
      expect(newState.otherProp).toBe(initialState.otherProp);
    });
  });

  describe('ADD_WAIVER_SET_EXPIRY_TIME action', function() {
    it('sets the expiryTime prop in the state', function() {
      const initialState = {
        loading: false,
        loadError: null,
        submitMaskState: false,
        submitError: null,
        waiverComments: {},
        availableWaiverScopes: null,
        selectedWaiverScope: null,
        applyToAllComponents: null,
        violationDetails: null,
        expiryTime: null,
        otherProp: { prop: 'foo' }
      };

      let newState = reducer(initialState, {
        type: 'ADD_WAIVER_SET_EXPIRY_TIME',
        payload: '7'
      });
      expect(newState.expiryTime).toEqual('7');
      expect(newState.otherProp).toBe(initialState.otherProp);

      newState = reducer(initialState, {
        type: 'ADD_WAIVER_SET_EXPIRY_TIME',
        payload: '60'
      });

      expect(newState.expiryTime).toEqual('60');
      expect(newState.otherProp).toBe(initialState.otherProp);

      newState = reducer(initialState, {
        type: 'ADD_WAIVER_SET_EXPIRY_TIME',
        payload: null
      });

      expect(newState.expiryTime).toEqual(null);
      expect(newState.otherProp).toBe(initialState.otherProp);

      newState = reducer(initialState, {
        type: 'ADD_WAIVER_SET_EXPIRY_TIME',
        payload: 'never'
      });

      expect(newState.expiryTime).toEqual('never');
      expect(newState.otherProp).toBe(initialState.otherProp);
    });
  });

  describe('UI_ROUTER_ON_FINISH action', function() {
    it('clears state on onFinish', function() {
      const currentState = {
        loading: true,
        loadError: 'load error',
        submitMaskState: true,
        submitError: 'submit error',
        waiverComments: initState('A comment'),
        availableWaiverScopes: 'abc',
        selectedWaiverScope: 'pqr',
        applyToAllComponents: true,
        violationDetails: 'xyz',
        otherProp: { prop: 'foo' }
      };

      const newState = reducer(currentState, {
        type: '@@reduxUiRouter/onFinish'
      });

      expect(newState.loading).toBe(false);
      expect(newState.loadError).toBeNull();
      expect(newState.submitMaskState).toBeNull();
      expect(newState.submitError).toBeNull();
      expect(newState.availableWaiverScopes).toBeNull();
      expect(newState.selectedWaiverScope).toBeNull();
      expect(newState.applyToAllComponents).toBe(false);
      expect(newState.waiverComments).toEqual(initState(''));
      expect(newState.otherProp).toBeUndefined();
    });
  });
});
