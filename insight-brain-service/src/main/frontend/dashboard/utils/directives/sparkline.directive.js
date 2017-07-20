/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
/* global d3 */

export default
function sparklineDirective() {
  return {
    scope: {
      data: '='
    },
    link: function postLink(scope, element) {
      function sparkline() {
        var data = scope.data || [],
            config = {};

        d3.select(element[0]).select('svg').remove();

        var guideHeight = 16, guidePadding = 3, transitionDuration = 50, digitLength = 10, graphPadding = 2;

        function getGuidePositions(snapX, snapY, yValue) {
          // Calculate rectangle width. Each digit takes ~7 pixels with 7 pixels for single digits and 6 pixel pad
          var digits = yValue === 0 ? 0 : Math.log(yValue) / Math.log(10);
          var width = Math.floor(digits) * digitLength + digitLength + 2 * guidePadding;
          var x = Math.max(Math.min(snapX - width / 2, config.width - width), 0);
          var y;
          if (snapY > config.height / 2) {
            y = Math.max(snapY - guideHeight - guidePadding, 0);
          }
          else {
            y = Math.min(snapY + guidePadding, config.height - guideHeight);
          }
          return {
            width: width,
            rect: {
              x: x,
              y: y
            },
            text: {
              x: x + guidePadding,
              y: y + guideHeight - guidePadding + 1
            }
          };
        }

        var svg = d3.select(element[0]).append('svg')
            .attr('class', 'chart');

        config.height = parseInt(svg.style('height'), 10) || 25;
        config.width = parseInt(svg.style('width'), 10) || 100;

        var group = svg.append('g');

        var yScale = d3.scaleLinear().range([config.height - graphPadding, graphPadding]),
            pastX = d3.scaleLinear().range(
                [graphPadding, config.width - config.width / data.length - graphPadding]),
            recentX = d3.scaleLinear().range(
                [config.width - config.width / data.length - graphPadding, config.width - graphPadding]);

        pastX.domain([0, data.length - 2]);
        recentX.domain([0, 1]);
        yScale.domain([0, d3.max(data)]);

        var area = d3.area().x(function(d, index) {
          return pastX(index);
        }).y0(config.height).y1(function(d) {
          return yScale(d);
        });

        var line = d3.line().x(function(d, index) {
          return pastX(index);
        }).y(function(d) {
          return yScale(d);
        });

        group.append('path')
            .datum(data.slice(0, data.length - 1))
            .attr('d', area)
            .attr('class', 'fill base');

        group.append('path')
            .datum(data.slice(0, data.length - 1))
            .attr('d', line)
            .attr('class', 'line base');

        area = d3.area().x(function(d, index) {
          return recentX(index);
        }).y0(config.height).y1(function(d) {
          return yScale(d);
        });

        line = d3.line().x(function(d, index) {
          return recentX(index);
        }).y(function(d) {
          return yScale(d);
        });

        var trailingClass = (data[data.length - 1] - data[data.length - 2] > 0) ? 'up' : 'down';

        group.append('path')
            .datum(data.slice(data.length - 2))
            .attr('d', area)
            .attr('class', 'fill ' + trailingClass);

        group.append('path')
            .datum(data.slice(data.length - 2))
            .attr('d', line)
            .attr('class', 'line ' + trailingClass);

        var guideLine = group.append('line')
            .attr('opacity', 0)
            .attr('x1', 0)
            .attr('y1', 0)
            .attr('x2', 0)
            .attr('y2', config.height)
            .attr('class', 'guideline');

        var circlePoint = group.append('circle')
            .attr('opacity', 0)
            .attr('r', 3)
            .attr('class', 'guide-circle');

        var guideRectangle = group.append('rect')
            .attr('opacity', 0)
            .attr('rx', 4)
            .attr('ry', 4)
            .attr('height', guideHeight)
            .attr('class', 'guide-rect');

        var guideText = group.append('text')
            .attr('opacity', 0)
            .attr('class', 'guide-text');

        var hoverElements = [guideLine, circlePoint, guideRectangle, guideText];

        var guideSpace = group.append('rect')
            .attr('w', 0)
            .attr('h', 0)
            .attr('width', config.width)
            .attr('height', config.height)
            .attr('fill', 'transparent');

        guideSpace.on('mouseover', function() {
          angular.forEach(hoverElements, function(element) {
            element.transition(transitionDuration).attr('opacity', 1);
          });
        }).on('mousemove', function() {
          var position = d3.mouse(this),
              x = position[0],
              dataX = Math.round(pastX.invert(x)),
              snapX = pastX(dataX),
              yValue = data[dataX],
              snapY = yScale(yValue);

          circlePoint.attr('cx', snapX).attr('cy', snapY);

          var guidePositions = getGuidePositions(snapX, snapY, yValue);
          guideRectangle
              .attr('width', guidePositions.width)
              .attr('x', guidePositions.rect.x)
              .attr('y', guidePositions.rect.y);

          guideText
              .attr('x', guidePositions.text.x)
              .attr('y', guidePositions.text.y)
              .text(yValue);

          d3.select(element[0]).select('.guideline').attr('transform', function() {
            return 'translate(' + snapX + ',0)';
          });
        });

        d3.select(element[0]).on('mouseout', function() {
          var position = d3.mouse(this),
              x = position[0],
              y = position[1];
          if (x < 0 || x > config.width || y < 0 || y > config.height) {
            angular.forEach(hoverElements, function(element) {
              element.transition(transitionDuration).attr('opacity', 0);
            });
          }
        });
      }

      sparkline();
    }
  };
}
