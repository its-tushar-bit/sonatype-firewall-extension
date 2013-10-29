/*
 Copyright (c) 2011-2013 Sonatype, Inc. All rights reserved.
 Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 "Sonatype" is a trademark of Sonatype, Inc.
 */
(function() {
  'use strict';
  var mockData;

  mockData = angular.extend(angular.copy(TrendingReportMockData.get()),  ChartMockData.getDiffData())

  describe('Controller: DiffChartCtrl', function() {
    var DiffchartCtrl, scope;
    beforeEach(module('ReportTrending','CLMLocation'));
    DiffchartCtrl = {};
    scope = {};
    beforeEach(inject(function($controller, $rootScope, $httpBackend, CLMLocations) {
      scope = $rootScope.$new();
      $httpBackend.expectGET(SpecUtil.toRegExp(CLMLocations.getTrendingReportUrl())).respond(mockData);
      DiffchartCtrl = $controller('DiffChartCtrl', {
        $scope: scope
      });
      return $httpBackend.flush();
    }));
    afterEach(inject(function($httpBackend) {
      $httpBackend.verifyNoOutstandingExpectation();
      return $httpBackend.verifyNoOutstandingRequest();
    }));
    it('should provide a data selector for previous violations', function() {
      var value;
      expect(scope.barGraphSelector).not.toBe(null);
      value = scope.barGraphSelector(mockData.diffData.security[0]);
      return expect(value).toBe(10);
    });
    it('should provide a data selector for current violations', function() {
      var value;
      expect(scope.barGraphSelector).not.toBe(null);
      value = scope.barGraphSelector(mockData.diffData.license[0]);
      return expect(value).toBe(22);
    });
    it('should provide a diff selector', function() {
      var value;
      expect(scope.barGraphDiffSelector).not.toBe(null);
      value = scope.barGraphDiffSelector(mockData.diffData.security[0]);
      return expect(value).toBe(20);
    });
    it('should provide a color renderer', function() {
      var color;
      expect(scope.barGraphRenderer).not.toBe(null);
      color = scope.barGraphRenderer(mockData.diffData.security[0]);
      return expect(color).toBe('#DB2852');
    });
    it('should provide a text color renderer', function() {
      var color;
      expect(scope.barGraphTextRenderer).not.toBe(null);
      color = scope.barGraphTextRenderer(mockData.diffData.security[0]);
      return expect(color).toBe('white');
    });
    return it('should provide a background color renderer', function() {
      var color;
      expect(scope.getBackgroundColor).not.toBe(null);
      color = scope.getBackgroundColor(0);
      expect(color).toBe('#EEE');
      color = scope.getBackgroundColor(1);
      return expect(color).toBe('#F4F4F4');
    });
  });

}).call(this);
