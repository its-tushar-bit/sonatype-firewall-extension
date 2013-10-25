/**
 * @license Copyright (c) 2012-2013 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

(function() {
  'use strict';

  var reportTrendingModule = angular.module('ReportTrending', []);

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
        var data = scope.data || [random(), random(), random(), random()];
        sparkline(element[0], data, config);
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

  reportTrendingModule.controller('TrendingReportController', [
    '$scope', function($scope) {
      $scope.alerts = [];
      // TODO - KR CLM-1076 -simulated data used until backend service is provided
      $scope.data = {"meta": {
        "periodStart": 'October 2, 2013',
        "periodEnd": 'October 22, 2013'
      }, "components": {
        "inApplications": 40,
        "exact": 20,
        "partial": 15,
        "unknown": 5
      }, "applications": {
        "total": 15,
        "risks": [
          {
            "name": "Application Five",
            "risk": 4,
            "critical": 20,
            "severe": 19,
            "moderate": 18,
            "none": 17
          },
          {
            "name": "Application Four",
            "risk": 3,
            "critical": 16,
            "severe": 15,
            "moderate": 14,
            "none": 13
          },
          {
            "name": "Application Three",
            "risk": 2,
            "critical": 12,
            "severe": 11,
            "moderate": 10,
            "none": 9
          },
          {
            "name": "Application Two",
            "risk": 1,
            "critical": 8,
            "severe": 7,
            "moderate": 6,
            "none": 5
          },
          {
            "name": "Application One",
            "risk": 0,
            "critical": 4,
            "severe": 3,
            "moderate": 2,
            "none": 1
          }
        ]
      }, "policies": [
        {
          "name": "Architecture-Banned",
          "threat": 10,

          "lastViolationCount": 4,
          "firstViolationCount": 4,
          "sparklineData": [4, 6, 2, 4],
          "violationsDifference": 0
        },
        {
          "name": "Security-Critical",
          "threat": 10,

          "lastViolationCount": 1,
          "firstViolationCount": 1,
          "sparklineData": [1, 3, 2, 1],
          "violationsDifference": 0
        },
        {
          "name": "License-Banned",
          "threat": 10,

          "lastViolationCount": 2,
          "firstViolationCount": 12,
          "sparklineData": [12, 10, 6, 2],
          "violationsDifference": -10
        },
        {
          "name": "License-Copyleft",
          "threat": 9,

          "lastViolationCount": 5,
          "firstViolationCount": 10,
          "sparklineData": [10, 11, 5, 5],
          "violationsDifference": -5
        },
        {
          "name": "Security-High",
          "threat": 9,

          "lastViolationCount": 2,
          "firstViolationCount": 2,
          "sparklineData": [2, 4, 1, 2],
          "violationsDifference": 0
        },
        {
          "name": "Security-Medium",
          "threat": 7,

          "lastViolationCount": 3,
          "firstViolationCount": 12,
          "sparklineData": [12, 12, 2, 3],
          "violationsDifference": -9
        },
        {
          "name": "License-Non-Standard",
          "threat": 6,

          "lastViolationCount": 8,
          "firstViolationCount": 1,
          "sparklineData": [1, 4, 4, 8],
          "violationsDifference": 7
        },
        {
          "name": "License-Unknown",
          "threat": 5,

          "lastViolationCount": 8,
          "firstViolationCount": 6,
          "sparklineData": [6, 7, 4, 8],
          "violationsDifference": 2
        },
        {
          "name": "Security-Low",
          "threat": 3,

          "lastViolationCount": 1,
          "firstViolationCount": 3,
          "sparklineData": [3, 5, 2, 1],
          "violationsDifference": -2
        },
        {
          "name": "Architecture-Deprecated",
          "threat": 1,

          "lastViolationCount": 3,
          "firstViolationCount": 2,
          "sparklineData": [2, 5, 4, 3],
          "violationsDifference": 1
        },
        {
          "name": "Architecture-Quality",
          "threat": 1,

          "lastViolationCount": 9,
          "firstViolationCount": 9,
          "sparklineData": [9, 9, 9, 9],
          "violationsDifference": 0
        },
        {
          "name": "Component-Indeterminate",
          "threat": 1,

          "lastViolationCount": 10,
          "firstViolationCount": 2,
          "sparklineData": [2, 5, 2, 10],
          "violationsDifference": 8
        },
        {
          "name": "Component-Unknown",
          "threat": 0,

          "lastViolationCount": 7,
          "firstViolationCount": 10,
          "sparklineData": [10, 8, 6, 7],
          "violationsDifference": -3
        }
      ],
        "partialMatches": [
          {
            "groupId": "org.springframework",
            "artifactId": "spring-web",
            "version": "3.0.5",
            "count": 5
          },
          {
            "groupId": "org.springframework",
            "artifactId": "spring-web",
            "version": "3.0.5",
            "count": 5
          },
          {
            "groupId": "org.springframework",
            "artifactId": "spring-web",
            "version": "3.0.5",
            "count": 4
          },
          {
            "groupId": "org.springframework",
            "artifactId": "spring-web",
            "version": "3.0.5",
            "count": 2
          },
          {
            "groupId": "org.springframework",
            "artifactId": "spring-web",
            "version": "3.0.5",
            "count": 2
          }
        ],
        "topPolicyViolations": {
          "security": [
            {
              "groupId": "org.springframework",
              "artifactId": "spring-web",
              "version": "3.0.5",
              "risk": 4,
              "critical": 20,
              "severe": 19,
              "moderate": 18,
              "none": 17
            },
            {
              "groupId": "org.springframework",
              "artifactId": "spring-web",
              "version": "3.0.5",
              "risk": 3,
              "critical": 16,
              "severe": 15,
              "moderate": 14,
              "none": 13
            },
            {
              "groupId": "org.springframework",
              "artifactId": "spring-web",
              "version": "3.0.5",
              "risk": 2,
              "critical": 12,
              "severe": 11,
              "moderate": 10,
              "none": 9
            },
            {
              "groupId": "org.springframework",
              "artifactId": "spring-web",
              "version": "3.0.5",
              "risk": 1,
              "critical": 8,
              "severe": 7,
              "moderate": 6,
              "none": 5
            },
            {
              "groupId": "org.eclipse.birt.runtime.3_7_1",
              "artifactId": "org.eclipse.equinox.app",
              "version": "1.3.100",
              "risk": 0,
              "critical": 4,
              "severe": 3,
              "moderate": 2,
              "none": 1
            }
          ],
          "quality": [
            {
              "groupId": "org.springframework",
              "artifactId": "spring-web",
              "version": "3.0.5",
              "risk": 4,
              "critical": 20,
              "severe": 19,
              "moderate": 18,
              "none": 17
            },
            {
              "groupId": "org.springframework",
              "artifactId": "spring-web",
              "version": "3.0.5",
              "risk": 3,
              "critical": 16,
              "severe": 15,
              "moderate": 14,
              "none": 13
            },
            {
              "groupId": "org.springframework",
              "artifactId": "spring-web",
              "version": "3.0.5",
              "risk": 2,
              "critical": 12,
              "severe": 11,
              "moderate": 10,
              "none": 9
            },
            {
              "groupId": "org.springframework",
              "artifactId": "spring-web",
              "version": "3.0.5",
              "risk": 1,
              "critical": 8,
              "severe": 7,
              "moderate": 6,
              "none": 5
            },
            {
              "groupId": "org.powermock",
              "artifactId": "powermock-mockito-release-full",
              "version": "1.4.11",
              "risk": 0,
              "critical": 4,
              "severe": 3,
              "moderate": 2,
              "none": 1
            }
          ],
          "license": [
            {
              "groupId": "org.springframework",
              "artifactId": "spring-web",
              "version": "3.0.5",
              "risk": 4,
              "critical": 20,
              "severe": 19,
              "moderate": 18,
              "none": 17
            },
            {
              "groupId": "org.springframework",
              "artifactId": "spring-web",
              "version": "3.0.5",
              "risk": 3,
              "critical": 16,
              "severe": 15,
              "moderate": 14,
              "none": 13
            },
            {
              "groupId": "org.springframework",
              "artifactId": "spring-web",
              "version": "3.0.5",
              "risk": 2,
              "critical": 12,
              "severe": 11,
              "moderate": 10,
              "none": 9
            },
            {
              "groupId": "org.springframework",
              "artifactId": "spring-web",
              "version": "3.0.5",
              "risk": 1,
              "critical": 8,
              "severe": 7,
              "moderate": 6,
              "none": 5
            },
            {
              "groupId": "org.springframework",
              "artifactId": "spring-web",
              "version": "3.0.5",
              "risk": 0,
              "critical": 4,
              "severe": 3,
              "moderate": 2,
              "none": 1
            }
          ],
          "all": [
            {
              "groupId": "org.springframework",
              "artifactId": "spring-web",
              "version": "3.0.5",
              "risk": 4,
              "critical": 20,
              "severe": 19,
              "moderate": 18,
              "none": 17
            },
            {
              "groupId": "org.springframework",
              "artifactId": "spring-web",
              "version": "3.0.5",
              "risk": 3,
              "critical": 16,
              "severe": 15,
              "moderate": 14,
              "none": 13
            },
            {
              "groupId": "org.springframework",
              "artifactId": "spring-web",
              "version": "3.0.5",
              "risk": 2,
              "critical": 12,
              "severe": 11,
              "moderate": 10,
              "none": 9
            },
            {
              "groupId": "org.springframework",
              "artifactId": "spring-web",
              "version": "3.0.5",
              "risk": 1,
              "critical": 8,
              "severe": 7,
              "moderate": 6,
              "none": 5
            },
            {
              "groupId": "org.springframework",
              "artifactId": "spring-web",
              "version": "3.0.5",
              "risk": 0,
              "critical": 4,
              "severe": 3,
              "moderate": 2,
              "none": 1
            }
          ],
          "other": [
            {
              "groupId": "org.springframework",
              "artifactId": "spring-web",
              "version": "3.0.5",
              "risk": 4,
              "critical": 20,
              "severe": 19,
              "moderate": 18,
              "none": 17
            },
            {
              "groupId": "org.springframework",
              "artifactId": "spring-web",
              "version": "3.0.5",
              "risk": 3,
              "critical": 16,
              "severe": 15,
              "moderate": 14,
              "none": 13
            },
            {
              "groupId": "org.springframework",
              "artifactId": "spring-web",
              "version": "3.0.5",
              "risk": 2,
              "critical": 12,
              "severe": 11,
              "moderate": 10,
              "none": 9
            },
            {
              "groupId": "org.springframework",
              "artifactId": "spring-web",
              "version": "3.0.5",
              "risk": 1,
              "critical": 8,
              "severe": 7,
              "moderate": 6,
              "none": 5
            },
            {
              "groupId": "org.springframework",
              "artifactId": "spring-web",
              "version": "3.0.5",
              "risk": 0,
              "critical": 4,
              "severe": 3,
              "moderate": 2,
              "none": 1
            }
          ]
        }, "highPolicyCount": 5, "mediumPolicyCount": 3, "lowPolicyCount": 1, "nullPolicyCount": 4, "highPolicyViolationCount": 14, "mediumPolicyViolationCount": 19, "lowPolicyViolationCount": 1, "nullPolicyViolationCount": 29, "totalPolicyViolationCount": 63,
        "diffData": {
          "security": [
            {
              "threat": "critical",
              "violations": 3,
              "previousViolations": 3
            },
            {
              "threat": "severe",
              "violations": 3,
              "previousViolations": 12
            },
            {
              "threat": "moderate",
              "violations": 1,
              "previousViolations": 3
            },
            {
              "threat": "null",
              "violations": 0,
              "previousViolations": 0
            }
          ],
          "license": [
            {
              "threat": "critical",
              "violations": 7,
              "previousViolations": 22
            },
            {
              "threat": "severe",
              "violations": 16,
              "previousViolations": 7
            },
            {
              "threat": "moderate",
              "violations": 0,
              "previousViolations": 0
            },
            {
              "threat": "null",
              "violations": 0,
              "previousViolations": 0
            }
          ],
          "quality": [
            {
              "threat": "critical",
              "violations": 4,
              "previousViolations": 4
            },
            {
              "threat": "severe",
              "violations": 0,
              "previousViolations": 0
            },
            {
              "threat": "moderate",
              "violations": 0,
              "previousViolations": 0
            },
            {
              "threat": "null",
              "violations": 12,
              "previousViolations": 11
            }
          ],
          "other": [
            {
              "threat": "critical",
              "violations": 0,
              "previousViolations": 0
            },
            {
              "threat": "severe",
              "violations": 0,
              "previousViolations": 0
            },
            {
              "threat": "moderate",
              "violations": 0,
              "previousViolations": 0
            },
            {
              "threat": "null",
              "violations": 17,
              "previousViolations": 12
            }
          ]
        }};
      //data.get().then(function(results) {
      //  $scope.data = results[0];
      //  $scope.diffchart = 'views/diffChart.html';
      //  $scope.percentageChart = 'views/percChart.html';
      //  $scope.policyProgressionTable = 'views/policyProgressionTable.html';
      //  $scope.alerts = $scope.alerts.concat(results[1]);
      //}, function(results) {
      //  $scope.alerts = $scope.alerts.concat(results[1]);
      //});
      //$scope.closeAlert = function(index) {
      //  $scope.alerts.splice(index, 1);
      //};
    }
  ]);
}());