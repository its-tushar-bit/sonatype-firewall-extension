import applicationReportModule from '../../../main/frontend/applicationReport/module';

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

  describe('loadReport', function() {
    it('fires LOAD_REPORT_FAILED action if report request fails', function() {
      const store = SpecUtil.mockReduxStore({});
      const errorSpy = jasmine.createSpy('errorSpy');
      store.dispatch(applicationReportActions.loadReport('appId', 'scanId')).catch(errorSpy);

      expect(store.getActions().length).toBe(1);
      expect(store.getActions()[0]).toEqual({
        type: 'LOAD_REPORT_REQUESTED'
      });

      mockFetchReportDataFailure();
      $httpBackend.flush();

      expect(errorSpy).toHaveBeenCalled();
      expect(store.getActions().length).toBe(2);
      expect(store.getActions()[1]).toEqual({
        type: 'LOAD_REPORT_FAILED',
        payload: 'test error'
      });
    });

    it('fires LOAD_REPORT_FULFILLED action if report request succeeds', function() {
      const store = SpecUtil.mockReduxStore({});
      const errorSpy = jasmine.createSpy('errorSpy');
      store.dispatch(applicationReportActions.loadReport('appId', 'scanId', false)).catch(errorSpy);

      expect(store.getActions().length).toBe(1);
      expect(store.getActions()[0]).toEqual({
        type: 'LOAD_REPORT_REQUESTED'
      });

      mockFetchReportDataSuccess();

      $httpBackend.flush();

      expect(errorSpy).not.toHaveBeenCalled();
      expect(store.getActions().length).toBe(2);
      expect(store.getActions()[1]).toEqual({
        type: 'LOAD_REPORT_FULFILLED',
        payload: {
          report: {
            allEntries: [],
            scanId: 'scanId'
          },
          metadata: {
            reportTitle: 'test'
          },
          isUnknownJs: false,
          reportVersion: 3
        }
      });
    });

    it('fetches unknown js results when so told', function() {
      const store = SpecUtil.mockReduxStore({});
      const errorSpy = jasmine.createSpy('errorSpy');
      store.dispatch(applicationReportActions.loadReport('appId', 'scanId', true)).catch(errorSpy);

      expect(store.getActions().length).toBe(1);
      expect(store.getActions()[0]).toEqual({
        type: 'LOAD_REPORT_REQUESTED'
      });

      mockFetchReportDataSuccess(true);
      $httpBackend.flush();

      expect(errorSpy).not.toHaveBeenCalled();
      expect(store.getActions().length).toBe(2);
      expect(store.getActions()[1]).toEqual({
        type: 'LOAD_REPORT_FULFILLED',
        payload: {
          report: {
            allEntries: [{
              filenames: ['foo.js'],
              policyThreatLevel: 0,
              policyName: 'None',
              waived: false,
              grandfathered: false,
              derivedComponentName: 'foo.js',
              derivedViolationState: 'notViolating'
            }],
            scanId: 'scanId'
          },
          metadata: {
            reportTitle: 'test'
          },
          isUnknownJs: true,
          reportVersion: 3
        }
      });
    });
  });

  describe('reloadReport', function() {
    let initialState;
    beforeEach(function() {
      initialState = {
        applicationReport: {
          isUnknownJs: false,
          metadata: {
            application: { publicId: 'appId' }
          },
          selectedReport: {
            scanId: 'scanId'
          },
          reportVersion: 3
        }
      };
    });

    it('fires LOAD_REPORT_FAILED action if report request fails', function() {
      const store = SpecUtil.mockReduxStore(initialState);
      const errorSpy = jasmine.createSpy('errorSpy');
      store.dispatch(applicationReportActions.reloadReport()).catch(errorSpy);

      // should not dispatch 'LOAD_REPORT_REQUESTED'
      expect(store.getActions().length).toBe(0);

      mockFetchReportDataFailure();
      $httpBackend.flush();

      expect(errorSpy).toHaveBeenCalled();
      expect(store.getActions().length).toBe(1);
      expect(store.getActions()[0]).toEqual({
        type: 'LOAD_REPORT_FAILED',
        payload: 'test error'
      });
    });

    it('fires LOAD_REPORT_FULFILLED action if report request succeeds', function() {
      const store = SpecUtil.mockReduxStore(initialState);
      const errorSpy = jasmine.createSpy('errorSpy');
      store.dispatch(applicationReportActions.reloadReport()).catch(errorSpy);

      // should not dispatch 'LOAD_REPORT_REQUESTED'
      expect(store.getActions().length).toBe(0);

      mockFetchReportDataSuccess();
      $httpBackend.flush();

      expect(errorSpy).not.toHaveBeenCalled();
      expect(store.getActions().length).toBe(1);
      expect(store.getActions()[0]).toEqual({
        type: 'LOAD_REPORT_FULFILLED',
        payload: {
          report: {
            allEntries: [],
            scanId: 'scanId'
          },
          metadata: {
            reportTitle: 'test'
          },
          isUnknownJs: false,
          reportVersion: 3
        }
      });
    });

    it('fetches unknown js results if it was requested before', function() {
      initialState.applicationReport.isUnknownJs = true;
      const store = SpecUtil.mockReduxStore(initialState);
      const errorSpy = jasmine.createSpy('errorSpy');
      store.dispatch(applicationReportActions.reloadReport()).catch(errorSpy);

      // should not dispatch 'LOAD_REPORT_REQUESTED'
      expect(store.getActions().length).toBe(0);

      mockFetchReportDataSuccess(true);
      $httpBackend.flush();

      expect(errorSpy).not.toHaveBeenCalled();
      expect(store.getActions().length).toBe(1);
      expect(store.getActions()[0]).toEqual({
        type: 'LOAD_REPORT_FULFILLED',
        payload: {
          report: {
            allEntries: [{
              filenames: ['foo.js'],
              policyThreatLevel: 0,
              policyName: 'None',
              waived: false,
              grandfathered: false,
              derivedComponentName: 'foo.js',
              derivedViolationState: 'notViolating'
            }],
            scanId: 'scanId'
          },
          metadata: {
            reportTitle: 'test'
          },
          isUnknownJs: true,
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

  describe('reevaluateReport', function() {
    it('fires REEVALUATE_REPORT_FAILED action if the reevaluation request fails', function() {
      const initialState = {
            applicationReport: {
              metadata: {
                application: { publicId: 'appId' }
              },
              selectedReport: {
                scanId: 'scanId'
              }
            }
          },
          store = SpecUtil.mockReduxStore(initialState),
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
      const initialState = {
            applicationReport: {
              metadata: {
                application: { publicId: 'appId' }
              },
              selectedReport: {
                scanId: 'scanId'
              },
              isUnknownJs: {},
              reportVersion: 3
            }
          },
          store = SpecUtil.mockReduxStore(initialState),
          errorSpy = jasmine.createSpy('errorSpy');

      store.dispatch(applicationReportActions.reevaluateReport()).catch(errorSpy);

      expect(store.getActions().length).toBe(1);
      expect(store.getActions()[0]).toEqual({
        type: 'REEVALUATE_REPORT_REQUESTED'
      });

      $httpBackend.expectPOST(SpecUtil.toRegExp(CLMLocations.getReportReevaluateUrl('appId', 'scanId'))).respond(200);
      mockFetchReportDataSuccess(true);
      $httpBackend.flush();

      expect(errorSpy).not.toHaveBeenCalled();
      expect(store.getActions().length).toBe(4);

      expect(store.getActions()[1]).toEqual({
        type: 'REEVALUATE_REPORT_FULFILLED'
      });

      expect(store.getActions()[2]).toEqual({
        type: 'LOAD_REPORT_REQUESTED'
      });

      expect(store.getActions()[3]).toEqual({
        type: 'LOAD_REPORT_FULFILLED',
        payload: {
          report: {
            allEntries: [{
              filenames: ['foo.js'],
              policyThreatLevel: 0,
              policyName: 'None',
              waived: false,
              grandfathered: false,
              derivedComponentName: 'foo.js',
              derivedViolationState: 'notViolating'
            }],
            scanId: 'scanId'
          },
          metadata: {
            reportTitle: 'test'
          },
          isUnknownJs: {},
          reportVersion: 3
        }
      });

      // existing isUnknownJs property in the state should be passed into the loadReport code
      expect(store.getActions()[3].payload.isUnknownJs).toBe(initialState.applicationReport.isUnknownJs);
    });

    it('does not fire REEVALUATE_REPORT_FAILED if the load afterwards fails', function() {
      const initialState = {
            applicationReport: {
              metadata: {
                application: { publicId: 'appId' }
              },
              selectedReport: {
                scanId: 'scanId'
              }
            }
          },
          store = SpecUtil.mockReduxStore(initialState),
          errorSpy = jasmine.createSpy('errorSpy');

      store.dispatch(applicationReportActions.reevaluateReport()).catch(errorSpy);

      expect(store.getActions().length).toBe(1);
      expect(store.getActions()[0]).toEqual({
        type: 'REEVALUATE_REPORT_REQUESTED'
      });

      $httpBackend.expectPOST(SpecUtil.toRegExp(CLMLocations.getReportReevaluateUrl('appId', 'scanId'))).respond(200);
      mockFetchReportDataFailure();
      $httpBackend.flush();

      expect(errorSpy).toHaveBeenCalled();
      expect(store.getActions().length).toBe(4);

      expect(store.getActions()[1]).toEqual({
        type: 'REEVALUATE_REPORT_FULFILLED'
      });

      expect(store.getActions()[2]).toEqual({
        type: 'LOAD_REPORT_REQUESTED'
      });

      expect(store.getActions()[3]).toEqual({
        type: 'LOAD_REPORT_FAILED',
        payload: 'test error'
      });
    });
  });

  describe('reevaluateReportCancelled', function() {
    it('returns a REEVALUATE_REPORT_CANCELLED action with no payload', function() {
      const action = applicationReportActions.reevaluateReportCancelled();

      expect(action.type).toBe('REEVALUATE_REPORT_CANCELLED');
      expect(action.payload).not.toBeDefined();
    });
  });

  describe('resetReportViewSettings', function() {
    it('returns a RESET_REPORT_VIEW_SETTINGS action with no payload', function() {
      const action = applicationReportActions.resetReportViewSettings();

      expect(action.type).toBe('RESET_REPORT_VIEW_SETTINGS');
      expect(action.payload).not.toBeDefined();
    });
  });

  function mockFetchReportDataSuccess(isUnknownJs) {
    $httpBackend.expectGET(SpecUtil.toRegExp(CLMLocations.getReportMetadataUrl('appId', 'scanId'))).respond(
        {reportTitle: 'test'});
    $httpBackend.expectGET(SpecUtil.toRegExp(CLMLocations.getReportPolicyThreatsUrl('appId', 'scanId'))).respond(
        {version: 3, aaData: []});
    $httpBackend.expectGET(SpecUtil.toRegExp(CLMLocations.getReportBomUrl('appId', 'scanId'))).respond(null);
    $httpBackend.expectGET(SpecUtil.toRegExp(CLMLocations.getReportDataUrl('appId', 'scanId'))).respond(null);
    $httpBackend.expectGET(SpecUtil.toRegExp(CLMLocations.getReportPartialMatchedUrl('appId', 'scanId')))
        .respond(null);

    if (isUnknownJs) {
      $httpBackend.expectGET(SpecUtil.toRegExp(CLMLocations.getReportUnknownJsUrl('appId', 'scanId'))).respond({
        aaData: [{
          filenames: ['foo.js']
        }]
      });
    }
  }

  function mockFetchReportDataFailure() {
    $httpBackend.expectGET(SpecUtil.toRegExp(CLMLocations.getReportMetadataUrl('appId', 'scanId'))).respond(500,
        'test error');
    $httpBackend.expectGET(SpecUtil.toRegExp(CLMLocations.getReportPolicyThreatsUrl('appId', 'scanId'))).respond(200);
    $httpBackend.expectGET(SpecUtil.toRegExp(CLMLocations.getReportBomUrl('appId', 'scanId'))).respond(200);
    $httpBackend.expectGET(SpecUtil.toRegExp(CLMLocations.getReportDataUrl('appId', 'scanId'))).respond(200);
    $httpBackend.expectGET(SpecUtil.toRegExp(CLMLocations.getReportPartialMatchedUrl('appId', 'scanId')))
        .respond(200);
  }
});
