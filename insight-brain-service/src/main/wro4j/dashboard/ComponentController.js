/**
 * @license Copyright (c) 2014 Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
 /* global angular, d3 */
(function() {
  'use strict';

  var componentModule = angular.module('ComponentModule', ['ui.router', 'CommonServices', 'CLMLocation']);

  componentModule.controller('componentController', ['$scope', '$state', '$q', '$http', 'CLMLocations',
    function($scope, $state, $q, $http, CLMLocations) {
    $scope.hash = $state.params.hash;

    $scope.doLoad = function() {
      var hash = $state.params.hash;
      var promises = [
        $http.get(CLMLocations.getComponentDetailsUrl(hash)),
        $http.get(CLMLocations.getComponentNameUrl(hash))
      ];

      $q.all(promises).then(function(results) {
        $scope.applicationComponents = results[0].data;
        $scope.name = results[1].data;

        var totalRisk = 0;
        for (var i = 0; i < $scope.applicationComponents.length; i++) {
          var applicationComponent = $scope.applicationComponents[i];

          var risk = 0;
          for (var j = 0; j < applicationComponent.policyViolations.length; j++) {
            risk += applicationComponent.policyViolations[j].threatLevel;
          }
          applicationComponent.risk = risk;
          totalRisk += applicationComponent.risk;
        }
        $scope.totalRisk = totalRisk;
      }, function (error) {
        $scope.error = error;
      });
    };

    $scope.doLoad();
  }]);

  componentModule.directive('riskPie', [function() {
    return {
      scope: {
        risk: '@',
        width: '@',
        height: '@',
        clazz: '@'
      },
      link: function(scope, element) {
        var radius = (Math.min(scope.width, scope.height) / 2) - 1;
        var arc = d3.svg.arc()
            .outerRadius(radius)
            .innerRadius(0);

        var leftMostArcPoint = 1.5 * Math.PI;
        var halfArcLength = (scope.risk) * Math.PI;
        var data = [
          {
            data: scope.risk,
            startAngle: leftMostArcPoint - halfArcLength,
            endAngle: leftMostArcPoint + halfArcLength,
            value: scope.risk
          }
        ];

        var svg = d3.select(element[0]).append('svg')
          .attr('width', scope.width)
          .attr('height', scope.height)
          .append('g')
          .attr('transform', 'translate(' + scope.width / 2 + ',' + scope.height / 2 + ')')
          .attr('class', scope.clazz);

        svg.append('circle')
          .attr('cx', 0)
          .attr('cy', 0)
          .attr('r', radius);

        var g = svg.selectAll('.arc')
          .data(data)
          .enter().append('g');

        g.append('path')
          .attr('d', arc);
      }
    };
  }]);
}());