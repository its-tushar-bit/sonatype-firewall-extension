/**
 * @license Copyright (c) 2012-2013 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

(function() {
  'use strict';

  var reportTrendingModule = angular.module('ReportTrending', []);

  reportTrendingModule.controller('TrendingReportController', ['$scope', 'trendingReportService', 'Messages', function($scope, trendingReportService, Messages) {
    $scope.alerts = [];

    $scope.doLoad = function() {
      $scope.error = null;
      trendingReportService.get().then(function(trendingReport) {
        $scope.data = trendingReport;
        $scope.diffchart = '../report-assets/trending/diffChart.html?' + clmBuildTimestamp
        $scope.percentageChart = '../report-assets/trending/percChart.html?' + clmBuildTimestamp
        $scope.policyProgressionTable = '../report-assets/trending/policyProgressionTable.html?' + clmBuildTimestamp
      }, function(error) {
        $scope.error = error;
      });
    };
    $scope.doLoad();

    var fmtG = d3.time.format('%b %e, %Y');

    $scope.format = function(date) {
      return fmtG(new Date(date));
    };


  }]);

  reportTrendingModule.directive('componentViolations', function() {
    return {
      scope: {
        title: '@',
        components: '='
      },
      replace: true,
      template:
        '<div ng-show="components">' +
        '<h2 class="marginTop section-header">{{ title }}</h2>' +
        '<div class="row" ng-repeat="component in components">' +
        '<div class="gav wordwrap">{{ component.groupId }}<span class="wrap-force-break"> : </span>{{ component.artifactId }}<span class="wrap-force-break"> : </span>{{ component.version }}</div>' +
        '<div class="counters" chiclets base-class="threat-chiclet-small" always-show="true" critical="component.critical" severe="component.severe"' +
        ' moderate="component.moderate" none="component.none"></div>' +
        '</div>' +
        '</div>'
    };
  });

  reportTrendingModule.directive('sparkline', function() {
    return {
      scope:{
        data:'='
      },
      link: function postLink(scope, element, attrs) {

        function random(seed) {
          return Math.floor(Math.random() * (seed || 9) + 1);
        }

        function sparkline(element, data, config) {
          config.width = config.width || 100;
          config.height = config.height || 25;

          var svg = d3.select(element).append('svg').attr('width', config.width).attr('height', config.height).append('g'),
            y = d3.scale.linear().range([config.height, 0]),
            x = d3.scale.linear().range([0, config.width]);

          x.domain([0, data.length-1]);
          y.domain([0, d3.max(data)]);

          var area = d3.svg.area().x(function(d, index) {
            return x(index);
          }).y0(config.height).y1(function(d) {
            return y(d);
          });

          svg.append('path')
            .datum(data)
            .attr('class', 'area')
            .attr('d', area);
        }

        var config = {
          width : attrs.width,
          height: attrs.height
        };
        sparkline(element[0], scope.data, config);
      }
    };
  });

  reportTrendingModule.directive('horizontalPercentageChart', [
    function() {
      return {
        restrict: 'A',
        scope: {
          height: '=',
          width: '=',
          data: '=',
          labelSelector: '=',
          percentageSelector: '=',
          colorRenderer: '='
        },
        link: function(scope, element) {
          var colorRenderer, data, height, percentageSelector, sumValues, svg, svgRectangles, svgTexts, totalLeft, width, x, xSelector;
          data = scope.data;
          height = scope.height || 100;
          width = scope.width || 150;
          percentageSelector = scope.percentageSelector || function(d) {
            return d;
          };
          colorRenderer = scope.colorRenderer || function(d) {
            return d;
          };
          sumValues = d3.sum(data, percentageSelector);
          x = d3.scale.linear().domain([0, d3.sum(data, percentageSelector)]).rangeRound([0, width]);
          svg = d3.select(element[0]).append('svg');
          svg.attr('class', 'chart').attr('width', width).attr('height', height);
          svgRectangles = svg.selectAll('rect').data(data).enter();
          svgRectangles.append('rect').attr('x', function(d, i) {
            var j, left;
            left = 0;
            j = 0;
            while (j < i) {
              left += percentageSelector(data[j]);
              j++;
            }
            return x(left);
          }).attr('y', 15).attr('height', function() {
            return height - 15;
          }).attr('width', function(d) {
            return x(percentageSelector(d));
          }).style('fill', colorRenderer);
          svgTexts = svg.selectAll('text').data(data).enter();
          totalLeft = 0;
          xSelector = function(d, i, element, textSelector) {
            var j, left, minLeft, minRight;
            left = 0;
            minRight = 0;
            j = 0;
            while (j < i) {
              left += percentageSelector(data[j]);
              j++;
            }
            while (j < data.length) {
              minRight += svgTexts.append('text').attr('opacity', 0).attr('text-anchor', 'left').attr('fill', 'black').style('font-weight', 'bold').text(textSelector(j)).node().getBBox().width + 10;
              j++;
            }
            minLeft = x(left) + 5;
            minRight = minRight - 5;
            left = minLeft > totalLeft ? minLeft : totalLeft + 10;
            left = minLeft + minRight < width ? left : width - minRight;
            totalLeft = percentageSelector(d) > 0 ? left + element.getBBox().width : totalLeft;
            return left;
          };
          svgTexts.append('text').attr('y', function() {
            return (height / 2) + 5;
          }).attr('dy', '.35em').attr('text-anchor', 'left').attr('fill', 'black').text(function(d) {
            if (percentageSelector(d) > 0) {
              return d3.round(percentageSelector(d) / sumValues * 100) + '%';
            } else {
              return '';
            }
          }).attr('x', function(d, i) {
            return xSelector(d, i, this, function(j) {
              if (percentageSelector(data[j]) > 0) {
                return d3.round(percentageSelector(data[j]) / sumValues * 100) + '%';
              } else {
                return '';
              }
            });
          });
          if (scope.labelSelector) {
            totalLeft = 0;
            return svgTexts.append('text').attr('dy', '1em').attr('text-anchor', 'left').attr('fill', 'black').style('font-weight', 'bold').text(function(d) {
              if (percentageSelector(d) > 0) {
                return scope.labelSelector(d);
              } else {
                return '';
              }
            }).attr('x', function(d, i) {
              return xSelector(d, i, this, function(j) {
                if (percentageSelector(data[j]) > 0) {
                  return scope.labelSelector(data[j]);
                } else {
                  return '';
                }
              });
            });
          }
        }
      };
    }
  ]);

  reportTrendingModule.directive('barChart', [
    function() {
      return {
        restrict: 'A',
        scope: {
          height: '=',
          width: '=',
          data: '=',
          selector: '=',
          diffSelector: '=',
          colorRenderer: '=',
          textRenderer: '=',
          backgroundColor: '=',
          yMax: '='
        },
        link: function(scope, element) {
          var backgroundColor, barChartTexts, barchart, barchartRectangles, colorRenderer, data, dataSelector, height, textRenderer, width, x, y, yMax;
          data = scope.data;
          height = scope.height || 100;
          width = scope.width || 150;
          dataSelector = scope.selector || function(d) {
            return d;
          };
          colorRenderer = scope.colorRenderer || function(d) {
            return d;
          };
          textRenderer = scope.textRenderer || function(d) {
            return d;
          };
          backgroundColor = scope.backgroundColor || 'white';
          yMax = scope.yMax || d3.max(data, dataSelector);
          x = d3.scale.linear().domain([0, data.length]).rangeRound([0, width]);
          y = d3.scale.linear().domain([0, yMax]).rangeRound([0, height]);
          barchart = d3.select(element[0]).append('svg');
          barchart.attr('class', 'chart').attr('width', width).attr('height', height).style('background-color', backgroundColor);
          barchart.append('defs').append('pattern').attr('id', 'diagonalHatch').attr('patternUnits', 'userSpaceOnUse').attr('width', 4).attr('height', 4).append('path').attr('d', 'M-1,1 l2,-2 M0,4 l4,-4 M3,5 l2,-2').attr('stroke', backgroundColor).attr('stroke-width', 2);
          barchartRectangles = barchart.selectAll('rect').data(data).enter();
          barchartRectangles.append('rect').attr('x', function(d, i) {
            return x(i);
          }).attr('y', function(d) {
            return height - y(dataSelector(d));
          }).attr('height', function(d) {
            return y(dataSelector(d));
          }).attr('width', width / data.length).style('fill', colorRenderer).style('stroke', backgroundColor);
          barChartTexts = barchart.selectAll('text').data(data).enter();
          if (scope.diffSelector) {
            barchartRectangles.append('rect').attr('x', function(d, i) {
              return x(i);
            }).attr('y', function(d) {
              var diff;
              diff = d3.max([dataSelector(d), scope.diffSelector(d)]);
              return height - y(diff);
            }).attr('height', function(d) {
              return y(d3.max([0, scope.diffSelector(d) - dataSelector(d)]));
            }).attr('width', width / data.length).attr('fill', colorRenderer).attr('stroke', backgroundColor);
            barchartRectangles.append('rect').attr('x', function(d, i) {
              return x(i);
            }).attr('y', function(d) {
              var diff;
              diff = d3.max([dataSelector(d), scope.diffSelector(d)]);
              return height - y(diff);
            }).attr('height', function(d) {
              return y(d3.max([0, scope.diffSelector(d) - dataSelector(d)]));
            }).attr('width', width / data.length).attr('fill', 'url(#diagonalHatch)').attr('stroke', backgroundColor);
            barChartTexts.append('text').attr('x', function(d, i) {
              return x(i) + width / data.length / 2;
            }).attr('y', function(d) {
              var textY;
              textY = y(dataSelector(d)) > 12 ? y(dataSelector(d)) : 12;
              textY = textY < height - 12 ? textY : height - 12;
              return height - textY - 3;
            }).attr('text-anchor', 'middle').style('fill', 'black').style('font-weight', 'bold').text(function(d) {
              if (scope.diffSelector(d) - dataSelector(d) > 0) {
                return '-' + (scope.diffSelector(d) - dataSelector(d));
              } else {
                return '';
              }
            });
          }
          return barChartTexts.append('text').attr('x', function(d, i) {
            return x(i) + width / data.length / 2;
          }).attr('y', function() {
            return height - 3;
          }).attr('text-anchor', 'middle').style('fill', function(d) {
            if (dataSelector(d) !== 0 && y(dataSelector(d)) > 12) {
              return textRenderer(d);
            } else {
              return 'black';
            }
          }).style('font-weight', 'bold').text(dataSelector);
        }
      };
    }
  ]);

  reportTrendingModule.factory('colors', [
    function() {
      return {
        barFromThreatName: function(threatName) {
          switch (threatName.toLowerCase()) {
            case 'critical':
              return '#DB2852';
            case 'severe':
              return '#F7941E';
            case 'moderate':
              return '#F5C649';
            case 'null':
              return '#0047b2';
            default:
              return 'black';
          }
        },
        textFromThreatName: function(threatName) {
          switch (threatName.toLowerCase()) {
            case 'critical':
            case 'severe':
            case 'null':
              return 'white';
            default:
              return 'black';
          }
        },
        threatLevelClass: function(threatLevel) {
          switch (threatLevel) {
            case 10:
            case 9:
            case 8:
              return 'threat-chiclet-critical';
            case 7:
            case 6:
            case 5:
            case 4:
              return 'threat-chiclet-severe';
            case 3:
            case 2:
              return 'threat-chiclet-moderate';
            case 1:
            case 0:
              return 'threat-chiclet-none';
            default:
              return 'black';
          }
        }
      };
    }
  ]);

  reportTrendingModule.service('trendingReportService', ['$http', '$q', '$timeout', 'CLMLocations', function($http, $q, $timeout, CLMLocations) {
    return {
      get: function() {
        var defer = $q.defer();
        var pollFunction = function() {
          $http.get(CLMLocations.getTrendingReportUrl(),
            { params: { timestamp: new Date().getTime() } }).success(function(trendingReport) {
            if (trendingReport) {
              defer.resolve(trendingReport);
            }
            else {
              $timeout(pollFunction, 2000);
            }
          }).error(function() {
              return defer.reject(arguments);
            });
        };
        pollFunction();
        return defer.promise;
      }
    }
  }]);
}());