/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import reportModule from '../../../main/frontend/ReportApp';
import applicationMockData from '../application/ApplicationMockData';

describe('reportApp', function () {
  var scope, state, $httpBackend, CLMLocations, $controller;

  beforeEach(
    angular.mock.module(reportModule.name, function ($provide) {
      SpecUtil.mockNgRedux($provide);

      $provide.service('pendoService', function () {
        return jasmine.createSpyObj('pendoService', ['start']);
      });
    })
  );

  beforeEach(inject(function (
    $rootScope,
    $state,
    _$controller_,
    _$httpBackend_,
    _CLMLocations_
  ) {
    $rootScope.licensed = true;
    scope = $rootScope.$new();
    state = $state;
    $httpBackend = _$httpBackend_;
    CLMLocations = _CLMLocations_;
    $controller = _$controller_;
  }));

  afterEach(function () {
    scope.$destroy();
    $httpBackend.verifyNoOutstandingExpectation();
    $httpBackend.verifyNoOutstandingRequest();
  });

  describe('doLoad tries to get page 1 with 50 results', function () {
    it('handles no reports', function () {
      var mockStageData = MockData.getActionStageData();
      $httpBackend
        .expectGET(CLMLocations.getActionStageUrl())
        .respond(mockStageData);
      $httpBackend
        .expectGET(
          '/rest/application/services/summary?order=APP_NAME_ASC&page=1&pageSize=50'
        )
        .respond([]);
      const vm = $controller('ReportViolationsController', {
        $scope: scope,
        $state: state,
      });

      expect(vm.stages).toBeUndefined();
      expect(vm.applications).toEqual([]);
      expect(vm.noReports).toBeFalsy();
      expect(vm.showReports).toBeTruthy();

      $httpBackend.flush();

      expect(vm.stages).toBeDefined();
      expect(vm.stages.length).toEqual(mockStageData.length);
      expect(vm.stages[0].id).toEqual(mockStageData[0].id);
      expect(vm.stages[vm.stages.length - 1].name).toEqual(
        mockStageData[mockStageData.length - 1].name
      );

      expect(vm.applications).toBeDefined();
      expect(vm.applications.length).toBe(0);
      expect(vm.noReports).toBe(true);
      expect(vm.showReports).toBe(false);
    });

    it('loads reports', function () {
      var mockStageData = MockData.getActionStageData();
      var mockApplicationSummaryData = applicationMockData.getApplicationSummaryData();
      $httpBackend
        .expectGET(CLMLocations.getActionStageUrl())
        .respond(mockStageData);
      $httpBackend
        .expectGET(
          '/rest/application/services/summary?order=APP_NAME_ASC&page=1&pageSize=50'
        )
        .respond(mockApplicationSummaryData);
      const vm = $controller('ReportViolationsController', {
        $scope: scope,
        $state: state,
      });

      expect(vm.stages).toBeUndefined();
      expect(vm.applications).toEqual([]);
      expect(vm.noReports).toBeFalsy();
      expect(vm.showReports).toBeTruthy();

      $httpBackend.flush();

      expect(vm.stages).toBeDefined();
      expect(vm.stages.length).toEqual(mockStageData.length);
      expect(vm.stages[0].id).toEqual(mockStageData[0].id);
      expect(vm.stages[vm.stages.length - 1].name).toEqual(
        mockStageData[mockStageData.length - 1].name
      );

      expect(vm.applications).toBeDefined();
      expect(vm.applications.length).toBe(mockApplicationSummaryData.length);

      expect(vm.noReports).toBe(false);
      expect(vm.showReports).toBe(true);
    });
  });

  describe('search', function () {
    let vm;

    beforeEach(function () {
      $httpBackend
        .expectGET(CLMLocations.getActionStageUrl())
        .respond(MockData.getActionStageData());
      $httpBackend
        .expectGET(
          '/rest/application/services/summary?order=APP_NAME_ASC&page=1&pageSize=50'
        )
        .respond(applicationMockData.getApplicationSummaryData());
      vm = $controller('ReportViolationsController', {
        $scope: scope,
        $state: state,
      });
      scope.vm = vm;
      $httpBackend.flush();
    });

    describe('when filter changes and search is executed', function () {
      it('filters by Application or Organization name', function () {
        vm.appFilter = 'appl';
        vm.sortAndFilter();
        $httpBackend
          .expectGET(
            '/rest/application/services/summary?nameFilter=appl&order=APP_NAME_ASC&page=1&pageSize=50'
          )
          .respond([]);
        $httpBackend.flush();
        expect(vm.applications).toEqual([]);
      });

      it('does not filter if app filter is Null', function () {
        vm.appFilter = null;
        vm.sortAndFilter();
        $httpBackend
          .expectGET(
            '/rest/application/services/summary?order=APP_NAME_ASC&page=1&pageSize=50'
          )
          .respond([]);
        $httpBackend.flush();
        expect(vm.applications).toEqual([]);
      });

      it('does not filter if app filter is Empty', function () {
        vm.appFilter = '';
        vm.sortAndFilter();
        $httpBackend
          .expectGET(
            '/rest/application/services/summary?order=APP_NAME_ASC&page=1&pageSize=50'
          )
          .respond([]);
        $httpBackend.flush();
        expect(vm.applications).toEqual([]);
      });
    });

    describe('when sort field changes', function () {
      it('sorts', function () {
        vm.sortChange(['name']);
        $httpBackend
          .expectGET(
            '/rest/application/services/summary?order=APP_NAME_ASC&page=1&pageSize=50'
          )
          .respond(['app1']);
        $httpBackend.flush();
        expect(vm.applications).toEqual(['app1']);

        vm.sortChange(['-name']);
        $httpBackend
          .expectGET(
            '/rest/application/services/summary?order=APP_NAME_DESC&page=1&pageSize=50'
          )
          .respond(['app2']);
        $httpBackend.flush();
        expect(vm.applications).toEqual(['app2']);

        vm.sortChange(['organizationName']);
        $httpBackend
          .expectGET(
            '/rest/application/services/summary?order=ORG_NAME_ASC&page=1&pageSize=50'
          )
          .respond(['app3']);
        $httpBackend.flush();
        expect(vm.applications).toEqual(['app3']);

        vm.sortChange(['-organizationName']);
        $httpBackend
          .expectGET(
            '/rest/application/services/summary?order=ORG_NAME_DESC&page=1&pageSize=50'
          )
          .respond(['app4']);
        $httpBackend.flush();
        expect(vm.applications).toEqual(['app4']);
      });
    });
  });

  describe('pages', function () {
    let vm;

    beforeEach(function () {
      $httpBackend
        .expectGET(CLMLocations.getActionStageUrl())
        .respond(MockData.getActionStageData());
      vm = $controller('ReportViolationsController', {
        $scope: scope,
        $state: state,
      });
      scope.vm = vm;
    });

    it('has an initial size of 50', function () {
      $httpBackend
        .expectGET(
          '/rest/application/services/summary?order=APP_NAME_ASC&page=1&pageSize=50'
        )
        .respond([]);
      expect($httpBackend.flush).not.toThrow();
    });

    describe('when load more results is pressed', function () {
      it('increases the pages', function () {
        $httpBackend
          .expectGET(
            '/rest/application/services/summary?order=APP_NAME_ASC&page=1&pageSize=50'
          )
          .respond([]);
        expect($httpBackend.flush).not.toThrow();

        vm.loadMoreResults();

        $httpBackend
          .expectGET(
            '/rest/application/services/summary?order=APP_NAME_ASC&page=2&pageSize=50'
          )
          .respond([]);
        expect($httpBackend.flush).not.toThrow();
      });
    });

    describe('when sorting or filtering', function () {
      it('resets the pages to 1', function () {
        $httpBackend
          .expectGET(
            '/rest/application/services/summary?order=APP_NAME_ASC&page=1&pageSize=50'
          )
          .respond([]);
        expect($httpBackend.flush).not.toThrow();

        vm.loadMoreResults();

        $httpBackend
          .expectGET(
            '/rest/application/services/summary?order=APP_NAME_ASC&page=2&pageSize=50'
          )
          .respond([]);
        expect($httpBackend.flush).not.toThrow();

        vm.sortAndFilter();

        $httpBackend
          .expectGET(
            '/rest/application/services/summary?order=APP_NAME_ASC&page=1&pageSize=50'
          )
          .respond([]);
        expect($httpBackend.flush).not.toThrow();
      });
    });

    describe('has more results', function () {
      it('is set to true if a full page of results was last returned', function () {
        $httpBackend
          .expectGET(
            '/rest/application/services/summary?order=APP_NAME_ASC&page=1&pageSize=50'
          )
          .respond(applicationMockData.getApplicationSummaryData(50));
        $httpBackend.flush();

        expect(vm.hasMoreResults).toBeTruthy();
      });

      it('is set to false if a partial page of results was last returned', function () {
        $httpBackend
          .expectGET(
            '/rest/application/services/summary?order=APP_NAME_ASC&page=1&pageSize=50'
          )
          .respond(applicationMockData.getApplicationSummaryData(49));
        $httpBackend.flush();

        expect(vm.hasMoreResults).toBeFalsy();
      });
    });
  });
});
