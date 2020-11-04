/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import axios from 'axios';
import applicationReportModule from '../../../main/frontend/applicationReport/module';
import { serializeComponentIdentifier } from '../../../main/frontend/util/componentIdentifierUtils';
import * as CLMLocation from '../../../main/frontend/util/CLMLocation';
import { STATE_GO } from '../../../main/frontend/reduxUiRouter/routerActions';

const createMockState = (isUnknownJs, bomData, unknownJsData, metadata, embeddable) => ({
  applicationReport: {
    reportParameters: {
      appId: 'appId',
      scanId: 'scanId',
      isUnknownJs,
      embeddable: !!embeddable
    },
    bomData,
    unknownJsData,
    metadata
  }
});
const mockMetadata = { reportTitle: 'test' };
const mockUnknownJsData = {
  aaData: [{
    filenames: ['foo.js']
  }]
};
const mockBomData = {
  aaData: [{
    foo: 'bar'
  }]
};
const mockLicenseData = {
  aaData: []
};
const mockReportData = { fooReport: 'barReport' };

describe('applicationReportActions', function() {
  let applicationReportActions, mockAxiosCalls;

  beforeEach(angular.mock.module(applicationReportModule.name, function() {
    mockAxiosCalls = SpecUtil.axiosMockerGenerator(axios);
  }));

  beforeEach(inject(function(_applicationReportActions_) {
    applicationReportActions = _applicationReportActions_;
  }));

  describe('setReportParameters', () => {
    it('dispatches SET_REPORT_PARAMETERS action', () => {
      const store = SpecUtil.mockReduxStore({});
      store.dispatch(
          applicationReportActions.setReportParameters('appId', 'scanId', true, false, 'policyViolationId'));

      expect(store.getActions().length).toBe(1);
      expect(store.getActions()[0]).toEqual({
        type: 'SET_REPORT_PARAMETERS',
        payload: {
          appId: 'appId',
          scanId: 'scanId',
          isUnknownJs: true,
          embeddable: false,
          policyViolationId: 'policyViolationId'
        }
      });

      store.dispatch(
          applicationReportActions.setReportParameters('appId', 'scanId', true, false));
      expect(store.getActions().length).toBe(2);
      expect(store.getActions()[1]).toEqual({
        type: 'SET_REPORT_PARAMETERS',
        payload: {
          appId: 'appId',
          scanId: 'scanId',
          isUnknownJs: true,
          embeddable: false,
          policyViolationId: undefined
        }
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
            unknownJsData: mockUnknownJsData
          }
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
            unknownJsData: mockUnknownJsData
          }
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
            unknownJsData: mockUnknownJsData
          }
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
            unknownJsData: undefined
          }
        });
        done();
      });
    });

    it('does not fetch common data if bomData, metadata are set on the state and unknownJs is false', (done) => {
      const store = SpecUtil.mockReduxStore(createMockState(false, mockBomData, mockUnknownJsData, mockMetadata));

      expectCommonDataCalls(false, expectAdditionalCalls);

      store.dispatch(applicationReportActions[actionCreatorName]()).then(() => {
        expect(store.getActions()[1]).toEqual({
          type: 'LOAD_COMMON_DATA_UNNECESSARY'
        });
        done();
      });
    });

    it('does not fetch common data if bomData, metadata and unknownJsData are all on the state', (done) => {
      const store = SpecUtil.mockReduxStore(createMockState(true, mockBomData, mockUnknownJsData, mockMetadata));

      expectCommonDataCalls(false, expectAdditionalCalls);

      store.dispatch(applicationReportActions[actionCreatorName]()).then(() => {
        expect(store.getActions()[1]).toEqual({
          type: 'LOAD_COMMON_DATA_UNNECESSARY'
        });
        done();
      });
    });

    it('fires LOAD_COMMON_DATA_FAILED action and no further actions if the common data request fails', (done) => {
      const store = SpecUtil.mockReduxStore(createMockState(true, undefined, undefined, undefined));

      expectCommonDataCalls(false, expectAdditionalCalls);

      store.dispatch(applicationReportActions[actionCreatorName]()).then(() => {
        expect(store.getActions().length).toBe(2);
        expect(store.getActions()[1]).toEqual({
          type: 'LOAD_COMMON_DATA_FAILED',
          payload: 'Error 500'
        });
        done();
      });
    });

    it('redirects to the old app report page if this is an XC report', function(done) {
      const store = SpecUtil.mockReduxStore(createMockState(false, undefined, undefined, undefined));

      mockAxiosCalls({
        get: {
          [CLMLocation.getReportBomUrl('appId', 'scanId')]: { data: mockBomData },
          [CLMLocation.getReportMetadataUrl('appId', 'scanId')]: { data: { expandedCoverage: true } }
        }
      });

      store.dispatch(applicationReportActions[actionCreatorName]()).then(() => {
        expect(store.getActions().length).toBe(2);
        expect(store.getActions()[1]).toEqual({
          type: STATE_GO,
          payload: {
            to: 'report',
            params: { publicId: 'appId', scanId: 'scanId' },
            options: undefined
          }
        });
        done();
      });
    });

    it('redirects to the old iframe URL if this is an XC report and the embeddable flag is set', function (done) {
      const store = SpecUtil.mockReduxStore(createMockState(false, undefined, undefined, undefined, true));
      const spyRedirect = spyOn(CLMLocation, 'redirectTo').and.callFake(() => {});

      mockAxiosCalls({
        get: {
          [CLMLocation.getReportBomUrl('appId', 'scanId')]: { data: mockBomData },
          [CLMLocation.getReportMetadataUrl('appId', 'scanId')]: { data: { expandedCoverage: true } }
        }
      });

      store.dispatch(applicationReportActions[actionCreatorName]()).then(() => {
        expect(spyRedirect).toHaveBeenCalledWith(CLMLocation.getExpandedCoverageEmbeddableUrl('appId', 'scanId'));
        done();
      });
    });
  }

  describe('loadReport', function() {
    it('dispatches a LOAD_REPORT_REQUESTED action', function() {
      const store = SpecUtil.mockReduxStore(createMockState(false, mockBomData, mockUnknownJsData, mockMetadata));
      const errorSpy = jasmine.createSpy('errorSpy');
      store.dispatch(applicationReportActions.loadReport()).catch(errorSpy);

      expect(store.getActions().length).toBe(2);
      expect(store.getActions()[0]).toEqual({
        type: 'LOAD_REPORT_REQUESTED'
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

    it('fires LOAD_REPORT_FAILED action if report request fails', function(done) {
      const store = SpecUtil.mockReduxStore(createMockState(true, undefined, mockUnknownJsData, mockMetadata));

      expectCommonDataCalls(true, expectReportDataCalls(false));

      store.dispatch(applicationReportActions.loadReport()).then(() => {
        expect(store.getActions().length).toBe(3);
        expect(store.getActions()[2].type).toEqual('LOAD_REPORT_FAILED');
        done();
      });
    });

    it('fires LOAD_REPORT_FULFILLED action if report request succeeds', function(done) {
      const componentIdentifier = {
        format: 'maven',
        coordinates: {
          artifactId: 'logback-access',
          classifier: '',
          extension: 'jar',
          groupId: 'ch.qos.logback',
          version: '0.6'
        }
      };
      const bomData = {
        aaData: [{
          foo: 'bar',
          componentIdentifier
        }]
      };
      const store = SpecUtil.mockReduxStore(createMockState(false, bomData, undefined, mockMetadata));

      expectCommonDataCalls(true, expectReportDataCalls(true));

      store.dispatch(applicationReportActions.loadReport()).then(() => {
        expect(store.getActions().length).toBe(3);
        expect(store.getActions()[0]).toEqual({
          type: 'LOAD_REPORT_REQUESTED'
        });
        expect(store.getActions()[2]).toEqual({
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
                    version: '0.6'
                  }
                },
                serializedComponentIdentifier: serializeComponentIdentifier(componentIdentifier),
                policyThreatLevel: 0,
                policyName: 'None',
                waived: false,
                grandfathered: false,
                derivedComponentName: 'unknown',
                derivedDependencyType: 'direct',
                derivedViolationState: 'notViolating',
                dependencyInfo: { isDirectDependency: true }
              }
            ],
            fooReport: 'barReport',
            reportVersion: 3,
            isInnerSourceEnabled: false
          }
        });
        done();
      });
    });
  });

  describe('loadReportRawData', function() {
    it('dispatches a LOAD_REPORT_RAW_DATA_REQUESTED action', function() {
      const store = SpecUtil.mockReduxStore(createMockState(false, mockBomData, mockUnknownJsData, mockMetadata));
      store.dispatch(applicationReportActions.loadReportRawData());

      expect(store.getActions().length).toBe(2);
      expect(store.getActions()[0]).toEqual({
        type: 'LOAD_REPORT_RAW_DATA_REQUESTED'
      });
    });

    testCommonDataLoading('loadReportRawData', expectReportRawDataCalls(true));

    it('fires LOAD_REPORT_RAW_DATA_FAILED action if report request fails', function(done) {
      const store = SpecUtil.mockReduxStore(createMockState(true, undefined, mockUnknownJsData, mockMetadata));

      expectCommonDataCalls(true, expectReportRawDataCalls(false));

      store.dispatch(applicationReportActions.loadReportRawData()).then(() => {
        expect(store.getActions().length).toBe(3);
        expect(store.getActions()[2].type).toEqual('LOAD_REPORT_RAW_DATA_FAILED');
        done();
      });
    });

    it('fires LOAD_REPORT_RAW_DATA_FULFILLED action if report request succeeds', function(done) {
      const bomData = { aaData: [{ foo: 'bar' }] };
      const store = SpecUtil.mockReduxStore(createMockState(false, bomData, undefined, mockMetadata));
      expectCommonDataCalls(true, expectReportRawDataCalls(true));

      store.dispatch(applicationReportActions.loadReportRawData()).then(() => {
        expect(store.getActions().length).toBe(3);
        expect(store.getActions()[2]).toEqual({
          type: 'LOAD_REPORT_RAW_DATA_FULFILLED',
          payload: [{
            derivedComponentName: 'unknown',
            license: undefined,
            licenseSortKey: '',
            foo: 'bar'
          }]
        });

        done();
      });

      expect(store.getActions().length).toBe(2);
      expect(store.getActions()[0]).toEqual({
        type: 'LOAD_REPORT_RAW_DATA_REQUESTED'
      });
    });
  });

  describe('setAggregateReportEntries', function() {
    it('returns a SET_AGGREGATE_REPORT_ENTRIES action with the specified payload value', function() {
      const payload = {},
          action = applicationReportActions.setAggregateReportEntries(payload);

      expect(action.type).toBe('SET_AGGREGATE_REPORT_ENTRIES');
      expect(action.payload).toBe(payload);
    });
  });

  describe('setSorting', function() {
    it('returns a SET_SORTING action with the specified payload value', function() {
      const payload = {},
          action = applicationReportActions.setSorting(payload);

      expect(action.type).toBe('SET_SORTING');
      expect(action.payload).toBe(payload);
    });
  });

  describe('setExactValueFilter', function() {
    it('returns a SET_EXACT_VALUE_FILTER action with payload having the specified fieldName and allowedValues',
        function() {
          const allowedValues = new Set(['foo', 'bar']),
              action = applicationReportActions.setExactValueFilter('fooField', allowedValues);

          expect(action.type).toBe('SET_EXACT_VALUE_FILTER');
          expect(action.payload).toEqual({
            fieldName: 'fooField',
            allowedValues
          });
        }
    );
  });

  describe('setStringFieldFilter', function() {
    it('returns a SET_SUBSTRING_FIELD_FILTER action with payload having the specified fieldName and filterString',
        function() {
          const action = applicationReportActions.setStringFieldFilter('fooField', 'bar');

          expect(action.type).toBe('SET_SUBSTRING_FIELD_FILTER');
          expect(action.payload).toEqual({
            fieldName: 'fooField',
            filterString: 'bar'
          });
        }
    );
  });

  describe('setRawDataStringFieldFilter', function() {
    it('returns a SET_RAW_DATA_SUBSTRING_FIELD_FILTER action with payload of specified fieldName and filterString',
        function() {
          const action = applicationReportActions.setRawDataStringFieldFilter('fooField', 'bar');

          expect(action.type).toBe('SET_RAW_DATA_SUBSTRING_FIELD_FILTER');
          expect(action.payload).toEqual({
            fieldName: 'fooField',
            filterString: 'bar'
          });
        }
    );
  });

  describe('setRawDataNumericMaxFilter', function() {
    it('returns a SET_RAW_DATA_NUMERIC_FIELD_MAX_FILTER action with correct payload',
        function() {
          const action = applicationReportActions.setRawDataNumericMaxFilter('fooField', 'bar');

          expect(action.type).toBe('SET_RAW_DATA_NUMERIC_FIELD_MAX_FILTER');
          expect(action.payload).toEqual({
            fieldName: 'fooField',
            filterValue: 'bar'
          });
        }
    );
  });

  describe('setRawDataNumericMinFilter', function() {
    it('returns a SET_RAW_DATA_NUMERIC_FIELD_MIN_FILTER action with correct payload',
        function() {
          const action = applicationReportActions.setRawDataNumericMinFilter('fooField', 'bar');

          expect(action.type).toBe('SET_RAW_DATA_NUMERIC_FIELD_MIN_FILTER');
          expect(action.payload).toEqual({
            fieldName: 'fooField',
            filterValue: 'bar'
          });
        }
    );
  });

  describe('reevaluateReport', function() {
    it('fires REEVALUATE_REPORT_FAILED action if the reevaluation request fails', function (done) {
      const store = SpecUtil.mockReduxStore(createMockState(false));

      mockAxiosCalls({
        post: {
          [CLMLocation.getReportReevaluateUrl('appId', 'scanId')]: Promise.reject({ status: 500, data: 'test error' })
        }
      });

      store.dispatch(applicationReportActions.reevaluateReport()).then(() => {
        expect(store.getActions().length).toBe(2);
        expect(store.getActions()[0]).toEqual({
          type: 'REEVALUATE_REPORT_REQUESTED'
        });
        expect(store.getActions()[1]).toEqual({
          type: 'REEVALUATE_REPORT_FAILED',
          payload: 'test error'
        });
        done();
      });
    });

    it('loads the report after reevaluation', function (done) {
      const store = SpecUtil.mockReduxStore(createMockState(false, mockBomData, mockUnknownJsData, mockMetadata));

      mockAxiosCalls({
        post: {
          [CLMLocation.getReportReevaluateUrl('appId', 'scanId')]: Promise.resolve({ data: '' })
        }
      });

      expectCommonDataCalls(true, expectReportDataCalls(true));

      store.dispatch(applicationReportActions.reevaluateReport()).then(() => {
        expect(store.getActions().length).toBe(5);
        expect(store.getActions()[0]).toEqual({
          type: 'REEVALUATE_REPORT_REQUESTED'
        });

        expect(store.getActions()[1]).toEqual({
          type: 'REEVALUATE_REPORT_FULFILLED'
        });

        expect(store.getActions()[2]).toEqual({
          type: 'LOAD_REPORT_REQUESTED'
        });

        expect(store.getActions()[3]).toEqual({
          type: 'LOAD_COMMON_DATA_FULFILLED',
          payload: {
            bomData: mockBomData,
            metadata: mockMetadata,
            unknownJsData: undefined
          }
        });
        expect(store.getActions()[4]).toEqual({
          type: 'LOAD_REPORT_FULFILLED',
          payload: {
            allEntries: [{
              filenames: ['foo.js'],
              policyThreatLevel: 0,
              policyName: 'None',
              waived: false,
              grandfathered: false,
              derivedComponentName: 'foo.js',
              derivedDependencyType: 'unknown',
              derivedViolationState: 'notViolating'
            }],
            fooReport: 'barReport',
            reportVersion: 3,
            isInnerSourceEnabled: false
          }
        });
        done();
      });
    });

    it('does not fire REEVALUATE_REPORT_FAILED if the load afterwards fails', function (done) {
      const store = SpecUtil.mockReduxStore(createMockState(false, mockBomData, mockUnknownJsData, mockMetadata));

      mockAxiosCalls({
        post: {
          [CLMLocation.getReportReevaluateUrl('appId', 'scanId')]: Promise.resolve({ data: '' })
        }
      });

      expectCommonDataCalls(true, expectReportDataCalls(false));

      store.dispatch(applicationReportActions.reevaluateReport()).then(() => {
        expect(store.getActions().length).toBe(5);
        expect(store.getActions()[0]).toEqual({
          type: 'REEVALUATE_REPORT_REQUESTED'
        });
        expect(store.getActions()[1]).toEqual({
          type: 'REEVALUATE_REPORT_FULFILLED'
        });

        expect(store.getActions()[2]).toEqual({
          type: 'LOAD_REPORT_REQUESTED'
        });

        expect(store.getActions()[3]).toEqual({
          type: 'LOAD_COMMON_DATA_FULFILLED',
          payload: {
            bomData: mockBomData,
            metadata: mockMetadata,
            unknownJsData: undefined
          }
        });

        expect(store.getActions()[4].type).toEqual('LOAD_REPORT_FAILED');
        done();
      });

      expect(store.getActions().length).toBe(1);
    });
  });

  describe('reevaluateReportCancelled', function() {
    it('returns a REEVALUATE_REPORT_CANCELLED action with no payload', function() {
      const action = applicationReportActions.reevaluateReportCancelled();

      expect(action.type).toBe('REEVALUATE_REPORT_CANCELLED');
      expect(action.payload).not.toBeDefined();
    });
  });

  describe('loadReportAllData', function() {
    it('dispatches a LOAD_REPORT_ALL_DATA_REQUESTED action', function() {
      const store = SpecUtil.mockReduxStore(createMockState(false, mockBomData, mockUnknownJsData, mockMetadata));
      const errorSpy = jasmine.createSpy('errorSpy');
      store.dispatch(applicationReportActions.loadReportAllData()).catch(errorSpy);

      expect(store.getActions().length).toBe(2);
      expect(store.getActions()[0]).toEqual({
        type: 'LOAD_REPORT_ALL_DATA_REQUESTED'
      });
    });

    testCommonDataLoading('loadReportAllData', {
      ...expectReportDataCalls(true),
      ...expectReportRawDataCalls(true)
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
        ...expectReportRawDataCalls(true)
      });

      store.dispatch(applicationReportActions.loadReportAllData()).finally(() => {
        expect(store.getActions().length).toBe(5);
        expect(store.getActions()[4]).toEqual({ type: 'GENERATE_VULNERABILITY_ENTRIES' });
        done();
      });
    });
  });

  describe('selectRootAncestor', function() {
    it('returns a SELECT_ROOT_ANCESTOR action with the specified payload value', function() {
      const payload = {},
          action = applicationReportActions.selectRootAncestor(payload);

      expect(action.type).toBe('SELECT_ROOT_ANCESTOR');
      expect(action.payload).toBe(payload);
    });
  });

  describe('unselectRootAncestor', function() {
    it('returns a UNSELECT_ROOT_ANCESTOR action with no payload', function() {
      const action = applicationReportActions.unselectRootAncestor();

      expect(action).toEqual({
        type: 'UNSELECT_ROOT_ANCESTOR'
      });
    });
  });

  describe('setSortingParameters', () => {
    it('dispatches SET_SORTING_PARAMETERS action', () => {
      const store = SpecUtil.mockReduxStore({});
      store.dispatch(
          applicationReportActions.setSortingParameters('key', ['a', 'b'], 'dir'));

      expect(store.getActions().length).toBe(1);
      expect(store.getActions()[0]).toEqual({
        type: 'SET_SORTING_PARAMETERS',
        payload: {
          key: 'key',
          sortFields: ['a', 'b'],
          dir: 'dir'
        }
      });
    });
  });

  function expectCommonDataCalls(isSuccess, additionalCalls = {}) {
    mockAxiosCalls({
      get: {
        [CLMLocation.getReportBomUrl('appId', 'scanId')]:
          isSuccess ? { data: mockBomData } : Promise.reject({ status: 500 }),
        [CLMLocation.getReportMetadataUrl('appId', 'scanId')]:
          isSuccess ? { data: mockMetadata } : Promise.reject({ status: 500 }),
        [CLMLocation.getReportUnknownJsUrl('appId', 'scanId')]: { data: mockUnknownJsData },
        ...additionalCalls
      }
    });
  }

  function expectReportDataCalls(isSuccess) {
    return {
      [CLMLocation.getReportPolicyThreatsUrl('appId', 'scanId')]:
        isSuccess ? { data: { version: 3, aaData: [] } } : Promise.reject({ status: 500 }),
      [CLMLocation.getReportDataUrl('appId', 'scanId')]:
        isSuccess ? { data: mockReportData } : Promise.reject({ status: 500 }),
      [CLMLocation.getReportPartialMatchedUrl('appId', 'scanId')]:
        isSuccess ? { data: { aaData: [] } } : Promise.reject({ status: 500 }),
      [CLMLocation.getDependenciesUrl('appId', 'scanId')]:
        isSuccess ? {
          data: {
            dependencyGraph: [{
              children: [{
                componentIdentifier: {
                  format: 'maven',
                  coordinates: {
                    artifactId: 'logback-access',
                    classifier: '',
                    extension: 'jar',
                    groupId: 'ch.qos.logback',
                    version: '0.6'
                  }
                }
              }]
            }, {
              componentIdentifier: {
                format: 'maven',
                coordinates: {
                  artifactId: 'logback-access',
                  classifier: '',
                  extension: 'jar',
                  groupId: 'ch.qos.logback',
                  version: '0.6'
                }
              }
            }]
          }
        } : Promise.reject({ status: 500 })
    };
  }

  function expectReportRawDataCalls(isSuccess) {
    return {
      [CLMLocation.getReportSecurityUrl('appId', 'scanId')]:
        isSuccess ? { data: { aaData: [] } } : Promise.reject({ status: 500 }),
      [CLMLocation.getReportLicenseUrl('appId', 'scanId')]:
        isSuccess ? { data: mockLicenseData } : Promise.reject({ status: 500 })
    };
  }
});
