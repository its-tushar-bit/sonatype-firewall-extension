/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

export default
function policyTrendController($scope, CLMLocations, $http, filters, createDashboardDataRequestPayload) {
  function delta(counts) {
    return counts.reduce(function(a, b) {
      return a + b;
    });
  }

  function calculateRunningTotals(counts, startValue) {
    var runningTotals = [startValue];
    for (var i = 0; i < counts.length; i++) {
      runningTotals[i + 1] = counts[i] + runningTotals[i];
    }
    return runningTotals;
  }

  function generateModel(policySummaryData) {
    var weeklyDeltaNew = policySummaryData.weeklyDeltaNew,
        weeklyDeltaFixed = policySummaryData.weeklyDeltaFixed,
        weeklyDeltaUnresolved = policySummaryData.weeklyDeltaUnresolved,
        weeklyDeltaWaived = policySummaryData.weeklyDeltaWaived,
        totalNew = policySummaryData.totalNew,
        totalFixed = policySummaryData.totalFixed,
        currentUnresolved = policySummaryData.currentUnresolved,
        totalWaived = policySummaryData.totalWaived,
        newDelta = delta(weeklyDeltaNew),
        fixedDelta = delta(weeklyDeltaFixed),
        unresolvedDelta = delta(weeklyDeltaUnresolved),
        waivedDelta = delta(weeklyDeltaWaived);
    return [
      {
        name: 'Pending',
        counts: currentUnresolved,
        avg: policySummaryData.ageAverageUnresolved,
        p90: policySummaryData.agePercentile90Unresolved,
        delta: unresolvedDelta,
        barChartData: weeklyDeltaUnresolved,
        sparklineData: calculateRunningTotals(weeklyDeltaUnresolved, currentUnresolved - unresolvedDelta),
        naturalOrder: false
      },
      {
        name: 'Waived',
        counts: totalWaived,
        avg: policySummaryData.ageAverageWaived,
        p90: policySummaryData.agePercentile90Waived,
        delta: waivedDelta,
        barChartData: weeklyDeltaWaived,
        sparklineData: calculateRunningTotals(weeklyDeltaWaived, totalWaived - waivedDelta),
        naturalOrder: false
      },
      {
        name: 'Fixed',
        counts: totalFixed,
        avg: policySummaryData.ageAverageFixed,
        p90: policySummaryData.agePercentile90Fixed,
        delta: fixedDelta,
        barChartData: weeklyDeltaFixed,
        sparklineData: calculateRunningTotals(weeklyDeltaFixed, totalFixed - fixedDelta),
        naturalOrder: true
      },
      {
        name: 'Discovered',
        counts: totalNew,
        delta: newDelta,
        barChartData: weeklyDeltaNew,
        sparklineData: calculateRunningTotals(weeklyDeltaNew, totalNew - newDelta)
      }
    ];
  }

  $scope.doLoad = function() {
    $scope.data = null;
    $scope.error = null;
    $http.post(CLMLocations.getPolicySummaryUrl(), createDashboardDataRequestPayload($scope.filters))
        .then(function(response) {
          $scope.policySummaryData = generateModel(response.data);
        }, function(error) {
          $scope.error = error;
        });
  };

  $scope.$on('pageChangeAccepted', function() {
    $scope.$dismiss();
  });

  $scope.filters = filters;
  $scope.doLoad();
}

policyTrendController.$inject = ['$scope', 'CLMLocations', '$http', 'filters', 'createDashboardDataRequestPayload'];
