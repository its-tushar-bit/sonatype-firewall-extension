/*
 Copyright (c) 2011-2013 Sonatype, Inc. All rights reserved.
 Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 "Sonatype" is a trademark of Sonatype, Inc.
 */
(function() {
  'use strict';

  describe('Controller: PercChartCtrl', function() {
    var PercchartCtrl, scope;
    beforeEach(module('ReportTrending'));
    PercchartCtrl = {};
    scope = {};
    beforeEach(inject(function($controller, $rootScope) {
      scope = $rootScope.$new();
      scope.data = TrendingReportMockData.get();
      PercchartCtrl = $controller('PercChartCtrl', {
        $scope: scope
      });
      scope.$digest();
    }));
    it('loads data for chart', function() {
      expect(scope.applicationComponents).toBeDefined();
      return expect(scope.applicationComponents.length).toBe(4);
    });
    it('should provide a data selector', function() {
      var value;
      expect(scope.componentPercentageSelector).not.toBe(null);
      value = scope.componentPercentageSelector(scope.applicationComponents[0]);
      return expect(value).toBe(20);
    });
    return it('should provide a color renderer', function() {
      var color;
      expect(scope.componentColorRenderer).not.toBe(null);
      color = scope.componentColorRenderer(scope.applicationComponents[0]);
      return expect(color).toBe('#838383');
    });
  });

}).call(this);