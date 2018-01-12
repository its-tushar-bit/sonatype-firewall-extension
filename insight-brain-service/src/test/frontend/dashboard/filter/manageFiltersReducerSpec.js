describe('manageFiltersReducer', function() {
  var reduce, otherObject;

  beforeEach(module('dashboardFilter'));

  beforeEach(inject(function($injector) {
    reduce = $injector.get('manageFiltersReducer');
    otherObject = {value: 'test value'};
  }));

  describe('unknown action', function() {
    it('returns original state', function() {
      var state = Object.freeze({foo: 'bar'});
      var action = {
        type: 'UNKNOWN'
      };
      var newState = reduce(state, action);
      expect(newState).toBe(state);
    });
  });

  describe('initial state', function() {
    it('is used if no state is provided', function() {
      var action = {
        type: 'UNKNOWN'
      };
      var newState = reduce(undefined, action);
      expect(newState).not.toBeUndefined();
    });
  });

  describe('UPDATE_FILTERS_FULFILLED action', function() {
    it('sets the appliedFilterName to the appliedFilterName in the payload', function() {
      var state = Object.freeze({ appliedFilterName: 'foo', other: otherObject });
      var action = {
        type: 'UPDATE_FILTERS_FULFILLED',
        payload: { appliedFilterName: 'bar' }
      };
      var newState = reduce(state, action);
      expect(newState.appliedFilterName).toBe('bar');
      expect(newState.other).toBe(otherObject); // other properties are not modified
    });
  });

  describe('FETCH_SAVED_FILTERS_FULFILLED action', function() {
    it('sets savedFilters to the payload and sets savedFilterListError to null', function() {
      var state = Object.freeze({ savedFilters: null, savedFilterListError: {}, other: otherObject });
      var action = {
        type: 'FETCH_SAVED_FILTERS_FULFILLED',
        payload: [{ name: 'foo' }, { name: 'bar' }]
      };
      var newState = reduce(state, action);
      expect(newState.savedFilters).toEqual([{ name: 'foo' }, { name: 'bar' }]);
      expect(newState.savedFilterListError).toBe(null);
      expect(newState.other).toBe(otherObject); // other properties are not modified
    });
  });

  describe('FETCH_SAVED_FILTERS_FAILED action', function() {
    it('sets savedFilterListError to the payload', function() {
      var state = Object.freeze({ savedFilterListError: null, other: otherObject });
      var error = {};
      var action = {
        type: 'FETCH_SAVED_FILTERS_FAILED',
        payload: error
      };
      var newState = reduce(state, action);
      expect(newState.savedFilterListError).toBe(error);
      expect(newState.other).toBe(otherObject); // other properties are not modified
    });
  });

  describe('APPLY_SAVED_FILTER action', function() {
    it('sets appliedFilterName from the payload', function() {
      var state = Object.freeze({ appliedFilterName: null, isManageFiltersOpen: true, other: otherObject });
      var action = {
        type: 'APPLY_SAVED_FILTER',
        payload: { name: 'foo' }
      };
      var newState = reduce(state, action);
      expect(newState.appliedFilterName).toBe('foo');
      expect(newState.other).toBe(otherObject); // other properties are not modified
    });
  });

  describe('SAVE_FILTER_REQUESTED action', function() {
    it('sets saveFilterSaving to true', function() {
      var state = Object.freeze({ saveFilterSaving: false, other: otherObject });
      var action = { type: 'SAVE_FILTER_REQUESTED' };
      var newState = reduce(state, action);
      expect(newState.saveFilterSaving).toBe(true);
      expect(newState.other).toBe(otherObject); // other properties are not modified
    });
  });

  describe('SAVE_FILTER_FULFILLED action', function() {
    it('sets saveFilterSuccess to true, savedFiltername from the payload name, and appends the payload to ' +
        'savedFilters', function() {
      var state = Object.freeze({
        saveFilterSaving: false,
        appliedFilterName: 'bar',
        savedFilters: Object.freeze([{ name: 'bar' }]),
        other: otherObject
      });
      var action = {
        type: 'SAVE_FILTER_FULFILLED',
        payload: { name: 'foo' }
      };
      var newState = reduce(state, action);
      expect(newState.savedFilters).toEqual([{ name: 'bar' }, { name: 'foo' }]);
      expect(newState.appliedFilterName).toBe('foo');
      expect(newState.saveFilterSuccess).toBe(true);
      expect(newState.other).toBe(otherObject); // other properties are not modified
    });
  });

  describe('SAVE_FILTER_FAILED action', function() {
    it('sets saveFilterSuccess and saveFilterSaving to false, and saveFilterError to the payload',
        function() {
          var state = Object.freeze({
            saveFilterSaving: true,
            saveFilterSuccess: true,
            saveFilterError: null,
            other: otherObject
          });
          var error = {};
          var action = {
            type: 'SAVE_FILTER_FAILED',
            payload: error
          };
          var newState = reduce(state, action);
          expect(newState.saveFilterSuccess).toBe(false);
          expect(newState.saveFilterSaving).toBe(false);
          expect(newState.saveFilterError).toBe(error);
          expect(newState.other).toBe(otherObject); // other properties are not modified
        }
    );
  });

  describe('DELETE_SPECIFIED_FILTERS_REQUESTED action', function() {
    it('sets deleteFiltersSaving to true', function() {
      var state = Object.freeze({ deleteFiltersSaving: false, other: otherObject });
      var action = { type: 'DELETE_SPECIFIED_FILTERS_REQUESTED' };
      var newState = reduce(state, action);
      expect(newState.deleteFiltersSaving).toBe(true);
      expect(newState.other).toBe(otherObject); // other properties are not modified
    });
  });

  describe('DELETE_SPECIFIED_FILTERS_FULFILLED action', function() {
    it('sets deleteFiltersSuccess to true', function() {
      var state = Object.freeze({ filtersToDelete: [], deleteFiltersSuccess: false, other: otherObject });
      var action = { type: 'DELETE_SPECIFIED_FILTERS_FULFILLED', payload: ['foo'] };
      var newState = reduce(state, action);
      expect(newState.deleteFiltersSuccess).toBe(true);
      expect(newState.other).toBe(otherObject); // other properties are not modified
    });

    it('resets appliedFilterName if it is in the payload', function() {
      var state = Object.freeze({
        appliedFilterName: 'bar',
        deleteFiltersSuccess: false,
        other: otherObject
      });
      var action = { type: 'DELETE_SPECIFIED_FILTERS_FULFILLED', payload: ['foo', 'bar'] };
      var newState = reduce(state, action);
      expect(newState.appliedFilterName).toBe(null);
      expect(newState.other).toBe(otherObject); // other properties are not modified
    });

    it('does not reset appliedFilterName if it is not in the payload', function() {
      var state = Object.freeze({
        filtersToDelete: ['foo', 'bar'],
        appliedFilterName: 'baz',
        deleteFiltersSuccess: false,
        other: otherObject
      });
      var action = { type: 'DELETE_SPECIFIED_FILTERS_FULFILLED', payload: ['foo', 'bar'] };
      var newState = reduce(state, action);
      expect(newState.filtersToDelete).toEqual(['foo', 'bar']);
      expect(newState.appliedFilterName).toBe('baz');
      expect(newState.other).toBe(otherObject); // other properties are not modified
    });
  });

  describe('DELETE_SPECIFIED_FILTERS_FAILED action', function() {
    it('resets deleteFiltersSaving and deleteFiltersSuccess and sets deleteFiltersError to the payload', function() {
      var error = {};
      var state = Object.freeze({
        deleteFiltersSaving: true,
        deleteFiltersSuccess: true,
        deleteFiltersError: null,
        other: otherObject
      });
      var action = {
        type: 'DELETE_SPECIFIED_FILTERS_FAILED',
        payload: error
      };
      var newState = reduce(state, action);
      expect(newState.deleteFiltersSaving).toBe(false);
      expect(newState.deleteFiltersSuccess).toBe(false);
      expect(newState.deleteFiltersError).toBe(error);
      expect(newState.other).toBe(otherObject); // other properties are not modified
    });
  });
});
