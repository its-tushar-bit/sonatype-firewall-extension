/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

const EMPTY_PREFIX = '~empty~';

export default
function successMetricsDataService($http, CLMLocations, ProductFeatures) {
  return {
    getMttrData: getMttrData,
    getAveragesData: getAveragesData,
    getApplicationCountsData: getApplicationCountsData,
    getComponentCountsData: getComponentCountsData,
    isRootOrgAvailable: isRootOrgAvailable,
    EMPTY_PREFIX: EMPTY_PREFIX
  };

  function getMttrData() {
    return $http.post(CLMLocations.getMttrUrl(), {}).then(function({ data }) {
      const monthsOfMttr = data.length;

      if (monthsOfMttr === 0) {
        return data;
      }
      else {
        if (monthsOfMttr < 12) {
          var paddedMonths = [];
          var missingMonthCount = 12 - monthsOfMttr;
          var paddedDate = new Date(data[0].timePeriodStart);

          for (var i = 0; i < missingMonthCount; i++) {
            /*
             * The second parameter sets the day to the first to avoid wrapping. For example, if the date is the 30th of
             * the given month, when March is hit it would show up twice since Feb 30th isn't a valid date. (it wraps to
             * March) This is only a problem when the mttr data is empty.
             */
            paddedDate.setMonth(paddedDate.getMonth() - 1, 1);
            paddedMonths.unshift({timePeriodStart: paddedDate.getTime()});
          }

          return paddedMonths.concat(data);
        }
        return data;
      }
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

  function getComponentCountsData() {
    return $http.post(CLMLocations.getSuccessMetricsComponentCountsUrl(), {}).then(function({data}) {
      var componentCountMostApplications = data.componentsInTheMostApplications.length;
      if (componentCountMostApplications > 0 && componentCountMostApplications < 5) {
        data.componentsInTheMostApplications = data.componentsInTheMostApplications.concat(
            padMissingComponents(componentCountMostApplications));
      }
      var componentCountViolations = data.componentsWithTheMostViolations.length;
      if (componentCountViolations > 0 && componentCountViolations < 5) {
        data.componentsWithTheMostViolations = data.componentsWithTheMostViolations.concat(
            padMissingComponents(componentCountViolations));
      }

      return data;
    });

    function padMissingComponents(componentCount) {
      var paddedComponents = [],
          missingComponentCount = 5 - componentCount;

      for (var i = 0; i < missingComponentCount; i++) {
        // a unique component name is needed for the chart to display properly
        paddedComponents.push({componentDisplayName: EMPTY_PREFIX + i, count: 0});
      }
      return paddedComponents;
    }
  }

  function isRootOrgAvailable() {
    return ProductFeatures.isAvailable('root-org');
  }
}

successMetricsDataService.$inject = ['$http', 'CLMLocations', 'ProductFeatures'];
