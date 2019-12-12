import dashboardFilterModule from '../../../../main/frontend/dashboard/filter/module';

describe('manageFiltersReducer', function() {
  var reduce, otherObject;

  beforeEach(angular.mock.module(dashboardFilterModule.name));

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

  function testSetsAppliedFilterNameAndShowDirtyAsterisk(actionType) {
    describe(actionType + ' action', function() {
      var initState, action, filterJson;

      beforeEach(function() {
        filterJson = {
          organizationFilters: ['orgId1', 'orgId2'],
          policyThreatCategoryFilters: ['QUALITY', 'OTHER', 'SECURITY'],
          stageTypeFilters: ['release', 'stage-release', 'build'],
          tagFilters: ['tagId1', 'tagId2', null],
          applicationFilters: ['applicationIdZ', 'applicationIdA', 'applicationIdQ'],
          policyViolationStates: ['OPEN', 'WAIVED'],
          maxDaysOld: 90,
          minPolicyThreatLevel: 3,
          maxPolicyThreatLevel: 6
        };
        initState = {
          appliedFilterName: 'foo',
          savedFilters: [{'name': 'Test1', 'filter': filterJson}],
          other: otherObject
        };
        action = {
          type: actionType,
          payload: {
            filter: filterJson,
            basedOnFilterName: 'Test1'
          }
        };
      });

      it('sets the appliedFilterName to the basedOnFilterName in the payload', function() {
        var state = Object.freeze(initState);
        var newState = reduce(state, action);
        expect(newState.appliedFilterName).toBe('Test1');
        expect(newState.other).toBe(otherObject); // other properties are not modified
      });

      it('sets showDirtyAsterisk to false if filter is same as corresponding saved filter', function() {
        initState.showDirtyAsterisk = true;
        var state = Object.freeze(initState);
        var newState = reduce(state, action);
        expect(newState.showDirtyAsterisk).toBe(false);
        expect(newState.other).toBe(otherObject); // other properties are not modified
      });

      it('sets showDirtyAsterisk to true if filter has changed', function() {
        initState.showDirtyAsterisk = false;
        var state = Object.freeze(initState);
        action.payload.filter = angular.copy(filterJson);
        action.payload.filter.minPolicyThreatLevel = 2;
        var newState = reduce(state, action);
        expect(newState.showDirtyAsterisk).toBe(true);
        expect(newState.other).toBe(otherObject); // other properties are not modified
      });
    });
  }

  testSetsAppliedFilterNameAndShowDirtyAsterisk('FETCH_CURRENT_FILTER_FULFILLED');
  testSetsAppliedFilterNameAndShowDirtyAsterisk('APPLY_FILTER_FULFILLED');

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
    it('sets saveFilterSuccess to true, sets appliedFilterName from the payload name, appends the payload to ' +
        'savedFilters and resets showDirtyAsterisk', function() {
      var state = Object.freeze({
        saveFilterSaving: false,
        showDirtyAsterisk: true,
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
      expect(newState.showDirtyAsterisk).toBe(false);
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

  describe('SET_DISPLAY_SAVE_FILTER_MODAL action', function() {
    it('resets saveFilterSaving, saveFilterError and saveFilterSuccess', function() {
      var state = Object.freeze({
        saveFilterSaving: true,
        saveFilterSuccess: true,
        saveFilterError: true,
        warning: 'overwrite',
        other: otherObject
      });
      var action = { type: 'SET_DISPLAY_SAVE_FILTER_MODAL' };
      var newState = reduce(state, action);
      expect(newState.saveFilterSaving).toBe(false);
      expect(newState.saveFilterError).toBe(null);
      expect(newState.saveFilterSuccess).toBe(false);
      expect(newState.other).toBe(otherObject);
    });
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

  describe('CLEAR_FILTER action', function() {
    it('resets appliedFilterName and showDirtyAsterisk', function() {
      var state = Object.freeze({
        appliedFilterName: 'Test filter name',
        showDirtyAsterisk: true,
        other: otherObject
      });
      var newState = reduce(state, {type: 'CLEAR_FILTER'});
      expect(newState.appliedFilterName).toBeNull();
      expect(newState.showDirtyAsterisk).toBe(false);
      expect(newState.other).toBe(otherObject); // other properties are not modified
    });
  });
});
