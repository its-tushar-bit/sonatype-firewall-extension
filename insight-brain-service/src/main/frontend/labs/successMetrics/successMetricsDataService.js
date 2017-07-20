/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
export default
function successMetricsDataService($http, CLMLocations, ProductFeatures) {
  return {
    getMttrData: getMttrData,
    getAveragesData: getAveragesData,
    getApplicationCountsData: getApplicationCountsData,
    isRootOrgAvailable: isRootOrgAvailable
  };

  function getMttrData() {
    return $http.post(CLMLocations.getMttrUrl(), {}).then(function(response) {
      var monthsOfMttr = response.data.length;
      if (monthsOfMttr < 12) {
        var paddedMonths = [];
        var missingMonthCount = 12 - monthsOfMttr;
        var paddedDate = new Date();

        if (monthsOfMttr > 0) {
          paddedDate = new Date(response.data[0].timePeriodStart);
        }

        for (var i = 0; i < missingMonthCount; i++) {
          /*
           * The second parameter sets the day to the first to avoid wrapping. For example, if the date is the 30th of
           * the given month, when March is hit it would show up twice since Feb 30th isn't a valid date. (it wraps to
           * March) This is only a problem when the mttr data is empty.
           */
          paddedDate.setMonth(paddedDate.getMonth() - 1, 1);
          paddedMonths.unshift({timePeriodStart: paddedDate.getTime()});
        }

        return paddedMonths.concat(response.data);
      }
      return response.data;
    });
  }

  function getAveragesData() {
    var threatCategoryAccessors = ['security', 'license', 'quality', 'other'],
        threatLevelAccessors = [
          'averageDiscoveredLow', 'averageDiscoveredModerate', 'averageDiscoveredSevere',
          'averageDiscoveredCritical'
        ];

    // turn a series of (key, value) pairs into an object
    function pairsToObj(pairs) {
      var retval = {};

      pairs.forEach(function(pair) {
        var key = pair[0],
            value = pair[1];

        retval[key] = value;
      });

      return retval;
    }

    return $http.post(CLMLocations.getViolationAveragesUrl(), {}).then(function(response) {
      var monthAverages = response.data.averageDiscoveredPolicyViolations;

      // the rest endpoint returns separate data for each month.  We need to combine into overall averages
      var threatCategoryPairs = threatCategoryAccessors.map(function(threatCategoryAccessor) {

        var threatLevelPairs = threatLevelAccessors.map(function(threatLevelAccessor) {

          // average this value across all months
          var average = monthAverages.reduce(function(acc, monthData) {
            return acc + monthData[threatCategoryAccessor][threatLevelAccessor];
          }, 0) / monthAverages.length || 0;

          return [threatLevelAccessor, Math.round(average)];
        });

        return [threatCategoryAccessor, pairsToObj(threatLevelPairs)];
      });

      var result = pairsToObj(threatCategoryPairs);
      result.monthCount = monthAverages.length;
      result.activeApplicationCount = response.data.activeApplicationCount;
      result.averageEvaluations = monthAverages.reduce(function(acc, monthData) {
        return acc + monthData.evaluationCount;
      }, 0) / monthAverages.length || 0;
      result.averagePolicyViolations = threatCategoryAccessors.reduce(function(categoryAcc, threatCategoryAccessor) {
        return categoryAcc + threatLevelAccessors.reduce(function(levelAcc, threatLevelAccessor) {
          return levelAcc + result[threatCategoryAccessor][threatLevelAccessor];
        }, 0);
      }, 0) || 0;
      result.averageCriticalPolicyViolations = threatCategoryAccessors.reduce(
          function(categoryAcc, threatCategoryAccessor) {
            return categoryAcc + result[threatCategoryAccessor].averageDiscoveredCritical;
          }, 0) || 0;
      return result;
    });
  }

  function getApplicationCountsData() {
    return $http.post(CLMLocations.getSuccessMetricsApplicationCountsUrl(), {}).then(function(response) {
      return response.data;
    });
  }

  function isRootOrgAvailable() {
    return ProductFeatures.isAvailable('root-org');
  }
}

successMetricsDataService.$inject = ['$http', 'CLMLocations', 'ProductFeatures'];
