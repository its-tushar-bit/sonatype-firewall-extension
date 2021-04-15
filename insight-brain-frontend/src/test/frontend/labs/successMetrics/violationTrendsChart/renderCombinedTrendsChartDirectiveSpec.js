/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
/* global describe, beforeEach, it, expect, inject */
import successMetricsModule from '../../../../../main/frontend/labs/successMetrics/module';
import generateDataset from './mockTrendsChartDataset';

describe('renderCombinedTrendsChartDirective', function () {
  beforeEach(angular.mock.module(successMetricsModule.name));

  var $compile, $rootScope;

  beforeEach(inject(function (_$compile_, _$rootScope_) {
    $compile = _$compile_;
    $rootScope = _$rootScope_;
    $rootScope.data = generateDataset();
    $rootScope.statistics = {
      deltaMax: 0,
      deltaMin: -5,
      discoveredMax: 6,
      waivedMax: 7,
      fixedMax: 8,
    };
  }));

  it('renders 4 bar charts guide-line and tooltips', function () {
    var element = $compile(`
      <div id="violation-trends-chart">
        <render-combined-trends-chart data="data" statistics="statistics"></render-combined-trends-chart>
      </div>
    `)($rootScope);
    expect(
      element[0].querySelectorAll('div.component.plot.xy-plot.bar-plot').length
    ).toBe(4);
    expect(
      element[0].querySelectorAll('div.component.guide-line-layer.vertical')
        .length
    ).toBe(1);
    expect(element[0].querySelectorAll('#deltaBarTooltip').length).toBe(1);
    expect(element[0].querySelectorAll('#newBarTooltip').length).toBe(1);
    expect(element[0].querySelectorAll('#waivedBarTooltip').length).toBe(1);
    expect(element[0].querySelectorAll('#fixedBarTooltip').length).toBe(1);
    expect(element[0].querySelectorAll('#guidelineTooltip').length).toBe(1);
  });
});
