import applicationReportModule from '../../../main/frontend/applicationReport/module';

describe('applicationReportActions', function() {
  let applicationReportActions, initialState, CLMLocations, $httpBackend;

  beforeEach(angular.mock.module(applicationReportModule.name));

  beforeEach(inject(function($injector) {
    applicationReportActions = $injector.get('applicationReportActions');
    CLMLocations = $injector.get('CLMLocations');
    $httpBackend = $injector.get('$httpBackend');

    initialState = {
      loading: false
    };
  }));

  afterEach(function() {
    $httpBackend.verifyNoOutstandingExpectation();
    $httpBackend.verifyNoOutstandingRequest();
  });

  describe('loadReport', function() {
    it('fires LOAD_REPORT_FAILED action if report request fails', function() {
      const store = SpecUtil.mockReduxStore(initialState);
      const errorSpy = jasmine.createSpy('errorSpy');
      store.dispatch(applicationReportActions.loadReport('appId', 'scanId')).catch(errorSpy);

      expect(store.getActions().length).toBe(1);
      expect(store.getActions()[0]).toEqual({
        type: 'LOAD_REPORT_REQUESTED'
      });

      $httpBackend.expectGET(SpecUtil.toRegExp(CLMLocations.getReportMetadataUrl('appId', 'scanId'))).respond(500,
          'test error');
      $httpBackend.expectGET(SpecUtil.toRegExp(CLMLocations.getReportPolicyThreatsUrl('appId', 'scanId'))).respond(200);
      $httpBackend.expectGET(SpecUtil.toRegExp(CLMLocations.getReportBomUrl('appId', 'scanId'))).respond(200);
      $httpBackend.expectGET(SpecUtil.toRegExp(CLMLocations.getReportDataUrl('appId', 'scanId'))).respond(200);
      $httpBackend.flush();

      expect(errorSpy).toHaveBeenCalled();
      expect(store.getActions().length).toBe(2);
      expect(store.getActions()[1]).toEqual({
        type: 'LOAD_REPORT_FAILED',
        payload: 'test error'
      });
    });

    it('fires LOAD_REPORT_FULFILLED action if report request succeeds', function() {
      const store = SpecUtil.mockReduxStore(initialState);
      const errorSpy = jasmine.createSpy('errorSpy');
      store.dispatch(applicationReportActions.loadReport('appId', 'scanId')).catch(errorSpy);

      expect(store.getActions().length).toBe(1);
      expect(store.getActions()[0]).toEqual({
        type: 'LOAD_REPORT_REQUESTED'
      });

      $httpBackend.expectGET(SpecUtil.toRegExp(CLMLocations.getReportMetadataUrl('appId', 'scanId'))).respond(
          {reportTitle: 'test'});
      $httpBackend.expectGET(SpecUtil.toRegExp(CLMLocations.getReportPolicyThreatsUrl('appId', 'scanId'))).respond(
          null);
      $httpBackend.expectGET(SpecUtil.toRegExp(CLMLocations.getReportBomUrl('appId', 'scanId'))).respond(null);
      $httpBackend.expectGET(SpecUtil.toRegExp(CLMLocations.getReportDataUrl('appId', 'scanId'))).respond(null);

      $httpBackend.flush();

      expect(errorSpy).not.toHaveBeenCalled();
      expect(store.getActions().length).toBe(2);
      expect(store.getActions()[1]).toEqual({
        type: 'LOAD_REPORT_FULFILLED',
        payload: {
          reportTitle: 'test',
          allEntries: []
        }
      });
    });

    it('fetches unknown js results when so told', function() {
      const store = SpecUtil.mockReduxStore(initialState);
      const errorSpy = jasmine.createSpy('errorSpy');
      store.dispatch(applicationReportActions.loadReport('appId', 'scanId', true)).catch(errorSpy);

      expect(store.getActions().length).toBe(1);
      expect(store.getActions()[0]).toEqual({
        type: 'LOAD_REPORT_REQUESTED'
      });

      $httpBackend.expectGET(SpecUtil.toRegExp(CLMLocations.getReportMetadataUrl('appId', 'scanId'))).respond(
          {reportTitle: 'test'});
      $httpBackend.expectGET(SpecUtil.toRegExp(CLMLocations.getReportPolicyThreatsUrl('appId', 'scanId'))).respond(
          null);
      $httpBackend.expectGET(SpecUtil.toRegExp(CLMLocations.getReportBomUrl('appId', 'scanId'))).respond(null);
      $httpBackend.expectGET(SpecUtil.toRegExp(CLMLocations.getReportDataUrl('appId', 'scanId'))).respond(null);
      $httpBackend.expectGET(SpecUtil.toRegExp(CLMLocations.getReportUnknownJsUrl('appId', 'scanId'))).respond(null);
      $httpBackend.flush();

      expect(errorSpy).not.toHaveBeenCalled();
      expect(store.getActions().length).toBe(2);
      expect(store.getActions()[1]).toEqual({
        type: 'LOAD_REPORT_FULFILLED',
        payload: {
          reportTitle: 'test',
          allEntries: []
        }
      });
    });
  });
});
