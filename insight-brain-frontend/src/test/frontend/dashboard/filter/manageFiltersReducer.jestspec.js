/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { clone } from 'ramda';
import defaultFilter from '../../../../main/frontend/dashboard/filter/defaultFilter';
import { filterToJson } from '../../../../main/frontend/dashboard/filter/dashboardFilterService';
import reduce, {
  WARNING_OVERWRITE,
  WARNING_NAME_IN_USE,
} from '../../../../main/frontend/dashboard/filter/manageFiltersReducer';

describe('manageFiltersReducer', function () {
  let otherObject;

  beforeEach(function () {
    otherObject = { value: 'test value' };
  });

  describe('unknown action', function () {
    it('returns original state', function () {
      const state = Object.freeze({ foo: 'bar' });
      const action = {
        type: 'UNKNOWN',
      };
      const newState = reduce(state, action);
      expect(newState).toBe(state);
    });
  });

  describe('initial state', function () {
    it('is used if no state is provided', function () {
      const action = {
        type: 'UNKNOWN',
      };
      const newState = reduce(undefined, action);
      expect(newState).not.toBeUndefined();
    });
  });

  function testSetsAppliedFilterNameAndShowDirtyAsterisk(actionType) {
    describe(actionType + ' action', function () {
      let initState, action, filterJson;

      beforeEach(function () {
        filterJson = {
          organizationFilters: ['orgId1', 'orgId2'],
          policyThreatCategoryFilters: ['QUALITY', 'OTHER', 'SECURITY'],
          stageTypeFilters: ['release', 'stage-release', 'build'],
          tagFilters: ['tagId1', 'tagId2', null],
          applicationFilters: ['applicationIdZ', 'applicationIdA', 'applicationIdQ'],
          policyViolationStates: ['OPEN', 'WAIVED'],
          maxDaysOld: 90,
          minPolicyThreatLevel: 3,
          maxPolicyThreatLevel: 6,
        };
        initState = {
          appliedFilter: null,
          appliedFilterName: 'foo',
          savedFilters: [{ name: 'Test1', filter: filterJson }],
          other: otherObject,
        };
        action = {
          type: actionType,
          payload: {
            filter: filterJson,
            basedOnFilterName: 'Test1',
          },
        };
      });

      it('sets the appliedFilter to the filter in the payload', function () {
        const state = Object.freeze(initState);
        const newState = reduce(state, action);
        expect(newState.appliedFilter).toBe(filterJson);
        expect(newState.other).toBe(otherObject); // other properties are not modified
      });

      it('sets the appliedFilterName to the basedOnFilterName in the payload', function () {
        const state = Object.freeze(initState);
        const newState = reduce(state, action);
        expect(newState.appliedFilterName).toBe('Test1');
        expect(newState.other).toBe(otherObject); // other properties are not modified
      });

      it('sets the appliedFilterName to null if payload.basedOnFilterName is null', function () {
        const state = Object.freeze(initState);
        action.payload.basedOnFilterName = null;
        const newState = reduce(state, action);
        expect(newState.appliedFilterName).toBeNull();
        expect(newState.other).toBe(otherObject); // other properties are not modified
      });

      it('sets showDirtyAsterisk to false if filter is same as corresponding saved filter', function () {
        initState.showDirtyAsterisk = true;
        const state = Object.freeze(initState);
        const newState = reduce(state, action);
        expect(newState.showDirtyAsterisk).toBe(false);
        expect(newState.other).toBe(otherObject); // other properties are not modified
      });

      it('sets showDirtyAsterisk to true if saved filter has changed', function () {
        initState.showDirtyAsterisk = false;
        const state = Object.freeze(initState);
        action.payload.filter = clone(filterJson);
        action.payload.filter.minPolicyThreatLevel = 2;
        const newState = reduce(state, action);
        expect(newState.showDirtyAsterisk).toBe(true);
        expect(newState.other).toBe(otherObject); // other properties are not modified
      });

      it('sets showDirtyAsterisk to true if filter is Default and has changed', function () {
        initState.showDirtyAsterisk = false;
        const state = Object.freeze(initState);
        action.payload.basedOnFilterName = null;
        action.payload.filter = filterToJson(defaultFilter);
        action.payload.filter.minPolicyThreatLevel = 3;
        const newState = reduce(state, action);
        expect(newState.showDirtyAsterisk).toBe(true);
        expect(newState.other).toBe(otherObject); // other properties are not modified
      });

      it('sets showDirtyAsterisk to false if filter is Default and has not changed', function () {
        initState.showDirtyAsterisk = true;
        const state = Object.freeze(initState);
        action.payload.basedOnFilterName = null;
        action.payload.filter = filterToJson(defaultFilter);
        const newState = reduce(state, action);
        expect(newState.showDirtyAsterisk).toBe(false);
        expect(newState.other).toBe(otherObject); // other properties are not modified
      });
    });
  }

  testSetsAppliedFilterNameAndShowDirtyAsterisk('FETCH_CURRENT_FILTER_FULFILLED');
  testSetsAppliedFilterNameAndShowDirtyAsterisk('APPLY_FILTER_FULFILLED');

  describe('FETCH_SAVED_FILTERS_FULFILLED action', function () {
    it('sets savedFilters to the payload and sets savedFilterListError to null', function () {
      const state = Object.freeze({
        savedFilters: null,
        savedFilterListError: {},
        other: otherObject,
      });
      const action = {
        type: 'FETCH_SAVED_FILTERS_FULFILLED',
        payload: [{ name: 'foo' }, { name: 'bar' }],
      };
      const newState = reduce(state, action);
      expect(newState.savedFilters).toEqual([{ name: 'foo' }, { name: 'bar' }]);
      expect(newState.savedFilterListError).toBe(null);
      expect(newState.other).toBe(otherObject); // other properties are not modified
    });
  });

  describe('FETCH_SAVED_FILTERS_FAILED action', function () {
    it('sets savedFilterListError to the payload', function () {
      const state = Object.freeze({
        savedFilterListError: null,
        other: otherObject,
      });
      const error = {};
      const action = {
        type: 'FETCH_SAVED_FILTERS_FAILED',
        payload: error,
      };
      const newState = reduce(state, action);
      expect(newState.savedFilterListError).toBe(error);
      expect(newState.other).toBe(otherObject); // other properties are not modified
    });
  });

  describe('SAVE_FILTER_REQUESTED action', function () {
    it('sets saveFilterMaskState to false', function () {
      const state = Object.freeze({
        saveFilterMaskState: null,
        other: otherObject,
      });
      const action = { type: 'SAVE_FILTER_REQUESTED' };
      const newState = reduce(state, action);
      expect(newState.saveFilterMaskState).toBe(false);
      expect(newState.other).toBe(otherObject); // other properties are not modified
    });
  });

  describe('SAVE_FILTER_OVERWRITE_REQUESTED action', function () {
    it('sets saveFilterError to null, saveFilterWarning to WARNING_OVERWRITE', function () {
      const state = Object.freeze({
        saveFilterError: 'xyz',
        saveFilterWarning: null,
        other: otherObject,
      });
      const action = { type: 'SAVE_FILTER_OVERWRITE_REQUESTED' };
      const newState = reduce(state, action);
      expect(newState.saveFilterError).toBeNull();
      expect(newState.saveFilterWarning).toBe(WARNING_OVERWRITE);
      expect(newState.existingDuplicateFilterName).toBeNull();
      expect(newState.other).toBe(otherObject); // other properties are not modified
    });
  });

  describe('SAVE_DUPLICATE_FILTER_REQUESTED action', function () {
    it('sets saveFilterError to null, saveFilterWarning to WARNING_NAME_IN_USE', function () {
      const state = Object.freeze({
        existingDuplicateFilterName: null,
        saveFilterError: 'xyz',
        saveFilterWarning: null,
        other: otherObject,
      });
      const action = { type: 'SAVE_DUPLICATE_FILTER_REQUESTED', payload: null };
      const newState = reduce(state, action);
      expect(newState.saveFilterError).toBeNull();
      expect(newState.saveFilterWarning).toBe(WARNING_NAME_IN_USE);
      expect(newState.existingDuplicateFilterName).toBeNull();
      expect(newState.other).toBe(otherObject); // other properties are not modified
    });

    it('sets saveFilterError to null, saveFilterWarning to WARNING_NAME_IN_USE when space in name', function () {
      const state = Object.freeze({
        existingDuplicateFilterName: null,
        saveFilterError: 'xyz',
        saveFilterWarning: null,
        other: otherObject,
      });
      const action = {
        type: 'SAVE_DUPLICATE_FILTER_REQUESTED',
        payload: 'Test 1',
      };
      const newState = reduce(state, action);
      expect(newState.saveFilterError).toBeNull();
      expect(newState.saveFilterWarning).toBe(WARNING_NAME_IN_USE);
      expect(newState.existingDuplicateFilterName).toEqual('Test 1');
      expect(newState.other).toBe(otherObject); // other properties are not modified
    });

    it('sets saveFilterError to null, saveFilterWarning to WARNING_NAME_IN_USE when case is different', function () {
      const state = Object.freeze({
        existingDuplicateFilterName: null,
        saveFilterError: 'xyz',
        saveFilterWarning: null,
        other: otherObject,
      });
      const action = {
        type: 'SAVE_DUPLICATE_FILTER_REQUESTED',
        payload: 'TeSt1',
      };
      const newState = reduce(state, action);
      expect(newState.saveFilterError).toBeNull();
      expect(newState.saveFilterWarning).toBe(WARNING_NAME_IN_USE);
      expect(newState.existingDuplicateFilterName).toEqual('TeSt1');
      expect(newState.other).toBe(otherObject); // other properties are not modified
    });
  });

  describe('SAVE_CONFIRM_CANCELLED action', function () {
    it('sets saveFilterError to null, saveFilterWarning to null', function () {
      const state = Object.freeze({
        saveFilterError: 'yyy',
        saveFilterWarning: 'zzz',
        other: otherObject,
      });
      const action = { type: 'SAVE_CONFIRM_CANCELLED' };
      const newState = reduce(state, action);
      expect(newState.saveFilterError).toBeNull();
      expect(newState.saveFilterWarning).toBeNull();
      expect(newState.existingDuplicateFilterName).toBeNull();
      expect(newState.other).toBe(otherObject); // other properties are not modified
    });
  });

  describe('SAVE_FILTER_FULFILLED action', function () {
    it(
      'sets saveFilterMaskState to true, sets appliedFilterName from the payload name, appends the payload to ' +
        'savedFilters and resets showDirtyAsterisk',
      function () {
        const state = Object.freeze({
          saveFilterMaskState: false,
          showDirtyAsterisk: true,
          appliedFilterName: 'bar',
          savedFilters: Object.freeze([{ name: 'bar' }]),
          saveFilterWarning: 'foo',
          other: otherObject,
        });
        const action = {
          type: 'SAVE_FILTER_FULFILLED',
          payload: { name: 'foo' },
        };
        const newState = reduce(state, action);
        expect(newState.savedFilters).toEqual([{ name: 'bar' }, { name: 'foo' }]);
        expect(newState.appliedFilterName).toBe('foo');
        expect(newState.saveFilterMaskState).toBe(true);
        expect(newState.showDirtyAsterisk).toBe(false);
        expect(newState.saveFilterWarning).toBeNull();
        expect(newState.existingDuplicateFilterName).toBeNull();
        expect(newState.other).toBe(otherObject); // other properties are not modified
      }
    );
  });

  describe('SAVE_FILTER_FAILED action', function () {
    it('sets saveFilterMaskState to null, and saveFilterError to the payload', function () {
      const state = Object.freeze({
        saveFilterMaskState: true,
        saveFilterError: null,
        other: otherObject,
      });
      const error = {};
      const action = {
        type: 'SAVE_FILTER_FAILED',
        payload: error,
      };
      const newState = reduce(state, action);
      expect(newState.saveFilterMaskState).toBe(null);
      expect(newState.saveFilterError).toBe(error);
      expect(newState.other).toBe(otherObject); // other properties are not modified
    });
  });

  describe('SET_DISPLAY_SAVE_FILTER_MODAL action', function () {
    it('resets saveFilterMaskState and saveFilterError', function () {
      const state = Object.freeze({
        saveFilterMaskState: true,
        saveFilterError: true,
        warning: 'overwrite',
        other: otherObject,
      });
      const action = { type: 'SET_DISPLAY_SAVE_FILTER_MODAL' };
      const newState = reduce(state, action);
      expect(newState.saveFilterMaskState).toBe(null);
      expect(newState.saveFilterError).toBe(null);
      expect(newState.other).toBe(otherObject);
    });
  });

  describe('DELETE_FILTER_REQUESTED action', function () {
    it('sets deleteFilterMaskState to false', function () {
      const state = Object.freeze({
        deleteFilterMaskState: null,
        other: otherObject,
      });
      const action = { type: 'DELETE_FILTER_REQUESTED' };
      const newState = reduce(state, action);
      expect(newState.deleteFilterMaskState).toBe(false);
      expect(newState.other).toBe(otherObject); // other properties are not modified
    });
  });

  describe('DELETE_FILTER_FULFILLED action', function () {
    it('sets deleteFilterMaskState to true', function () {
      const state = Object.freeze({
        deleteFilterMaskState: false,
        other: otherObject,
      });
      const action = { type: 'DELETE_FILTER_FULFILLED', payload: 'foo' };
      const newState = reduce(state, action);
      expect(newState.deleteFilterMaskState).toBe(true);
      expect(newState.other).toBe(otherObject); // other properties are not modified
    });

    describe('if deleted filter is applied filter', function () {
      it('resets appliedFilterName', function () {
        const state = Object.freeze({
          appliedFilterName: 'bar',
          other: otherObject,
        });
        const action = { type: 'DELETE_FILTER_FULFILLED', payload: 'bar' };
        const newState = reduce(state, action);
        expect(newState.appliedFilterName).toBe(null);
        expect(newState.other).toBe(otherObject); // other properties are not modified
      });

      it('sets showDirtyAsterisk to false if applied filter is same as default filter', function () {
        const state = Object.freeze({
          appliedFilterName: 'bar',
          appliedFilter: filterToJson(defaultFilter),
          showDirtyAsterisk: true,
          other: otherObject,
        });
        const action = { type: 'DELETE_FILTER_FULFILLED', payload: 'bar' };
        const newState = reduce(state, action);
        expect(newState.showDirtyAsterisk).toBe(false);
        expect(newState.other).toBe(otherObject); // other properties are not modified
      });

      it('sets showDirtyAsterisk to true if applied filter is different from default filter', function () {
        const state = Object.freeze({
          appliedFilterName: 'bar',
          appliedFilter: filterToJson(defaultFilter),
          showDirtyAsterisk: false,
          other: otherObject,
        });
        state.appliedFilter.minPolicyThreatLevel = 3;
        const action = { type: 'DELETE_FILTER_FULFILLED', payload: 'bar' };
        const newState = reduce(state, action);
        expect(newState.showDirtyAsterisk).toBe(true);
        expect(newState.other).toBe(otherObject); // other properties are not modified
      });
    });

    describe('if deleted filter is not applied filter', function () {
      it('does not reset appliedFilterName', function () {
        const state = Object.freeze({
          appliedFilterName: 'baz',
          deleteFilterMaskState: false,
          other: otherObject,
        });
        const action = { type: 'DELETE_FILTER_FULFILLED', payload: 'bar' };
        const newState = reduce(state, action);
        expect(newState.appliedFilterName).toBe('baz');
        expect(newState.other).toBe(otherObject); // other properties are not modified
      });

      it('does not set showDirtyAsterisk to false if applied filter is same as default filter', function () {
        const state = Object.freeze({
          appliedFilterName: 'baz',
          appliedFilter: filterToJson(defaultFilter),
          showDirtyAsterisk: true,
          other: otherObject,
        });
        const action = { type: 'DELETE_FILTER_FULFILLED', payload: 'bar' };
        const newState = reduce(state, action);
        expect(newState.showDirtyAsterisk).toBe(true);
        expect(newState.other).toBe(otherObject); // other properties are not modified
      });
    });
  });

  describe('DELETE_FILTER_FAILED action', function () {
    it('resets deleteFilterMaskState and sets deleteFilterError to the payload', function () {
      const error = {};
      const state = Object.freeze({
        deleteFilterMaskState: true,
        deleteFilterError: null,
        other: otherObject,
      });
      const action = {
        type: 'DELETE_FILTER_FAILED',
        payload: error,
      };
      const newState = reduce(state, action);
      expect(newState.deleteFilterMaskState).toBe(null);
      expect(newState.deleteFilterError).toBe(error);
      expect(newState.other).toBe(otherObject); // other properties are not modified
    });
  });

  describe('SELECT_FILTER_TO_DELETE action', function () {
    it('resets deleteFilter flags and sets filterToDelete to the payload', function () {
      const state = Object.freeze({
        filterToDelete: 'foo',
        deleteFilterError: 'error',
        deleteFilterMaskState: true,
        other: otherObject,
      });
      const action = {
        type: 'SELECT_FILTER_TO_DELETE',
        payload: 'bar',
      };
      const newState = reduce(state, action);
      expect(newState.filterToDelete).toBe('bar');
      expect(newState.deleteFilterError).toBeNull();
      expect(newState.deleteFilterMaskState).toBe(null);
      expect(newState.other).toBe(otherObject); // other properties are not modified
    });
  });

  describe('HIDE_DELETE_FILTER_MODAL action', function () {
    it('resets filterToDelete', function () {
      const state = Object.freeze({
        filterToDelete: 'foo',
        other: otherObject,
      });
      const action = {
        type: 'HIDE_DELETE_FILTER_MODAL',
      };
      const newState = reduce(state, action);
      expect(newState.filterToDelete).toBeNull();
      expect(newState.other).toBe(otherObject); // other properties are not modified
    });
  });
});
