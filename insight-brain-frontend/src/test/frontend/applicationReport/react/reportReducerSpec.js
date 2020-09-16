/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import reduce from '../../../../main/frontend/applicationReport/react/reportReducer';

describe('reactReducer', () => {
  const otherObject = {value: 'test value'};

  describe('unknown action', function() {
    it('returns original state', function() {
      const state = Object.freeze({foo: 'bar'});
      const action = {type: 'UNKNOWN'};
      const newState = reduce(state, action);
      expect(newState).toBe(state);
    });
  });

  describe('initial state', function() {
    let initialState;
    beforeEach(() => {
      initialState = reduce(undefined, {type: 'UNKNOWN'});
    });

    it('is used if no state is provided', function() {
      expect(initialState).not.toBeUndefined();
    });

    it('has default fields', function() {
      expect(initialState.loading).toBe(false);
      expect(initialState.loadError).toBe(null);
      expect(initialState.metadataDetails).toBe(null);
      expect(initialState.inputState).toEqual({
        value: '',
        isPristine: true,
        trimmedValue: ''
      });
    });

    it('is immutable', function() {
      expect(() => {
        initialState.newProp = 'newProp';
      }).toThrowError(TypeError);

      expect(() => {
        initialState.loading = true;
      }).toThrowError(TypeError);

      expect(() => {
        initialState.inputState.newProp = 'newProp';
      }).toThrowError(TypeError);

      expect(() => {
        initialState.metadataDetails = {
          title: 'title'
        };
      }).toThrowError(TypeError);
    });
  });

  describe('REACT_APP_REPORT_LOAD_METADATA_REQUESTED action', () => {
    it('sets loading flag and unsets loadError and the metadata values', function() {
      const state = Object.freeze({
        loading: false,
        loadError: 'test error',
        metadataDetails: {title: 'title'},
        other: otherObject
      });
      const newState = reduce(state, {type: 'REACT_APP_REPORT_LOAD_METADATA_REQUESTED'});
      expect(newState).toEqual({
        loading: true,
        loadError: null,
        metadataDetails: null,
        other: otherObject
      });
      expect(newState.other).toBe(otherObject); // other properties are not modified
    });
  });

  describe('REACT_APP_REPORT_LOAD_METADATA_FAILED action', () => {
    it('sets loadError and unsets loading flag', function() {
      const state = Object.freeze({
        loading: true,
        loadError: null,
        other: otherObject
      });
      const newState = reduce(state, {type: 'REACT_APP_REPORT_LOAD_METADATA_FAILED', payload: 'load error!!!'});
      expect(newState).toEqual({
        loading: false,
        loadError: 'load error!!!',
        other: otherObject
      });
      expect(newState.other).toBe(otherObject); // other properties are not modified
    });
  });

});
