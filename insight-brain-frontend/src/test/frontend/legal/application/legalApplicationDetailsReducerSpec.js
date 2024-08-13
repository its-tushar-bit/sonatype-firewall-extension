/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

import { NO_LICENSE_THREAT_GROUP_ASSIGNED } from '../../../../main/frontend/legal/advancedLegalConstants';
import legalApplicationDetailsReducer from '../../../../main/frontend/legal/application/legalApplicationDetailsReducer';
import {
  LEGAL_APPLICATION_DETAILS_LOAD_DATA_FAILED,
  LEGAL_APPLICATION_DETAILS_LOAD_DATA_FULFILLED,
  LEGAL_APPLICATION_DETAILS_LOAD_DATA_REQUESTED,
} from '../../../../main/frontend/legal/application/legalApplicationDetailsActions';
import {
  LEGAL_APPLICATION_DETAILS_SET_COMPONENT_NAME_FILTER,
  LEGAL_APPLICATION_DETAILS_SET_LICENSE_NAME_FILTER,
  LEGAL_APPLICATION_DETAILS_SET_SORT,
  LEGAL_APPLICATION_DETAILS_TOGGLE_FILTER,
} from '../../../../main/frontend/legal/application/filter/legalApplicationDetailsFilterActions';

const otherObject = { value: 'test value' };

describe('legalApplicationDetailsReducer', function () {
  describe('initial state', function () {
    it('has default fields', function () {
      const action = { type: 'UNKNOWN' };
      const newState = legalApplicationDetailsReducer(undefined, action);

      expect(newState.loading).toEqual(false);
      expect(newState.error).toBeNull();
      expect(newState.applicationName).toBeNull();
      expect(newState.stageName).toBeNull();
      expect(newState.components).toEqual({
        results: [],
        filteredResults: [],
        licenseThreatGroups: [],
      });
      expect(newState.componentFilter).toEqual('');
      expect(newState.licenseFilter).toEqual('');
      expect(newState.filterSidebarOpen).toEqual(false);
      expect(newState.reviewStatusFilter).toEqual([]);
      expect(newState.licenseThreatGroupFilter).toEqual([]);
      expect(newState.sort).toEqual({});
      expect(newState.page).toEqual(1);
      expect(newState.selected).toEqual({
        progressOptions: new Set(),
        licenseThreatGroups: new Set(),
      });
    });
  });

  describe('unknown action', function () {
    it('returns original state', function () {
      const state = { foo: 'bar' };
      const action = {
        type: 'UNKNOWN',
      };
      const newState = legalApplicationDetailsReducer(state, action);
      expect(newState).toBe(state);
    });
  });

  describe('LEGAL_APPLICATION_DETAILS_TOGGLE_FILTER_SIDEBAR', function () {
    it('sets filterSidebarOpen', function () {
      const action = {
        type: 'LEGAL_APPLICATION_DETAILS_TOGGLE_FILTER_SIDEBAR',
        payload: 'filterSidebarOpen',
      };
      const newState = legalApplicationDetailsReducer({}, action);
      expect(newState).toEqual({ filterSidebarOpen: 'filterSidebarOpen' });
    });
  });

  describe('LEGAL_APPLICATION_DETAILS_LOAD_DATA_REQUESTED action', function () {
    it('updates to loading', function () {
      const state = Object.freeze({
        error: null,
        loading: false,
        components: {
          results: [],
        },
        other: otherObject,
      });
      const action = {
        type: LEGAL_APPLICATION_DETAILS_LOAD_DATA_REQUESTED,
      };
      const newState = legalApplicationDetailsReducer(state, action);
      expect(newState.loading).toEqual(true);
      expect(newState.error).toBeNull();
      expect(newState.components).toEqual({
        results: [],
      });
      expect(newState.other).toBe(otherObject); // other properties are not modified
    });
  });

  describe('LEGAL_APPLICATION_DETAILS_LOAD_DATA_FULFILLED action', function () {
    it('updates components state with appropriate data', function () {
      const state = Object.freeze({
        error: null,
        loading: true,
        applicationName: 'applicationNamevalue',
        stageName: 'stageNameValue',
        components: {
          results: [],
          licenseThreatGroups: [],
        },
        selected: {
          licenseThreatGroups: new Set(),
        },
        other: otherObject,
      });

      const components = Object.freeze([
        {
          licenses: [
            {
              licenseThreatGroups: [{ licenseThreatGroupName: 'a' }, { licenseThreatGroupName: 'b' }],
            },
            {
              licenseThreatGroups: [{ licenseThreatGroupName: 'c' }],
            },
          ],
        },
        {
          licenses: [
            {
              licenseThreatGroups: [{ licenseThreatGroupName: 'a' }],
            },
            {
              licenseThreatGroups: [{ licenseThreatGroupName: 'd' }],
            },
          ],
        },
      ]);

      const action = {
        type: LEGAL_APPLICATION_DETAILS_LOAD_DATA_FULFILLED,
        payload: { components: components, application: { name: 'applicationNamevalue' }, stageName: 'stageNameValue' },
      };

      const newState = legalApplicationDetailsReducer(state, action);

      expect(newState.loading).toEqual(false);
      expect(newState.error).toBeNull();
      expect(newState.applicationName).toEqual('applicationNamevalue');
      expect(newState.stageName).toEqual('stageNameValue');
      expect(newState.components).toEqual({
        results: components,
        filteredResults: components,
        licenseThreatGroups: ['a', 'b', 'c', 'd'],
      });

      expect(newState.other).toBe(otherObject); // other properties are not modified
    });

    it('assigns licenseThreatGroups, including the "No LTG Assigned"', function () {
      const state = Object.freeze({
        error: null,
        loading: true,
        applicationName: 'applicationNamevalue',
        stageName: 'stageNameValue',
        components: {
          results: [],
          licenseThreatGroups: [],
        },
        selected: {
          licenseThreatGroups: new Set(),
        },
        other: otherObject,
      });

      const components = Object.freeze([
        {
          licenses: [
            {
              licenseThreatGroups: [{ licenseThreatGroupName: 'a' }, { licenseThreatGroupName: 'b' }],
            },
          ],
        },
        {
          licenses: [
            {
              licenseName: 'hi',
            },
          ],
        },
      ]);

      const action = {
        type: LEGAL_APPLICATION_DETAILS_LOAD_DATA_FULFILLED,
        payload: { components: components, application: { name: 'applicationNamevalue' }, stageName: 'stageNameValue' },
      };

      const newState = legalApplicationDetailsReducer(state, action);

      expect(newState.loading).toEqual(false);
      expect(newState.error).toBeNull();
      expect(newState.applicationName).toEqual('applicationNamevalue');
      expect(newState.stageName).toEqual('stageNameValue');
      expect(newState.components).toEqual({
        results: components,
        filteredResults: components,
        licenseThreatGroups: ['a', 'b', 'No LTG Assigned'],
      });

      expect(newState.other).toBe(otherObject); // other properties are not modified
    });

    it('filters selected licenseThreatGroups', function () {
      const state = Object.freeze({
        error: null,
        loading: true,
        applicationName: 'applicationNamevalue',
        stageName: 'stageNameValue',
        components: {
          results: [],
          licenseThreatGroups: [],
        },
        selected: {
          licenseThreatGroups: new Set(['a', 'x', 'y']),
        },
        other: otherObject,
      });

      const components = Object.freeze([
        {
          licenses: [
            {
              licenseThreatGroups: [{ licenseThreatGroupName: 'a' }, { licenseThreatGroupName: 'b' }],
            },
            {
              licenseThreatGroups: [{ licenseThreatGroupName: 'c' }],
            },
          ],
        },
        {
          licenses: [
            {
              licenseThreatGroups: [{ licenseThreatGroupName: 'd' }],
            },
          ],
        },
      ]);

      const action = {
        type: LEGAL_APPLICATION_DETAILS_LOAD_DATA_FULFILLED,
        payload: { components: components, application: { name: 'applicationNamevalue' }, stageName: 'stageNameValue' },
      };

      const newState = legalApplicationDetailsReducer(state, action);

      expect(newState.loading).toEqual(false);
      expect(newState.error).toBeNull();
      expect(newState.applicationName).toEqual('applicationNamevalue');
      expect(newState.stageName).toEqual('stageNameValue');
      expect(newState.components).toEqual({
        results: components,
        filteredResults: components,
        licenseThreatGroups: ['a', 'b', 'c', 'd'],
      });

      expect(newState.selected).toEqual({
        licenseThreatGroups: new Set(['a']),
      });

      expect(newState.other).toBe(otherObject); // other properties are not modified
    });
  });

  describe('LEGAL_APPLICATION_DETAILS_LOAD_DATA_FAILED action', function () {
    it('updates components state with an error', function () {
      const state = Object.freeze({
        error: null,
        loading: true,
        components: {
          results: [],
        },
        other: otherObject,
      });
      const action = {
        type: LEGAL_APPLICATION_DETAILS_LOAD_DATA_FAILED,
        payload: 'some components error',
      };
      const newState = legalApplicationDetailsReducer(state, action);
      expect(newState.loading).toEqual(false);
      expect(newState.error).toEqual('some components error');
      expect(newState.components).toEqual({
        results: [],
      });
      expect(newState.other).toBe(otherObject); // other properties are not modified
    });
  });

  describe('filter states', function () {
    describe('unknown action', function () {
      it('returns original state', function () {
        const state = Object.freeze({ foo: 'bar' });
        const action = {
          type: 'UNKNOWN',
        };
        const newState = legalApplicationDetailsReducer(state, action);
        expect(newState).toBe(state);
      });
    });

    describe('LEGAL_APPLICATION_DETAILS_TOGGLE_FILTER action', function () {
      let initState;

      beforeEach(function () {
        initState = {
          other: otherObject,
          components: {
            results: [
              {
                hash: 'hash1',
                licenses: [
                  { licenseThreatGroups: [{ licenseThreatGroupName: 'group1' }] },
                  { licenseThreatGroups: [{ licenseThreatGroupName: 'group2' }] },
                ],
                reviewStatus: 'status1',
              },
              {
                hash: 'hash2',
                licenses: [
                  {
                    licenseThreatGroups: [{ licenseThreatGroupName: 'group1' }, { licenseThreatGroupName: 'group3' }],
                  },
                ],
                reviewStatus: 'status2',
              },
              {
                hash: 'hash3',
                licenses: [{ licenseThreatGroups: [] }],
                reviewStatus: 'status3',
              },
            ],
            filteredResults: [],
            error: null,
            loading: false,
          },
          filtersAreDirty: false,
          appliedFilter: {},
          sort: {},
          selected: Object.freeze({
            progressOptions: new Set(),
            licenseThreatGroups: new Set(),
          }),
        };
      });

      it('sets selected progressOptions', function () {
        const state = Object.freeze(initState);
        let action = {
          type: LEGAL_APPLICATION_DETAILS_TOGGLE_FILTER,
          payload: {
            filterName: 'progressOptions',
            selectedIds: new Set(['status1', 'status2']),
          },
        };
        let newState = legalApplicationDetailsReducer(state, action);
        expect(newState.selected.progressOptions).toBe(action.payload.selectedIds);
        expect(newState.components.filteredResults.length).toBe(2);
        expect(newState.components.filteredResults[0].hash).toBe('hash1');
        expect(newState.components.filteredResults[1].hash).toBe('hash2');
        expect(newState.other).toBe(otherObject); // other properties are not modified

        action = {
          type: LEGAL_APPLICATION_DETAILS_TOGGLE_FILTER,
          payload: {
            filterName: 'progressOptions',
            selectedIds: new Set(['status1']),
          },
        };
        newState = legalApplicationDetailsReducer(state, action);
        expect(newState.selected.progressOptions).toBe(action.payload.selectedIds);
        expect(newState.components.filteredResults.length).toBe(1);
        expect(newState.components.filteredResults[0].hash).toBe('hash1');
        expect(newState.other).toBe(otherObject); // other properties are not modified

        action = {
          type: LEGAL_APPLICATION_DETAILS_TOGGLE_FILTER,
          payload: {
            filterName: 'progressOptions',
            selectedIds: new Set(['status2']),
          },
        };
        newState = legalApplicationDetailsReducer(state, action);
        expect(newState.selected.progressOptions).toBe(action.payload.selectedIds);
        expect(newState.components.filteredResults.length).toBe(1);
        expect(newState.components.filteredResults[0].hash).toBe('hash2');
        expect(newState.other).toBe(otherObject); // other properties are not modified
      });

      it('sets selected license threat groups', function () {
        const state = Object.freeze(initState);
        let action = {
          type: LEGAL_APPLICATION_DETAILS_TOGGLE_FILTER,
          payload: {
            filterName: 'licenseThreatGroups',
            selectedIds: new Set(['group1', 'group2', 'group3', 'group4']),
          },
        };
        let newState = legalApplicationDetailsReducer(state, action);
        expect(newState.selected.licenseThreatGroups).toBe(action.payload.selectedIds);
        expect(newState.components.filteredResults.length).toBe(2);
        expect(newState.components.filteredResults[0].hash).toBe('hash1');
        expect(newState.components.filteredResults[1].hash).toBe('hash2');
        expect(newState.other).toBe(otherObject); // other properties are not modified

        action = {
          type: LEGAL_APPLICATION_DETAILS_TOGGLE_FILTER,
          payload: {
            filterName: 'licenseThreatGroups',
            selectedIds: new Set(['group1']),
          },
        };
        newState = legalApplicationDetailsReducer(state, action);
        expect(newState.selected.licenseThreatGroups).toBe(action.payload.selectedIds);
        expect(newState.components.filteredResults.length).toBe(2);
        expect(newState.components.filteredResults[0].hash).toBe('hash1');
        expect(newState.components.filteredResults[1].hash).toBe('hash2');
        expect(newState.other).toBe(otherObject); // other properties are not modified

        action = {
          type: LEGAL_APPLICATION_DETAILS_TOGGLE_FILTER,
          payload: {
            filterName: 'licenseThreatGroups',
            selectedIds: new Set(['group2']),
          },
        };
        newState = legalApplicationDetailsReducer(state, action);
        expect(newState.selected.licenseThreatGroups).toBe(action.payload.selectedIds);
        expect(newState.components.filteredResults.length).toBe(1);
        expect(newState.components.filteredResults[0].hash).toBe('hash1');
        expect(newState.other).toBe(otherObject); // other properties are not modified

        action = {
          type: LEGAL_APPLICATION_DETAILS_TOGGLE_FILTER,
          payload: {
            filterName: 'licenseThreatGroups',
            selectedIds: new Set(['group3']),
          },
        };
        newState = legalApplicationDetailsReducer(state, action);
        expect(newState.selected.licenseThreatGroups).toBe(action.payload.selectedIds);
        expect(newState.components.filteredResults.length).toBe(1);
        expect(newState.components.filteredResults[0].hash).toBe('hash2');
        expect(newState.other).toBe(otherObject); // other properties are not modified

        action = {
          type: LEGAL_APPLICATION_DETAILS_TOGGLE_FILTER,
          payload: {
            filterName: 'licenseThreatGroups',
            selectedIds: new Set([NO_LICENSE_THREAT_GROUP_ASSIGNED]),
          },
        };
        newState = legalApplicationDetailsReducer(state, action);
        expect(newState.selected.licenseThreatGroups).toBe(action.payload.selectedIds);
        expect(newState.components.filteredResults.length).toBe(1);
        expect(newState.components.filteredResults[0].hash).toBe('hash3');
        expect(newState.other).toBe(otherObject); // other properties are not modified
      });
    });

    describe('LEGAL_APPLICATION_DETAILS_SET_COMPONENT_NAME_FILTER and LEGAL_APPLICATION_DETAILS_SET_LICENSE_NAME_FILTER actions', function () {
      let initState;

      beforeEach(function () {
        initState = {
          other: otherObject,
          components: {
            results: [
              {
                hash: 'hash1',
                displayName: 'org.component1',
                licenses: [
                  { licenseName: 'Apache', licenseThreatGroups: [{ licenseThreatGroupName: 'group1' }] },
                  { licenseName: 'GPL', licenseThreatGroups: [{ licenseThreatGroupName: 'group2' }] },
                ],
                reviewStatus: 'status1',
              },
              {
                hash: 'hash2',
                displayName: 'com.component2',
                licenses: [
                  {
                    licenseName: 'GPL-3',
                    licenseThreatGroups: [{ licenseThreatGroupName: 'group1' }, { licenseThreatGroupName: 'group3' }],
                  },
                ],
                reviewStatus: 'status2',
              },
              {
                displayName: 'org.component3',
                hash: 'hash3',
                licenses: [{ licenseName: 'MIT', licenseThreatGroups: [] }],
                reviewStatus: 'status3',
              },
            ],
            filteredResults: [],
            error: null,
            loading: false,
          },
          filtersAreDirty: false,
          appliedFilter: {},
          sort: {},
          selected: Object.freeze({
            progressOptions: new Set(),
            licenseThreatGroups: new Set(),
          }),
        };
      });

      it('filters by component name', function () {
        const state = Object.freeze(initState);
        let action = {
          type: LEGAL_APPLICATION_DETAILS_SET_COMPONENT_NAME_FILTER,
          payload: { filter: 'org' },
        };
        let newState = legalApplicationDetailsReducer(state, action);
        expect(newState.components.filteredResults.length).toBe(2);
        expect(newState.components.filteredResults[0].hash).toBe('hash1');
        expect(newState.components.filteredResults[1].hash).toBe('hash3');
        expect(newState.other).toBe(otherObject); // other properties are not modified

        action = {
          type: LEGAL_APPLICATION_DETAILS_SET_COMPONENT_NAME_FILTER,
          payload: { filter: 'com.' },
        };
        newState = legalApplicationDetailsReducer(state, action);
        expect(newState.components.filteredResults.length).toBe(1);
        expect(newState.components.filteredResults[0].hash).toBe('hash2');
        expect(newState.other).toBe(otherObject); // other properties are not modified

        action = {
          type: LEGAL_APPLICATION_DETAILS_SET_COMPONENT_NAME_FILTER,
          payload: { filter: '' },
        };
        newState = legalApplicationDetailsReducer(state, action);
        expect(newState.components.filteredResults.length).toBe(3);
        expect(newState.components.filteredResults[0].hash).toBe('hash1');
        expect(newState.components.filteredResults[1].hash).toBe('hash2');
        expect(newState.components.filteredResults[2].hash).toBe('hash3');
        expect(newState.other).toBe(otherObject); // other properties are not modified
      });

      it('filters by license name', function () {
        const state = Object.freeze(initState);
        let action = {
          type: LEGAL_APPLICATION_DETAILS_SET_LICENSE_NAME_FILTER,
          payload: { filter: 'gpl' },
        };
        let newState = legalApplicationDetailsReducer(state, action);
        expect(newState.components.filteredResults.length).toBe(2);
        expect(newState.components.filteredResults[0].hash).toBe('hash1');
        expect(newState.components.filteredResults[1].hash).toBe('hash2');
        expect(newState.other).toBe(otherObject); // other properties are not modified

        action = {
          type: LEGAL_APPLICATION_DETAILS_SET_LICENSE_NAME_FILTER,
          payload: { filter: 'apa' },
        };
        newState = legalApplicationDetailsReducer(state, action);
        expect(newState.components.filteredResults.length).toBe(1);
        expect(newState.components.filteredResults[0].hash).toBe('hash1');
        expect(newState.other).toBe(otherObject); // other properties are not modified

        action = {
          type: LEGAL_APPLICATION_DETAILS_SET_LICENSE_NAME_FILTER,
          payload: { filter: '' },
        };
        newState = legalApplicationDetailsReducer(state, action);
        expect(newState.components.filteredResults.length).toBe(3);
        expect(newState.components.filteredResults[0].hash).toBe('hash1');
        expect(newState.components.filteredResults[1].hash).toBe('hash2');
        expect(newState.components.filteredResults[2].hash).toBe('hash3');
        expect(newState.other).toBe(otherObject); // other properties are not modified
      });

      it('filters by component and license name', function () {
        const state = Object.freeze(initState);
        let action1 = {
          type: LEGAL_APPLICATION_DETAILS_SET_LICENSE_NAME_FILTER,
          payload: { filter: 'gpl' },
        };
        let action2 = {
          type: LEGAL_APPLICATION_DETAILS_SET_COMPONENT_NAME_FILTER,
          payload: { filter: 'org.' },
        };
        let newState = legalApplicationDetailsReducer(state, action1);
        newState = legalApplicationDetailsReducer(newState, action2);

        expect(newState.components.filteredResults.length).toBe(1);
        expect(newState.components.filteredResults[0].hash).toBe('hash1');
        expect(newState.other).toBe(otherObject); // other properties are not modified

        action1 = {
          type: LEGAL_APPLICATION_DETAILS_SET_LICENSE_NAME_FILTER,
          payload: { filter: 'apa' },
        };
        action2 = {
          type: LEGAL_APPLICATION_DETAILS_SET_COMPONENT_NAME_FILTER,
          payload: { filter: 'com.' },
        };
        newState = legalApplicationDetailsReducer(state, action1);
        newState = legalApplicationDetailsReducer(newState, action2);

        expect(newState.components.filteredResults.length).toBe(0);
        expect(newState.other).toBe(otherObject); // other properties are not modified
      });

      it('filters by component name and LTG', function () {
        const state = Object.freeze(initState);
        let action1 = {
          type: LEGAL_APPLICATION_DETAILS_SET_COMPONENT_NAME_FILTER,
          payload: { filter: 'org.' },
        };
        let action2 = {
          type: LEGAL_APPLICATION_DETAILS_TOGGLE_FILTER,
          payload: {
            filterName: 'progressOptions',
            selectedIds: new Set(['status1']),
          },
        };
        let newState = legalApplicationDetailsReducer(state, action1);
        newState = legalApplicationDetailsReducer(newState, action2);

        expect(newState.components.filteredResults.length).toBe(1);
        expect(newState.components.filteredResults[0].hash).toBe('hash1');
        expect(newState.other).toBe(otherObject); // other properties are not modified
      });
    });

    describe('LEGAL_APPLICATION_DETAILS_SET_SORT actions', function () {
      let initState;

      beforeEach(function () {
        initState = {
          other: otherObject,
          components: {
            results: [
              {
                hash: 'hash1',
                displayName: 'org.component1',
                licenses: [
                  { licenseName: 'Apache', licenseThreatGroups: [{ licenseThreatGroupName: 'group1' }] },
                  { licenseName: 'GPL', licenseThreatGroups: [{ licenseThreatGroupName: 'group2' }] },
                ],
                reviewStatus: 'IN_PROGRESS',
                reviewTotalCount: 0,
                reviewCompletedCount: 0,
              },
              {
                hash: 'hash2',
                displayName: 'com.component2',
                licenses: [
                  {
                    licenseName: 'GPL-3',
                    licenseThreatGroups: [{ licenseThreatGroupName: 'group1' }, { licenseThreatGroupName: 'group3' }],
                  },
                ],
                reviewStatus: 'FLAGGED',
                reviewTotalCount: 3,
                reviewCompletedCount: 2,
              },
              {
                displayName: 'org.component3',
                hash: 'hash3',
                licenses: [{ licenseName: 'Apache', licenseThreatGroups: [] }],
                reviewStatus: 'COMPLETED',
                reviewTotalCount: 5,
                reviewCompletedCount: 0,
              },
            ],
            filteredResults: [],
            error: null,
            loading: false,
          },
          filtersAreDirty: false,
          appliedFilter: {},
          sort: {},
          selected: Object.freeze({
            progressOptions: new Set(),
            licenseThreatGroups: new Set(),
          }),
        };
      });

      it('sorts by component name', function () {
        const state = Object.freeze(initState);
        let action = {
          type: LEGAL_APPLICATION_DETAILS_SET_SORT,
          payload: { column: 'component', sortOrder: 'asc' },
        };
        let newState = legalApplicationDetailsReducer(state, action);
        expect(newState.components.filteredResults.length).toBe(3);
        expect(newState.components.filteredResults[0].hash).toBe('hash2');
        expect(newState.components.filteredResults[1].hash).toBe('hash1');
        expect(newState.components.filteredResults[2].hash).toBe('hash3');
        expect(newState.other).toBe(otherObject); // other properties are not modified

        action = {
          type: LEGAL_APPLICATION_DETAILS_SET_SORT,
          payload: { column: 'component', sortOrder: 'desc' },
        };
        newState = legalApplicationDetailsReducer(state, action);
        expect(newState.components.filteredResults.length).toBe(3);
        expect(newState.components.filteredResults[0].hash).toBe('hash3');
        expect(newState.components.filteredResults[1].hash).toBe('hash1');
        expect(newState.components.filteredResults[2].hash).toBe('hash2');
      });

      it('sorts by license name', function () {
        const state = Object.freeze(initState);
        let action = {
          type: LEGAL_APPLICATION_DETAILS_SET_SORT,
          payload: { column: 'licenses', sortOrder: 'asc' },
        };
        let newState = legalApplicationDetailsReducer(state, action);
        expect(newState.components.filteredResults.length).toBe(3);
        expect(newState.components.filteredResults[0].hash).toBe('hash3');
        expect(newState.components.filteredResults[1].hash).toBe('hash1');
        expect(newState.components.filteredResults[2].hash).toBe('hash2');
        expect(newState.other).toBe(otherObject); // other properties are not modified

        action = {
          type: LEGAL_APPLICATION_DETAILS_SET_SORT,
          payload: { column: 'licenses', sortOrder: 'desc' },
        };
        newState = legalApplicationDetailsReducer(state, action);
        expect(newState.components.filteredResults.length).toBe(3);
        expect(newState.components.filteredResults[0].hash).toBe('hash2');
        expect(newState.components.filteredResults[1].hash).toBe('hash1');
        expect(newState.components.filteredResults[2].hash).toBe('hash3');
      });

      it('sorts by review progress', function () {
        const state = Object.freeze(initState);
        let action = {
          type: LEGAL_APPLICATION_DETAILS_SET_SORT,
          payload: { column: 'progress', sortOrder: 'asc' },
        };
        let newState = legalApplicationDetailsReducer(state, action);
        expect(newState.components.filteredResults.length).toBe(3);
        expect(newState.components.filteredResults[0].hash).toBe('hash1');
        expect(newState.components.filteredResults[1].hash).toBe('hash3');
        expect(newState.components.filteredResults[2].hash).toBe('hash2');
        expect(newState.other).toBe(otherObject); // other properties are not modified

        action = {
          type: LEGAL_APPLICATION_DETAILS_SET_SORT,
          payload: { column: 'progress', sortOrder: 'desc' },
        };
        newState = legalApplicationDetailsReducer(state, action);
        expect(newState.components.filteredResults.length).toBe(3);
        expect(newState.components.filteredResults[0].hash).toBe('hash2');
        expect(newState.components.filteredResults[1].hash).toBe('hash1');
        expect(newState.components.filteredResults[2].hash).toBe('hash3');
      });

      it('sorts by status', function () {
        const state = Object.freeze(initState);
        let action = {
          type: LEGAL_APPLICATION_DETAILS_SET_SORT,
          payload: { column: 'status', sortOrder: 'asc' },
        };
        let newState = legalApplicationDetailsReducer(state, action);
        expect(newState.components.filteredResults.length).toBe(3);
        expect(newState.components.filteredResults[0].hash).toBe('hash3');
        expect(newState.components.filteredResults[1].hash).toBe('hash1');
        expect(newState.components.filteredResults[2].hash).toBe('hash2');
        expect(newState.other).toBe(otherObject); // other properties are not modified

        action = {
          type: LEGAL_APPLICATION_DETAILS_SET_SORT,
          payload: { column: 'status', sortOrder: 'desc' },
        };
        newState = legalApplicationDetailsReducer(state, action);
        expect(newState.components.filteredResults.length).toBe(3);
        expect(newState.components.filteredResults[0].hash).toBe('hash2');
        expect(newState.components.filteredResults[1].hash).toBe('hash1');
        expect(newState.components.filteredResults[2].hash).toBe('hash3');
      });
    });
  });
});
