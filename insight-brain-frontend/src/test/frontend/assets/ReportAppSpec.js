describe('reportApp', function() {
  var scope, state, $httpBackend, CLMLocations, $controller;

  beforeEach(module('ReportModule', 'MainModule', function($provide) {
    SpecUtil.mockNgRedux($provide);
  }));

  beforeEach(inject(function($rootScope, $state, _$controller_, _$httpBackend_, _CLMLocations_) {
    $rootScope.licensed = true;
    scope = $rootScope.$new();
    scope.getSortField = jasmine.createSpy('getSortField').and.returnValue(['name']);
    state = $state;
    $httpBackend = _$httpBackend_;
    CLMLocations = _CLMLocations_;
    $controller = _$controller_;
  }));

  afterEach(function() {
    scope.$destroy();
    $httpBackend.verifyNoOutstandingExpectation();
    $httpBackend.verifyNoOutstandingRequest();
  });

  describe('doLoad', function() {
    it('handles no reports', function() {
      var mockStageData = MockData.getActionStageData();
      $httpBackend.expectGET(SpecUtil.toRegExp(CLMLocations.getActionStageUrl())).respond(mockStageData);
      $httpBackend.expectGET(SpecUtil.toRegExp('/rest/application/services/summary')).respond([]);
      $controller('ReportViolationsController', { $scope: scope, $state: state });

      expect(scope.stages).toBeUndefined();
      expect(scope.applications).toBeUndefined();
      expect(scope.noReports).toBeUndefined();
      expect(scope.showReports).toBeUndefined();

      $httpBackend.flush();

      expect(scope.stages).toBeDefined();
      expect(scope.stages.length).toEqual(mockStageData.length);
      expect(scope.stages[0].id).toEqual(mockStageData[0].id);
      expect(scope.stages[scope.stages.length - 1].name).toEqual(mockStageData[mockStageData.length - 1].name);

      expect(scope.applications).toBeDefined();
      expect(scope.applications.length).toBe(0);
      expect(scope.noReports).toBe(true);
      expect(scope.showReports).toBe(false);
    });

    it('loads reports, sorts and assigns index', function() {
      var mockStageData = MockData.getActionStageData();
      var mockApplicationSummaryData = ApplicationMockData.getApplicationSummaryData();
      $httpBackend.expectGET(SpecUtil.toRegExp(CLMLocations.getActionStageUrl())).respond(mockStageData);
      $httpBackend.expectGET(SpecUtil.toRegExp('/rest/application/services/summary')).respond(
          mockApplicationSummaryData);
      $controller('ReportViolationsController', { $scope: scope, $state: state });

      expect(scope.stages).toBeUndefined();
      expect(scope.applications).toBeUndefined();
      expect(scope.noReports).toBeUndefined();
      expect(scope.showReports).toBeUndefined();

      $httpBackend.flush();

      expect(scope.stages).toBeDefined();
      expect(scope.stages.length).toEqual(mockStageData.length);
      expect(scope.stages[0].id).toEqual(mockStageData[0].id);
      expect(scope.stages[scope.stages.length - 1].name).toEqual(mockStageData[mockStageData.length - 1].name);

      expect(scope.applications).toBeDefined();
      expect(scope.applications.length).toBe(mockApplicationSummaryData.length);
      // should ne sorted by name
      expect(scope.applications[0].id).toBe(mockApplicationSummaryData[2].id);
      // should index
      expect(scope.applications[0].index).toBe(0);

      expect(scope.noReports).toBe(false);
      expect(scope.showReports).toBe(true);
    });
  });

  describe('$watch', function () {
    beforeEach(function() {
      $httpBackend.expectGET(SpecUtil.toRegExp(CLMLocations.getActionStageUrl())).respond(MockData.getActionStageData());
      $httpBackend.expectGET(SpecUtil.toRegExp('/rest/application/services/summary')).respond(
          ApplicationMockData.getApplicationSummaryData());
      $controller('ReportViolationsController', { $scope: scope, $state: state });
      $httpBackend.flush();
    });

    describe('when filter changes', function () {
      it('filters by Application Name, sorts and assigns index', function() {
        scope.appFilter = 'appl';
        scope.$digest();
        expect(scope.applications.length).toBe(2);
        expect(scope.applications[0].name).toBe('application2');
        expect(scope.applications[0].index).toBe(0);
        expect(scope.applications[1].name).toBe('application3');
        expect(scope.applications[1].index).toBe(1);
        scope.appFilter = 'foobar';
        scope.$digest();
        expect(scope.applications.length).toBe(0);
      });

      it('filters by Organization Name, sorts and assigns index', function() {
        scope.appFilter = 'big'; // case insensitive
        scope.$digest();
        expect(scope.applications.length).toBe(2);
        expect(scope.applications[0].name).toBe('app1');
        expect(scope.applications[0].index).toBe(0);
        expect(scope.applications[1].name).toBe('application2');
        expect(scope.applications[1].index).toBe(1);
        scope.appFilter = 'foobar';
        scope.$digest();
        expect(scope.applications.length).toBe(0);
      });

      it('does not filter if app filter is Null', function() {
        scope.appFilter = null;
        scope.$digest();
        expect(scope.applications.length).toBe(3);
      });

      it('does not filter if app filter is Empty', function() {
        scope.appFilter = '';
        scope.$digest();
        expect(scope.applications.length).toBe(3);
      });
    });

    describe('when sort field changes', function() {
      it('filters, sorts and assigns index', function() {
        var mockApplicationSummaryData = ApplicationMockData.getApplicationSummaryData();
        scope.appFilter = 'big';
        scope.$digest();
        expect(scope.applications.length).toBe(2);
        expect(scope.applications[0].id).toBe(mockApplicationSummaryData[2].id);
        expect(scope.applications[0].index).toBe(0);
        expect(scope.applications[1].index).toBe(1);

        scope.getSortField.and.returnValue(['-name']);
        scope.$digest();

        expect(scope.applications.length).toBe(2);
        expect(scope.applications[0].id).toBe(mockApplicationSummaryData[1].id);
        expect(scope.applications[0].index).toBe(0);
        expect(scope.applications[1].index).toBe(1);
      });
    });
  });
});
