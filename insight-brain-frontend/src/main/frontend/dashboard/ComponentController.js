/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
/* global d3 */
import commonServicesModule from '../util/CommonServices';
import CLMLocationModule from '../util/CLMLocation';
import storesModule from '../util/Stores';

var componentModule = angular.module('ComponentModule', [
  'ui.router',
  commonServicesModule.name,
  CLMLocationModule.name,
  storesModule.name,
]);

componentModule.controller('componentController', [
  '$scope',
  '$state',
  '$q',
  '$http',
  'StageTypeStore',
  'CLMLocations',
  function ($scope, $state, $q, $http, StageTypeStore, CLMLocations) {
    $scope.hash = $state.params.hash;

    $scope.doLoad = function () {
      var hash = $state.params.hash;
      var promises = [
        $http.get(CLMLocations.getComponentDetailsUrl(hash)),
        $http.get(CLMLocations.getComponentNameUrl(hash)),
        StageTypeStore.getDashboardStages(),
      ];

      $q.all(promises).then(
        function (results) {
          $scope.applicationComponents = results[0].data;
          $scope.component = { displayName: results[1].data };
          $scope.stageTypes = results[2];

          var totalRisk = 0;
          for (var i = 0; i < $scope.applicationComponents.length; i++) {
            var applicationComponent = $scope.applicationComponents[i];

            var risk = 0;
            for (var j = 0; j < applicationComponent.policyViolations.length; j++) {
              risk += applicationComponent.policyViolations[j].threatLevel;
            }
            applicationComponent.risk = risk;
            applicationComponent.uiIdentifier = i;
            totalRisk += applicationComponent.risk;
          }
          $scope.totalRisk = totalRisk;
        },
        function (error) {
          $scope.error = error;
        }
      );
    };

    $scope.formatRiskPercent = function (riskValue, totalRisk) {
      if (totalRisk === 0 || riskValue === 0) {
        return '0%';
      } else {
        const riskPercent = (riskValue / totalRisk) * 100;

        return riskPercent < 1 ? '< 1%' : `${Math.round(riskPercent)}%`;
      }
    };

    $scope.doLoad();
  },
]);

componentModule.directive('riskPie', [
  function () {
    return {
      scope: {
        risk: '@',
        width: '@',
        height: '@',
        clazz: '@',
      },
      link: function (scope, element) {
        var radius = Math.min(scope.width, scope.height) / 2 - 1;
        var arc = d3.arc().outerRadius(radius).innerRadius(0);

        var leftMostArcPoint = 1.5 * Math.PI;
        var halfArcLength = scope.risk * Math.PI;
        var data = [
          {
            data: scope.risk,
            startAngle: leftMostArcPoint - halfArcLength,
            endAngle: leftMostArcPoint + halfArcLength,
            value: scope.risk,
          },
        ];

        var svg = d3
          .select(element[0])
          .append('svg')
          .attr('width', scope.width)
          .attr('height', scope.height)
          .append('g')
          .attr('transform', 'translate(' + scope.width / 2 + ',' + scope.height / 2 + ')')
          .attr('class', scope.clazz);

        svg.append('circle').attr('cx', 0).attr('cy', 0).attr('r', radius);

        var g = svg.selectAll('.arc').data(data).enter().append('g');

        g.append('path').attr('d', arc);
      },
    };
  },
]);

export default componentModule;
