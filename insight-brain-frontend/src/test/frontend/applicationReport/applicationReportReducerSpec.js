import applicationReportModule from '../../../main/frontend/applicationReport/module';

describe('applicationReportReducer', function() {
  let reduce;
  const otherObject = {value: 'test value'};

  beforeEach(angular.mock.module(applicationReportModule.name));

  beforeEach(inject(function($injector) {
    reduce = $injector.get('applicationReportReducer');
  }));

  describe('unknown action', function() {
    it('returns original state', function() {
      const state = Object.freeze({foo: 'bar'});
      const action = {type: 'UNKNOWN'};
      const newState = reduce(state, action);
      expect(newState).toBe(state);
    });
  });

  describe('initial state', function() {
    it('is used if no state is provided', function() {
      const action = {type: 'UNKNOWN'};
      const newState = reduce(undefined, action);
      expect(newState).not.toBeUndefined();
    });

    it('has default fields', function() {
      const action = {type: 'UNKNOWN'};
      const newState = reduce(undefined, action);
      expect(newState.loading).toBe(false);
      expect(newState.loadError).toBe(null);
      expect(newState.selectedReport).toBe(null);
    });
  });

  describe('LOAD_REPORT_REQUESTED action', function() {
    it('sets loading flag and unsets error and selectedReport values', function() {
      const state = Object.freeze({
        loading: false,
        loadError: 'test error',
        selectedReport: 'test report',
        other: otherObject
      });
      const newState = reduce(state, {type: 'LOAD_REPORT_REQUESTED'});
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
      const state = Object.freeze({
        loading: true,
        loadError: null,
        selectedReport: null,
        other: otherObject
      });
      const entries = [
        {policyThreatLevel: 1}, {policyThreatLevel: 3}, {policyThreatLevel: 4, waived: true}, {policyThreatLevel: 6},
        {policyThreatLevel: 9}, {policyThreatLevel: 10, grandfathered: true}
      ];
      const newState = reduce(state, {
        type: 'LOAD_REPORT_FULFILLED',
        payload: {allEntries: entries}
      });
      expect(newState).toEqual({
        loading: false,
        loadError: null,
        selectedReport: {
          allEntries: entries,
          displayedEntries: entries,
          moderateViolationCount: 1,
          severeViolationCount: 1,
          criticalViolationCount: 1,
          nonLowViolationCount: 3
        },
        other: otherObject
      });
      expect(newState.other).toBe(otherObject); // other properties are not modified
    });
  });

  describe('LOAD_REPORT_FAILED action', function() {
    it('unsets loading flag and sets error value', function() {
      const state = Object.freeze({
        loading: true,
        loadError: null,
        selectedReport: null,
        other: otherObject
      });
      const newState = reduce(state, {type: 'LOAD_REPORT_FAILED', payload: 'test error'});
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
