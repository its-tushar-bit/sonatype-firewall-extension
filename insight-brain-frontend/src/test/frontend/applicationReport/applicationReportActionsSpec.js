import applicationReportModule from '../../../main/frontend/applicationReport/module';

const createMockState = (isUnknownJs, bomData, unknownJsData, metadata) => ({
  applicationReport: {
    reportParameters: {
      appId: 'appId',
      scanId: 'scanId',
      isUnknownJs
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
  let applicationReportActions, CLMLocations, $httpBackend;

  beforeEach(angular.mock.module(applicationReportModule.name));

  beforeEach(inject(function($injector) {
    applicationReportActions = $injector.get('applicationReportActions');
    CLMLocations = $injector.get('CLMLocations');
    $httpBackend = $injector.get('$httpBackend');
  }));

  afterEach(function() {
    $httpBackend.verifyNoOutstandingExpectation();
    $httpBackend.verifyNoOutstandingRequest();
  });

  describe('setReportParameters', () => {
    it('dispatches SET_REPORT_PARAMETERS action', () => {
      const store = SpecUtil.mockReduxStore({});
      store.dispatch(applicationReportActions.setReportParameters('appId', 'scanId', true));

      expect(store.getActions().length).toBe(1);
      expect(store.getActions()[0]).toEqual({
        type: 'SET_REPORT_PARAMETERS',
        payload: {
          appId: 'appId',
          scanId: 'scanId',
          isUnknownJs: true
        }
      });
    });
  });

  /**
   * Tests of common behavior for all action creators that should conditionally fetch the common data (bom, metadata,
   * and unknownjs).  The tests defined here expect the action creator to initially fire some action, the details
   * of which are not specific to the common data and which are therefore not tested here.
   * @param actionCreatorName The property on applicationReportActions containing the action creator function to invoke
   * @param expectAdditionalCalls A function that sets up additional HTTP call expectations specific to the action
   * creator under test which come after the common data HTTP expectations, when appropriate
   */
  function testCommonDataLoading(actionCreatorName, expectAdditionalCalls) {
    it('fetches common data if bomData is not on the state', () => {
      const store = SpecUtil.mockReduxStore(createMockState(true, undefined, mockUnknownJsData, mockMetadata));
      const errorSpy = jasmine.createSpy('errorSpy');

      store.dispatch(applicationReportActions[actionCreatorName]()).catch(errorSpy);

      expect(store.getActions().length).toBe(1);

      expectCommonDataCalls(true);
      expectUnknownJsCall();
      expectAdditionalCalls();
      $httpBackend.flush(3);

      expect(store.getActions().length).toBe(2);
      expect(store.getActions()[1]).toEqual({
        type: 'LOAD_COMMON_DATA_FULFILLED',
        payload: {
          bomData: mockBomData,
          metadata: mockMetadata,
          unknownJsData: mockUnknownJsData
        }
      });

      $httpBackend.flush();
    });

    it('fetches common data if metadata is not on the state', () => {
      const store = SpecUtil.mockReduxStore(createMockState(true, mockBomData, mockUnknownJsData, undefined));
      const errorSpy = jasmine.createSpy('errorSpy');

      store.dispatch(applicationReportActions[actionCreatorName]()).catch(errorSpy);

      expect(store.getActions().length).toBe(1);

      expectCommonDataCalls(true);
      expectUnknownJsCall();
      expectAdditionalCalls();
      $httpBackend.flush(3);

      expect(store.getActions().length).toBe(2);
      expect(store.getActions()[1]).toEqual({
        type: 'LOAD_COMMON_DATA_FULFILLED',
        payload: {
          bomData: mockBomData,
          metadata: mockMetadata,
          unknownJsData: mockUnknownJsData
        }
      });

      $httpBackend.flush();
    });

    it('fetches common data if unknownJs is on and unknownJsData is not on the state', () => {
      const store = SpecUtil.mockReduxStore(createMockState(true, mockBomData, undefined, mockMetadata));
      const errorSpy = jasmine.createSpy('errorSpy');

      store.dispatch(applicationReportActions[actionCreatorName]()).catch(errorSpy);

      expect(store.getActions().length).toBe(1);

      expectCommonDataCalls(true);
      expectUnknownJsCall();
      expectAdditionalCalls();
      $httpBackend.flush(3);

      expect(store.getActions().length).toBe(2);
      expect(store.getActions()[1]).toEqual({
        type: 'LOAD_COMMON_DATA_FULFILLED',
        payload: {
          bomData: mockBomData,
          metadata: mockMetadata,
          unknownJsData: mockUnknownJsData
        }
      });

      $httpBackend.flush();
    });

    it('does not fetch unknownJsData if unknownJs is off', () => {
      const store = SpecUtil.mockReduxStore(createMockState(false, undefined, undefined, undefined));
      const errorSpy = jasmine.createSpy('errorSpy');

      store.dispatch(applicationReportActions[actionCreatorName]()).catch(errorSpy);

      expect(store.getActions().length).toBe(1);

      expectCommonDataCalls(true);
      expectAdditionalCalls();
      $httpBackend.flush(2);

      expect(store.getActions().length).toBe(2);
      expect(store.getActions()[1]).toEqual({
        type: 'LOAD_COMMON_DATA_FULFILLED',
        payload: {
          bomData: mockBomData,
          metadata: mockMetadata,
          unknownJsData: undefined
        }
      });

      $httpBackend.flush();
    });

    it('does not fetch common data if bomData, metadata are set on the state and unknownJs is false', () => {
      const store = SpecUtil.mockReduxStore(createMockState(false, mockBomData, mockUnknownJsData, mockMetadata));
      const errorSpy = jasmine.createSpy('errorSpy');

      store.dispatch(applicationReportActions[actionCreatorName]()).catch(errorSpy);

      expect(store.getActions().length).toBe(1);

      expectAdditionalCalls();
      $httpBackend.flush();
    });

    it('does not fetch common data if bomData, metadata and unknownJsData are all on the state', () => {
      const store = SpecUtil.mockReduxStore(createMockState(true, mockBomData, mockUnknownJsData, mockMetadata));
      const errorSpy = jasmine.createSpy('errorSpy');

      store.dispatch(applicationReportActions[actionCreatorName]()).catch(errorSpy);

      expect(store.getActions().length).toBe(1);

      expectAdditionalCalls();
      $httpBackend.flush();
    });

    it('fires LOAD_COMMON_DATA_FAILED action and no further actions if the common data request fails', () => {
      const store = SpecUtil.mockReduxStore(createMockState(true, undefined, undefined, undefined));
      const errorSpy = jasmine.createSpy('errorSpy');
      store.dispatch(applicationReportActions[actionCreatorName]()).catch(errorSpy);

      expect(store.getActions().length).toBe(1);

      expectCommonDataCalls(false);
      expectUnknownJsCall();
      $httpBackend.flush();

      expect(errorSpy).toHaveBeenCalled();
      expect(store.getActions().length).toBe(2);
      expect(store.getActions()[1]).toEqual({
        type: 'LOAD_COMMON_DATA_FAILED',
        payload: 'Error 500'
      });
    });
  }

  describe('loadReport', function() {
    it('dispatches a LOAD_REPORT_REQUESTED action', function() {
      const store = SpecUtil.mockReduxStore(createMockState(false, mockBomData, mockUnknownJsData, mockMetadata));
      const errorSpy = jasmine.createSpy('errorSpy');
      store.dispatch(applicationReportActions.loadReport()).catch(errorSpy);

      expect(store.getActions().length).toBe(1);
      expect(store.getActions()[0]).toEqual({
        type: 'LOAD_REPORT_REQUESTED'
      });

      expectReportDataCalls(false);
      $httpBackend.flush();
    });

    testCommonDataLoading('loadReport', () => expectReportDataCalls(true));

    it('loads report information if forceCache is true, even if it is already on the state', () => {
      const store = SpecUtil.mockReduxStore(createMockState(true, mockBomData, mockUnknownJsData, mockMetadata));
      const errorSpy = jasmine.createSpy('errorSpy');

      store.dispatch(applicationReportActions.loadReport(true)).catch(errorSpy);

      expect(store.getActions().length).toBe(1);

      expectCommonDataCalls(true);
      expectUnknownJsCall();
      expectReportDataCalls(true);

      $httpBackend.flush();
    });

    it('fires LOAD_REPORT_FAILED action if report request fails', function() {
      const store = SpecUtil.mockReduxStore(createMockState(true, undefined, mockUnknownJsData, mockMetadata));
      const errorSpy = jasmine.createSpy('errorSpy');
      store.dispatch(applicationReportActions.loadReport()).catch(errorSpy);

      expect(store.getActions().length).toBe(1);
      expect(store.getActions()[0]).toEqual({
        type: 'LOAD_REPORT_REQUESTED'
      });

      expectCommonDataCalls(true);
      expectUnknownJsCall();
      expectReportDataCalls(false);
      $httpBackend.flush();

      expect(errorSpy).toHaveBeenCalled();
      expect(store.getActions().length).toBe(3);
      expect(store.getActions()[2].type).toEqual('LOAD_REPORT_FAILED');
    });

    it('fires LOAD_REPORT_FULFILLED action if report request succeeds', function() {
      const bomData = {aaData: [{foo: 'bar'}]};
      const store = SpecUtil.mockReduxStore(createMockState(false, bomData, undefined, mockMetadata));

      const errorSpy = jasmine.createSpy('errorSpy');
      store.dispatch(applicationReportActions.loadReport()).catch(errorSpy);

      expect(store.getActions().length).toBe(1);
      expect(store.getActions()[0]).toEqual({
        type: 'LOAD_REPORT_REQUESTED'
      });

      expectReportDataCalls(true);

      $httpBackend.flush();

      expect(errorSpy).not.toHaveBeenCalled();
      expect(store.getActions().length).toBe(2);
      expect(store.getActions()[1]).toEqual({
        type: 'LOAD_REPORT_FULFILLED',
        payload: {
          allEntries: [
            {
              foo: 'bar',
              policyThreatLevel: 0,
              policyName: 'None',
              waived: false,
              grandfathered: false,
              derivedComponentName: 'unknown',
              derivedViolationState: 'notViolating'
            }
          ],
          fooReport: 'barReport',
          reportVersion: 3
        }
      });
    });
  });

  describe('loadReportRawData', function() {
    it('dispatches a LOAD_REPORT_RAW_DATA_REQUESTED action', function() {
      const store = SpecUtil.mockReduxStore(createMockState(false, mockBomData, mockUnknownJsData, mockMetadata));
      const errorSpy = jasmine.createSpy('errorSpy');
      store.dispatch(applicationReportActions.loadReportRawData()).catch(errorSpy);

      expect(store.getActions().length).toBe(1);
      expect(store.getActions()[0]).toEqual({
        type: 'LOAD_REPORT_RAW_DATA_REQUESTED'
      });

      expectReportRawDataCalls(false);
      $httpBackend.flush();
    });

    testCommonDataLoading('loadReportRawData', () => expectReportRawDataCalls(true));

    it('fires LOAD_REPORT_RAW_DATA_FAILED action if report request fails', function() {
      const store = SpecUtil.mockReduxStore(createMockState(true, undefined, mockUnknownJsData, mockMetadata));
      const errorSpy = jasmine.createSpy('errorSpy');
      store.dispatch(applicationReportActions.loadReportRawData()).catch(errorSpy);

      expect(store.getActions().length).toBe(1);
      expect(store.getActions()[0]).toEqual({
        type: 'LOAD_REPORT_RAW_DATA_REQUESTED'
      });

      expectCommonDataCalls(true);
      expectUnknownJsCall();
      expectReportRawDataCalls(false);
      $httpBackend.flush();

      expect(errorSpy).toHaveBeenCalled();
      expect(store.getActions().length).toBe(3);
      expect(store.getActions()[2].type).toEqual('LOAD_REPORT_RAW_DATA_FAILED');
    });

    it('fires LOAD_REPORT_RAW_DATA_FULFILLED action if report request succeeds', function() {
      const bomData = {aaData: [{foo: 'bar'}]};
      const store = SpecUtil.mockReduxStore(createMockState(false, bomData, undefined, mockMetadata));

      const errorSpy = jasmine.createSpy('errorSpy');
      store.dispatch(applicationReportActions.loadReportRawData()).catch(errorSpy);

      expect(store.getActions().length).toBe(1);
      expect(store.getActions()[0]).toEqual({
        type: 'LOAD_REPORT_RAW_DATA_REQUESTED'
      });

      expectReportRawDataCalls(true);

      $httpBackend.flush();

      expect(errorSpy).not.toHaveBeenCalled();
      expect(store.getActions().length).toBe(2);
      expect(store.getActions()[1]).toEqual({
        type: 'LOAD_REPORT_RAW_DATA_FULFILLED',
        payload: [{
          derivedComponentName: 'unknown',
          license: undefined,
          licenseSortKey: '',
          foo: 'bar'
        }]
      });
    });
  });

  describe('reloadReport', function() {
    it('dispatches a RELOAD_REPORT_REQUESTED action', function() {
      const store = SpecUtil.mockReduxStore(createMockState(false, mockBomData, mockUnknownJsData, mockMetadata));
      const errorSpy = jasmine.createSpy('errorSpy');
      store.dispatch(applicationReportActions.reloadReport()).catch(errorSpy);

      expect(store.getActions().length).toBe(1);
      expect(store.getActions()[0]).toEqual({
        type: 'RELOAD_REPORT_REQUESTED'
      });

      expectReportDataCalls(false);
      $httpBackend.flush();
    });

    testCommonDataLoading('reloadReport', () => expectReportDataCalls(true));

    it('fires LOAD_REPORT_FAILED action if report request fails', function() {
      const store = SpecUtil.mockReduxStore(createMockState(true, undefined, mockUnknownJsData, mockMetadata));
      const errorSpy = jasmine.createSpy('errorSpy');
      store.dispatch(applicationReportActions.reloadReport()).catch(errorSpy);

      expect(store.getActions().length).toBe(1);
      expect(store.getActions()[0]).toEqual({
        type: 'RELOAD_REPORT_REQUESTED'
      });

      expectCommonDataCalls(true);
      expectUnknownJsCall();
      expectReportDataCalls(false);
      $httpBackend.flush();

      expect(errorSpy).toHaveBeenCalled();
      expect(store.getActions().length).toBe(3);
      expect(store.getActions()[2].type).toEqual('LOAD_REPORT_FAILED');
    });

    it('fires LOAD_REPORT_FULFILLED action if report request succeeds', function() {
      const bomData = {aaData: [{foo: 'bar'}]};
      const store = SpecUtil.mockReduxStore(createMockState(false, bomData, undefined, mockMetadata));

      const errorSpy = jasmine.createSpy('errorSpy');
      store.dispatch(applicationReportActions.reloadReport()).catch(errorSpy);

      expect(store.getActions().length).toBe(1);
      expect(store.getActions()[0]).toEqual({
        type: 'RELOAD_REPORT_REQUESTED'
      });

      expectReportDataCalls(true);

      $httpBackend.flush();

      expect(errorSpy).not.toHaveBeenCalled();
      expect(store.getActions().length).toBe(2);
      expect(store.getActions()[1]).toEqual({
        type: 'LOAD_REPORT_FULFILLED',
        payload: {
          allEntries: [
            {
              foo: 'bar',
              policyThreatLevel: 0,
              policyName: 'None',
              waived: false,
              grandfathered: false,
              derivedComponentName: 'unknown',
              derivedViolationState: 'notViolating'
            }
          ],
          fooReport: 'barReport',
          reportVersion: 3
        }
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
    it('fires REEVALUATE_REPORT_FAILED action if the reevaluation request fails', function() {
      const store = SpecUtil.mockReduxStore(createMockState(false)),
          errorSpy = jasmine.createSpy('errorSpy');

      store.dispatch(applicationReportActions.reevaluateReport()).catch(errorSpy);

      expect(store.getActions().length).toBe(1);
      expect(store.getActions()[0]).toEqual({
        type: 'REEVALUATE_REPORT_REQUESTED'
      });

      $httpBackend.expectPOST(SpecUtil.toRegExp(CLMLocations.getReportReevaluateUrl('appId', 'scanId'))).respond(500,
          'test error');

      $httpBackend.flush();

      expect(errorSpy).toHaveBeenCalled();
      expect(store.getActions().length).toBe(2);
      expect(store.getActions()[1]).toEqual({
        type: 'REEVALUATE_REPORT_FAILED',
        payload: 'test error'
      });
    });

    it('loads the report after reevaluation', function() {
      const store = SpecUtil.mockReduxStore(createMockState(false, mockBomData, mockUnknownJsData, mockMetadata));
      const errorSpy = jasmine.createSpy('errorSpy');

      store.dispatch(applicationReportActions.reevaluateReport()).catch(errorSpy);

      expect(store.getActions().length).toBe(1);
      expect(store.getActions()[0]).toEqual({
        type: 'REEVALUATE_REPORT_REQUESTED'
      });

      $httpBackend.expectPOST(SpecUtil.toRegExp(CLMLocations.getReportReevaluateUrl('appId', 'scanId'))).respond(200);

      expectCommonDataCalls(true);
      expectReportDataCalls(true);
      $httpBackend.flush();

      expect(errorSpy).not.toHaveBeenCalled();
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
            derivedViolationState: 'notViolating'
          }],
          fooReport: 'barReport',
          reportVersion: 3
        }
      });
    });

    it('does not fire REEVALUATE_REPORT_FAILED if the load afterwards fails', function() {
      const store = SpecUtil.mockReduxStore(createMockState(false, mockBomData, mockUnknownJsData, mockMetadata));
      const errorSpy = jasmine.createSpy('errorSpy');

      store.dispatch(applicationReportActions.reevaluateReport()).catch(errorSpy);

      expect(store.getActions().length).toBe(1);
      expect(store.getActions()[0]).toEqual({
        type: 'REEVALUATE_REPORT_REQUESTED'
      });

      $httpBackend.expectPOST(SpecUtil.toRegExp(CLMLocations.getReportReevaluateUrl('appId', 'scanId'))).respond(200);
      expectCommonDataCalls(true);
      expectReportDataCalls(false);
      $httpBackend.flush();

      expect(errorSpy).toHaveBeenCalled();
      expect(store.getActions().length).toBe(5);

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
    });
  });

  describe('reevaluateReportCancelled', function() {
    it('returns a REEVALUATE_REPORT_CANCELLED action with no payload', function() {
      const action = applicationReportActions.reevaluateReportCancelled();

      expect(action.type).toBe('REEVALUATE_REPORT_CANCELLED');
      expect(action.payload).not.toBeDefined();
    });
  });

  function expectCommonDataCalls(isSuccess) {
    if (isSuccess) {
      $httpBackend.expectGET(SpecUtil.toRegExp(
          CLMLocations.getReportBomUrl('appId', 'scanId'))).respond(200, mockBomData);
      $httpBackend.expectGET(SpecUtil.toRegExp(
          CLMLocations.getReportMetadataUrl('appId', 'scanId'))).respond(200, mockMetadata);
    }
    else {
      $httpBackend.expectGET(SpecUtil.toRegExp(
          CLMLocations.getReportBomUrl('appId', 'scanId'))).respond(500);
      $httpBackend.expectGET(SpecUtil.toRegExp(
          CLMLocations.getReportMetadataUrl('appId', 'scanId'))).respond(500);
    }
  }

  function expectUnknownJsCall() {
    $httpBackend.expectGET(SpecUtil.toRegExp(
        CLMLocations.getReportUnknownJsUrl('appId', 'scanId'))).respond(200, mockUnknownJsData);
  }

  function expectReportDataCalls(isSuccess) {
    const httpCode = isSuccess ? 200 : 500;

    $httpBackend.expectGET(SpecUtil.toRegExp(CLMLocations.getReportPolicyThreatsUrl('appId', 'scanId'))).respond(
        httpCode, { version: 3, aaData: [] });
    $httpBackend.expectGET(SpecUtil.toRegExp(CLMLocations.getReportDataUrl('appId', 'scanId'))).respond(
        httpCode, mockReportData);
    $httpBackend.expectGET(SpecUtil.toRegExp(CLMLocations.getReportPartialMatchedUrl('appId', 'scanId')))
        .respond(httpCode);
  }

  function expectReportRawDataCalls(isSuccess) {
    const httpCode = isSuccess ? 200 : 500;

    $httpBackend.expectGET(SpecUtil.toRegExp(CLMLocations.getReportSecurityUrl('appId', 'scanId'))).respond(httpCode);
    $httpBackend.expectGET(SpecUtil.toRegExp(CLMLocations.getReportLicenseUrl('appId', 'scanId'))).respond(
        httpCode, mockLicenseData);
  }
});
