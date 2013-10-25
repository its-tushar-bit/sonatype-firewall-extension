/*
 Copyright (c) 2011-2013 Sonatype, Inc. All rights reserved.
 Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 "Sonatype" is a trademark of Sonatype, Inc.
 */
describe('TrendingController tests', function() {
  beforeEach(module('ReportTrending', 'CLMLocation'));

  describe('TrendingReportController', function() {
    var scope;

    beforeEach(inject(function($rootScope) {
      scope = $rootScope.$new();
    }));

    afterEach(inject(function($httpBackend) {
      $httpBackend.verifyNoOutstandingExpectation();
      $httpBackend.verifyNoOutstandingRequest();
      scope.$destroy();
    }));

    it('should load data into scope', inject(function($controller, $httpBackend, CLMLocations) {
      $httpBackend.expectGET(SpecUtil.toRegExp(CLMLocations.getTrendingReportUrl())).respond(TrendingReportMockData.get());
      $controller('TrendingReportController', { $scope: scope });
      $httpBackend.flush();

      expect(scope.data).not.toBeUndefined();
      expect(scope.data.meta.generatedBy).toBe('Author');
    }));

    it('should keep requesting data until it is available', inject(function($controller, $httpBackend, $timeout, CLMLocations) {
      $httpBackend.expectGET(SpecUtil.toRegExp(CLMLocations.getTrendingReportUrl())).respond(null);
      $controller('TrendingReportController', { $scope: scope });

      $httpBackend.flush();

      $httpBackend.expectGET(SpecUtil.toRegExp(CLMLocations.getTrendingReportUrl())).respond(TrendingReportMockData.get());

      $timeout.flush();
      $httpBackend.flush();

      expect(scope.data).not.toBeUndefined();
      expect(scope.data.meta.generatedBy).toBe('Author');
    }));

    it('handles errors to the trending report service', inject(function($controller, $httpBackend, CLMLocations) {
      $httpBackend.expectGET(SpecUtil.toRegExp(CLMLocations.getTrendingReportUrl())).respond(500, 'Fake Error');
      $controller('TrendingReportController', { $scope: scope });

      $httpBackend.flush();

      expect(scope.error).not.toBeUndefined();
      expect(scope.error[0]).toBe('Fake Error');
    }));

    it('provides a date format function', inject(function($controller, $httpBackend, CLMLocations) {
      $httpBackend.expectGET(SpecUtil.toRegExp(CLMLocations.getTrendingReportUrl())).respond(TrendingReportMockData.get());
      var trendingReportController = $controller('TrendingReportController', { $scope: scope });
      $httpBackend.flush();

      expect(scope.format).not.toBeUndefined();
      expect(scope.format(1382661636262)).toBe('Oct 24, 2013');

    }));

    it('provides allows specification of a custom pattern for date formatting', inject(function($controller, $httpBackend, CLMLocations) {
      $httpBackend.expectGET(SpecUtil.toRegExp(CLMLocations.getTrendingReportUrl())).respond(TrendingReportMockData.get());
      var trendingReportController = $controller('TrendingReportController', { $scope: scope });
      $httpBackend.flush();

      expect(scope.format).not.toBeUndefined();
      expect(scope.format(1382661636262, '%b %e - %I:%M %p, %Y' )).toMatch(/Oct 2.*, 2013/);

    }));
  });

  describe('Directive: componentViolations', function () {
    var element,
        scope;

    beforeEach(inject(function ($rootScope) {
      scope = $rootScope.$new();
    }));

    it('should display one row per component', inject(function ($compile) {
      scope.components = TrendingReportMockData.get().topPolicyViolations.security;
      element = angular.element('<div component-violations title="Security Policy Violators" components="components"></div>');
      element = $compile(element)(scope);
      scope.$digest();
      expect(element.find('.row').length).toBe(5);
      expect(element.find('.wordwrap').text()).toContain('org.eclipse.birt.runtime.3_7_1 : org.eclipse.equinox.app : 1.3.100');
    }));
  });

  describe('Directive: sparkline', function () {
    var element,
        scope;

    beforeEach(inject(function ($rootScope) {
      scope = $rootScope.$new();
    }));

    it('sparkline should have reasonable defaults', inject(function ($compile) {
      element = angular.element('<div sparkline></div>');
      element = $compile(element)(scope);

      var svg = element.find('svg');
      expect(svg).toBeDefined();
      expect(+svg.attr('width')).toBe(100);
      expect(+svg.attr('height')).toBe(25);
    }));

    it('sparkline should respect size configuration', inject(function ($compile) {
      element = angular.element('<div sparkline width="100" height="200"></div>');
      element = $compile(element)(scope);

      var svg = element.find('svg');
      expect(svg).toBeDefined();
      expect(+svg.attr('width')).toBe(100);
      expect(+svg.attr('height')).toBe(200);
    }));

    it('sparkline should render the correct number of data points', inject(function ($compile) {
      scope.sparklineData = [0,1,2,3,4];
      element = angular.element('<div sparkline data="sparklineData"></div>');
      element = $compile(element)(scope);

      //we expect x,y for each point, plus the 'move to' zero path command http://www.w3.org/TR/SVG/paths.html#PathDataMovetoCommands
      var path = element.find('path');
      expect(path).toBeDefined();
      expect(path.attr('d').split(',').length).toBe(11);
    }));
  });

  describe('Directive: chart', function() {
    var scope = {};
    var mockPercData = ChartMockData.getPercentageData();

    beforeEach(inject(function($controller, $rootScope) {
      return scope = $rootScope.$new();
    }));
    describe('perc chart', function() {
      it('should build a chart to the correct size', inject(function($compile) {
        var element, svg;
        scope.data = [1];
        element = angular.element("<div horizontal-percentage-chart data='data' height='150' width='100'></div>");
        element = $compile(element)(scope);
        svg = element.find('svg');
        expect(svg).toBeDefined();
        expect(+svg.attr('width')).toBe(100);
        expect(+svg.attr('height')).toBe(150);
      }));
      it('should render correct number of bars', inject(function($compile) {
        var element, rects, svg;
        scope.data = [0, 1, 2, 3];
        element = angular.element("<div horizontal-percentage-chart data='data'></div>");
        element = $compile(element)(scope);
        svg = element.find('svg');
        expect(svg).toBeDefined();
        rects = svg.find('rect');
        expect(rects).toBeDefined();
        expect(rects.length).toBe(scope.data.length);
      }));
      it('should render correct number of texts', inject(function($compile) {
        var element, rects, svg;
        scope.data = [0, 1, 2, 3];
        element = angular.element("<div horizontal-percentage-chart data='data'></div>");
        element = $compile(element)(scope);
        svg = element.find('svg');
        expect(svg).toBeDefined();
        rects = svg.find('text');
        expect(rects).toBeDefined();
        expect(rects.length).toBe(4 + (4 + 8 + 12 + 16));
      }));
      it('should render the largest bar to the maximum width', inject(function($compile) {
        var element, rects, svg;
        scope.data = [5];
        element = angular.element("<div horizontal-percentage-chart data='data' width='100'></div>");
        element = $compile(element)(scope);
        svg = element.find('svg');
        expect(svg).toBeDefined();
        rects = svg.find('rect');
        expect(rects).toBeDefined();
        expect(rects.length).toBe(1);
        expect(+rects.attr('width')).toBe(100);
      }));
      it('should allow bar color selection', inject(function($compile) {
        var element, rects, svg;
        scope.data = [5];
        scope.colorRenderer = function() {
          return '#ff0000';
        };
        element = angular.element("<div horizontal-percentage-chart data='data' color-renderer='colorRenderer'></div>");
        element = $compile(element)(scope);
        svg = element.find('svg');
        expect(svg).toBeDefined();
        rects = svg.find('rect');
        expect(rects).toBeDefined();
        expect(rects.attr('style')).toMatch(/^fill: #ff0000; ?$/);
      }));
      return it('should allow for data selection', inject(function($compile) {
        var element, rects, svg;
        scope.data = mockPercData;
        scope.selector = function(d) {
          return d.value;
        };
        element = angular.element("<div horizontal-percentage-chart data='data' percentage-selector='selector' width='100'></div>");
        element = $compile(element)(scope);
        svg = element.find('svg');
        expect(svg).toBeDefined();
        rects = svg.find('rect');
        expect(rects).toBeDefined();
        expect(rects.length).toBe(1);
        expect(+rects.attr('width')).toBe(100);
      }));
    });

    describe('diff chart', function() {
      var mockDiffData = ChartMockData.getDiffData();

      it('should build a chart to the correct size', inject(function($compile) {
        var element, svg;
        scope.data = [0, 1, 2, 3];
        element = angular.element("<div bar-chart data='data' height='150' width='100'></div>");
        element = $compile(element)(scope);
        svg = element.find('svg');
        expect(svg).toBeDefined();
        expect(+svg.attr('width')).toBe(100);
        expect(+svg.attr('height')).toBe(150);
      }));
      it('should render correct number of bars', inject(function($compile) {
        var element, rects, svg;
        scope.data = [0, 1, 2, 3];
        element = angular.element("<div bar-chart data='data'></div>");
        element = $compile(element)(scope);
        svg = element.find('svg');
        expect(svg).toBeDefined();
        rects = svg.find('rect');
        expect(rects).toBeDefined();
        expect(rects.length).toBe(scope.data.length);
      }));
      it('should render correct number of texts', inject(function($compile) {
        var element, rects, svg;
        scope.data = [0, 1, 2, 3];
        element = angular.element("<div bar-chart data='data'></div>");
        element = $compile(element)(scope);
        svg = element.find('svg');
        expect(svg).toBeDefined();
        rects = svg.find('text');
        expect(rects).toBeDefined();
        expect(rects.length).toBe(scope.data.length);
      }));
      it('should render the largest bar to the maximum height', inject(function($compile) {
        var element, rects, svg;
        scope.data = [5];
        element = angular.element("<div bar-chart data='data' height='100'></div>");
        element = $compile(element)(scope);
        svg = element.find('svg');
        expect(svg).toBeDefined();
        rects = svg.find('rect');
        expect(rects).toBeDefined();
        expect(rects.length).toBe(1);
        expect(+rects.attr('height')).toBe(100);
      }));
      it('should allow bar color selection', inject(function($compile) {
        var element, rects, svg;
        scope.data = [5];
        scope.colorRenderer = function() {
          return '#ff0000';
        };
        element = angular.element("<div bar-chart data='data' color-renderer='colorRenderer'></div>");
        element = $compile(element)(scope);
        svg = element.find('svg');
        expect(svg).toBeDefined();
        rects = svg.find('rect');
        expect(rects).toBeDefined();
        expect(rects.attr('style')).toMatch(/^fill: #ff0000; stroke: #ffffff; ?$/);
      }));
      it('should allow text color selection', inject(function($compile) {
        var element, svg, texts;
        scope.data = [5];
        scope.textRenderer = function() {
          return '#ff0000';
        };
        element = angular.element("<div bar-chart data='data' text-renderer='textRenderer'></div>");
        element = $compile(element)(scope);
        svg = element.find('svg');
        expect(svg).toBeDefined();
        texts = svg.find('text');
        expect(texts).toBeDefined();
        expect(texts.attr('style')).toMatch(/^fill: #ff0000;( font-weight: bold;)? ?$/);
      }));
      it('should respect a yMax value', inject(function($compile) {
        var element, rects, svg;
        scope.data = [5];
        element = angular.element("<div bar-chart y-max='10' data='data' height='100'></div>");
        element = $compile(element)(scope);
        svg = element.find('svg');
        expect(svg).toBeDefined();
        rects = svg.find('rect');
        expect(rects).toBeDefined();
        expect(rects.length).toBe(1);
        expect(+rects.attr('height')).toBe(50);
      }));
      it('should allow for data selection', inject(function($compile) {
        var element, rects, svg;
        scope.data = mockDiffData.diffData.security;
        scope.selector = function(d) {
          return d.violations;
        };
        element = angular.element("<div bar-chart data='data' selector='selector' height='100'></div>");
        element = $compile(element)(scope);
        svg = element.find('svg');
        expect(svg).toBeDefined();
        rects = svg.find('rect');
        expect(rects).toBeDefined();
        expect(rects.length).toBe(1);
        expect(+rects.attr('height')).toBe(100);
      }));
      it('should allow for diff values', inject(function($compile) {
        var element, rects, svg;
        scope.data = mockDiffData.diffData.security;
        scope.selector = function(d) {
          return d.violations;
        };
        scope.diffSelector = function(d) {
          return d.previousViolations;
        };
        element = angular.element("<div bar-chart data='data' y-max='20' selector='selector' diff-selector='diffSelector' height='100'></div>");
        element = $compile(element)(scope);
        svg = element.find('svg');
        expect(svg).toBeDefined();
        rects = svg.find('rect');
        expect(rects).toBeDefined();
        expect(rects.length).toBe(3);
        expect(+$(rects[0]).attr('y')).toBe(50);
        expect(+$(rects[0]).attr('height')).toBe(50);
        expect(+$(rects[1]).attr('y')).toBe(0);
        expect(+$(rects[1]).attr('height')).toBe(50);
        expect(+$(rects[2]).attr('y')).toBe(0);
        expect(+$(rects[2]).attr('height')).toBe(50);
      }));
    });
  });

  describe('Service: colors', function() {
    var colors, testCase, testCases, _i, _len, _results;
    colors = {};
    beforeEach(inject(function(_colors_) {
      colors = _colors_;
    }));
    it('should get bar color from threat', function() {
      expect(colors.barFromThreatName('critical')).toBe('#DB2852');
      expect(colors.barFromThreatName('severe')).toBe('#F7941E');
      expect(colors.barFromThreatName('moderate')).toBe('#F5C649');
      expect(colors.barFromThreatName('null')).toBe('#0047b2');
    });
    it('should get text color from threat', function() {
      expect(colors.textFromThreatName('critical')).toBe('white');
      expect(colors.textFromThreatName('severe')).toBe('white');
      expect(colors.textFromThreatName('moderate')).toBe('black');
      expect(colors.textFromThreatName('null')).toBe('white');
    });
    testCases = [
      {
        input: 0,
        expected: 'threat-chiclet-none'
      }, {
        input: 1,
        expected: 'threat-chiclet-none'
      }, {
        input: 2,
        expected: 'threat-chiclet-moderate'
      }, {
        input: 3,
        expected: 'threat-chiclet-moderate'
      }, {
        input: 4,
        expected: 'threat-chiclet-severe'
      }, {
        input: 5,
        expected: 'threat-chiclet-severe'
      }, {
        input: 6,
        expected: 'threat-chiclet-severe'
      }, {
        input: 7,
        expected: 'threat-chiclet-severe'
      }, {
        input: 8,
        expected: 'threat-chiclet-critical'
      }, {
        input: 9,
        expected: 'threat-chiclet-critical'
      }, {
        input: 10,
        expected: 'threat-chiclet-critical'
      }, {
        input: 'a',
        expected: 'black'
      }
    ];
    _results = [];
    for (_i = 0, _len = testCases.length; _i < _len; _i++) {
      testCase = testCases[_i];
      _results.push((function(input, expected) {
        return it("should be colored: " + expected + " for the threat level: " + input, function() {
          return expect(colors.threatLevelClass(input)).toEqual(expected);
        });
      })(testCase.input, testCase.expected));
    }
  });
});