/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import reportModule from '../../../main/frontend/ReportApp';
import applicationMockData from '../application/ApplicationMockData';

describe('reportApp', function() {
  var scope, state, $httpBackend, CLMLocations, $controller;

  beforeEach(angular.mock.module(reportModule.name, function($provide) {
    SpecUtil.mockNgRedux($provide);

    $provide.service('pendoService', function() {
      return jasmine.createSpyObj('pendoService', ['start']);
    });
  }));

  beforeEach(inject(function($rootScope, $state, _$controller_, _$httpBackend_, _CLMLocations_) {
    $rootScope.licensed = true;
    scope = $rootScope.$new();
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
      $httpBackend.expectGET(CLMLocations.getActionStageUrl()).respond(mockStageData);
      $httpBackend.expectGET('/rest/application/services/summary').respond([]);
      const vm = $controller('ReportViolationsController', { $scope: scope, $state: state });

      expect(vm.stages).toBeUndefined();
      expect(vm.applications).toBeUndefined();
      expect(vm.noReports).toBeUndefined();
      expect(vm.showReports).toBeUndefined();

      $httpBackend.flush();

      expect(vm.stages).toBeDefined();
      expect(vm.stages.length).toEqual(mockStageData.length);
      expect(vm.stages[0].id).toEqual(mockStageData[0].id);
      expect(vm.stages[vm.stages.length - 1].name).toEqual(mockStageData[mockStageData.length - 1].name);

      expect(vm.applications).toBeDefined();
      expect(vm.applications.length).toBe(0);
      expect(vm.noReports).toBe(true);
      expect(vm.showReports).toBe(false);
    });

    it('loads reports, sorts and assigns index', function() {
      var mockStageData = MockData.getActionStageData();
      var mockApplicationSummaryData = applicationMockData.getApplicationSummaryData();
      $httpBackend.expectGET(CLMLocations.getActionStageUrl()).respond(mockStageData);
      $httpBackend.expectGET('/rest/application/services/summary').respond(
          mockApplicationSummaryData);
      const vm = $controller('ReportViolationsController', { $scope: scope, $state: state });

      expect(vm.stages).toBeUndefined();
      expect(vm.applications).toBeUndefined();
      expect(vm.noReports).toBeUndefined();
      expect(vm.showReports).toBeUndefined();

      $httpBackend.flush();

      expect(vm.stages).toBeDefined();
      expect(vm.stages.length).toEqual(mockStageData.length);
      expect(vm.stages[0].id).toEqual(mockStageData[0].id);
      expect(vm.stages[vm.stages.length - 1].name).toEqual(mockStageData[mockStageData.length - 1].name);

      expect(vm.applications).toBeDefined();
      expect(vm.applications.length).toBe(mockApplicationSummaryData.length);
      // should ne sorted by name
      expect(vm.applications[0].id).toBe(mockApplicationSummaryData[2].id);
      // should index
      expect(vm.applications[0].index).toBe(0);

      expect(vm.noReports).toBe(false);
      expect(vm.showReports).toBe(true);
    });
  });

  describe('$watch', function () {
    let vm;

    beforeEach(function() {
      $httpBackend.expectGET(CLMLocations.getActionStageUrl()).respond(MockData.getActionStageData());
      $httpBackend.expectGET('/rest/application/services/summary').respond(
          applicationMockData.getApplicationSummaryData());
      vm = $controller('ReportViolationsController', { $scope: scope, $state: state });
      scope.vm = vm;
      $httpBackend.flush();
    });

    describe('when filter changes', function () {
      it('filters by Application Name, sorts and assigns index', function() {
        vm.appFilter = 'appl';
        scope.$digest();
        expect(vm.applications.length).toBe(2);
        expect(vm.applications[0].name).toBe('application2');
        expect(vm.applications[0].index).toBe(0);
        expect(vm.applications[1].name).toBe('application3');
        expect(vm.applications[1].index).toBe(1);
        vm.appFilter = 'foobar';
        scope.$digest();
        expect(vm.applications.length).toBe(0);
      });

      it('filters by Organization Name, sorts and assigns index', function() {
        vm.appFilter = 'big'; // case insensitive
        scope.$digest();
        expect(vm.applications.length).toBe(2);
        expect(vm.applications[0].name).toBe('app1');
        expect(vm.applications[0].index).toBe(0);
        expect(vm.applications[1].name).toBe('application2');
        expect(vm.applications[1].index).toBe(1);
        vm.appFilter = 'foobar';
        scope.$digest();
        expect(vm.applications.length).toBe(0);
      });

      it('does not filter if app filter is Null', function() {
        vm.appFilter = null;
        scope.$digest();
        expect(vm.applications.length).toBe(3);
      });

      it('does not filter if app filter is Empty', function() {
        vm.appFilter = '';
        scope.$digest();
        expect(vm.applications.length).toBe(3);
      });
    });

    describe('when sort field changes', function() {
      it('filters, sorts and assigns index', function() {
        var mockApplicationSummaryData = applicationMockData.getApplicationSummaryData();
        vm.appFilter = 'big';
        scope.$digest();
        expect(vm.applications.length).toBe(2);
        expect(vm.applications[0].id).toBe(mockApplicationSummaryData[2].id);
        expect(vm.applications[0].index).toBe(0);
        expect(vm.applications[1].index).toBe(1);

        vm.sortFields = ['-name'];
        scope.$digest();

        expect(vm.applications.length).toBe(2);
        expect(vm.applications[0].id).toBe(mockApplicationSummaryData[1].id);
        expect(vm.applications[0].index).toBe(0);
        expect(vm.applications[1].index).toBe(1);
      });
    });
  });
});
