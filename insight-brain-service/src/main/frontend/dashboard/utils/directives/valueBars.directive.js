/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
/* global d3 */

export default
function valueBarsDirective(windowEventsFactory) {
  return {
    restrict: 'A',
    scope: {
      data: '=data'
    },
    replace: true,
    link: function(scope, element) {
      function barChart() {
        var data = scope.data;

        d3.select(element[0]).select('svg').remove();

        // allow the svg element to take up all the parent's space
        var chart = d3.select(element[0])
            .append('svg')
            .attr('class', 'chart');

        var width = $(chart[0]).width() || 100,
            height = $(chart[0]).height() || 25;

        var y = d3.scale.linear()
            .domain(d3.extent(data))
            .range([0, height]);
        var x = d3.scale.ordinal()
            .domain(d3.range(data.length))
            .rangeRoundBands([0, width], 0.2);

        // baseline will render at bottom for positive data, somewhere in between for positive/negative data
        // Need to fudge a bit if we're drawing at the bottom of the container to account for the stroke-width
        var baseline = Math.min(height - 0.50, height - y(0));

        chart.selectAll('g')
            .data(data).enter()
            .append('rect')
            .attr('x', function(d, i) {
              return x(i);
            })
            .attr('y', function(d) {
              // render starting above or at the center line, depending on +/-
              return (d > 0) ? height - y(d) : baseline;
            })
            .attr('height', function(d) {
              return Math.abs(y(d) - y(0));
            })
            .attr('width', x.rangeBand())
            .attr('class', function(d) {
              return (d > 0) ? 'bar up' : 'bar down';
            })
            // tooltip to highlight actual figures involved
            .append('title').text(function(d) {
              return d;
            });

        chart.append('line')
            .attr('x1', x(0))
            .attr('x2', x(data.length - 1) + x.rangeBand())
            .attr('y1', baseline)
            .attr('y2', baseline)
            .attr('stroke', '#777777')
            .attr('stroke-width', '0.5');
      }

      windowEventsFactory.addResizeHandler(scope, element, barChart);
      barChart();
    }
  };
}

valueBarsDirective.$inject = ['windowEventsFactory'];
