import applicationReportModule from '../../../main/frontend/applicationReport/module';

describe('applicationReportActions', function() {
  var applicationReportActions, initialState, CLMLocations, $httpBackend;

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
      var store = SpecUtil.mockReduxStore(initialState);
      var errorSpy = jasmine.createSpy('errorSpy');
      store.dispatch(applicationReportActions.loadReport('appId', 'scanId')).catch(errorSpy);

      expect(store.getActions().length).toBe(1);
      expect(store.getActions()[0]).toEqual({
        type: 'LOAD_REPORT_REQUESTED'
      });

      $httpBackend.expectGET(CLMLocations.getReportMetadataUrl('appId', 'scanId')).respond(500, 'test error');
      $httpBackend.flush();

      expect(errorSpy).toHaveBeenCalled();
      expect(store.getActions().length).toBe(2);
      expect(store.getActions()[1]).toEqual({
        type: 'LOAD_REPORT_FAILED',
        payload: 'test error'
      });
    });

    it('fires LOAD_REPORT_FULFILLED action if report request succeeds', function() {
      var store = SpecUtil.mockReduxStore(initialState);
      var errorSpy = jasmine.createSpy('errorSpy');
      store.dispatch(applicationReportActions.loadReport('appId', 'scanId')).catch(errorSpy);

      expect(store.getActions().length).toBe(1);
      expect(store.getActions()[0]).toEqual({
        type: 'LOAD_REPORT_REQUESTED'
      });

      $httpBackend.expectGET(CLMLocations.getReportMetadataUrl('appId', 'scanId')).respond('data');
      $httpBackend.flush();

      expect(errorSpy).not.toHaveBeenCalled();
      expect(store.getActions().length).toBe(2);
      expect(store.getActions()[1]).toEqual({
        type: 'LOAD_REPORT_FULFILLED',
        payload: 'data'
      });
    });
  });
});
