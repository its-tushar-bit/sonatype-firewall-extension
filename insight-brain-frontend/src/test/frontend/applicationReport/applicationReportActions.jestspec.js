/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import axios from 'axios';
import { serializeComponentIdentifier } from 'MainRoot/util/componentIdentifierUtils';
import * as applicationReportActions from 'MainRoot/applicationReport/applicationReportActions';
import * as CLMLocation from 'MainRoot/util/CLMLocation';
import * as routerSelectors from 'MainRoot/reduxUiRouter/routerSelectors';
import * as applicationReportSelectors from 'MainRoot/applicationReport/applicationReportSelectors';
import { dependencyTreeData } from '../dependencyTree/dependencyTreeMockData';

import 'TestRoot/SpecUtil';

const createMockState = (isUnknownJs, bomData, unknownJsData, metadata, embeddable) => ({
  applicationReport: {
    dependencyTree: dependencyTreeData,
    reportParameters: {
      appId: 'appId',
      scanId: 'scanId',
      isUnknownJs,
      embeddable: !!embeddable,
    },
    bomData,
    unknownJsData,
    metadata,
  },
});
const mockMetadata = { reportTitle: 'test', stageId: 'build' };
const mockUnknownJsData = {
  aaData: [
    {
      filenames: ['foo.js'],
    },
  ],
};
const mockBomData = {
  aaData: [
    {
      foo: 'bar',
    },
  ],
};
const mockLicenseData = {
  aaData: [],
};
const mockReportData = { fooReport: 'barReport' };

describe('applicationReportActions', function () {
  let mockAxiosCalls;

  beforeEach(function () {
    mockAxiosCalls = SpecUtil.axiosMockerGenerator(axios);
  });

  describe('setReportParameters', () => {
    it('dispatches SET_REPORT_PARAMETERS action', () => {
      const store = SpecUtil.mockReduxStore({});

      store.dispatch(
        applicationReportActions.setReportParameters(
          'appId',
          'scanId',
          true,
          false,
          'policyViolationId',
          'componentHash',
          'tabId',
          true
        )
      );
      expect(store.getActions().length).toBe(1);
      expect(store.getActions()[0]).toEqual({
        type: 'SET_REPORT_PARAMETERS',
        payload: {
          appId: 'appId',
          scanId: 'scanId',
          isUnknownJs: true,
          embeddable: false,
          policyViolationId: 'policyViolationId',
          componentHash: 'componentHash',
          tabId: 'tabId',
          isNotFiltered: true,
        },
      });

      store.dispatch(applicationReportActions.setReportParameters('appId', 'scanId', true, false));
      expect(store.getActions().length).toBe(2);
      expect(store.getActions()[1]).toEqual({
        type: 'SET_REPORT_PARAMETERS',
        payload: {
          appId: 'appId',
          scanId: 'scanId',
          isUnknownJs: true,
          embeddable: false,
          policyViolationId: undefined,
          componentHash: undefined,
          tabId: undefined,
          isNotFiltered: undefined,
        },
      });
    });
  });

  /**
   * Tests of common behavior for all action creators that should conditionally fetch the common data (bom, metadata,
   * and unknownjs).  The tests defined here expect the action creator to initially fire some action, the details
   * of which are not specific to the common data and which are therefore not tested here.
   * @param actionCreatorName The property on applicationReportActions containing the action creator function to invoke
   * @param expectAdditionalCalls An object that sets up additional HTTP call expectations specific to the action
   * creator under test which come after the common data HTTP expectations, when appropriate
   */
  function testCommonDataLoading(actionCreatorName, expectAdditionalCalls) {
    it('fetches common data if bomData is not on the state', (done) => {
      const store = SpecUtil.mockReduxStore(createMockState(true, undefined, mockUnknownJsData, mockMetadata));

      expectCommonDataCalls(true, expectAdditionalCalls);

      store.dispatch(applicationReportActions[actionCreatorName]()).then(() => {
        expect(store.getActions()[1]).toEqual({
          type: 'LOAD_COMMON_DATA_FULFILLED',
          payload: {
            bomData: mockBomData,
            metadata: mockMetadata,
            unknownJsData: mockUnknownJsData,
          },
        });
        done();
      });

      expect(store.getActions().length).toBe(1);
    });

    it('fetches common data if metadata is not on the state', (done) => {
      const store = SpecUtil.mockReduxStore(createMockState(true, mockBomData, mockUnknownJsData, undefined));

      expectCommonDataCalls(true, expectAdditionalCalls);

      store.dispatch(applicationReportActions[actionCreatorName]()).then(() => {
        expect(store.getActions()[1]).toEqual({
          type: 'LOAD_COMMON_DATA_FULFILLED',
          payload: {
            bomData: mockBomData,
            metadata: mockMetadata,
            unknownJsData: mockUnknownJsData,
          },
        });
        done();
      });
    });

    it('fetches common data if unknownJs is on and unknownJsData is not on the state', (done) => {
      const store = SpecUtil.mockReduxStore(createMockState(true, mockBomData, undefined, mockMetadata));

      expectCommonDataCalls(true, expectAdditionalCalls);

      store.dispatch(applicationReportActions[actionCreatorName]()).then(() => {
        expect(store.getActions()[1]).toEqual({
          type: 'LOAD_COMMON_DATA_FULFILLED',
          payload: {
            bomData: mockBomData,
            metadata: mockMetadata,
            unknownJsData: mockUnknownJsData,
          },
        });
        done();
      });
    });

    it('does not fetch unknownJsData if unknownJs is off', (done) => {
      const store = SpecUtil.mockReduxStore(createMockState(false, undefined, undefined, undefined));

      expectCommonDataCalls(true, expectAdditionalCalls);

      store.dispatch(applicationReportActions[actionCreatorName]()).then(() => {
        expect(store.getActions()[1]).toEqual({
          type: 'LOAD_COMMON_DATA_FULFILLED',
          payload: {
            bomData: mockBomData,
            metadata: mockMetadata,
            unknownJsData: undefined,
          },
        });
        done();
      });
    });

    it('does not fetch common data if bomData, metadata are set on the state and unknownJs is false', (done) => {
      const store = SpecUtil.mockReduxStore(createMockState(false, mockBomData, mockUnknownJsData, mockMetadata));

      expectCommonDataCalls(false, expectAdditionalCalls);

      store.dispatch(applicationReportActions[actionCreatorName]()).then(() => {
        expect(store.getActions()[1]).toEqual({
          type: 'LOAD_COMMON_DATA_UNNECESSARY',
        });
        done();
      });
    });

    it('does not fetch common data if bomData, metadata and unknownJsData are all on the state', (done) => {
      const store = SpecUtil.mockReduxStore(createMockState(true, mockBomData, mockUnknownJsData, mockMetadata));

      expectCommonDataCalls(false, expectAdditionalCalls);

      store.dispatch(applicationReportActions[actionCreatorName]()).then(() => {
        expect(store.getActions()[1]).toEqual({
          type: 'LOAD_COMMON_DATA_UNNECESSARY',
        });
        done();
      });
    });

    it('fires LOAD_COMMON_DATA_FAILED action if the common data request fails', (done) => {
      const store = SpecUtil.mockReduxStore(createMockState(true, undefined, undefined, undefined));

      expectCommonDataCalls(false, expectAdditionalCalls);

      store.dispatch(applicationReportActions[actionCreatorName]()).then(() => {
        expect(store.getActions()[1]).toEqual({
          type: 'LOAD_COMMON_DATA_FAILED',
          payload: 'Error 500',
        });
        done();
      });
    });
  }

  describe('loadReport', function () {
    it('dispatches a LOAD_REPORT_REQUESTED action', function () {
      const store = SpecUtil.mockReduxStore(createMockState(false, mockBomData, mockUnknownJsData, mockMetadata));
      store.dispatch(applicationReportActions.loadReport());

      expect(store.getActions().length).toBe(2);
      expect(store.getActions()[0]).toEqual({
        type: 'LOAD_REPORT_REQUESTED',
      });
    });

    testCommonDataLoading('loadReport', expectReportDataCalls(true));

    it('loads report information if forceCache is true, even if it is already on the state', (done) => {
      const store = SpecUtil.mockReduxStore(createMockState(true, mockBomData, mockUnknownJsData, mockMetadata));

      expectCommonDataCalls(true, expectReportDataCalls(true));

      store.dispatch(applicationReportActions.loadReport(true)).then(() => {
        expect(store.getActions().length).toBe(3);
        expect(store.getActions()[2].type).toEqual('LOAD_REPORT_FULFILLED');
        done();
      });
    });

    it('fires LOAD_REPORT_FAILED action if report request fails', function (done) {
      const store = SpecUtil.mockReduxStore(createMockState(true, undefined, mockUnknownJsData, mockMetadata));

      expectCommonDataCalls(true, expectReportDataCalls(false));

      store.dispatch(applicationReportActions.loadReport()).then(() => {
        expect(store.getActions().length).toBe(3);
        expect(store.getActions()[2].type).toEqual('LOAD_REPORT_FAILED');
        done();
      });
    });

    it('handles partial failure when common data succeeds but report data fails', function (done) {
      const store = SpecUtil.mockReduxStore(createMockState(false, undefined, undefined, undefined));

      expectCommonDataCalls(true, expectReportDataCalls(false));

      store.dispatch(applicationReportActions.loadReport()).then(() => {
        const actions = store.getActions();

        expect(actions.length).toBe(3);
        expect(actions[0]).toEqual({ type: 'LOAD_REPORT_REQUESTED' });
        expect(actions[1]).toEqual({
          type: 'LOAD_COMMON_DATA_FULFILLED',
          payload: {
            bomData: mockBomData,
            metadata: mockMetadata,
            unknownJsData: undefined,
          },
        });
        expect(actions[2].type).toEqual('LOAD_REPORT_FAILED');
        done();
      });
    });

    it('handles partial failure when report data succeeds but common data fails', function (done) {
      const store = SpecUtil.mockReduxStore(createMockState(false, undefined, undefined, undefined));

      expectCommonDataCalls(false, expectReportDataCalls(true));

      store.dispatch(applicationReportActions.loadReport()).then(() => {
        const actions = store.getActions();

        expect(actions.length).toBe(3);
        expect(actions[0]).toEqual({ type: 'LOAD_REPORT_REQUESTED' });
        expect(actions[1]).toEqual({
          type: 'LOAD_COMMON_DATA_FAILED',
          payload: 'Error 500',
        });
        expect(actions[2].type).toEqual('LOAD_REPORT_FAILED');
        done();
      });
    });

    it('dispatches both LOAD_COMMON_DATA_FAILED and LOAD_REPORT_FAILED when common data fails', function (done) {
      const store = SpecUtil.mockReduxStore(createMockState(true, undefined, undefined, undefined));

      expectCommonDataCalls(false, expectReportDataCalls(true));

      store.dispatch(applicationReportActions.loadReport()).then(() => {
        const actions = store.getActions();

        // Should dispatch BOTH error actions due to the new parallel loading logic
        expect(actions.length).toBe(3);
        expect(actions[0]).toEqual({ type: 'LOAD_REPORT_REQUESTED' });
        expect(actions[1]).toEqual({
          type: 'LOAD_COMMON_DATA_FAILED',
          payload: 'Error 500',
        });
        expect(actions[2].type).toEqual('LOAD_REPORT_FAILED');
        done();
      });
    });

    it('fires LOAD_REPORT_FULFILLED action if report request succeeds', function (done) {
      const componentIdentifier = {
        format: 'maven',
        coordinates: {
          artifactId: 'logback-access',
          classifier: '',
          extension: 'jar',
          groupId: 'ch.qos.logback',
          version: '0.6',
        },
      };
      const bomData = {
        aaData: [
          {
            foo: 'bar',
            componentIdentifier,
          },
        ],
      };
      const store = SpecUtil.mockReduxStore(createMockState(false, bomData, undefined, mockMetadata));

      expectCommonDataCalls(true, expectReportDataCalls(true));

      store.dispatch(applicationReportActions.loadReport()).then(() => {
        const actions = store.getActions();

        expect(actions.length).toBe(3);
        expect(actions[0]).toEqual({ type: 'LOAD_REPORT_REQUESTED' });
        expect(actions[2]).toEqual({
          type: 'LOAD_REPORT_FULFILLED',
          payload: {
            allEntries: [
              {
                foo: 'bar',
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
                serializedComponentIdentifier: serializeComponentIdentifier(componentIdentifier),
                policyThreatLevel: 0,
                policyName: 'None',
                waived: false,
                legacyViolation: false,
                derivedComponentName: 'unknown',
                derivedDependencyType: 'direct',
                derivedViolationState: 'notViolating',
                derivedInnerSource: false,
                directDependency: true,
                hasDependencyTypeInfo: true,
                dependencyInfo: { isDirectDependency: true },
                isOnlyInnerSourceTransitiveDependency: false,
              },
            ],
            fooReport: 'barReport',
            reportVersion: 3,
            isInnerSourceEnabled: false,
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
        done();
      });
    });
  });

  describe('selectComponent', function () {
    it('set the selected component with InnerSource', function (done) {
      const state = createMockState(false, mockBomData, mockUnknownJsData, mockMetadata);

      state.applicationReport.selectedReport = {
        displayedEntries: [
          {
            componentName: 'a',
            innerSource: true,
            innerSourceData: [
              {
                ownerApplicationId: 'id',
                ownerApplicationName: 'appName',
              },
            ],
          },
          {
            componentName: 'b',
          },
        ],
      };

      const store = SpecUtil.mockReduxStore(state);

      const selectedComponent = {
        componentName: 'a',
        innerSource: true,
        innerSourceData: [
          {
            ownerApplicationId: 'id',
            ownerApplicationName: 'appName',
          },
        ],
      };

      store.dispatch(applicationReportActions.selectComponent(0)).then(() => {
        expect(store.getActions().length).toBe(1);
        expect(store.getActions()[0]).toEqual({
          type: 'SELECT_COMPONENT',
          payload: {
            component: selectedComponent,
            componentIndex: 0,
          },
        });
        done();
      });
    });

    it('request and set the selected component without InnerSource', function (done) {
      const state = createMockState(false, mockBomData, mockUnknownJsData, mockMetadata);

      state.applicationReport.selectedReport = {
        displayedEntries: [
          {
            componentName: 'a',
          },
          {
            componentName: 'b',
          },
        ],
      };

      const store = SpecUtil.mockReduxStore(state);

      const selectedComponent = {
        componentName: 'b',
      };

      store.dispatch(applicationReportActions.selectComponent(1)).then(function () {
        expect(store.getActions().length).toBe(1);
        expect(store.getActions()[0]).toEqual({
          type: 'SELECT_COMPONENT',
          payload: {
            component: selectedComponent,
            componentIndex: 1,
          },
        });
        done();
      });
    });
  });

  describe('loadReportRawData', function () {
    it('dispatches a LOAD_REPORT_RAW_DATA_REQUESTED action', function () {
      const store = SpecUtil.mockReduxStore(createMockState(false, mockBomData, mockUnknownJsData, mockMetadata));
      store.dispatch(applicationReportActions.loadReportRawData());

      expect(store.getActions().length).toBe(2);
      expect(store.getActions()[0]).toEqual({
        type: 'LOAD_REPORT_RAW_DATA_REQUESTED',
      });
    });

    testCommonDataLoading('loadReportRawData', expectReportRawDataCalls(true));

    it('fires LOAD_REPORT_RAW_DATA_FAILED action if report request fails', function (done) {
      const store = SpecUtil.mockReduxStore(createMockState(true, undefined, mockUnknownJsData, mockMetadata));

      expectCommonDataCalls(true, expectReportRawDataCalls(false));

      store.dispatch(applicationReportActions.loadReportRawData()).then(() => {
        expect(store.getActions().length).toBe(3);
        expect(store.getActions()[2].type).toEqual('LOAD_REPORT_RAW_DATA_FAILED');
        done();
      });
    });

    it('fires LOAD_REPORT_RAW_DATA_FULFILLED action if report request succeeds', function (done) {
      const bomData = { aaData: [{ foo: 'bar' }] };
      const store = SpecUtil.mockReduxStore(createMockState(false, bomData, undefined, mockMetadata));
      expectCommonDataCalls(true, expectReportRawDataCalls(true));

      store.dispatch(applicationReportActions.loadReportRawData()).then(() => {
        expect(store.getActions().length).toBe(3);
        expect(store.getActions()[2]).toEqual({
          type: 'LOAD_REPORT_RAW_DATA_FULFILLED',
          payload: [
            {
              derivedComponentName: 'unknown',
              license: undefined,
              licenseSortKey: '',
              foo: 'bar',
            },
          ],
        });

        done();
      });

      expect(store.getActions().length).toBe(2);
      expect(store.getActions()[0]).toEqual({
        type: 'LOAD_REPORT_RAW_DATA_REQUESTED',
      });
    });
  });

  describe('toggleAggregateReportEntries', function () {
    it('returns a TOGGLE_AGGREGATE_REPORT_ENTRIES action with no payload', function () {
      const action = applicationReportActions.toggleAggregateReportEntries();

      expect(action.type).toBe('TOGGLE_AGGREGATE_REPORT_ENTRIES');
      expect(action.payload).not.toBeDefined();
    });
  });

  describe('setSorting', function () {
    it('returns a SET_SORTING action with the specified payload value', function () {
      const payload = {},
        action = applicationReportActions.setSorting(payload);

      expect(action.type).toBe('SET_SORTING');
      expect(action.payload).toBe(payload);
    });
  });

  describe('setExactValueFilter', function () {
    it('returns a SET_EXACT_VALUE_FILTER action with payload having the specified fieldName and allowedValues', function () {
      const allowedValues = new Set(['foo', 'bar']),
        action = applicationReportActions.setExactValueFilter('fooField', allowedValues);

      expect(action.type).toBe('SET_EXACT_VALUE_FILTER');
      expect(action.payload).toEqual({
        fieldName: 'fooField',
        allowedValues,
      });
    });
  });

  describe('setStringFieldFilter', function () {
    it('returns a SET_SUBSTRING_FIELD_FILTER action with payload having the specified fieldName and filterString', function () {
      const action = applicationReportActions.setStringFieldFilter('fooField', 'bar');

      expect(action.type).toBe('SET_SUBSTRING_FIELD_FILTER');
      expect(action.payload).toEqual({
        fieldName: 'fooField',
        filterString: 'bar',
      });
    });
  });

  describe('setRawDataStringFieldFilter', function () {
    it('returns a SET_RAW_DATA_SUBSTRING_FIELD_FILTER action with payload of specified fieldName and filterString', function () {
      const action = applicationReportActions.setRawDataStringFieldFilter('fooField', 'bar');

      expect(action.type).toBe('SET_RAW_DATA_SUBSTRING_FIELD_FILTER');
      expect(action.payload).toEqual({
        fieldName: 'fooField',
        filterString: 'bar',
      });
    });
  });

  describe('setRawDataNumericMaxFilter', function () {
    it('returns a SET_RAW_DATA_NUMERIC_FIELD_MAX_FILTER action with correct payload', function () {
      const action = applicationReportActions.setRawDataNumericMaxFilter('fooField', 'bar');

      expect(action.type).toBe('SET_RAW_DATA_NUMERIC_FIELD_MAX_FILTER');
      expect(action.payload).toEqual({
        fieldName: 'fooField',
        filterValue: 'bar',
      });
    });
  });

  describe('setRawDataNumericMinFilter', function () {
    it('returns a SET_RAW_DATA_NUMERIC_FIELD_MIN_FILTER action with correct payload', function () {
      const action = applicationReportActions.setRawDataNumericMinFilter('fooField', 'bar');

      expect(action.type).toBe('SET_RAW_DATA_NUMERIC_FIELD_MIN_FILTER');
      expect(action.payload).toEqual({
        fieldName: 'fooField',
        filterValue: 'bar',
      });
    });
  });

  describe('reevaluateReport', function () {
    it('fires REEVALUATE_REPORT_FAILED action if the reevaluation request fails', function (done) {
      const store = SpecUtil.mockReduxStore(createMockState(false));

      mockAxiosCalls({
        post: {
          [CLMLocation.getReportReevaluateUrl('appId', 'scanId')]: () =>
            Promise.reject({ status: 500, data: 'test error' }),
        },
      });

      store.dispatch(applicationReportActions.reevaluateReport()).then(() => {
        expect(store.getActions().length).toBe(2);
        expect(store.getActions()[0]).toEqual({
          type: 'REEVALUATE_REPORT_REQUESTED',
        });
        expect(store.getActions()[1]).toEqual({
          type: 'REEVALUATE_REPORT_FAILED',
          payload: 'test error',
        });
        done();
      });
    });

    it('loads the report after reevaluation', function (done) {
      const store = SpecUtil.mockReduxStore(createMockState(false, mockBomData, mockUnknownJsData, mockMetadata));

      mockAxiosCalls({
        post: {
          [CLMLocation.getReportReevaluateUrl('appId', 'scanId')]: Promise.resolve({ data: '' }),
        },
      });

      expectCommonDataCalls(true, expectReportDataCalls(true));

      store.dispatch(applicationReportActions.reevaluateReport()).then(() => {
        const actions = store.getActions();

        expect(actions.length).toBe(5);
        expect(actions[0]).toEqual({ type: 'REEVALUATE_REPORT_REQUESTED' });

        expect(actions[1]).toEqual({ type: 'REEVALUATE_REPORT_FULFILLED' });

        expect(actions[2]).toEqual({ type: 'LOAD_REPORT_REQUESTED' });

        expect(actions[3]).toEqual({
          type: 'LOAD_COMMON_DATA_FULFILLED',
          payload: {
            bomData: mockBomData,
            metadata: mockMetadata,
            unknownJsData: undefined,
          },
        });

        expect(actions[4]).toEqual({
          type: 'LOAD_REPORT_FULFILLED',
          payload: {
            allEntries: [
              {
                filenames: ['foo.js'],
                policyThreatLevel: 0,
                policyName: 'None',
                waived: false,
                legacyViolation: false,
                derivedComponentName: 'foo.js',
                derivedDependencyType: 'unknown',
                derivedInnerSource: false,
                derivedViolationState: 'notViolating',
                isOnlyInnerSourceTransitiveDependency: false,
                directDependency: undefined,
                hasDependencyTypeInfo: false,
              },
            ],
            fooReport: 'barReport',
            reportVersion: 3,
            isInnerSourceEnabled: false,
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
        done();
      });
    });

    it('does not fire REEVALUATE_REPORT_FAILED if the load afterwards fails', function (done) {
      const store = SpecUtil.mockReduxStore(createMockState(false, mockBomData, mockUnknownJsData, mockMetadata));

      mockAxiosCalls({
        post: {
          [CLMLocation.getReportReevaluateUrl('appId', 'scanId')]: Promise.resolve({ data: '' }),
        },
      });

      expectCommonDataCalls(true, expectReportDataCalls(false));

      store.dispatch(applicationReportActions.reevaluateReport()).then(() => {
        expect(store.getActions().length).toBe(5);
        expect(store.getActions()[0]).toEqual({
          type: 'REEVALUATE_REPORT_REQUESTED',
        });
        expect(store.getActions()[1]).toEqual({
          type: 'REEVALUATE_REPORT_FULFILLED',
        });

        expect(store.getActions()[2]).toEqual({
          type: 'LOAD_REPORT_REQUESTED',
        });

        expect(store.getActions()[3]).toEqual({
          type: 'LOAD_COMMON_DATA_FULFILLED',
          payload: {
            bomData: mockBomData,
            metadata: mockMetadata,
            unknownJsData: undefined,
          },
        });

        expect(store.getActions()[4].type).toEqual('LOAD_REPORT_FAILED');
        done();
      });

      expect(store.getActions().length).toBe(1);
    });
  });

  describe('reevaluateReportCancelled', function () {
    it('returns a REEVALUATE_REPORT_CANCELLED action with no payload', function () {
      const action = applicationReportActions.reevaluateReportCancelled();

      expect(action.type).toBe('REEVALUATE_REPORT_CANCELLED');
      expect(action.payload).not.toBeDefined();
    });
  });

  describe('loadReportAllData', function () {
    it('dispatches a LOAD_REPORT_ALL_DATA_REQUESTED action', function () {
      const store = SpecUtil.mockReduxStore(createMockState(false, mockBomData, mockUnknownJsData, mockMetadata));
      store.dispatch(applicationReportActions.loadReportAllData());

      expect(store.getActions().length).toBe(2);
      expect(store.getActions()[0]).toEqual({
        type: 'LOAD_REPORT_ALL_DATA_REQUESTED',
      });
    });

    testCommonDataLoading('loadReportAllData', {
      ...expectReportDataCalls(true),
      ...expectReportRawDataCalls(true),
    });

    it('does not load the raw data if it is already loaded', function (done) {
      const state = createMockState(false, mockBomData, mockUnknownJsData, mockMetadata);

      state.applicationReport.reportRawData = [];

      const store = SpecUtil.mockReduxStore(state);

      store.dispatch(applicationReportActions.loadReportAllData()).then(() => {
        expect(store.getActions()[2].type).toEqual('LOAD_REPORT_RAW_DATA_UNNECESSARY');
        done();
      });
    });

    it('does not load the policy data if it is already loaded', function (done) {
      const state = createMockState(false, mockBomData, mockUnknownJsData, mockMetadata);

      state.applicationReport.selectedReport = {};

      const store = SpecUtil.mockReduxStore(state);
      expectCommonDataCalls(true, expectReportRawDataCalls(true));

      store.dispatch(applicationReportActions.loadReportAllData()).then(() => {
        expect(store.getActions()[2].type).toEqual('LOAD_REPORT_UNNECESSARY');
        done();
      });
    });

    it('dispatches GENERATE_VULNERABILITY_ENTRIES after all data is loaded', function (done) {
      const store = SpecUtil.mockReduxStore(createMockState(false, mockBomData, mockUnknownJsData, mockMetadata));

      expectCommonDataCalls(true, {
        ...expectReportDataCalls(true),
        ...expectReportRawDataCalls(true),
      });

      store.dispatch(applicationReportActions.loadReportAllData()).then(() => {
        expect(store.getActions().length).toBe(5);
        expect(store.getActions()[4]).toEqual({
          type: 'GENERATE_VULNERABILITY_ENTRIES',
        });
        done();
      });
    });
  });

  describe('selectRootAncestor', function () {
    it('returns a SELECT_ROOT_ANCESTOR action with the specified payload value', function () {
      const payload = {},
        action = applicationReportActions.selectRootAncestor(payload);

      expect(action.type).toBe('SELECT_ROOT_ANCESTOR');
      expect(action.payload).toBe(payload);
    });
  });

  describe('unselectRootAncestor', function () {
    it('returns a UNSELECT_ROOT_ANCESTOR action with no payload', function () {
      const action = applicationReportActions.unselectRootAncestor();

      expect(action).toEqual({
        type: 'UNSELECT_ROOT_ANCESTOR',
      });
    });
  });

  describe('setSortingParameters', () => {
    it('dispatches SET_SORTING_PARAMETERS action', () => {
      const store = SpecUtil.mockReduxStore({});
      store.dispatch(applicationReportActions.setSortingParameters('key', ['a', 'b'], 'dir'));

      expect(store.getActions().length).toBe(1);
      expect(store.getActions()[0]).toEqual({
        type: 'SET_SORTING_PARAMETERS',
        payload: {
          key: 'key',
          sortFields: ['a', 'b'],
          dir: 'dir',
        },
      });
    });
  });

  describe('openInnerSourceProducerReportModal', function () {
    it('returns a OPEN_INNERSOURCE_PRODUCER_REPORT_MODAL action with no payload', function () {
      const action = applicationReportActions.openInnerSourceProducerReportModal();

      expect(action.type).toBe('OPEN_INNERSOURCE_PRODUCER_REPORT_MODAL');
      expect(action.payload).not.toBeDefined();
    });
  });

  describe('closeInnerSourceProducerReportModal', function () {
    it('returns a CLOSE_INNERSOURCE_PRODUCER_REPORT_MODAL action with no payload', function () {
      const action = applicationReportActions.closeInnerSourceProducerReportModal();

      expect(action.type).toBe('CLOSE_INNERSOURCE_PRODUCER_REPORT_MODAL');
      expect(action.payload).not.toBeDefined();
    });
  });

  describe('openInnerSourceProducerPermissionsModal', function () {
    it('returns a OPEN_INNERSOURCE_PRODUCER_PERMISSIONS_MODAL action with no payload', function () {
      const action = applicationReportActions.openInnerSourceProducerPermissionsModal();

      expect(action.type).toBe('OPEN_INNERSOURCE_PRODUCER_PERMISSIONS_MODAL');
      expect(action.payload).not.toBeDefined();
    });
  });

  describe('closeInnerSourceProducerPermissionsModal', function () {
    it('returns a CLOSE_INNERSOURCE_PRODUCER_PERMISSIONS_MODAL action with no payload', function () {
      const action = applicationReportActions.closeInnerSourceProducerPermissionsModal();

      expect(action.type).toBe('CLOSE_INNERSOURCE_PRODUCER_PERMISSIONS_MODAL');
      expect(action.payload).not.toBeDefined();
    });
  });

  describe('loadReportIfNeeded', function () {
    it('calls `loadReport` if there is no selected report in the state', function () {
      const store = SpecUtil.mockReduxStore({ applicationReport: { reportParameters: {} } });
      jest.spyOn(routerSelectors, 'selectRouterCurrentParams').mockReturnValue({ scanId: 'scanId-1' });
      jest.spyOn(applicationReportSelectors, 'selectSelectedReport').mockReturnValue(null);
      jest.spyOn(applicationReportSelectors, 'selectReportParameters').mockReturnValue({
        appId: 'appId',
        scanId: 'scanId',
        isUnknownJs: false,
      });

      store.dispatch(applicationReportActions.loadReportIfNeeded());

      expect(store.getActions()).toHaveActionType('LOAD_REPORT_REQUESTED');
    });
    it('calls `loadReport` if the report in memory is different to the `scanId` parameter in the url', function () {
      const store = SpecUtil.mockReduxStore({ applicationReport: { reportParameters: { scanId: 'report-id' } } });
      jest.spyOn(routerSelectors, 'selectRouterCurrentParams').mockReturnValue({ scanId: 'scanId-1' });
      jest.spyOn(applicationReportSelectors, 'selectSelectedReport').mockReturnValue({ id: 'report-id' });
      jest.spyOn(applicationReportSelectors, 'selectReportParameters').mockReturnValue({
        scanId: 'report-id',
        appId: 'appId',
      });

      store.dispatch(applicationReportActions.loadReportIfNeeded());

      expect(store.getActions()).toHaveActionType('LOAD_REPORT_REQUESTED');
    });
    it('does not calls `loadReport` if the report is already in the state', function () {
      const store = SpecUtil.mockReduxStore({ applicationReport: { reportParameters: { scanId: 'report-id' } } });
      jest.spyOn(routerSelectors, 'selectRouterCurrentParams').mockReturnValue({ scanId: 'report-id' });
      jest.spyOn(applicationReportSelectors, 'selectSelectedReport').mockReturnValue({ id: 'report-id' });
      jest.spyOn(applicationReportSelectors, 'selectReportParameters').mockReturnValue({
        scanId: 'report-id',
        appId: 'appId',
      });

      store.dispatch(applicationReportActions.loadReportIfNeeded());

      expect(store.getActions()).not.toHaveActionType('LOAD_REPORT_REQUESTED');
      expect(store.getActions()).toHaveActionType('LOAD_REPORT_UNNECESSARY');
    });
  });

  describe('setDependencyTreeRouterParamsForBackButton', () => {
    it('calls setDependencyTreeRouterParams with the current params', () => {
      const routerParams = { scanId: 'testScanId', publicId: 'testPublicId' };
      const store = SpecUtil.mockReduxStore({});
      jest.spyOn(routerSelectors, 'selectRouterCurrentParams').mockReturnValue(routerParams);

      store.dispatch(applicationReportActions.setDependencyTreeRouterParamsForBackButton());

      expect(store.getActions()).toHaveAction({
        type: 'SET_DEPENDENCY_TREE_ROUTER_PARAMS',
        payload: routerParams,
      });
    });
  });

  describe('goToAddContainerImageWaiverPage', () => {
    it('calls stateGo with the appropriate parameters', () => {
      const mockRouterParams = {
        publicId: 'publicId',
        scanId: 'scanId',
      };
      jest.spyOn(routerSelectors, 'selectRouterCurrentParams').mockReturnValue(mockRouterParams);
      const store = SpecUtil.mockReduxStore({});

      store.dispatch(applicationReportActions.goToAddContainerImageWaiverPage());

      expect(store.getActions()).toHaveAction({
        type: '@@reduxUiRouter/stateGo',
        payload: {
          to: 'firewall.addContainerImageWaiver',
          params: mockRouterParams,
          options: undefined,
        },
      });
    });
  });

  describe('goToComponentDetailsPage', () => {
    it('calls stateGo with the appropriate parameters', () => {
      const mockRouterParams = {
        scanId: 'scanId',
        publicId: 'publicId',
      };
      jest.spyOn(routerSelectors, 'selectRouterCurrentParams').mockReturnValue(mockRouterParams);
      const store = SpecUtil.mockReduxStore({});

      store.dispatch(applicationReportActions.goToComponentDetailsPage('hash'));

      expect(store.getActions()).toHaveAction({
        type: '@@reduxUiRouter/stateGo',
        payload: {
          to: 'applicationReport.componentDetails',
          params: { ...mockRouterParams, hash: 'hash' },
          options: undefined,
        },
      });
    });

    it('calls stateGo with Container Images Firewall Evaluation', () => {
      const mockRouterParams = {
        scanId: 'scanId',
        publicId: 'publicId',
      };
      jest.spyOn(routerSelectors, 'selectRouterCurrentParams').mockReturnValue(mockRouterParams);
      const store = SpecUtil.mockReduxStore({});

      store.dispatch(applicationReportActions.goToComponentDetailsPage('hash', true));

      expect(store.getActions()).toHaveAction({
        type: '@@reduxUiRouter/stateGo',
        payload: {
          to: 'firewall.containerComponentDetails.overview',
          params: { ...mockRouterParams, hash: 'hash' },
          options: undefined,
        },
      });
    });
  });

  function expectCommonDataCalls(isSuccess, additionalCalls = {}) {
    mockAxiosCalls({
      get: {
        [CLMLocation.getReportBomUrl('appId', 'scanId')]: isSuccess
          ? { data: mockBomData }
          : () => Promise.reject({ status: 500 }),
        [CLMLocation.getReportMetadataUrl('appId', 'scanId')]: isSuccess
          ? { data: mockMetadata }
          : () => Promise.reject({ status: 500 }),
        [CLMLocation.getReportUnknownJsUrl('appId', 'scanId')]: {
          data: mockUnknownJsData,
        },
        ...additionalCalls,
      },
    });
  }

  function expectReportDataCalls(isSuccess) {
    return {
      [CLMLocation.getReportPolicyThreatsUrl('appId', 'scanId')]: isSuccess
        ? { data: { version: 3, aaData: [] } }
        : () => Promise.reject({ status: 500 }),
      [CLMLocation.getReportDataUrl('appId', 'scanId')]: isSuccess
        ? { data: mockReportData }
        : () => Promise.reject({ status: 500 }),
      [CLMLocation.getReportPartialMatchedUrl('appId', 'scanId')]: isSuccess
        ? { data: { aaData: [] } }
        : () => Promise.reject({ status: 500 }),
      [CLMLocation.getDependenciesUrl('appId', 'scanId')]: isSuccess
        ? {
            data: {
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
          }
        : () => Promise.reject({ status: 500 }),
    };
  }

  function expectReportRawDataCalls(isSuccess) {
    return {
      [CLMLocation.getReportSecurityUrl('appId', 'scanId')]: isSuccess
        ? { data: { aaData: [] } }
        : () => Promise.reject({ status: 500 }),
      [CLMLocation.getReportLicenseUrl('appId', 'scanId')]: isSuccess
        ? { data: mockLicenseData }
        : () => Promise.reject({ status: 500 }),
    };
  }
});
