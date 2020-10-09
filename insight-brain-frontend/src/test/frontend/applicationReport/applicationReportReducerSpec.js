/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

import reduce from '../../../main/frontend/applicationReport/applicationReportReducer';

describe('applicationReportReducer', function() {
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
    it('is used if no state is provided', function() {
      const action = {type: 'UNKNOWN'};
      const newState = reduce(undefined, action);
      expect(newState).not.toBeUndefined();
    });

    it('has default fields', function() {
      const action = {type: 'UNKNOWN'};
      const newState = reduce(undefined, action);
      expect(newState.pendingLoads).toEqual(new Set());
      expect(newState.loadError).toBe(null);
      expect(newState.reevaluating).toBe(false);
      expect(newState.reevaluationError).toBe(null);
      expect(newState.selectedReport).toBe(null);
      expect(newState.aggregate).toBe(true);
      expect(newState.sortFields).toEqual(['-policyThreatLevel', 'policyName', 'derivedComponentName']);
      expect(newState.rawDataSortFields).toEqual(
          ['derivedComponentName', 'licenseSortKey', 'securityCode', '-cvssScore']);
      expect(newState.exactValueFilters).toEqual({});
      expect(newState.substringFilters).toEqual({});
      expect(newState.rawDataSubstringFilters).toEqual({});
      expect(newState.rawDataNumericFilters).toEqual({});
      expect(newState.isUnknownJs).toBe(false);
      expect(newState.policyTypeFilterEnabled).toBe(true);
      expect(newState.vulnerabilities).toBe(null);
      expect(newState.vulnerabilitiesPageEnabled).toBe(true);
      expect(newState.selectedRootAncestor).toBeNull();
    });

    it('is immutable', function() {
      const action = {type: 'UNKNOWN'};
      const state = reduce(undefined, action);

      // Overall state object
      expect(() => {
        state.newProp = 'newProp';
      }).toThrowError(TypeError);

      // Nested object-properties
      expect(() => {
        state.sortFields = [];
      }).toThrowError(TypeError);

      expect(() => {
        state.rawDataSortFields = [];
      }).toThrowError(TypeError);

      expect(() => {
        state.exactValueFilters.newProp = 'newProp';
      }).toThrowError(TypeError);

      expect(() => {
        state.substringFilters.newProp = 'newProp';
      }).toThrowError(TypeError);

      // NOTE pendingLoads Set is not actually immutable, as Object.freeze doesn't work on Sets
    });
  });

  describe('SET_REPORT_PARAMETERS action', function() {
    it('adds report parameters to state', function() {
      const state = {};
      const newState = reduce(state, {
        type: 'SET_REPORT_PARAMETERS',
        payload: {
          appId: 'appId',
          scanId: 'scanId',
          isUnknownJs: false
        }
      });
      expect(newState.reportParameters).toEqual({
        appId: 'appId',
        scanId: 'scanId',
        isUnknownJs: false
      });
    });

    it('sets the state to the initState value', () => {
      const state = {
        foo: 'bar',
        changeMe: 'can haz overwrite plz?'
      };
      const newState = reduce(state, {
        type: 'SET_REPORT_PARAMETERS',
        payload: {
          appId: 'appId',
          scanId: 'scanId',
          isUnknownJs: false
        }
      });
      expect(newState).toEqual({
        pendingLoads: new Set(),
        reevaluating: false,
        loadError: null,
        reevaluationError: null,
        aggregate: true,
        rawDataSortFields: ['derivedComponentName', 'licenseSortKey', 'securityCode', '-cvssScore'],
        sortFields: ['-policyThreatLevel', 'policyName', 'derivedComponentName'],
        exactValueFilters: {},
        reportRawData: null,
        reportParameters: {
          appId: 'appId',
          scanId: 'scanId',
          isUnknownJs: false
        },
        substringFilters: {},
        rawDataSubstringFilters: {},
        rawDataNumericFilters: {},
        selectedReport: null,
        selectedComponentIndex: null,
        selectedRootAncestor: null,
        policyTypeFilterEnabled: true,
        isUnknownJs: false,
        vulnerabilities: null,
        vulnerabilitiesPageEnabled: true,
        isInnerSourceEnabled: false,
        sortConfiguration: {
          key: 'policyThreatLevel',
          sortFields: ['-policyThreatLevel', 'policyName', 'derivedComponentName'],
          dir: 'desc'
        }
      });
    });
  });

  describe('LOAD_REPORT_REQUESTED action', function() {
    it('adds "policy" and "common" to pendingLoads and unsets error and selectedReport values', function() {
      const state = Object.freeze({
        pendingLoads: new Set(['foo']),
        loadError: 'test error',
        selectedReport: 'test report',
        other: otherObject
      });
      const newState = reduce(state, {type: 'LOAD_REPORT_REQUESTED'});
      expect(newState).toEqual({
        pendingLoads: new Set(['foo', 'policy', 'common']),
        loadError: null,
        selectedReport: null,
        other: otherObject
      });
      expect(newState.other).toBe(otherObject); // other properties are not modified
    });
  });

  describe('LOAD_COMMON_DATA_FULFILLED', function() {
    it('sets the specified bomData, metatdata, and unknownJsData on the state', function() {
      const state = Object.freeze({
        pendingLoads: new Set(['foo']),
        other: otherObject
      });

      const bomData = {},
          metadata = {},
          unknownJsData = {};

      const newState = reduce(state, {
        type: 'LOAD_COMMON_DATA_FULFILLED',
        payload: { bomData, metadata, unknownJsData }
      });

      expect(newState.bomData).toBe(bomData);
      expect(newState.metadata).toBe(metadata);
      expect(newState.unknownJsData).toBe(unknownJsData);
      expect(newState.other).toBe(otherObject); // other properties are not modified
    });

    it('removes "common" from pendingLoads', function() {
      const state = Object.freeze({
        pendingLoads: new Set(['foo', 'common']),
        other: otherObject
      });

      const newState = reduce(state, { type: 'LOAD_COMMON_DATA_FULFILLED', payload: {} });

      expect(newState.pendingLoads).toEqual(new Set(['foo']));
    });
  });

  describe('LOAD_REPORT_FULFILLED action', function() {
    it('removes "policy" from pendingLoads and sets selectedReport values without innerSource enabled', function() {
      const state = Object.freeze({
        pendingLoads: new Set(['foo', 'policy']),
        loadError: null,
        selectedReport: null,
        policyTypeFilterEnabled: null,
        other: otherObject
      });
      const entries = [
        {policyThreatLevel: 1}, {policyThreatLevel: 3}, {policyThreatLevel: 4, waived: true}, {policyThreatLevel: 6},
        {policyThreatLevel: 9}, {policyThreatLevel: 10, grandfathered: true}
      ];
      const newState = reduce(state, {
        type: 'LOAD_REPORT_FULFILLED',
        payload: {
          allEntries: entries,
          reportVersion: 3,
          isInnerSourceEnabled: false
        }
      });
      expect(newState).toEqual({
        pendingLoads: new Set(['foo']),
        loadError: null,
        selectedReport: {
          allEntries: entries,
          displayedEntries: entries,
          moderateViolationCount: 1,
          severeViolationCount: 1,
          criticalViolationCount: 1,
          nonLowViolationCount: 3,
          reportVersion: 3,
          isInnerSourceEnabled: false
        },
        policyTypeFilterEnabled: false,
        vulnerabilitiesPageEnabled: jasmine.anything(),
        isInnerSourceEnabled: false,
        other: otherObject
      });
      expect(newState.other).toBe(otherObject); // other properties are not modified
    });

    it('removes "policy" from pendingLoads and sets selectedReport values with innerSource enabled', function() {
      const state = Object.freeze({
        pendingLoads: new Set(['foo', 'policy']),
        loadError: null,
        selectedReport: null,
        policyTypeFilterEnabled: null,
        other: otherObject
      });
      const entries = [
        {policyThreatLevel: 10, grandfathered: true, ownerApplicationName: 'myISApp', innerSource: true},
        {policyThreatLevel: 10}, {policyThreatLevel: 6, ownerApplicationName: 'myISApp'}
      ];
      const newState = reduce(state, {
        type: 'LOAD_REPORT_FULFILLED',
        payload: {
          allEntries: entries,
          reportVersion: 3,
          isInnerSourceEnabled: true
        }
      });
      expect(newState).toEqual({
        pendingLoads: new Set(['foo']),
        loadError: null,
        selectedReport: {
          allEntries: entries,
          displayedEntries: [
            {
              policyThreatLevel: 10
            },
            {
              grandfathered: true,
              innerSource: true,
              ownerApplicationName: 'myISApp',
              policyThreatLevel: 10
            },
            {
              ownerApplicationName: 'myISApp',
              policyThreatLevel: 6
            }
          ],
          moderateViolationCount: 0,
          severeViolationCount: 1,
          criticalViolationCount: 1,
          nonLowViolationCount: 2,
          reportVersion: 3,
          isInnerSourceEnabled: true
        },
        policyTypeFilterEnabled: false,
        vulnerabilitiesPageEnabled: jasmine.anything(),
        isInnerSourceEnabled: true,
        other: otherObject
      });
      expect(newState.other).toBe(otherObject); // other properties are not modified
    });

    it('sets vulnerabilitiesPageEnabled to true if the reportVersion is at least 5', function() {
      const state = Object.freeze({
            pendingLoads: new Set()
          }),
          newStateV6 = reduce(state, {
            type: 'LOAD_REPORT_FULFILLED',
            payload: {
              allEntries: [],
              reportVersion: 6
            }
          }),
          newStateV5 = reduce(state, {
            type: 'LOAD_REPORT_FULFILLED',
            payload: {
              allEntries: [],
              reportVersion: 5
            }
          }),
          newStateV4 = reduce(state, {
            type: 'LOAD_REPORT_FULFILLED',
            payload: {
              allEntries: [],
              reportVersion: 4
            }
          }),
          newStateV1 = reduce(state, {
            type: 'LOAD_REPORT_FULFILLED',
            payload: {
              allEntries: [],
              reportVersion: 1
            }
          }),
          newStateVNil = reduce(state, {
            type: 'LOAD_REPORT_FULFILLED',
            payload: { allEntries: [] }
          });

      expect(newStateVNil.vulnerabilitiesPageEnabled).toBe(false);
      expect(newStateV1.vulnerabilitiesPageEnabled).toBe(false);
      expect(newStateV4.vulnerabilitiesPageEnabled).toBe(false);
      expect(newStateV5.vulnerabilitiesPageEnabled).toBe(true);
      expect(newStateV6.vulnerabilitiesPageEnabled).toBe(true);
    });

    it('sets the displayedEntries in the selectedReport based on the current aggregation etc settings', function() {
      const state = Object.freeze({
            selectedReport: null,
            aggregate: true,
            exactValueFilters: {
              policyThreatLevel: new Set([1, 4, 5, 6])
            },
            sortFields: ['-policyThreatLevel']
          }),
          entries = [{
            hash: '1',
            policyThreatLevel: 1,
            policyName: 'P1',
            waived: false,
            grandfathered: false
          }, {
            hash: '2',
            policyThreatLevel: 3,
            policyName: 'P2',
            waived: false,
            grandfathered: false
          }, {
            hash: '4',
            policyThreatLevel: 4,
            policyName: 'P3',
            waived: true,
            grandfathered: false,
            displayName: {parts: []}
          }, {
            hash: '1',
            policyThreatLevel: 6,
            policyName: 'P4',
            waived: false,
            grandfathered: false
          }, {
            hash: '5',
            policyThreatLevel: 4,
            policyName: 'P5',
            waived: false,
            grandfathered: false
          }, {
            hash: '6',
            policyThreatLevel: 10,
            policyName: 'P6',
            waived: false,
            grandfathered: true,
            displayName: {parts: []}
          }],
          newState = reduce(state, {
            type: 'LOAD_REPORT_FULFILLED',
            payload: {
              allEntries: entries,
              reportVersion: 3
            }
          });

      expect(newState.selectedReport.displayedEntries).toEqual([{
        hash: '1',
        policyThreatLevel: 6,
        policyName: 'P4',
        waived: false,
        grandfathered: false
      }, {
        hash: '5',
        policyThreatLevel: 4,
        policyName: 'P5',
        waived: false,
        grandfathered: false
      }]);
    });

    it('sets selectedComponentIndex while in aggregated mode if a component was previously selected', function() {
      const state = Object.freeze({
        pendingLoads: new Set(['policy']),
        loadError: null,
        aggregate: true,
        sortFields: ['derivedComponentName'],
        selectedComponentIndex: 0,
        selectedReport: {
          displayedEntries: [{
            hash: '1',
            derivedComponentName: '1'
          }, {
            hash: '2',
            derivedComponentName: '2'
          }, {
            hash: '3',
            derivedComponentName: '3'
          }]
        },
        other: otherObject
      });

      const entries = [{
        hash: '1',
        derivedComponentName: '5'
      }, {
        hash: '2',
        derivedComponentName: '2'
      }, {
        hash: '3',
        derivedComponentName: '3'
      }];

      const newState = reduce(state, {
        type: 'LOAD_REPORT_FULFILLED',
        payload: {
          allEntries: entries,
          reportVersion: 3
        }
      });

      expect(newState.selectedComponentIndex).toBe(2);
    });

    it('sets selectedComponentIndex while in non aggregated mode if a component was previously selected', function() {
      const state = Object.freeze({
        pendingLoads: new Set(['policy']),
        loadError: null,
        aggregate: false,
        sortFields: ['derivedComponentName', 'policyName'],
        selectedComponentIndex: 1,
        selectedReport: {
          displayedEntries: [{
            hash: '1',
            policyName: 'P1',
            derivedComponentName: '1'
          }, {
            hash: '1',
            policyName: 'P2',
            derivedComponentName: '1'
          }, {
            hash: '2',
            policyName: 'P2',
            derivedComponentName: '2'
          }, {
            hash: '3',
            policyName: 'P3',
            derivedComponentName: '3'
          }]
        },
        other: otherObject
      });

      const entries = [{
        hash: '1',
        policyName: 'P1',
        derivedComponentName: '5'
      }, {
        hash: '1',
        policyName: 'P2',
        derivedComponentName: '5'
      }, {
        hash: '2',
        policyName: 'P2',
        derivedComponentName: '2'
      }, {
        hash: '3',
        policyName: 'P3',
        derivedComponentName: '3'
      }];

      const newState = reduce(state, {
        type: 'LOAD_REPORT_FULFILLED',
        payload: {
          allEntries: entries,
          reportVersion: 3
        }
      });

      expect(newState.selectedComponentIndex).toBe(3);
    });

    it('sets policyTypeFilterEnabled to true if the report version is bigger than 3', function () {
      const state = Object.freeze({
        policyTypeFilterEnabled: false,
        other: otherObject
      });
      const newState = reduce(state, {
        type: 'LOAD_REPORT_FULFILLED',
        payload: {
          allEntries: [],
          metadata: {reportTitle: 'test'},
          isUnknownJs: false,
          reportVersion: 4
        }
      });
      expect(newState.policyTypeFilterEnabled).toBe(true);
      expect(newState.other).toBe(otherObject); //confirm no side-effects
    });

    it('sets policyTypeFilterEnabled to false if the report version is lower than 4', function () {
      const state = Object.freeze({
        policyTypeFilterEnabled: true,
        other: otherObject
      });
      const newState = reduce(state, {
        type: 'LOAD_REPORT_FULFILLED',
        payload: {
          allEntries: [],
          metadata: {reportTitle: 'test'},
          isUnknownJs: false,
          reportVersion: 3
        }
      });
      expect(newState.policyTypeFilterEnabled).toBe(false);
      expect(newState.other).toBe(otherObject);
    });
  });

  describe('LOAD_REPORT_RAW_DATA_REQUESTED', () => {
    it('adds "raw" and "common" to the pendingLoads and unsets error and reportRawData values', function() {
      const state = Object.freeze({
        pendingLoads: new Set(['foo']),
        loadError: 'test error',
        reportRawData: 'test report',
        other: otherObject
      });
      const newState = reduce(state, {type: 'LOAD_REPORT_RAW_DATA_REQUESTED'});
      expect(newState).toEqual({
        pendingLoads: new Set(['foo', 'raw', 'common']),
        loadError: null,
        reportRawData: null,
        other: otherObject
      });
      expect(newState.other).toBe(otherObject); // other properties are not modified
    });
  });

  describe('LOAD_REPORT_ALL_DATA_REQUESTED', () => {
    it('adds "policy", "raw" and "common" to the pendingLoads', function() {
      const state = Object.freeze({
        pendingLoads: new Set(['foo']),
        other: otherObject
      });
      const newState = reduce(state, {type: 'LOAD_REPORT_ALL_DATA_REQUESTED'});
      expect(newState).toEqual({
        pendingLoads: new Set(['foo', 'raw', 'common', 'policy']),
        other: otherObject
      });
      expect(newState.other).toBe(otherObject); // other properties are not modified
    });
  });

  describe('LOAD_REPORT_RAW_DATA_FULFILLED action', () => {
    it('removes "raw" from pendingLoads and does not change other values on the state', function() {
      const state = Object.freeze({
        pendingLoads: new Set(['foo', 'raw']),
        other: otherObject
      });
      const newState = reduce(state, {
        type: 'LOAD_REPORT_RAW_DATA_FULFILLED',
        payload: []
      });
      expect(newState.pendingLoads).toEqual(new Set(['foo']));
      expect(newState.other).toBe(otherObject); // other properties are not modified
    });

    it('sets the raw data information on the allEntries section of reportRawData state', () => {
      const state = {};
      const rawDataEntries = [
        {
          derivedComponentName: 'foo',
          license: 'undefined'
        }
      ];

      const newState = reduce(state, {
        type: 'LOAD_REPORT_RAW_DATA_FULFILLED',
        payload: rawDataEntries
      });

      expect(newState.reportRawData.allEntries).toEqual(rawDataEntries);
    });

    it('sets the appropriate raw data information on the displayedEntries section of reportRawData state', () => {
      const state = {};
      const rawDataEntries = [
        {
          derivedComponentName: 'foo',
          license: 'undefined'
        }
      ];

      const newState = reduce(state, {
        type: 'LOAD_REPORT_RAW_DATA_FULFILLED',
        payload: rawDataEntries
      });

      expect(newState.reportRawData.displayedEntries).toEqual(rawDataEntries);
    });
  });

  describe('REEVALUATE_REPORT_REQUESTED action', function() {
    it('sets reevaluating flag and unsets reevaluationError', function() {
      const state = Object.freeze({
        reevaluating: false,
        reevaluationError: 'Error',
        other: otherObject
      });
      const newState = reduce(state, {type: 'REEVALUATE_REPORT_REQUESTED'});
      expect(newState).toEqual({
        reevaluating: true,
        reevaluationError: null,
        other: otherObject
      });
      expect(newState.other).toBe(otherObject); // other properties are not modified
    });
  });

  describe('REEVALUATE_REPORT_FULFILLED action', function() {
    it('unsets reevaluating flag and reevaluationError', function() {
      const state = Object.freeze({
        reevaluating: true,
        reevaluationError: 'asdf',
        other: otherObject
      });
      const newState = reduce(state, {type: 'REEVALUATE_REPORT_FULFILLED'});
      expect(newState).toEqual({
        reevaluating: false,
        reevaluationError: null,
        other: otherObject
      });
      expect(newState.other).toBe(otherObject); // other properties are not modified
    });
  });

  describe('REEVALUATE_REPORT_CANCELLED action', function() {
    it('unsets reevaluating flag and reevaluationError', function() {
      const state = Object.freeze({
        reevaluating: true,
        reevaluationError: 'asdf',
        other: otherObject
      });
      const newState = reduce(state, {type: 'REEVALUATE_REPORT_CANCELLED'});
      expect(newState).toEqual({
        reevaluating: false,
        reevaluationError: null,
        other: otherObject
      });
      expect(newState.other).toBe(otherObject); // other properties are not modified
    });
  });

  describe('REEVALUATE_REPORT_FAILED action', function() {
    it('unsets reevaluating flag and sets the reevaluationError to the payload', function() {
      const state = Object.freeze({
        reevaluating: true,
        reevaluationError: null,
        other: otherObject
      });
      const payload = 'Error!';
      const newState = reduce(state, { type: 'REEVALUATE_REPORT_FAILED', payload });
      expect(newState).toEqual({
        reevaluating: false,
        reevaluationError: payload,
        other: otherObject
      });
      expect(newState.other).toBe(otherObject); // other properties are not modified
    });
  });

  describe('LOAD_REPORT_FAILED action', function() {
    it('removes "policy" from pendingLoads and sets error value', function() {
      const state = Object.freeze({
        pendingLoads: new Set(['foo', 'policy']),
        loadError: null,
        selectedReport: null,
        other: otherObject
      });
      const newState = reduce(state, {type: 'LOAD_REPORT_FAILED', payload: 'test error'});
      expect(newState).toEqual({
        pendingLoads: new Set(['foo']),
        loadError: 'test error',
        selectedReport: null,
        other: otherObject
      });
      expect(newState.other).toBe(otherObject); // other properties are not modified
    });
  });

  describe('LOAD_REPORT_RAW_DATA_FAILED action', function() {
    it('removes "raw" from pendingLoads and sets error value', function() {
      const state = Object.freeze({
        pendingLoads: new Set(['foo', 'raw']),
        loadError: null,
        reportRawData: null,
        other: otherObject
      });
      const newState = reduce(state, {type: 'LOAD_REPORT_RAW_DATA_FAILED', payload: 'test error'});
      expect(newState).toEqual({
        pendingLoads: new Set(['foo']),
        loadError: 'test error',
        reportRawData: null,
        other: otherObject
      });
      expect(newState.other).toBe(otherObject); // other properties are not modified
    });
  });

  describe('LOAD_COMMON_DATA_FAILED action', function() {
    it('removes "common" from pendingLoads and sets error value', function() {
      const state = Object.freeze({
        pendingLoads: new Set(['foo', 'common']),
        loadError: null,
        reportRawData: null,
        other: otherObject
      });
      const newState = reduce(state, {type: 'LOAD_COMMON_DATA_FAILED', payload: 'test error'});
      expect(newState).toEqual({
        pendingLoads: new Set(['foo']),
        loadError: 'test error',
        reportRawData: null,
        other: otherObject
      });
      expect(newState.other).toBe(otherObject); // other properties are not modified
    });
  });

  describe('LOAD_COMMON_DATA_UNNECESSARY', function() {
    it('removes "common" from pendingLoads', function() {
      const state = Object.freeze({
        pendingLoads: new Set(['foo', 'common']),
        other: otherObject
      });
      const newState = reduce(state, { type: 'LOAD_COMMON_DATA_UNNECESSARY' });
      expect(newState).toEqual({
        pendingLoads: new Set(['foo']),
        other: otherObject
      });
      expect(newState.other).toBe(otherObject); // other properties are not modified
    });
  });

  describe('SELECT_COMPONENT action', function() {
    it('sets selectedComponentIndex to payload and unsets selectedRootAncestor', function() {
      const state = Object.freeze({
        selectedComponentIndex: null,
        selectedRootAncestor: {},
        other: otherObject
      });
      const newState = reduce(state, {type: 'SELECT_COMPONENT', payload: 42});
      expect(newState).toEqual({
        selectedComponentIndex: 42,
        selectedRootAncestor: null,
        other: otherObject
      });
      expect(newState.other).toBe(otherObject); // other properties are not modified
    });
  });

  describe('SELECT_ROOT_ANCESTOR action', function() {
    it('sets selectedRootAncestor to payload', function() {
      const state = Object.freeze({
        selectedRootAncestor: null,
        other: otherObject
      });
      const newState = reduce(state, {type: 'SELECT_ROOT_ANCESTOR', payload: {foo: 'bar'}});
      expect(newState).toEqual({
        selectedRootAncestor: {foo: 'bar'},
        other: otherObject
      });
      expect(newState.other).toBe(otherObject); // other properties are not modified
    });
  });

  describe('UNSELECT_ROOT_ANCESTOR action', function() {
    it('unsets selectedRootAncestor', function() {
      const state = Object.freeze({
        selectedRootAncestor: {},
        other: otherObject
      });
      const newState = reduce(state, {type: 'UNSELECT_ROOT_ANCESTOR'});
      expect(newState).toEqual({
        selectedRootAncestor: null,
        other: otherObject
      });
      expect(newState.other).toBe(otherObject); // other properties are not modified
    });
  });

  describe('SET_AGGREGATE_REPORT_ENTRIES', function() {
    it('sets the aggregate flag from the payload', function() {
      const state = Object.freeze({
            aggregate: false,
            other: otherObject
          }),
          action = { type: 'SET_AGGREGATE_REPORT_ENTRIES', payload: true },
          newState = reduce(state, action);

      expect(newState).toEqual({
        aggregate: true,
        other: otherObject
      });
      expect(newState.other).toBe(otherObject);
    });

    it('updates the displayedEntries in the selectedReport', function() {
      const entries = [{
            hash: '6',
            policyThreatLevel: 10,
            policyName: 'P1',
            grandfathered: false,
            waived: false,
            derivedViolationState: 'open'
          }, {
            hash: '4',
            policyThreatLevel: 8,
            policyName: 'P2',
            grandfathered: false,
            waived: true,
            displayName: {parts: []},
            derivedViolationState: 'open'
          }, {
            hash: '1',
            policyThreatLevel: 6,
            policyName: 'P3',
            grandfathered: false,
            waived: true,
            displayName: {parts: []},
            derivedViolationState: 'open'
          }, {
            hash: '5',
            policyThreatLevel: 4,
            policyName: 'P4',
            grandfathered: false,
            waived: false,
            derivedViolationState: 'open'
          }, {
            hash: '2',
            policyThreatLevel: 3,
            policyName: 'P5',
            grandfathered: false,
            waived: false,
            derivedViolationState: 'open'
          }, {
            hash: '1',
            policyThreatLevel: 1,
            policyName: 'P6',
            grandfathered: false,
            waived: false,
            derivedViolationState: 'open'
          }],
          state = Object.freeze({
            selectedReport: {
              allEntries: entries,
              displayedEntries: entries
            },
            aggregate: false,
            sortFields: ['-policyThreatLevel']
          }),
          newState = reduce(state, {
            type: 'SET_AGGREGATE_REPORT_ENTRIES',
            payload: true
          });

      expect(newState.selectedReport.displayedEntries).toEqual([{
        hash: '6',
        policyThreatLevel: 10,
        policyName: 'P1',
        grandfathered: false,
        waived: false,
        derivedViolationState: 'open'
      }, {
        hash: '5',
        policyThreatLevel: 4,
        policyName: 'P4',
        grandfathered: false,
        waived: false,
        derivedViolationState: 'open'
      }, {
        hash: '2',
        policyThreatLevel: 3,
        policyName: 'P5',
        grandfathered: false,
        waived: false,
        derivedViolationState: 'open'
      }, {
        hash: '1',
        policyThreatLevel: 1,
        policyName: 'P6',
        grandfathered: false,
        waived: false,
        derivedViolationState: 'open'
      }, {
        hash: '4',
        policyThreatLevel: 0,
        policyName: 'None',
        waived: true,
        grandfathered: false,
        displayName: {parts: []},
        derivedViolationState: 'waived'
      }]);
    });
  });

  describe('SET_SORTING', function() {
    it('sets the sorting fields from the payload', function() {
      const state = Object.freeze({
            sortFields: ['foo'],
            other: otherObject
          }),
          action = { type: 'SET_SORTING', payload: ['bar'] },
          newState = reduce(state, action);

      expect(newState).toEqual({
        sortFields: ['bar'],
        other: otherObject
      });
      expect(newState.other).toBe(otherObject);
    });

    it('sorts the displayedEntries in the selectedReport', function() {
      const entries = [{
            policyThreatLevel: 10
          }, {
            policyThreatLevel: 5
          }, {
            policyThreatLevel: 1
          }],
          state = Object.freeze({
            selectedReport: {
              allEntries: entries,
              displayedEntries: entries
            },
            aggregate: false,
            sortFields: ['-policyThreatLevel']
          }),
          newState = reduce(state, {
            type: 'SET_SORTING',
            payload: ['policyThreatLevel']
          });

      expect(newState.selectedReport.displayedEntries).toEqual([{
        policyThreatLevel: 1
      }, {
        policyThreatLevel: 5
      }, {
        policyThreatLevel: 10
      }]);
    });
  });

  describe('SET_EXACT_VALUE_FILTER', function() {
    it('sets the specified property on the exactValueFilters to the specified value', function() {
      const otherFieldFilter = new Set(['asdf']),
          fooFieldFilter = new Set(['bar']),
          state = Object.freeze({
            exactValueFilters: Object.freeze({
              otherField: otherFieldFilter
            }),
            other: otherObject
          }),
          action = {
            type: 'SET_EXACT_VALUE_FILTER',
            payload: {
              fieldName: 'fooField',
              allowedValues: fooFieldFilter
            }
          },
          newState = reduce(state, action);

      expect(newState).toEqual({
        exactValueFilters: {
          fooField: fooFieldFilter,
          otherField: otherFieldFilter
        },
        other: otherObject
      });

      expect(newState.other).toBe(otherObject);
    });

    it('filters the displayedEntries based on the resulting exactValueFilters', function() {
      const state = Object.freeze({
            exactValueFilters: Object.freeze({
              otherField: new Set(['asdf'])
            }),
            selectedReport: Object.freeze({
              allEntries: Object.freeze([{
                fooField: 'bar'
              }, {
                fooField: 'bar',
                otherField: 'asdf'
              }, {
                fooField: 'bar',
                otherField: 'baz'
              }, {
                fooField: 'asdf',
                otherField: 'asdf'
              }, {
                fooField: 'baz',
                otherField: 'asdf'
              }, {
                fooField: 'bar',
                otherField: 'asdf'
              }])
            })
          }),
          action = {
            type: 'SET_EXACT_VALUE_FILTER',
            payload: {
              fieldName: 'fooField',
              allowedValues: new Set(['bar', 'baz'])
            }
          },
          newState = reduce(state, action);

      expect(newState.selectedReport.displayedEntries).toEqual([{
        fooField: 'bar',
        otherField: 'asdf'
      }, {
        fooField: 'baz',
        otherField: 'asdf'
      }, {
        fooField: 'bar',
        otherField: 'asdf'
      }]);

      expect(newState.selectedReport.allEntries).toBe(state.selectedReport.allEntries);
    });
  });

  describe('SET_SUBSTRING_FIELD_FILTER', function() {
    it('sets the specified property on the substringFilters to the specified value', function() {
      const state = Object.freeze({
            substringFilters: Object.freeze({
              otherField: 'asdf'
            }),
            other: otherObject
          }),
          action = {
            type: 'SET_SUBSTRING_FIELD_FILTER',
            payload: {
              fieldName: 'fooField',
              filterString: 'bar'
            }
          },
          newState = reduce(state, action);

      expect(newState).toEqual({
        substringFilters: {
          fooField: 'bar',
          otherField: 'asdf'
        },
        other: otherObject
      });

      expect(newState.other).toBe(otherObject);
    });

    it('filters the displayedEntries based on the resulting substringFilters', function() {
      const state = Object.freeze({
            substringFilters: Object.freeze({
              otherField: 'asdf'
            }),
            selectedReport: Object.freeze({
              allEntries: Object.freeze([{
                otherField: 'asdfasdf',
                fooField: 'qwerty'
              }, {
                otherField: 'asdfasdf',
                fooField: 'bar'
              }, {
                otherField: '',
                fooField: 'bar'
              }, {
                otherField: 'asdfasdf',
                fooField: ''
              }, {
                otherField: 'dfasdfas',
                fooField: 'foobarbaz'
              }, {
                otherField: 'bar',
                fooField: 'asdf'
              }])
            })
          }),
          action = {
            type: 'SET_SUBSTRING_FIELD_FILTER',
            payload: {
              fieldName: 'fooField',
              filterString: 'bar'
            }
          },
          newState = reduce(state, action);

      expect(newState.selectedReport.displayedEntries).toEqual([{
        otherField: 'asdfasdf',
        fooField: 'bar'
      }, {
        otherField: 'dfasdfas',
        fooField: 'foobarbaz'
      }]);

      expect(newState.selectedReport.allEntries).toBe(state.selectedReport.allEntries);
    });
  });

  describe('SET_RAW_DATA_SUBSTRING_FIELD_FILTER', function() {
    it('sets the specified property on the rawDataSubstringFilters to the specified value', function() {
      const state = Object.freeze({
            rawDataSubstringFilters: Object.freeze({
              otherField: 'asdf'
            }),
            other: otherObject
          }),
          action = {
            type: 'SET_RAW_DATA_SUBSTRING_FIELD_FILTER',
            payload: {
              fieldName: 'fooField',
              filterString: 'bar'
            }
          },
          newState = reduce(state, action);

      expect(newState).toEqual({
        rawDataSubstringFilters: {
          fooField: 'bar',
          otherField: 'asdf'
        },
        other: otherObject
      });

      expect(newState.other).toBe(otherObject);
    });

    it('filters the displayedEntries based on the resulting substringFilters', function() {
      const state = Object.freeze({
            rawDataSubstringFilters: Object.freeze({}),
            reportRawData: Object.freeze({
              allEntries: Object.freeze([{
                otherField: 'asdfasdf',
                fooField: 'qwerty'
              }, {
                otherField: 'asdfasdf',
                fooField: 'bar'
              }, {
                otherField: '',
                fooField: 'bar'
              }, {
                otherField: 'asdfasdf',
                fooField: ''
              }, {
                otherField: 'dfasdfas',
                fooField: 'foobarbaz'
              }, {
                otherField: 'bar',
                fooField: 'asdf'
              }])
            })
          }),
          action = {
            type: 'SET_RAW_DATA_SUBSTRING_FIELD_FILTER',
            payload: {
              fieldName: 'fooField',
              filterString: 'bar'
            }
          },
          newState = reduce(state, action);

      expect(newState.reportRawData.displayedEntries).toEqual([{
        otherField: 'asdfasdf',
        fooField: 'bar'
      }, {
        otherField: '',
        fooField: 'bar'
      }, {
        otherField: 'dfasdfas',
        fooField: 'foobarbaz'
      }]);

      expect(newState.reportRawData.allEntries).toBe(state.reportRawData.allEntries);
    });
  });

  describe('SET_RAW_DATA_NUMERIC_FIELD_MAX_FILTER', function() {
    it('sets the max value on the array in rawDataNumericFilters to the specified value', function() {
      const state = Object.freeze({
            rawDataNumericFilters: {
              otherField: [1, 5]
            },
            other: otherObject
          }),
          action = {
            type: 'SET_RAW_DATA_NUMERIC_FIELD_MAX_FILTER',
            payload: {
              fieldName: 'fooField',
              filterValue: 6
            }
          },
          newState = reduce(state, action);

      expect(newState).toEqual({
        rawDataNumericFilters: {
          fooField: [undefined, 6],
          otherField: [1, 5]
        },
        other: otherObject
      });

      expect(newState.other).toBe(otherObject);
    });

    it('doesnt overwrite the minimum value when you set the maximum value of rawDataNumericFilters', function() {
      const state = Object.freeze({
            rawDataNumericFilters: {
              otherField: [1, 5],
              fooField: [2]
            },
            other: otherObject
          }),
          action = {
            type: 'SET_RAW_DATA_NUMERIC_FIELD_MAX_FILTER',
            payload: {
              fieldName: 'fooField',
              filterValue: 6
            }
          },
          newState = reduce(state, action);

      expect(newState).toEqual({
        rawDataNumericFilters: {
          fooField: [2, 6],
          otherField: [1, 5]
        },
        other: otherObject
      });

      expect(newState.other).toBe(otherObject);
    });

    it('filters the displayedEntries based on a maximum numeric filter', function() {
      const state = Object.freeze({
            reportRawData: Object.freeze({
              allEntries: Object.freeze([{
                otherField: 'monkeybrains',
                fooField: 1
              }, {
                otherField: 'asdfasdf',
                fooField: 5
              }, {
                otherField: 'chocolate',
                fooField: 7
              }, {
                otherField: 'asdfasdf'
              }])
            })
          }),
          action = {
            type: 'SET_RAW_DATA_NUMERIC_FIELD_MAX_FILTER',
            payload: {
              fieldName: 'fooField',
              filterValue: 6
            }
          },
          newState = reduce(state, action);

      expect(newState.reportRawData.displayedEntries).toEqual([{
        otherField: 'monkeybrains',
        fooField: 1
      }, {
        otherField: 'asdfasdf',
        fooField: 5
      }]);

      expect(newState.reportRawData.allEntries).toBe(state.reportRawData.allEntries);
    });
  });

  describe('SET_RAW_DATA_NUMERIC_FIELD_MIN_FILTER', function() {
    it('sets the min value on the array in rawDataNumericFilters to the specified value', function() {
      const state = Object.freeze({
            rawDataNumericFilters: {
              otherField: [1, 5]
            },
            other: otherObject
          }),
          action = {
            type: 'SET_RAW_DATA_NUMERIC_FIELD_MIN_FILTER',
            payload: {
              fieldName: 'fooField',
              filterValue: 4
            }
          },
          newState = reduce(state, action);

      expect(newState).toEqual({
        rawDataNumericFilters: {
          fooField: [4],
          otherField: [1, 5]
        },
        other: otherObject
      });

      expect(newState.other).toBe(otherObject);
    });

    it('doesnt overwrite the max value when you set the maximum value of rawDataNumericFilters', function() {
      const state = Object.freeze({
            rawDataNumericFilters: {
              otherField: [1, 5],
              fooField: [undefined, 9]
            },
            other: otherObject
          }),
          action = {
            type: 'SET_RAW_DATA_NUMERIC_FIELD_MIN_FILTER',
            payload: {
              fieldName: 'fooField',
              filterValue: 4
            }
          },
          newState = reduce(state, action);

      expect(newState).toEqual({
        rawDataNumericFilters: {
          fooField: [4, 9],
          otherField: [1, 5]
        },
        other: otherObject
      });

      expect(newState.other).toBe(otherObject);
    });

    it('filters the displayedEntries based on a minimum numeric filter', function() {
      const state = Object.freeze({
            reportRawData: Object.freeze({
              allEntries: Object.freeze([{
                otherField: 'monkeybrains',
                fooField: 1
              }, {
                otherField: 'asdfasdf',
                fooField: 5
              }, {
                otherField: 'chocolate',
                fooField: 7
              }, {
                otherField: 'asdfasdf'
              }])
            })
          }),
          action = {
            type: 'SET_RAW_DATA_NUMERIC_FIELD_MIN_FILTER',
            payload: {
              fieldName: 'fooField',
              filterValue: 4
            }
          },
          newState = reduce(state, action);

      expect(newState.reportRawData.displayedEntries).toEqual([{
        otherField: 'asdfasdf',
        fooField: 5
      }, {
        otherField: 'chocolate',
        fooField: 7
      }]);

      expect(newState.reportRawData.allEntries).toBe(state.reportRawData.allEntries);
    });
  });

  describe('GENERATE_VULNERABILITY_ENTRIES', function() {
    it('sets and sorts the vulnerabilities if the selectedReport and rawDataEntries are both present', function() {
      const state = Object.freeze({
        vulnerabilities: [],
        selectedReport: {
          allEntries: [{
            policyThreatLevel: 7,
            componentIdentifier: {
              format: 'compFormat',
              coordinates: {
                foo: 'bar'
              }
            },
            constraints: [{
              conditions: [{
                conditionTriggerReference: {
                  type: 'SECURITY_VULNERABILITY_REFID',
                  value: 'CVE-1234'
                }
              }]
            }]
          }, {
            policyThreatLevel: 6,
            componentIdentifier: {
              format: 'compFormat',
              coordinates: {
                // different component from above
                foo: 'baz'
              }
            },
            constraints: [{
              conditions: [{
                conditionTriggerReference: {
                  type: 'SECURITY_VULNERABILITY_REFID',
                  value: 'CVE-1235'
                }
              }]
            }]
          }, {
            policyThreatLevel: 6,
            componentIdentifier: {
              format: 'compFormat',
              coordinates: {
                foo: 'bar'
              }
            },
            constraints: [{
              conditions: [{
                conditionTriggerReference: {
                  type: 'SECURITY_VULNERABILITY_REFID',
                  value: 'CVE-1235'
                }
              }]
            }]
          }, {
            policyThreatLevel: 9,
            waived: true,
            componentIdentifier: {
              format: 'compFormat',
              coordinates: {
                foo: 'bar'
              }
            },
            constraints: [{
              conditions: [{
                conditionTriggerReference: {
                  type: 'SECURITY_VULNERABILITY_REFID',
                  value: 'CVE-1237'
                }
              }]
            }]
          }, {
            policyThreatLevel: 6,
            componentIdentifier: {
              format: 'compFormat',
              coordinates: {
                foo: 'bar'
              }
            },
            constraints: [{
              conditions: [{
                conditionTriggerReference: {
                  type: 'SECURITY_VULNERABILITY_REFID',
                  value: 'CVE-1236'
                }
              }]
            }]
          }]
        },
        reportRawData: {
          allEntries: [{
            derivedComponentName: 'bar',
            componentIdentifier: {
              format: 'compFormat',
              coordinates: {
                foo: 'bar'
              }
            },
            securityCode: 'CVE-1234',
            cvssScore: 5
          }, {
            derivedComponentName: 'baz',
            componentIdentifier: {
              format: 'compFormat',
              coordinates: {
                foo: 'baz'
              }
            },
            securityCode: 'CVE-1235',
            cvssScore: 4
          }, {
            derivedComponentName: 'bar',
            componentIdentifier: {
              format: 'compFormat',
              coordinates: {
                foo: 'bar'
              }
            },
            securityCode: 'CVE-1235',
            cvssScore: 4
          }, {
            derivedComponentName: 'bar',
            componentIdentifier: {
              format: 'compFormat',
              coordinates: {
                foo: 'bar'
              }
            },
            securityCode: 'CVE-1236',
            cvssScore: 3
          }, {
            derivedComponentName: 'bar',
            componentIdentifier: {
              format: 'compFormat',
              coordinates: {
                foo: 'bar'
              }
            },
            securityCode: 'CVE-1237',
            cvssScore: 9
          }]
        }
      });

      const newState = reduce(state, { type: 'GENERATE_VULNERABILITY_ENTRIES' });

      expect(newState.vulnerabilities).toEqual([
        jasmine.objectContaining({
          policyThreatLevel: 7,
          securityCode: 'CVE-1234',
          cvssScore: 5,
          derivedComponentName: 'bar'
        }),
        jasmine.objectContaining({
          policyThreatLevel: 6,
          securityCode: 'CVE-1235',
          cvssScore: 4,
          derivedComponentName: 'bar'
        }),
        jasmine.objectContaining({
          policyThreatLevel: 6,
          securityCode: 'CVE-1235',
          cvssScore: 4,
          derivedComponentName: 'baz'
        }),
        jasmine.objectContaining({
          policyThreatLevel: 6,
          securityCode: 'CVE-1236',
          cvssScore: 3,
          derivedComponentName: 'bar'
        }),
        jasmine.objectContaining({
          policyThreatLevel: 0,
          securityCode: 'CVE-1237',
          cvssScore: 9,
          waived: true,
          derivedComponentName: 'bar'
        })
      ]);
    });

    it('returns the unchanged state if selectedReport is not present', function() {
      const state = Object.freeze({
        vulnerabilities: [],
        selectedReport: null,
        reportRawData: {
          allEntries: [{
            derivedComponentName: 'bar',
            componentIdentifier: {
              format: 'compFormat',
              coordinates: {
                foo: 'bar'
              }
            },
            securityCode: 'CVE-1234',
            cvssScore: 5
          }, {
            derivedComponentName: 'baz',
            componentIdentifier: {
              format: 'compFormat',
              coordinates: {
                foo: 'baz'
              }
            },
            securityCode: 'CVE-1235',
            cvssScore: 4
          }, {
            derivedComponentName: 'bar',
            componentIdentifier: {
              format: 'compFormat',
              coordinates: {
                foo: 'bar'
              }
            },
            securityCode: 'CVE-1235',
            cvssScore: 4
          }, {
            derivedComponentName: 'bar',
            componentIdentifier: {
              format: 'compFormat',
              coordinates: {
                foo: 'bar'
              }
            },
            securityCode: 'CVE-1236',
            cvssScore: 3
          }, {
            derivedComponentName: 'bar',
            componentIdentifier: {
              format: 'compFormat',
              coordinates: {
                foo: 'bar'
              }
            },
            securityCode: 'CVE-1237',
            cvssScore: 9
          }]
        }
      });

      const newState = reduce(state, { type: 'GENERATE_VULNERABILITY_ENTRIES' });

      expect(newState.vulnerabilities).toEqual([]);
    });

    it('returns the unchanged state if rawDataEntries is not present', function() {
      const state = Object.freeze({
        vulnerabilities: [],
        selectedReport: {
          allEntries: [{
            policyThreatLevel: 7,
            componentIdentifier: {
              format: 'compFormat',
              coordinates: {
                foo: 'bar'
              }
            },
            constraints: [{
              conditions: [{
                conditionTriggerReference: {
                  type: 'SECURITY_VULNERABILITY_REFID',
                  value: 'CVE-1234'
                }
              }]
            }]
          }, {
            policyThreatLevel: 6,
            componentIdentifier: {
              format: 'compFormat',
              coordinates: {
                // different component from above
                foo: 'baz'
              }
            },
            constraints: [{
              conditions: [{
                conditionTriggerReference: {
                  type: 'SECURITY_VULNERABILITY_REFID',
                  value: 'CVE-1235'
                }
              }]
            }]
          }, {
            policyThreatLevel: 6,
            componentIdentifier: {
              format: 'compFormat',
              coordinates: {
                foo: 'bar'
              }
            },
            constraints: [{
              conditions: [{
                conditionTriggerReference: {
                  type: 'SECURITY_VULNERABILITY_REFID',
                  value: 'CVE-1235'
                }
              }]
            }]
          }, {
            policyThreatLevel: 9,
            waived: true,
            componentIdentifier: {
              format: 'compFormat',
              coordinates: {
                foo: 'bar'
              }
            },
            constraints: [{
              conditions: [{
                conditionTriggerReference: {
                  type: 'SECURITY_VULNERABILITY_REFID',
                  value: 'CVE-1237'
                }
              }]
            }]
          }, {
            policyThreatLevel: 6,
            componentIdentifier: {
              format: 'compFormat',
              coordinates: {
                foo: 'bar'
              }
            },
            constraints: [{
              conditions: [{
                conditionTriggerReference: {
                  type: 'SECURITY_VULNERABILITY_REFID',
                  value: 'CVE-1236'
                }
              }]
            }]
          }]
        }
      });

      const newState = reduce(state, { type: 'GENERATE_VULNERABILITY_ENTRIES' });

      expect(newState.vulnerabilities).toEqual([]);
    });
  });

  describe('SET_SORTING_PARAMETERS action', function() {
    it('adds sorting parameters to state', function() {
      const state = {};
      const newState = reduce(state, {
        type: 'SET_SORTING_PARAMETERS',
        payload: {
          key: 'key',
          sortFields: ['a', 'b'],
          dir: 'dir'
        }
      });
      expect(newState.sortConfiguration).toEqual({
        key: 'key',
        sortFields: ['a', 'b'],
        dir: 'dir'
      });
    });

    it('update sorting parameters to state', function() {
      const state = {
        sortConfiguration: {
          key: 'key',
          sortFields: ['a', 'b'],
          dir: 'dir'
        }
      };
      const newState = reduce(state, {
        type: 'SET_SORTING_PARAMETERS',
        payload: {
          key: 'key2',
          sortFields: ['c', 'd'],
          dir: 'asc'
        }
      });
      expect(newState.sortConfiguration).toEqual({
        key: 'key2',
        sortFields: ['c', 'd'],
        dir: 'asc'
      });
    });
  });
});
