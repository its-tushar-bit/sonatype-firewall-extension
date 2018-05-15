describe('applicationReportReducer', function() {
  var reduce, otherObject;

  beforeEach(module('applicationReportModule'));

  beforeEach(inject(function($injector) {
    reduce = $injector.get('applicationReportReducer');
    otherObject = {value: 'test value'};
  }));

  describe('unknown action', function() {
    it('returns original state', function() {
      var state = Object.freeze({foo: 'bar'});
      var action = {type: 'UNKNOWN'};
      var newState = reduce(state, action);
      expect(newState).toBe(state);
    });
  });

  describe('initial state', function() {
    it('is used if no state is provided', function() {
      var action = {type: 'UNKNOWN'};
      var newState = reduce(undefined, action);
      expect(newState).not.toBeUndefined();
    });

    it('has default fields', function() {
      var action = {type: 'UNKNOWN'};
      var newState = reduce(undefined, action);
      expect(newState.loading).toBe(false);
      expect(newState.loadError).toBe(null);
      expect(newState.selectedReport).toBe(null);
    });
  });

  describe('LOAD_REPORT_REQUESTED action', function() {
    it('sets loading flag and unsets error and selectedReport values', function() {
      var state = Object.freeze({
        loading: false,
        loadError: 'test error',
        selectedReport: 'test report',
        other: otherObject
      });
      var newState = reduce(state, {type: 'LOAD_REPORT_REQUESTED'});
      expect(newState).toEqual({
        loading: true,
        loadError: null,
        selectedReport: null,
        other: otherObject
      });
      expect(newState.other).toBe(otherObject); // other properties are not modified
    });
  });

  describe('LOAD_REPORT_FULFILLED action', function() {
    it('unsets loading flag and sets selectedReport value', function() {
      var state = Object.freeze({
        loading: true,
        loadError: null,
        selectedReport: null,
        other: otherObject
      });
      var newState = reduce(state, {type: 'LOAD_REPORT_FULFILLED', payload: 'report'});
      expect(newState).toEqual({
        loading: false,
        loadError: null,
        selectedReport: 'report',
        other: otherObject
      });
      expect(newState.other).toBe(otherObject); // other properties are not modified
    });
  });

  describe('LOAD_REPORT_FAILED action', function() {
    it('unsets loading flag and sets error value', function() {
      var state = Object.freeze({
        loading: true,
        loadError: null,
        selectedReport: null,
        other: otherObject
      });
      var newState = reduce(state, {type: 'LOAD_REPORT_FAILED', payload: 'test error'});
      expect(newState).toEqual({
        loading: false,
        loadError: 'test error',
        selectedReport: null,
        other: otherObject
      });
      expect(newState.other).toBe(otherObject); // other properties are not modified
    });
  });

});
