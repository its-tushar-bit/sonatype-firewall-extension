/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

import reduce from 'MainRoot/applicationReport/applicationReportReducer';
import { dependencyTreeData } from '../dependencyTree/dependencyTreeMockData';

describe('applicationReportReducer', function () {
  const otherObject = { value: 'test value' };

  describe('unknown action', function () {
    it('returns original state', function () {
      const state = Object.freeze({ foo: 'bar' });
      const action = { type: 'UNKNOWN' };
      const newState = reduce(state, action);
      expect(newState).toBe(state);
    });
  });

  describe('initial state', function () {
    it('is used if no state is provided', function () {
      const action = { type: 'UNKNOWN' };
      const newState = reduce(undefined, action);
      expect(newState).not.toBeUndefined();
    });

    it('has default fields', function () {
      const action = { type: 'UNKNOWN' };
      const newState = reduce(undefined, action);
      expect(newState.pendingLoads).toEqual(new Set());
      expect(newState.loadError).toBe(null);
      expect(newState.reevaluating).toBe(false);
      expect(newState.reevaluationError).toBe(null);
      expect(newState.selectedReport).toBe(null);
      expect(newState.unscannedComponents).toEqual([]);
      expect(newState.reportHasUnscannedComponents).toBe(false);
      expect(newState.aggregate).toBe(true);
      expect(newState.sortFields).toEqual(['-policyThreatLevel', 'policyName', 'derivedComponentName']);
      expect(newState.exactValueFilters).toEqual({});
      expect(newState.substringFilters).toEqual({});
      expect(newState.rawDataSubstringFilters).toEqual({});
      expect(newState.rawDataNumericFilters).toEqual({});
      expect(newState.isUnknownJs).toBe(false);
      expect(newState.policyTypeFilterEnabled).toBe(true);
      expect(newState.vulnerabilities).toBe(null);
      expect(newState.vulnerabilitiesPageEnabled).toBe(true);
      expect(newState.rawSortConfiguration).toEqual({
        key: 'derivedComponentName',
        sortFields: ['derivedComponentName', 'licenseSortKey', 'securityCode', '-cvssScore'],
        dir: 'asc',
      });
      expect(newState.selectedRootAncestor).toBeNull();
    });

    it('is immutable', function () {
      const action = { type: 'UNKNOWN' };
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

  describe('SET_REPORT_PARAMETERS action', function () {
    it('adds report parameters to state', function () {
      const state = {};
      const newState = reduce(state, {
        type: 'SET_REPORT_PARAMETERS',
        payload: {
          appId: 'appId',
          scanId: 'scanId',
          isUnknownJs: false,
        },
      });
      expect(newState.reportParameters).toEqual({
        appId: 'appId',
        scanId: 'scanId',
        isUnknownJs: false,
      });
    });

    it('sets the state to the initState value', () => {
      const state = {
        foo: 'bar',
        changeMe: 'can haz overwrite plz?',
      };
      const newState = reduce(state, {
        type: 'SET_REPORT_PARAMETERS',
        payload: {
          appId: 'appId',
          scanId: 'scanId',
          isUnknownJs: false,
          isNotFiltered: true,
        },
      });

      expect(newState).toEqual({
        pendingLoads: new Set(),
        filterSidebarOpen: false,
        reevaluating: false,
        loadError: null,
        reevaluationError: null,
        aggregate: true,
        sortFields: ['-policyThreatLevel', 'policyName', 'derivedComponentName'],
        exactValueFilters: {},
        showFilterPopover: false,
        reportRawData: null,
        reportParameters: {
          appId: 'appId',
          scanId: 'scanId',
          isUnknownJs: false,
          isNotFiltered: true,
        },
        substringFilters: {},
        rawDataSubstringFilters: {},
        rawDataNumericFilters: {},
        selectedReport: null,
        unscannedComponents: [],
        reportHasUnscannedComponents: false,
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
          dir: 'desc',
        },
        rawSortConfiguration: {
          key: 'derivedComponentName',
          sortFields: ['derivedComponentName', 'licenseSortKey', 'securityCode', '-cvssScore'],
          dir: 'asc',
        },
        selectedComponent: null,
        dependencyTree: null,
        dependencyTreePageRouterParams: null,
        dependencyTreeSearchTerm: '',
        displayedDependencyTree: null,
        ownerType: 'APPLICATION',
        hostedRepoContext: null,
      });
    });

    it('sets the state to the new state containing same filters when isNotFiltered is undefined', () => {
      const state = {
        foo: 'bar',
        changeMe: 'can haz overwrite plz?',
        exactValueFilters: { derivedViolationState: {}, policyThreatLevel: {} },
        substringFilters: { derivedComponentName: 'test' },
        reportParameters: {
          appId: 'appId',
          scanId: 'scanId',
          isUnknownJs: false,
        },
      };
      const newState = reduce(state, {
        type: 'SET_REPORT_PARAMETERS',
        payload: {
          appId: 'appId',
          scanId: 'scanId',
          isUnknownJs: false,
        },
      });

      expect(newState).toEqual({
        ...newState,
        exactValueFilters: { derivedViolationState: {}, policyThreatLevel: {} },
        substringFilters: { derivedComponentName: 'test' },
      });
    });

    it('sets the state to the new state containing same filters when isNotFiltered is false', () => {
      const state = {
        foo: 'bar',
        changeMe: 'can haz overwrite plz?',
        exactValueFilters: { derivedViolationState: {}, policyThreatLevel: {} },
        substringFilters: { derivedComponentName: 'test' },
        reportParameters: {
          appId: 'appId',
          scanId: 'scanId',
          isUnknownJs: false,
          isNotFiltered: false,
        },
      };
      const newState = reduce(state, {
        type: 'SET_REPORT_PARAMETERS',
        payload: {
          appId: 'appId',
          scanId: 'scanId',
          isUnknownJs: false,
          isNotFiltered: false,
        },
      });

      expect(newState).toEqual({
        ...newState,
        exactValueFilters: { derivedViolationState: {}, policyThreatLevel: {} },
        substringFilters: { derivedComponentName: 'test' },
      });
    });
  });

  describe('LOAD_REPORT_REQUESTED action', function () {
    it('adds "policy" and "common" to pendingLoads and unsets error and selectedReport values', function () {
      const state = Object.freeze({
        pendingLoads: new Set(['foo']),
        loadError: 'test error',
        selectedReport: 'test report',
        other: otherObject,
      });
      const newState = reduce(state, { type: 'LOAD_REPORT_REQUESTED' });
      expect(newState).toEqual({
        pendingLoads: new Set(['foo', 'policy', 'common']),
        loadError: null,
        selectedReport: null,
        other: otherObject,
      });
      expect(newState.other).toBe(otherObject); // other properties are not modified
    });
  });

  describe('LOAD_COMMON_DATA_FULFILLED', function () {
    it('sets the specified bomData, metatdata, and unknownJsData on the state', function () {
      const state = Object.freeze({
        pendingLoads: new Set(['foo']),
        other: otherObject,
      });

      const bomData = {},
        metadata = {},
        unknownJsData = {};

      const newState = reduce(state, {
        type: 'LOAD_COMMON_DATA_FULFILLED',
        payload: { bomData, metadata, unknownJsData },
      });

      expect(newState.bomData).toBe(bomData);
      expect(newState.metadata).toBe(metadata);
      expect(newState.unknownJsData).toBe(unknownJsData);
      expect(newState.other).toBe(otherObject); // other properties are not modified
    });

    it('removes "common" from pendingLoads', function () {
      const state = Object.freeze({
        pendingLoads: new Set(['foo', 'common']),
        other: otherObject,
      });

      const newState = reduce(state, {
        type: 'LOAD_COMMON_DATA_FULFILLED',
        payload: {},
      });

      expect(newState.pendingLoads).toEqual(new Set(['foo']));
    });
  });

  describe('LOAD_REPORT_FULFILLED action', function () {
    it('removes "policy" from pendingLoads and sets selectedReport values without innerSource enabled', function () {
      const state = Object.freeze({
        pendingLoads: new Set(['foo', 'policy']),
        loadError: null,
        selectedReport: null,
        policyTypeFilterEnabled: null,
        other: otherObject,
        sortFields: ['-policyThreatLevel', 'policyName', 'derivedComponentName'],
        dependencyTree: null,
      });

      const entries = [
        { policyThreatLevel: 1, hash: 'a' },
        { policyThreatLevel: 3, hash: 'a' },
        { policyThreatLevel: 4, waived: true, waivedWithAutoWaiver: true, hash: 'b' },
        { policyThreatLevel: 6, hash: 'c' },
        { policyThreatLevel: 9, hash: 'd' },
        { policyThreatLevel: 10, legacyViolation: true, hash: 'e' },
      ];
      const sortedEntries = [
        { policyThreatLevel: 10, legacyViolation: true, hash: 'e' },
        { policyThreatLevel: 9, hash: 'd' },
        { policyThreatLevel: 6, hash: 'c' },
        { policyThreatLevel: 4, waived: true, waivedWithAutoWaiver: true, hash: 'b' },
        { policyThreatLevel: 3, hash: 'a' },
        { policyThreatLevel: 1, hash: 'a' },
      ];
      // Aggregated entries are processed and enhanced with more data
      const aggregatedEntries = [
        {
          policyThreatLevel: 9,
          hash: 'd',
          waived: false,
          waivedWithAutoWaiver: false,
          legacyViolation: false,
          waivedViolations: 0,
        },
        {
          policyThreatLevel: 6,
          hash: 'c',
          waived: false,
          waivedWithAutoWaiver: false,
          legacyViolation: false,
          waivedViolations: 0,
        },
        {
          policyThreatLevel: 3,
          hash: 'a',
          waived: false,
          waivedWithAutoWaiver: false,
          legacyViolation: false,
          waivedViolations: 0,
        },
        {
          policyThreatLevel: 0,
          legacyViolation: undefined,
          waived: true,
          waivedWithAutoWaiver: true,
          hash: 'b',
          policyName: 'None',
          derivedViolationState: 'waived',
          waivedViolations: 1,
        },
        {
          policyThreatLevel: 0,
          legacyViolation: true,
          waived: undefined,
          waivedWithAutoWaiver: false,
          hash: 'e',
          policyName: 'None',
          derivedViolationState: 'legacyViolation',
          waivedViolations: 0,
        },
      ];
      const newState = reduce(state, {
        type: 'LOAD_REPORT_FULFILLED',
        payload: {
          allEntries: entries,
          reportVersion: 3,
          isInnerSourceEnabled: false,
        },
      });

      expect(newState).toEqual({
        pendingLoads: new Set(['foo']),
        loadError: null,
        selectedReport: {
          allEntries: entries,
          displayedEntries: sortedEntries,
          aggregatedEntries,
          unfilteredAggregatedEntries: aggregatedEntries,
          moderateViolationCount: 1,
          severeViolationCount: 1,
          criticalViolationCount: 1,
          nonLowViolationCount: 3,
          activeProxyFailedViolationCount: 0,
          reportVersion: 3,
          isInnerSourceEnabled: false,
        },
        policyTypeFilterEnabled: false,
        vulnerabilitiesPageEnabled: expect.anything(),
        isInnerSourceEnabled: false,
        other: otherObject,
        sortFields: ['-policyThreatLevel', 'policyName', 'derivedComponentName'],
        dependencyTree: null,
      });
      expect(newState.other).toBe(otherObject); // other properties are not modified
    });

    it('removes "policy" from pendingLoads and sets selectedReport values with innerSource enabled', function () {
      const state = Object.freeze({
        pendingLoads: new Set(['foo', 'policy']),
        loadError: null,
        selectedReport: null,
        policyTypeFilterEnabled: null,
        other: otherObject,
        dependencyTree: null,
      });

      const entries = [
        {
          policyThreatLevel: 10,
          hash: 'a',
          legacyViolation: true,
          innerSource: true,
          innerSourceData: [
            {
              ownerApplicationId: '12345',
              ownerApplicationName: 'myISApp',
            },
          ],
        },
        { policyThreatLevel: 10, hash: 'b' },
        {
          policyThreatLevel: 6,
          hash: 'c',
          innerSourceData: [
            {
              ownerApplicationId: '12345',
              ownerApplicationName: 'myISApp',
            },
          ],
        },
      ];
      const aggregatedEntries = [
        {
          policyThreatLevel: 0,
          hash: 'a',
          waived: undefined,
          waivedWithAutoWaiver: false,
          legacyViolation: true,
          innerSource: true,
          innerSourceData: [
            {
              ownerApplicationId: '12345',
              ownerApplicationName: 'myISApp',
            },
          ],
          policyName: 'None',
          derivedViolationState: 'legacyViolation',
          waivedViolations: 0,
        },
        {
          policyThreatLevel: 10,
          hash: 'b',
          waived: false,
          waivedWithAutoWaiver: false,
          legacyViolation: false,
          waivedViolations: 0,
        },
        {
          policyThreatLevel: 6,
          hash: 'c',
          innerSourceData: [
            {
              ownerApplicationId: '12345',
              ownerApplicationName: 'myISApp',
            },
          ],
          waived: false,
          waivedWithAutoWaiver: false,
          legacyViolation: false,
          waivedViolations: 0,
        },
      ];
      const newState = reduce(state, {
        type: 'LOAD_REPORT_FULFILLED',
        payload: {
          allEntries: entries,
          reportVersion: 3,
          isInnerSourceEnabled: true,
        },
      });

      expect(newState).toEqual({
        pendingLoads: new Set(['foo']),
        loadError: null,
        selectedReport: {
          allEntries: entries,
          aggregatedEntries,
          unfilteredAggregatedEntries: aggregatedEntries,
          displayedEntries: [
            {
              hash: 'a',
              legacyViolation: true,
              innerSource: true,
              innerSourceData: [
                {
                  ownerApplicationName: 'myISApp',
                  ownerApplicationId: '12345',
                },
              ],
              policyThreatLevel: 10,
            },
            {
              policyThreatLevel: 10,
              hash: 'b',
            },
            {
              hash: 'c',
              innerSourceData: [
                {
                  ownerApplicationName: 'myISApp',
                  ownerApplicationId: '12345',
                },
              ],
              policyThreatLevel: 6,
            },
          ],
          moderateViolationCount: 0,
          severeViolationCount: 1,
          criticalViolationCount: 1,
          nonLowViolationCount: 2,
          activeProxyFailedViolationCount: 0,
          reportVersion: 3,
          isInnerSourceEnabled: true,
        },
        policyTypeFilterEnabled: false,
        vulnerabilitiesPageEnabled: expect.anything(),
        isInnerSourceEnabled: true,
        dependencyTree: null,
        other: otherObject,
      });
      expect(newState.other).toBe(otherObject); // other properties are not modified
    });

    it('sets vulnerabilitiesPageEnabled to true if the reportVersion is at least 5', function () {
      const state = Object.freeze({
          pendingLoads: new Set(),
        }),
        newStateV6 = reduce(state, {
          type: 'LOAD_REPORT_FULFILLED',
          payload: {
            allEntries: [],
            reportVersion: 6,
          },
        }),
        newStateV5 = reduce(state, {
          type: 'LOAD_REPORT_FULFILLED',
          payload: {
            allEntries: [],
            reportVersion: 5,
          },
        }),
        newStateV4 = reduce(state, {
          type: 'LOAD_REPORT_FULFILLED',
          payload: {
            allEntries: [],
            reportVersion: 4,
            dependencies: {
              dependencyTree: {},
            },
          },
        }),
        newStateV1 = reduce(state, {
          type: 'LOAD_REPORT_FULFILLED',
          payload: {
            allEntries: [],
            reportVersion: 1,
          },
        }),
        newStateVNil = reduce(state, {
          type: 'LOAD_REPORT_FULFILLED',
          payload: {
            allEntries: [],
          },
        });

      expect(newStateVNil.vulnerabilitiesPageEnabled).toBe(false);
      expect(newStateV1.vulnerabilitiesPageEnabled).toBe(false);
      expect(newStateV4.vulnerabilitiesPageEnabled).toBe(false);
      expect(newStateV5.vulnerabilitiesPageEnabled).toBe(true);
      expect(newStateV6.vulnerabilitiesPageEnabled).toBe(true);
    });

    it('sets the displayedEntries in the selectedReport based on the current aggregation etc settings', function () {
      const state = Object.freeze({
          selectedReport: null,
          aggregate: true,
          exactValueFilters: {
            policyThreatLevel: new Set([1, 4, 5, 6]),
          },
          sortFields: ['-policyThreatLevel'],
        }),
        entries = [
          {
            hash: '1',
            policyThreatLevel: 1,
            policyName: 'P1',
            waived: false,
            waivedWithAutoWaiver: false,
            legacyViolation: false,
          },
          {
            hash: '2',
            policyThreatLevel: 3,
            policyName: 'P2',
            waived: false,
            waivedWithAutoWaiver: false,
            legacyViolation: false,
          },
          {
            hash: '4',
            policyThreatLevel: 4,
            policyName: 'P3',
            waived: true,
            waivedWithAutoWaiver: true,
            legacyViolation: false,
            displayName: { parts: [] },
          },
          {
            hash: '1',
            policyThreatLevel: 6,
            policyName: 'P4',
            waived: false,
            waivedWithAutoWaiver: false,
            legacyViolation: false,
          },
          {
            hash: '5',
            policyThreatLevel: 4,
            policyName: 'P5',
            waived: false,
            waivedWithAutoWaiver: false,
            legacyViolation: false,
          },
          {
            hash: '6',
            policyThreatLevel: 10,
            policyName: 'P6',
            waived: false,
            waivedWithAutoWaiver: false,
            legacyViolation: true,
            displayName: { parts: [] },
          },
        ],
        aggregatedEntries = [
          {
            hash: '1',
            policyThreatLevel: 6,
            policyName: 'P4',
            waived: false,
            waivedWithAutoWaiver: false,
            legacyViolation: false,
            waivedViolations: 0,
          },
          {
            hash: '5',
            policyThreatLevel: 4,
            policyName: 'P5',
            waived: false,
            waivedWithAutoWaiver: false,
            legacyViolation: false,
            waivedViolations: 0,
          },
        ],
        newState = reduce(state, {
          type: 'LOAD_REPORT_FULFILLED',
          payload: {
            allEntries: entries,
            reportVersion: 3,
          },
        });

      expect(newState.selectedReport.displayedEntries).toEqual(aggregatedEntries);
      expect(newState.selectedReport.aggregatedEntries).toEqual(aggregatedEntries);
    });

    it('sets the aggregatedEntries in the selectedReport regardless of aggregation settings', function () {
      const state = Object.freeze({
          selectedReport: null,
          aggregate: false,
          exactValueFilters: {
            policyThreatLevel: new Set([1, 4, 5, 6]),
          },
          sortFields: ['-policyThreatLevel'],
        }),
        entries = [
          {
            hash: '1',
            policyThreatLevel: 1,
            policyName: 'P1',
            waived: false,
            waivedWithAutoWaiver: false,
            legacyViolation: false,
          },
          {
            hash: '2',
            policyThreatLevel: 3,
            policyName: 'P2',
            waived: false,
            waivedWithAutoWaiver: false,
            legacyViolation: false,
          },
          {
            hash: '4',
            policyThreatLevel: 4,
            policyName: 'P3',
            waived: true,
            waivedWithAutoWaiver: true,
            legacyViolation: false,
            displayName: { parts: [] },
          },
          {
            hash: '1',
            policyThreatLevel: 6,
            policyName: 'P4',
            waived: false,
            waivedWithAutoWaiver: false,
            legacyViolation: false,
          },
          {
            hash: '5',
            policyThreatLevel: 4,
            policyName: 'P5',
            waived: false,
            waivedWithAutoWaiver: false,
            legacyViolation: false,
          },
          {
            hash: '6',
            policyThreatLevel: 10,
            policyName: 'P6',
            waived: false,
            waivedWithAutoWaiver: false,
            legacyViolation: true,
            displayName: { parts: [] },
          },
        ],
        aggregatedEntries = [
          {
            hash: '1',
            policyThreatLevel: 6,
            policyName: 'P4',
            waived: false,
            waivedWithAutoWaiver: false,
            waivedViolations: 0,
            legacyViolation: false,
          },
          {
            hash: '5',
            policyThreatLevel: 4,
            policyName: 'P5',
            waived: false,
            waivedWithAutoWaiver: false,
            waivedViolations: 0,
            legacyViolation: false,
          },
        ],
        displayedEntries = [
          {
            hash: '1',
            policyThreatLevel: 6,
            policyName: 'P4',
            waived: false,
            waivedWithAutoWaiver: false,
            legacyViolation: false,
          },
          {
            hash: '4',
            policyThreatLevel: 4,
            policyName: 'P3',
            waived: true,
            waivedWithAutoWaiver: true,
            legacyViolation: false,
            displayName: { parts: [] },
          },
          {
            hash: '5',
            policyThreatLevel: 4,
            policyName: 'P5',
            waived: false,
            waivedWithAutoWaiver: false,
            legacyViolation: false,
          },
          {
            hash: '1',
            policyThreatLevel: 1,
            policyName: 'P1',
            waived: false,
            waivedWithAutoWaiver: false,
            legacyViolation: false,
          },
        ],
        newState = reduce(state, {
          type: 'LOAD_REPORT_FULFILLED',
          payload: {
            allEntries: entries,
            reportVersion: 3,
          },
        });

      expect(newState.selectedReport.displayedEntries).toEqual(displayedEntries);
      expect(newState.selectedReport.aggregatedEntries).toEqual(aggregatedEntries);
    });

    it('sets selectedComponentIndex while in aggregated mode if a component was previously selected', function () {
      const state = Object.freeze({
        pendingLoads: new Set(['policy']),
        loadError: null,
        aggregate: true,
        sortFields: ['derivedComponentName'],
        selectedComponentIndex: 0,
        selectedReport: {
          displayedEntries: [
            {
              hash: '1',
              derivedComponentName: '1',
            },
            {
              hash: '2',
              derivedComponentName: '2',
            },
            {
              hash: '3',
              derivedComponentName: '3',
            },
          ],
        },
        other: otherObject,
      });

      const entries = [
        {
          hash: '1',
          derivedComponentName: '5',
        },
        {
          hash: '2',
          derivedComponentName: '2',
        },
        {
          hash: '3',
          derivedComponentName: '3',
        },
      ];

      const newState = reduce(state, {
        type: 'LOAD_REPORT_FULFILLED',
        payload: {
          allEntries: entries,
          reportVersion: 3,
        },
      });

      expect(newState.selectedComponentIndex).toBe(2);
    });

    it('sets selectedComponentIndex while in non aggregated mode if a component was previously selected', function () {
      const state = Object.freeze({
        pendingLoads: new Set(['policy']),
        loadError: null,
        aggregate: false,
        sortFields: ['derivedComponentName', 'policyName'],
        selectedComponentIndex: 1,
        selectedReport: {
          displayedEntries: [
            {
              hash: '1',
              policyName: 'P1',
              derivedComponentName: '1',
            },
            {
              hash: '1',
              policyName: 'P2',
              derivedComponentName: '1',
            },
            {
              hash: '2',
              policyName: 'P2',
              derivedComponentName: '2',
            },
            {
              hash: '3',
              policyName: 'P3',
              derivedComponentName: '3',
            },
          ],
        },
        other: otherObject,
      });

      const entries = [
        {
          hash: '1',
          policyName: 'P1',
          derivedComponentName: '5',
        },
        {
          hash: '1',
          policyName: 'P2',
          derivedComponentName: '5',
        },
        {
          hash: '2',
          policyName: 'P2',
          derivedComponentName: '2',
        },
        {
          hash: '3',
          policyName: 'P3',
          derivedComponentName: '3',
        },
      ];

      const newState = reduce(state, {
        type: 'LOAD_REPORT_FULFILLED',
        payload: {
          allEntries: entries,
          reportVersion: 3,
        },
      });

      expect(newState.selectedComponentIndex).toBe(3);
    });

    it('sets policyTypeFilterEnabled to true if the report version is bigger than 3', function () {
      const state = Object.freeze({
        policyTypeFilterEnabled: false,
        other: otherObject,
      });
      const newState = reduce(state, {
        type: 'LOAD_REPORT_FULFILLED',
        payload: {
          allEntries: [],
          metadata: { reportTitle: 'test' },
          isUnknownJs: false,
          reportVersion: 4,
        },
      });
      expect(newState.policyTypeFilterEnabled).toBe(true);
      expect(newState.other).toBe(otherObject); //confirm no side-effects
    });

    it('sets policyTypeFilterEnabled to false if the report version is lower than 4', function () {
      const state = Object.freeze({
        policyTypeFilterEnabled: true,
        other: otherObject,
      });
      const newState = reduce(state, {
        type: 'LOAD_REPORT_FULFILLED',
        payload: {
          allEntries: [],
          metadata: { reportTitle: 'test' },
          isUnknownJs: false,
          reportVersion: 3,
        },
      });
      expect(newState.policyTypeFilterEnabled).toBe(false);
      expect(newState.other).toBe(otherObject);
    });

    it('sets reportHasUnscannedComponents to true if any selectedReport entries have scanError: true', function () {
      const state = Object.freeze({
        reportHasUnscannedComponents: false,
      });
      const newState = reduce(state, {
        type: 'LOAD_REPORT_FULFILLED',
        payload: {
          allEntries: [{ scanError: false }, { scanError: true }, { scanError: false }],
          metadata: { reportTitle: 'test unscanned components' },
        },
      });
      expect(newState.reportHasUnscannedComponents).toBe(true);
    });

    describe('dependency tree', () => {
      let entries;
      beforeEach(() => {
        entries = [
          {
            policyThreatLevel: 1,
            hash: 'a',
            componentIdentifier: {
              format: 'maven',
              coordinates: {
                artifactId: 'logback-access',
                classifier: '',
                extension: 'jar',
                groupId: 'ch.qos.logback',
                version: '0.6',
              },
            },
            derivedComponentName: 'logback-access : ch.qos.logback : 0.6',
            innerSource: true,
            directDependency: true,
            dependencyInfo: { isDirectDependency: true },
          },
          { policyThreatLevel: 3, hash: 'b' },
        ];
      });

      it('sets "dependencyTree" in the applicationReport', () => {
        const state = Object.freeze({
          selectedReport: null,
          dependencyTree: null,
          aggregatedEntries: [
            {
              policyThreatLevel: 1,
              hash: 'a',
              waived: false,
              legacyViolation: false,
              componentIdentifier: {
                format: 'maven',
                coordinates: {
                  artifactId: 'logback-access',
                  classifier: '',
                  extension: 'jar',
                  groupId: 'ch.qos.logback',
                  version: '0.6',
                },
              },
              derivedComponentName: 'logback-access : ch.qos.logback : 0.6',
              innerSource: true,
              directDependency: true,
              dependencyInfo: { isDirectDependency: true },
            },
            { policyThreatLevel: 3, hash: 'b' },
          ],
        });

        const newState = reduce(state, {
          type: 'LOAD_REPORT_FULFILLED',
          payload: {
            allEntries: entries,
            dependencies: {
              dependencyTree: {
                children: [
                  {
                    componentIdentifier: {
                      format: 'maven',
                      coordinates: {
                        artifactId: 'logback-access',
                        classifier: '',
                        extension: 'jar',
                        groupId: 'ch.qos.logback',
                        version: '0.6',
                      },
                    },
                  },
                ],
              },
            },
          },
        });

        expect(newState.dependencyTree).toEqual([
          {
            children: null,
            displayName: 'logback-access : ch.qos.logback : 0.6',
            hash: 'a',
            isOpen: true,
            policyThreatLevel: 1,
            treePath: [0],
            originalTreePath: [0],
            isInnerSource: true,
          },
        ]);
      });
    });
  });

  describe('LOAD_REPORT_RAW_DATA_REQUESTED', () => {
    it('adds "raw" and "common" to the pendingLoads and unsets error and reportRawData values', function () {
      const state = Object.freeze({
        pendingLoads: new Set(['foo']),
        loadError: 'test error',
        reportRawData: 'test report',
        other: otherObject,
      });
      const newState = reduce(state, {
        type: 'LOAD_REPORT_RAW_DATA_REQUESTED',
      });
      expect(newState).toEqual({
        pendingLoads: new Set(['foo', 'raw', 'common']),
        loadError: null,
        reportRawData: null,
        other: otherObject,
      });
      expect(newState.other).toBe(otherObject); // other properties are not modified
    });
  });

  describe('SELECT_COMPONENT action', function () {
    it('set selectedComponent, selectedComponentIndex values and unset selectedRootAncestor value', function () {
      const state = Object.freeze({
        selectedComponent: {},
        selectedComponentIndex: 0,
        other: otherObject,
      });
      const selectedComponent = {
        component: 'myComponent',
        componentIndex: 2,
      };
      const newState = reduce(state, {
        type: 'SELECT_COMPONENT',
        payload: selectedComponent,
      });
      expect(newState).toEqual({
        selectedComponent: selectedComponent.component,
        selectedComponentIndex: selectedComponent.componentIndex,
        selectedRootAncestor: null,
        other: otherObject,
      });
      expect(newState.other).toBe(otherObject); // other properties are not modified
    });
  });

  describe('LOAD_REPORT_ALL_DATA_REQUESTED', () => {
    it('adds "policy", "raw" and "common" to the pendingLoads', function () {
      const state = Object.freeze({
        pendingLoads: new Set(['foo']),
        other: otherObject,
      });
      const newState = reduce(state, {
        type: 'LOAD_REPORT_ALL_DATA_REQUESTED',
      });
      expect(newState).toEqual({
        pendingLoads: new Set(['foo', 'raw', 'common', 'policy']),
        other: otherObject,
      });
      expect(newState.other).toBe(otherObject); // other properties are not modified
    });
  });

  describe('LOAD_REPORT_RAW_DATA_FULFILLED action', () => {
    it('removes "raw" from pendingLoads and does not change other values on the state', function () {
      const state = Object.freeze({
        pendingLoads: new Set(['foo', 'raw']),
        rawSortConfiguration: {
          key: 'derivedComponentName',
          sortFields: ['derivedComponentName', 'licenseSortKey', 'securityCode', '-cvssScore'],
          dir: 'asc',
        },
        other: otherObject,
      });
      const newState = reduce(state, {
        type: 'LOAD_REPORT_RAW_DATA_FULFILLED',
        payload: [],
      });
      expect(newState.pendingLoads).toEqual(new Set(['foo']));
      expect(newState.other).toBe(otherObject); // other properties are not modified
    });

    it('sets the raw data information on the allEntries section of reportRawData state', () => {
      const state = {
        rawSortConfiguration: {
          key: 'derivedComponentName',
          sortFields: ['derivedComponentName', 'licenseSortKey', 'securityCode', '-cvssScore'],
          dir: 'asc',
        },
      };
      const rawDataEntries = [
        {
          derivedComponentName: 'foo',
          license: 'undefined',
        },
      ];

      const newState = reduce(state, {
        type: 'LOAD_REPORT_RAW_DATA_FULFILLED',
        payload: rawDataEntries,
      });

      expect(newState.reportRawData.allEntries).toEqual(rawDataEntries);
    });

    it('sets the appropriate raw data information on the displayedEntries section of reportRawData state', () => {
      const state = {
        rawSortConfiguration: {
          key: 'derivedComponentName',
          sortFields: ['derivedComponentName', 'licenseSortKey', 'securityCode', '-cvssScore'],
          dir: 'asc',
        },
      };
      const rawDataEntries = [
        {
          derivedComponentName: 'foo',
          license: 'undefined',
          securityCode: 'code-404',
          cvssScore: 4,
        },
      ];

      const newState = reduce(state, {
        type: 'LOAD_REPORT_RAW_DATA_FULFILLED',
        payload: rawDataEntries,
      });

      expect(newState.reportRawData.displayedEntries).toEqual([
        {
          ...rawDataEntries[0],
          key: 'null\u001dcode-404',
          cvssScore: '4.0',
        },
      ]);
    });
  });

  describe('REEVALUATE_REPORT_REQUESTED action', function () {
    it('sets reevaluating flag and unsets reevaluationError', function () {
      const state = Object.freeze({
        reevaluating: false,
        reevaluationError: 'Error',
        other: otherObject,
      });
      const newState = reduce(state, { type: 'REEVALUATE_REPORT_REQUESTED' });
      expect(newState).toEqual({
        reevaluating: true,
        reevaluationError: null,
        other: otherObject,
      });
      expect(newState.other).toBe(otherObject); // other properties are not modified
    });
  });

  describe('REEVALUATE_REPORT_FULFILLED action', function () {
    it('unsets reevaluating flag and reevaluationError', function () {
      const state = Object.freeze({
        reevaluating: true,
        reevaluationError: 'asdf',
        other: otherObject,
      });
      const newState = reduce(state, { type: 'REEVALUATE_REPORT_FULFILLED' });
      expect(newState).toEqual({
        reevaluating: false,
        reevaluationError: null,
        other: otherObject,
      });
      expect(newState.other).toBe(otherObject); // other properties are not modified
    });
  });

  describe('REEVALUATE_REPORT_CANCELLED action', function () {
    it('unsets reevaluating flag and reevaluationError', function () {
      const state = Object.freeze({
        reevaluating: true,
        reevaluationError: 'asdf',
        other: otherObject,
      });
      const newState = reduce(state, { type: 'REEVALUATE_REPORT_CANCELLED' });
      expect(newState).toEqual({
        reevaluating: false,
        reevaluationError: null,
        other: otherObject,
      });
      expect(newState.other).toBe(otherObject); // other properties are not modified
    });
  });

  describe('REEVALUATE_REPORT_FAILED action', function () {
    it('unsets reevaluating flag and sets the reevaluationError to the payload', function () {
      const state = Object.freeze({
        reevaluating: true,
        reevaluationError: null,
        other: otherObject,
      });
      const payload = 'Error!';
      const newState = reduce(state, {
        type: 'REEVALUATE_REPORT_FAILED',
        payload,
      });
      expect(newState).toEqual({
        reevaluating: false,
        reevaluationError: payload,
        other: otherObject,
      });
      expect(newState.other).toBe(otherObject); // other properties are not modified
    });
  });

  describe('LOAD_REPORT_FAILED action', function () {
    it('removes "policy" from pendingLoads and sets error value', function () {
      const state = Object.freeze({
        pendingLoads: new Set(['foo', 'policy']),
        loadError: null,
        selectedReport: null,
        other: otherObject,
      });
      const newState = reduce(state, {
        type: 'LOAD_REPORT_FAILED',
        payload: 'test error',
      });
      expect(newState).toEqual({
        pendingLoads: new Set(['foo']),
        loadError: 'test error',
        selectedReport: null,
        other: otherObject,
      });
      expect(newState.other).toBe(otherObject); // other properties are not modified
    });
  });

  describe('LOAD_REPORT_RAW_DATA_FAILED action', function () {
    it('removes "raw" from pendingLoads and sets error value', function () {
      const state = Object.freeze({
        pendingLoads: new Set(['foo', 'raw']),
        loadError: null,
        reportRawData: null,
        other: otherObject,
      });
      const newState = reduce(state, {
        type: 'LOAD_REPORT_RAW_DATA_FAILED',
        payload: 'test error',
      });
      expect(newState).toEqual({
        pendingLoads: new Set(['foo']),
        loadError: 'test error',
        reportRawData: null,
        other: otherObject,
      });
      expect(newState.other).toBe(otherObject); // other properties are not modified
    });
  });

  describe('LOAD_COMMON_DATA_FAILED action', function () {
    it('removes "common" from pendingLoads and sets error value', function () {
      const state = Object.freeze({
        pendingLoads: new Set(['foo', 'common']),
        loadError: null,
        reportRawData: null,
        other: otherObject,
      });
      const newState = reduce(state, {
        type: 'LOAD_COMMON_DATA_FAILED',
        payload: 'test error',
      });
      expect(newState).toEqual({
        pendingLoads: new Set(['foo']),
        loadError: 'test error',
        reportRawData: null,
        other: otherObject,
      });
      expect(newState.other).toBe(otherObject); // other properties are not modified
    });
  });

  describe('LOAD_COMMON_DATA_UNNECESSARY', function () {
    it('removes "common" from pendingLoads', function () {
      const state = Object.freeze({
        pendingLoads: new Set(['foo', 'common']),
        other: otherObject,
      });
      const newState = reduce(state, { type: 'LOAD_COMMON_DATA_UNNECESSARY' });
      expect(newState).toEqual({
        pendingLoads: new Set(['foo']),
        other: otherObject,
      });
      expect(newState.other).toBe(otherObject); // other properties are not modified
    });
  });

  describe('SELECT_ROOT_ANCESTOR action', function () {
    it('sets selectedRootAncestor to payload', function () {
      const state = Object.freeze({
        selectedRootAncestor: null,
        other: otherObject,
      });
      const newState = reduce(state, {
        type: 'SELECT_ROOT_ANCESTOR',
        payload: { foo: 'bar' },
      });
      expect(newState).toEqual({
        selectedRootAncestor: { foo: 'bar' },
        other: otherObject,
      });
      expect(newState.other).toBe(otherObject); // other properties are not modified
    });
  });

  describe('UNSELECT_ROOT_ANCESTOR action', function () {
    it('unsets selectedRootAncestor', function () {
      const state = Object.freeze({
        selectedRootAncestor: {},
        other: otherObject,
      });
      const newState = reduce(state, { type: 'UNSELECT_ROOT_ANCESTOR' });
      expect(newState).toEqual({
        selectedRootAncestor: null,
        other: otherObject,
      });
      expect(newState.other).toBe(otherObject); // other properties are not modified
    });
  });

  describe('TOGGLE_AGGREGATE_REPORT_ENTRIES', function () {
    it('toggles the aggregate flag', function () {
      const state = Object.freeze({
          aggregate: false,
          other: otherObject,
        }),
        action = { type: 'TOGGLE_AGGREGATE_REPORT_ENTRIES' },
        newState = reduce(state, action);

      expect(newState).toEqual({
        aggregate: true,
        other: otherObject,
      });
      expect(newState.other).toBe(otherObject);
    });

    it('updates the displayedEntries in the selectedReport', function () {
      const entries = [
          {
            hash: '6',
            policyThreatLevel: 10,
            policyName: 'P1',
            legacyViolation: false,
            waived: false,
            waivedWithAutoWaiver: false,
            derivedViolationState: 'open',
          },
          {
            hash: '4',
            policyThreatLevel: 8,
            policyName: 'P2',
            legacyViolation: false,
            waived: true,
            waivedWithAutoWaiver: true,
            displayName: { parts: [] },
            derivedViolationState: 'open',
          },
          {
            hash: '1',
            policyThreatLevel: 6,
            policyName: 'P3',
            legacyViolation: false,
            waived: true,
            waivedWithAutoWaiver: true,
            displayName: { parts: [] },
            derivedViolationState: 'open',
          },
          {
            hash: '5',
            policyThreatLevel: 4,
            policyName: 'P4',
            legacyViolation: false,
            waived: false,
            waivedWithAutoWaiver: false,
            derivedViolationState: 'open',
          },
          {
            hash: '2',
            policyThreatLevel: 3,
            policyName: 'P5',
            legacyViolation: false,
            waived: false,
            waivedWithAutoWaiver: false,
            derivedViolationState: 'open',
          },
          {
            hash: '1',
            policyThreatLevel: 1,
            policyName: 'P6',
            legacyViolation: false,
            waived: false,
            waivedWithAutoWaiver: false,
            derivedViolationState: 'open',
          },
        ],
        state = Object.freeze({
          selectedReport: {
            allEntries: entries,
            displayedEntries: entries,
          },
          aggregate: false,
          sortFields: ['-policyThreatLevel'],
        }),
        newState = reduce(state, { type: 'TOGGLE_AGGREGATE_REPORT_ENTRIES' });

      expect(newState.selectedReport.displayedEntries).toEqual([
        {
          hash: '6',
          policyThreatLevel: 10,
          policyName: 'P1',
          legacyViolation: false,
          waived: false,
          waivedWithAutoWaiver: false,
          derivedViolationState: 'open',
          waivedViolations: 0,
        },
        {
          hash: '5',
          policyThreatLevel: 4,
          policyName: 'P4',
          legacyViolation: false,
          waived: false,
          waivedWithAutoWaiver: false,
          derivedViolationState: 'open',
          waivedViolations: 0,
        },
        {
          hash: '2',
          policyThreatLevel: 3,
          policyName: 'P5',
          legacyViolation: false,
          waived: false,
          waivedWithAutoWaiver: false,
          derivedViolationState: 'open',
          waivedViolations: 0,
        },
        {
          hash: '1',
          policyThreatLevel: 1,
          policyName: 'P6',
          legacyViolation: false,
          waived: false,
          waivedWithAutoWaiver: true,
          derivedViolationState: 'open',
          waivedViolations: 1,
        },
        {
          hash: '4',
          policyThreatLevel: 0,
          policyName: 'None',
          waived: true,
          waivedWithAutoWaiver: true,
          legacyViolation: false,
          displayName: { parts: [] },
          derivedViolationState: 'waived',
          waivedViolations: 1,
        },
      ]);
    });
  });

  describe('SET_SORTING', function () {
    it('sets the sorting fields from the payload', function () {
      const state = Object.freeze({
          sortFields: ['foo'],
          other: otherObject,
        }),
        action = { type: 'SET_SORTING', payload: ['bar'] },
        newState = reduce(state, action);

      expect(newState).toEqual({
        sortFields: ['bar'],
        other: otherObject,
      });
      expect(newState.other).toBe(otherObject);
    });

    it('sorts the displayedEntries in the selectedReport', function () {
      const entries = [
          {
            policyThreatLevel: 10,
          },
          {
            policyThreatLevel: 5,
          },
          {
            policyThreatLevel: 1,
          },
        ],
        state = Object.freeze({
          selectedReport: {
            allEntries: entries,
            displayedEntries: entries,
          },
          aggregate: false,
          sortFields: ['-policyThreatLevel'],
        }),
        newState = reduce(state, {
          type: 'SET_SORTING',
          payload: ['policyThreatLevel'],
        });

      expect(newState.selectedReport.displayedEntries).toEqual([
        {
          policyThreatLevel: 1,
        },
        {
          policyThreatLevel: 5,
        },
        {
          policyThreatLevel: 10,
        },
      ]);
    });
  });

  describe('SET_EXACT_VALUE_FILTER', function () {
    it('sets the specified property on the exactValueFilters to the specified value', function () {
      const otherFieldFilter = new Set(['asdf']),
        fooFieldFilter = new Set(['bar']),
        state = Object.freeze({
          exactValueFilters: Object.freeze({
            otherField: otherFieldFilter,
          }),
          other: otherObject,
        }),
        action = {
          type: 'SET_EXACT_VALUE_FILTER',
          payload: {
            fieldName: 'fooField',
            allowedValues: fooFieldFilter,
          },
        },
        newState = reduce(state, action);

      expect(newState).toEqual({
        exactValueFilters: {
          fooField: fooFieldFilter,
          otherField: otherFieldFilter,
        },
        other: otherObject,
      });

      expect(newState.other).toBe(otherObject);
    });

    it('filters the displayedEntries based on the resulting exactValueFilters', function () {
      const state = Object.freeze({
          exactValueFilters: Object.freeze({
            otherField: new Set(['asdf']),
          }),
          selectedReport: Object.freeze({
            allEntries: Object.freeze([
              {
                fooField: 'bar',
              },
              {
                fooField: 'bar',
                otherField: 'asdf',
              },
              {
                fooField: 'bar',
                otherField: 'baz',
              },
              {
                fooField: 'asdf',
                otherField: 'asdf',
              },
              {
                fooField: 'baz',
                otherField: 'asdf',
              },
              {
                fooField: 'bar',
                otherField: 'asdf',
              },
            ]),
          }),
        }),
        action = {
          type: 'SET_EXACT_VALUE_FILTER',
          payload: {
            fieldName: 'fooField',
            allowedValues: new Set(['bar', 'baz']),
          },
        },
        newState = reduce(state, action);

      expect(newState.selectedReport.displayedEntries).toEqual([
        {
          fooField: 'bar',
          otherField: 'asdf',
        },
        {
          fooField: 'baz',
          otherField: 'asdf',
        },
        {
          fooField: 'bar',
          otherField: 'asdf',
        },
      ]);

      expect(newState.selectedReport.allEntries).toBe(state.selectedReport.allEntries);
    });
  });

  describe('SET_SUBSTRING_FIELD_FILTER', function () {
    it('sets the specified property on the substringFilters to the specified value', function () {
      const state = Object.freeze({
          substringFilters: Object.freeze({
            otherField: 'asdf',
          }),
          other: otherObject,
        }),
        action = {
          type: 'SET_SUBSTRING_FIELD_FILTER',
          payload: {
            fieldName: 'fooField',
            filterString: 'bar',
          },
        },
        newState = reduce(state, action);

      expect(newState).toEqual({
        substringFilters: {
          fooField: 'bar',
          otherField: 'asdf',
        },
        other: otherObject,
      });

      expect(newState.other).toBe(otherObject);
    });

    it('filters the displayedEntries based on the resulting substringFilters', function () {
      const state = Object.freeze({
          substringFilters: Object.freeze({
            otherField: 'asdf',
          }),
          selectedReport: Object.freeze({
            allEntries: Object.freeze([
              {
                otherField: 'asdfasdf',
                fooField: 'qwerty',
              },
              {
                otherField: 'asdfasdf',
                fooField: 'bar',
              },
              {
                otherField: '',
                fooField: 'bar',
              },
              {
                otherField: 'asdfasdf',
                fooField: '',
              },
              {
                otherField: 'dfasdfas',
                fooField: 'foobarbaz',
              },
              {
                otherField: 'bar',
                fooField: 'asdf',
              },
            ]),
          }),
        }),
        action = {
          type: 'SET_SUBSTRING_FIELD_FILTER',
          payload: {
            fieldName: 'fooField',
            filterString: 'bar',
          },
        },
        newState = reduce(state, action);

      expect(newState.selectedReport.displayedEntries).toEqual([
        {
          otherField: 'asdfasdf',
          fooField: 'bar',
        },
        {
          otherField: 'dfasdfas',
          fooField: 'foobarbaz',
        },
      ]);

      expect(newState.selectedReport.allEntries).toBe(state.selectedReport.allEntries);
    });
  });

  describe('SET_RAW_DATA_SUBSTRING_FIELD_FILTER', function () {
    it('sets the specified property on the rawDataSubstringFilters to the specified value', function () {
      const state = Object.freeze({
          rawDataSubstringFilters: Object.freeze({
            otherField: 'asdf',
          }),
          other: otherObject,
        }),
        action = {
          type: 'SET_RAW_DATA_SUBSTRING_FIELD_FILTER',
          payload: {
            fieldName: 'fooField',
            filterString: 'bar',
          },
        },
        newState = reduce(state, action);

      expect(newState).toEqual({
        rawDataSubstringFilters: {
          fooField: 'bar',
          otherField: 'asdf',
        },
        other: otherObject,
      });

      expect(newState.other).toBe(otherObject);
    });

    it('filters the displayedEntries based on the resulting substringFilters', function () {
      const state = Object.freeze({
          rawDataSubstringFilters: Object.freeze({}),
          rawSortConfiguration: {
            key: 'derivedComponentName',
            sortFields: ['derivedComponentName', 'licenseSortKey', 'securityCode', '-cvssScore'],
            dir: 'asc',
          },
          reportRawData: Object.freeze({
            allEntries: Object.freeze([
              {
                otherField: 'asdfasdf',
                fooField: 'qwerty',
              },
              {
                otherField: 'asdfasdf',
                fooField: 'bar',
              },
              {
                otherField: '',
                fooField: 'bar',
              },
              {
                otherField: 'asdfasdf',
                fooField: '',
              },
              {
                otherField: 'dfasdfas',
                fooField: 'foobarbaz',
              },
              {
                otherField: 'bar',
                fooField: 'asdf',
              },
            ]),
          }),
        }),
        action = {
          type: 'SET_RAW_DATA_SUBSTRING_FIELD_FILTER',
          payload: {
            fieldName: 'fooField',
            filterString: 'bar',
          },
        },
        newState = reduce(state, action);

      expect(newState.reportRawData.displayedEntries).toEqual([
        {
          otherField: 'asdfasdf',
          fooField: 'bar',
          key: 'null\u001dundefined',
          cvssScore: '',
        },
        {
          otherField: '',
          fooField: 'bar',
          key: 'null\u001dundefined',
          cvssScore: '',
        },
        {
          otherField: 'dfasdfas',
          fooField: 'foobarbaz',
          key: 'null\u001dundefined',
          cvssScore: '',
        },
      ]);

      expect(newState.reportRawData.allEntries).toBe(state.reportRawData.allEntries);
    });

    it('correctly calculates activeProxyFailedViolationCount in selectedReport', function () {
      const state = Object.freeze({
        pendingLoads: new Set(['policy']),
        sortFields: ['-policyThreatLevel'],
      });

      const entries = [
        // Should be counted: policyThreatLevel >= 2, not ignored, has fail/Proxy Failed
        {
          policyThreatLevel: 8,
          actions: [{ actionType: 'fail', actionSummary: 'Proxy Failed' }],
        },
        // Should be counted: policyThreatLevel >= 2, not ignored, has fail/Proxy Failed
        {
          policyThreatLevel: 4,
          actions: [{ actionType: 'fail', actionSummary: 'Proxy Failed' }],
        },
        // Should NOT be counted: policyThreatLevel < 2
        {
          policyThreatLevel: 1,
          actions: [{ actionType: 'fail', actionSummary: 'Proxy Failed' }],
        },
        // Should NOT be counted: legacyViolation true
        {
          policyThreatLevel: 8,
          legacyViolation: true,
          actions: [{ actionType: 'fail', actionSummary: 'Proxy Failed' }],
        },
        // Should NOT be counted: grandfathered true
        {
          policyThreatLevel: 8,
          grandfathered: true,
          actions: [{ actionType: 'fail', actionSummary: 'Proxy Failed' }],
        },
        // Should NOT be counted: waived true
        {
          policyThreatLevel: 8,
          waived: true,
          actions: [{ actionType: 'fail', actionSummary: 'Proxy Failed' }],
        },
        // Should NOT be counted: actions does not include fail/Proxy Failed
        {
          policyThreatLevel: 8,
          actions: [{ actionType: 'warn', actionSummary: 'Proxy Failed' }],
        },
        // Should NOT be counted: actions missing
        {
          policyThreatLevel: 8,
        },
        // Should NOT be counted: actions present but no fail/Proxy Failed
        {
          policyThreatLevel: 8,
          actions: [{ actionType: 'fail', actionSummary: 'Other' }],
        },
      ];

      const newState = reduce(state, {
        type: 'LOAD_REPORT_FULFILLED',
        payload: {
          allEntries: entries,
          reportVersion: 3,
          isInnerSourceEnabled: false,
        },
      });

      expect(newState.selectedReport.activeProxyFailedViolationCount).toBe(2);
    });
  });

  describe('SET_RAW_DATA_NUMERIC_FIELD_MAX_FILTER', function () {
    it('sets the max value on the array in rawDataNumericFilters to the specified value', function () {
      const state = Object.freeze({
          rawDataNumericFilters: {
            otherField: [1, 5],
          },
          other: otherObject,
        }),
        action = {
          type: 'SET_RAW_DATA_NUMERIC_FIELD_MAX_FILTER',
          payload: {
            fieldName: 'fooField',
            filterValue: 6,
          },
        },
        newState = reduce(state, action);

      expect(newState).toEqual({
        rawDataNumericFilters: {
          fooField: [undefined, 6],
          otherField: [1, 5],
        },
        other: otherObject,
      });

      expect(newState.other).toBe(otherObject);
    });

    it('doesnt overwrite the minimum value when you set the maximum value of rawDataNumericFilters', function () {
      const state = Object.freeze({
          rawDataNumericFilters: {
            otherField: [1, 5],
            fooField: [2],
          },
          other: otherObject,
        }),
        action = {
          type: 'SET_RAW_DATA_NUMERIC_FIELD_MAX_FILTER',
          payload: {
            fieldName: 'fooField',
            filterValue: 6,
          },
        },
        newState = reduce(state, action);

      expect(newState).toEqual({
        rawDataNumericFilters: {
          fooField: [2, 6],
          otherField: [1, 5],
        },
        other: otherObject,
      });

      expect(newState.other).toBe(otherObject);
    });

    it('filters the displayedEntries based on a maximum numeric filter', function () {
      const state = Object.freeze({
          rawSortConfiguration: {
            key: 'derivedComponentName',
            sortFields: ['derivedComponentName', 'licenseSortKey', 'securityCode', '-cvssScore'],
            dir: 'asc',
          },
          reportRawData: Object.freeze({
            allEntries: Object.freeze([
              {
                otherField: 'monkeybrains',
                fooField: 1,
              },
              {
                otherField: 'asdfasdf',
                fooField: 5,
              },
              {
                otherField: 'chocolate',
                fooField: 7,
              },
              {
                otherField: 'asdfasdf',
              },
            ]),
          }),
        }),
        action = {
          type: 'SET_RAW_DATA_NUMERIC_FIELD_MAX_FILTER',
          payload: {
            fieldName: 'fooField',
            filterValue: 6,
          },
        },
        newState = reduce(state, action);

      expect(newState.reportRawData.displayedEntries).toEqual([
        {
          otherField: 'monkeybrains',
          fooField: 1,
          key: 'null\u001dundefined',
          cvssScore: '',
        },
        {
          otherField: 'asdfasdf',
          fooField: 5,
          key: 'null\u001dundefined',
          cvssScore: '',
        },
      ]);

      expect(newState.reportRawData.allEntries).toBe(state.reportRawData.allEntries);
    });
  });

  describe('SET_RAW_DATA_NUMERIC_FIELD_MIN_FILTER', function () {
    it('sets the min value on the array in rawDataNumericFilters to the specified value', function () {
      const state = Object.freeze({
          rawDataNumericFilters: {
            otherField: [1, 5],
          },
          other: otherObject,
        }),
        action = {
          type: 'SET_RAW_DATA_NUMERIC_FIELD_MIN_FILTER',
          payload: {
            fieldName: 'fooField',
            filterValue: 4,
          },
        },
        newState = reduce(state, action);

      expect(newState).toEqual({
        rawDataNumericFilters: {
          fooField: [4],
          otherField: [1, 5],
        },
        other: otherObject,
      });

      expect(newState.other).toBe(otherObject);
    });

    it('doesnt overwrite the max value when you set the maximum value of rawDataNumericFilters', function () {
      const state = Object.freeze({
          rawDataNumericFilters: {
            otherField: [1, 5],
            fooField: [undefined, 9],
          },
          other: otherObject,
        }),
        action = {
          type: 'SET_RAW_DATA_NUMERIC_FIELD_MIN_FILTER',
          payload: {
            fieldName: 'fooField',
            filterValue: 4,
          },
        },
        newState = reduce(state, action);

      expect(newState).toEqual({
        rawDataNumericFilters: {
          fooField: [4, 9],
          otherField: [1, 5],
        },
        other: otherObject,
      });

      expect(newState.other).toBe(otherObject);
    });

    it('filters the displayedEntries based on a minimum numeric filter', function () {
      const state = Object.freeze({
          rawSortConfiguration: {
            key: 'derivedComponentName',
            sortFields: ['derivedComponentName', 'licenseSortKey', 'securityCode', '-cvssScore'],
            dir: 'asc',
          },
          reportRawData: Object.freeze({
            allEntries: Object.freeze([
              {
                otherField: 'monkeybrains',
                fooField: 1,
              },
              {
                otherField: 'asdfasdf',
                fooField: 5,
              },
              {
                otherField: 'chocolate',
                fooField: 7,
              },
              {
                otherField: 'asdfasdf',
              },
            ]),
          }),
        }),
        action = {
          type: 'SET_RAW_DATA_NUMERIC_FIELD_MIN_FILTER',
          payload: {
            fieldName: 'fooField',
            filterValue: 4,
          },
        },
        newState = reduce(state, action);

      expect(newState.reportRawData.displayedEntries).toEqual([
        {
          otherField: 'asdfasdf',
          fooField: 5,
          key: 'null\u001dundefined',
          cvssScore: '',
        },
        {
          otherField: 'chocolate',
          fooField: 7,
          key: 'null\u001dundefined',
          cvssScore: '',
        },
      ]);

      expect(newState.reportRawData.allEntries).toBe(state.reportRawData.allEntries);
    });
  });

  describe('GENERATE_VULNERABILITY_ENTRIES', function () {
    it('sets and sorts the vulnerabilities if the selectedReport and rawDataEntries are both present', function () {
      const state = Object.freeze({
        vulnerabilities: [],
        selectedReport: {
          allEntries: [
            {
              policyThreatLevel: 7,
              componentIdentifier: {
                format: 'compFormat',
                coordinates: {
                  foo: 'bar',
                },
              },
              constraints: [
                {
                  conditions: [
                    {
                      conditionTriggerReference: {
                        type: 'SECURITY_VULNERABILITY_REFID',
                        value: 'CVE-1234',
                      },
                    },
                  ],
                },
              ],
            },
            {
              policyThreatLevel: 6,
              componentIdentifier: {
                format: 'compFormat',
                coordinates: {
                  // different component from above
                  foo: 'baz',
                },
              },
              constraints: [
                {
                  conditions: [
                    {
                      conditionTriggerReference: {
                        type: 'SECURITY_VULNERABILITY_REFID',
                        value: 'CVE-1235',
                      },
                    },
                  ],
                },
              ],
            },
            {
              policyThreatLevel: 6,
              componentIdentifier: {
                format: 'compFormat',
                coordinates: {
                  foo: 'bar',
                },
              },
              constraints: [
                {
                  conditions: [
                    {
                      conditionTriggerReference: {
                        type: 'SECURITY_VULNERABILITY_REFID',
                        value: 'CVE-1235',
                      },
                    },
                  ],
                },
              ],
            },
            {
              policyThreatLevel: 9,
              waived: true,
              componentIdentifier: {
                format: 'compFormat',
                coordinates: {
                  foo: 'bar',
                },
              },
              constraints: [
                {
                  conditions: [
                    {
                      conditionTriggerReference: {
                        type: 'SECURITY_VULNERABILITY_REFID',
                        value: 'CVE-1237',
                      },
                    },
                  ],
                },
              ],
            },
            {
              policyThreatLevel: 6,
              componentIdentifier: {
                format: 'compFormat',
                coordinates: {
                  foo: 'bar',
                },
              },
              constraints: [
                {
                  conditions: [
                    {
                      conditionTriggerReference: {
                        type: 'SECURITY_VULNERABILITY_REFID',
                        value: 'CVE-1236',
                      },
                    },
                  ],
                },
              ],
            },
          ],
        },
        reportRawData: {
          allEntries: [
            {
              derivedComponentName: 'bar',
              componentIdentifier: {
                format: 'compFormat',
                coordinates: {
                  foo: 'bar',
                },
              },
              securityCode: 'CVE-1234',
              cvssScore: 5,
            },
            {
              derivedComponentName: 'baz',
              componentIdentifier: {
                format: 'compFormat',
                coordinates: {
                  foo: 'baz',
                },
              },
              securityCode: 'CVE-1235',
              cvssScore: 4,
            },
            {
              derivedComponentName: 'bar',
              componentIdentifier: {
                format: 'compFormat',
                coordinates: {
                  foo: 'bar',
                },
              },
              securityCode: 'CVE-1235',
              cvssScore: 4,
            },
            {
              derivedComponentName: 'bar',
              componentIdentifier: {
                format: 'compFormat',
                coordinates: {
                  foo: 'bar',
                },
              },
              securityCode: 'CVE-1236',
              cvssScore: 3,
            },
            {
              derivedComponentName: 'bar',
              componentIdentifier: {
                format: 'compFormat',
                coordinates: {
                  foo: 'bar',
                },
              },
              securityCode: 'CVE-1237',
              cvssScore: 9,
            },
          ],
        },
      });

      const newState = reduce(state, {
        type: 'GENERATE_VULNERABILITY_ENTRIES',
      });

      expect(newState.vulnerabilities).toEqual([
        expect.objectContaining({
          policyThreatLevel: 7,
          securityCode: 'CVE-1234',
          cvssScore: 5,
          derivedComponentName: 'bar',
        }),
        expect.objectContaining({
          policyThreatLevel: 6,
          securityCode: 'CVE-1235',
          cvssScore: 4,
          derivedComponentName: 'bar',
        }),
        expect.objectContaining({
          policyThreatLevel: 6,
          securityCode: 'CVE-1235',
          cvssScore: 4,
          derivedComponentName: 'baz',
        }),
        expect.objectContaining({
          policyThreatLevel: 6,
          securityCode: 'CVE-1236',
          cvssScore: 3,
          derivedComponentName: 'bar',
        }),
        expect.objectContaining({
          policyThreatLevel: 0,
          securityCode: 'CVE-1237',
          cvssScore: 9,
          waived: true,
          derivedComponentName: 'bar',
        }),
      ]);
    });

    it('returns the unchanged state if selectedReport is not present', function () {
      const state = Object.freeze({
        vulnerabilities: [],
        selectedReport: null,
        reportRawData: {
          allEntries: [
            {
              derivedComponentName: 'bar',
              componentIdentifier: {
                format: 'compFormat',
                coordinates: {
                  foo: 'bar',
                },
              },
              securityCode: 'CVE-1234',
              cvssScore: 5,
            },
            {
              derivedComponentName: 'baz',
              componentIdentifier: {
                format: 'compFormat',
                coordinates: {
                  foo: 'baz',
                },
              },
              securityCode: 'CVE-1235',
              cvssScore: 4,
            },
            {
              derivedComponentName: 'bar',
              componentIdentifier: {
                format: 'compFormat',
                coordinates: {
                  foo: 'bar',
                },
              },
              securityCode: 'CVE-1235',
              cvssScore: 4,
            },
            {
              derivedComponentName: 'bar',
              componentIdentifier: {
                format: 'compFormat',
                coordinates: {
                  foo: 'bar',
                },
              },
              securityCode: 'CVE-1236',
              cvssScore: 3,
            },
            {
              derivedComponentName: 'bar',
              componentIdentifier: {
                format: 'compFormat',
                coordinates: {
                  foo: 'bar',
                },
              },
              securityCode: 'CVE-1237',
              cvssScore: 9,
            },
          ],
        },
      });

      const newState = reduce(state, {
        type: 'GENERATE_VULNERABILITY_ENTRIES',
      });

      expect(newState.vulnerabilities).toEqual([]);
    });

    it('returns the unchanged state if rawDataEntries is not present', function () {
      const state = Object.freeze({
        vulnerabilities: [],
        selectedReport: {
          allEntries: [
            {
              policyThreatLevel: 7,
              componentIdentifier: {
                format: 'compFormat',
                coordinates: {
                  foo: 'bar',
                },
              },
              constraints: [
                {
                  conditions: [
                    {
                      conditionTriggerReference: {
                        type: 'SECURITY_VULNERABILITY_REFID',
                        value: 'CVE-1234',
                      },
                    },
                  ],
                },
              ],
            },
            {
              policyThreatLevel: 6,
              componentIdentifier: {
                format: 'compFormat',
                coordinates: {
                  // different component from above
                  foo: 'baz',
                },
              },
              constraints: [
                {
                  conditions: [
                    {
                      conditionTriggerReference: {
                        type: 'SECURITY_VULNERABILITY_REFID',
                        value: 'CVE-1235',
                      },
                    },
                  ],
                },
              ],
            },
            {
              policyThreatLevel: 6,
              componentIdentifier: {
                format: 'compFormat',
                coordinates: {
                  foo: 'bar',
                },
              },
              constraints: [
                {
                  conditions: [
                    {
                      conditionTriggerReference: {
                        type: 'SECURITY_VULNERABILITY_REFID',
                        value: 'CVE-1235',
                      },
                    },
                  ],
                },
              ],
            },
            {
              policyThreatLevel: 9,
              waived: true,
              componentIdentifier: {
                format: 'compFormat',
                coordinates: {
                  foo: 'bar',
                },
              },
              constraints: [
                {
                  conditions: [
                    {
                      conditionTriggerReference: {
                        type: 'SECURITY_VULNERABILITY_REFID',
                        value: 'CVE-1237',
                      },
                    },
                  ],
                },
              ],
            },
            {
              policyThreatLevel: 6,
              componentIdentifier: {
                format: 'compFormat',
                coordinates: {
                  foo: 'bar',
                },
              },
              constraints: [
                {
                  conditions: [
                    {
                      conditionTriggerReference: {
                        type: 'SECURITY_VULNERABILITY_REFID',
                        value: 'CVE-1236',
                      },
                    },
                  ],
                },
              ],
            },
          ],
        },
      });

      const newState = reduce(state, {
        type: 'GENERATE_VULNERABILITY_ENTRIES',
      });

      expect(newState.vulnerabilities).toEqual([]);
    });
  });

  describe('SET_SORTING_PARAMETERS action', function () {
    it('adds sorting parameters to state', function () {
      const state = {};
      const newState = reduce(state, {
        type: 'SET_SORTING_PARAMETERS',
        payload: {
          key: 'key',
          sortFields: ['a', 'b'],
          dir: 'dir',
        },
      });
      expect(newState.sortConfiguration).toEqual({
        key: 'key',
        sortFields: ['a', 'b'],
        dir: 'dir',
      });
    });

    it('update sorting parameters to state', function () {
      const state = {
        sortConfiguration: {
          key: 'key',
          sortFields: ['a', 'b'],
          dir: 'dir',
        },
      };
      const newState = reduce(state, {
        type: 'SET_SORTING_PARAMETERS',
        payload: {
          key: 'key2',
          sortFields: ['c', 'd'],
          dir: 'asc',
        },
      });
      expect(newState.sortConfiguration).toEqual({
        key: 'key2',
        sortFields: ['c', 'd'],
        dir: 'asc',
      });
    });
  });

  describe('APPLICATION_REPORT_TOGGLE_FILTER_SIDEBAR action', function () {
    it('sets filterSidebarOpen to payload', function () {
      const state = Object.freeze({
        filterSidebarOpen: false,
        other: otherObject,
      });
      const newStateWithTruePayload = reduce(state, {
        type: 'APPLICATION_REPORT_TOGGLE_FILTER_SIDEBAR',
        payload: true,
      });
      expect(newStateWithTruePayload.filterSidebarOpen).toBe(true);
      expect(newStateWithTruePayload.other).toBe(otherObject); // other properties are not modified

      const newStateWithFalsePayload = reduce(state, {
        type: 'APPLICATION_REPORT_TOGGLE_FILTER_SIDEBAR',
        payload: false,
      });
      expect(newStateWithFalsePayload.filterSidebarOpen).toBe(false);
      expect(newStateWithFalsePayload.other).toBe(otherObject); // other properties are not modified
    });
  });

  describe('TOGGLE_SHOW_FILTER_POPOVER action', function () {
    it('toggles showFilterPopover', function () {
      const state = Object.freeze({
        showFilterPopover: false,
        other: otherObject,
      });
      let newState = reduce(state, {
        type: 'TOGGLE_SHOW_FILTER_POPOVER',
      });
      expect(newState.showFilterPopover).toBe(true);
      expect(newState.other).toBe(otherObject);

      newState = reduce(newState, {
        type: 'TOGGLE_SHOW_FILTER_POPOVER',
      });
      expect(newState.showFilterPopover).toBe(false);
      expect(newState.other).toBe(otherObject);
    });
  });

  describe('OPEN_INNERSOURCE_PRODUCER_REPORT_MODAL action', function () {
    it('sets showInnerSourceProducerReportModal to true', function () {
      const state = Object.freeze({
        selectedComponent: {
          showInnerSourceProducerReportModal: false,
        },
        other: otherObject,
      });

      const newState = reduce(state, { type: 'OPEN_INNERSOURCE_PRODUCER_REPORT_MODAL' });
      expect(newState.selectedComponent.showInnerSourceProducerReportModal).toBe(true);
      expect(newState.other).toBe(otherObject); // other properties are not modified
    });
  });

  describe('CLOSE_INNERSOURCE_PRODUCER_REPORT_MODAL action', function () {
    it('sets showInnerSourceProducerReportModal to false', function () {
      const state = Object.freeze({
        selectedComponent: {
          showInnerSourceProducerReportModal: true,
        },
        other: otherObject,
      });

      const newState = reduce(state, { type: 'CLOSE_INNERSOURCE_PRODUCER_REPORT_MODAL' });
      expect(newState.selectedComponent.showInnerSourceProducerReportModal).toBe(false);
      expect(newState.other).toBe(otherObject); // other properties are not modified
    });
  });

  describe('OPEN_INNERSOURCE_PRODUCER_PERMISSIONS_MODAL action', function () {
    it('sets showInnerSourceProducerPermissionsModal to true', function () {
      const state = Object.freeze({
        selectedComponent: {
          showInnerSourceProducerPermissionsModal: false,
        },
        other: otherObject,
      });

      const newState = reduce(state, { type: 'OPEN_INNERSOURCE_PRODUCER_PERMISSIONS_MODAL' });
      expect(newState.selectedComponent.showInnerSourceProducerPermissionsModal).toBe(true);
      expect(newState.other).toBe(otherObject); // other properties are not modified
    });
  });

  describe('CLOSE_INNERSOURCE_PRODUCER_PERMISSIONS_MODAL action', function () {
    it('sets showInnerSourceProducerPermissionsModal to false', function () {
      const state = Object.freeze({
        selectedComponent: {
          showInnerSourceProducerPermissionsModal: true,
        },
        other: otherObject,
      });

      const newState = reduce(state, { type: 'CLOSE_INNERSOURCE_PRODUCER_PERMISSIONS_MODAL' });
      expect(newState.selectedComponent.showInnerSourceProducerPermissionsModal).toBe(false);
      expect(newState.other).toBe(otherObject); // other properties are not modified
    });
  });

  describe('DEPENDENCY_TREE_TOGGLE_TREE_PATH action', () => {
    it('sets "isOpen" data to the correct dependency', () => {
      const state = Object.freeze({ dependencyTree: dependencyTreeData, displayedDependencyTree: dependencyTreeData });

      expect(state.dependencyTree[1].isOpen).toBe(false);
      expect(state.displayedDependencyTree[1].isOpen).toBe(false);

      const newState = reduce(state, {
        type: 'DEPENDENCY_TREE_TOGGLE_TREE_PATH',
        payload: [1],
      });

      expect(newState.dependencyTree[1].isOpen).toBe(true);
      expect(newState.displayedDependencyTree[1].isOpen).toBe(true);
    });

    it('sets "isOpen" data to the correct nested dependency', () => {
      const state = Object.freeze({ dependencyTree: dependencyTreeData, displayedDependencyTree: dependencyTreeData });

      expect(state.dependencyTree[0].children[0].isOpen).toBe(true);
      expect(state.displayedDependencyTree[0].children[0].isOpen).toBe(true);

      const newState = reduce(state, {
        type: 'DEPENDENCY_TREE_TOGGLE_TREE_PATH',
        payload: [0, 'children', 0],
      });

      expect(newState.dependencyTree[0].children[0].isOpen).toBe(false);
      expect(newState.displayedDependencyTree[0].children[0].isOpen).toBe(false);
    });
  });

  describe('SET_DEPENDENCY_TREE_ROUTER_PARAMS action', () => {
    it('sets dependencyTreePageRouterParams', () => {
      const routerParams = { publicId: 'testPublicId', scanId: 'testScanId' };
      const state = Object.freeze({
        dependencyTreePageRouterParams: null,
      });

      const newState = reduce(state, {
        type: 'SET_DEPENDENCY_TREE_ROUTER_PARAMS',
        payload: routerParams,
      });

      expect(newState.dependencyTreePageRouterParams).toEqual(routerParams);
    });
  });

  describe('SET_DEPENDENCY_TREE_SEARCH_TERM action', () => {
    it('stores the search term in lower case', () => {
      const state = Object.freeze({});

      const newState = reduce(state, {
        type: 'SET_DEPENDENCY_TREE_SEARCH_TERM',
        payload: 'LANG',
      });

      expect(newState.dependencyTreeSearchTerm).toBe('lang');
    });

    it('filters dependency tree by the provided search term', () => {
      const state = Object.freeze({ dependencyTree: dependencyTreeData, displayedDependencyTree: null });

      const newState = reduce(state, {
        type: 'SET_DEPENDENCY_TREE_SEARCH_TERM',
        payload: 'LANG',
      });

      expect(newState.displayedDependencyTree.length).toBe(1);
      expect(newState.displayedDependencyTree[0].children.length).toBe(1);
      expect(newState.displayedDependencyTree[0].displayName).toBe('org.apache.commons : commons-lang3 : 3.3.2');
    });

    it('expands all the visible branches in the filtered dependency tree', () => {
      const state = Object.freeze({ dependencyTree: dependencyTreeData, displayedDependencyTree: null });

      const newState = reduce(state, {
        type: 'SET_DEPENDENCY_TREE_SEARCH_TERM',
        payload: 'LANG',
      });

      expect(newState.displayedDependencyTree[0].isOpen).toBe(true);
      expect(newState.displayedDependencyTree[0].children[0].isOpen).toBe(true);
    });
  });

  describe('EXPAND_ALL_DEPENDENCY_TREE_NODES action', () => {
    it('expands all nodes in dependency tree', () => {
      const state = Object.freeze({ dependencyTree: dependencyTreeData, displayedDependencyTree: dependencyTreeData });

      const newState = reduce(state, { type: 'EXPAND_ALL_DEPENDENCY_TREE_NODES' });

      newState.dependencyTree.forEach((node) => {
        if (node.children) {
          node.children.forEach((child) => expect(child.isOpen).toBe(true));
        }
        expect(node.isOpen).toBe(true);
      });

      newState.displayedDependencyTree.forEach((node) => {
        if (node.children) {
          node.children.forEach((child) => expect(child.isOpen).toBe(true));
        }
        expect(node.isOpen).toBe(true);
      });
    });

    it('expands all nodes in dependency tree when dependency tree is filtered', () => {
      const state = Object.freeze({
        dependencyTree: dependencyTreeData,
        displayedDependencyTree: [dependencyTreeData[0]],
      });

      const newState = reduce(state, { type: 'EXPAND_ALL_DEPENDENCY_TREE_NODES' });

      newState.dependencyTree.forEach((node) => {
        if (node.children) {
          node.children.forEach((child) => expect(child.isOpen).toBe(true));
        }
        expect(node.isOpen).toBe(true);
      });

      newState.displayedDependencyTree.forEach((node) => {
        if (node.children) {
          node.children.forEach((child) => expect(child.isOpen).toBe(true));
        }
        expect(node.isOpen).toBe(true);
      });
    });
  });

  describe('COLLAPSE_ALL_DEPENDENCY_TREE_NODES action', () => {
    it('collapse all nodes in dependency tree', () => {
      const state = Object.freeze({ dependencyTree: dependencyTreeData, displayedDependencyTree: dependencyTreeData });

      const newState = reduce(state, { type: 'COLLAPSE_ALL_DEPENDENCY_TREE_NODES' });

      newState.dependencyTree.forEach((node) => {
        if (node.children) {
          node.children.forEach((child) => expect(child.isOpen).toBe(false));
        }
        expect(node.isOpen).toBe(false);
      });

      newState.displayedDependencyTree.forEach((node) => {
        if (node.children) {
          node.children.forEach((child) => expect(child.isOpen).toBe(false));
        }
        expect(node.isOpen).toBe(false);
      });
    });

    it('collapse all nodes in dependency tree when dependency tree is filtered', () => {
      const state = Object.freeze({
        dependencyTree: dependencyTreeData,
        displayedDependencyTree: [dependencyTreeData[0]],
      });

      const newState = reduce(state, { type: 'COLLAPSE_ALL_DEPENDENCY_TREE_NODES' });

      newState.dependencyTree.forEach((node) => {
        if (node.children) {
          node.children.forEach((child) => expect(child.isOpen).toBe(false));
        }
        expect(node.isOpen).toBe(false);
      });

      newState.displayedDependencyTree.forEach((node) => {
        if (node.children) {
          node.children.forEach((child) => expect(child.isOpen).toBe(false));
        }
        expect(node.isOpen).toBe(false);
      });
    });
  });
});
