/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

const EMPTY_PREFIX = '~empty~';

const getData = ({ data }) => data;

export default
function successMetricsDataService($q, $http, CLMLocations, ApplicationStore) {
  return {
    getChartData: getChartData,
    getComponentCountsData: getComponentCountsData,
    getSuccessMetricsReportsForCurrentUser: getSuccessMetricsReportsForCurrentUser,
    createSuccessMetricsReportForCurrentUser: createSuccessMetricsReportForCurrentUser,
    deleteSuccessMetricsReport: deleteSuccessMetricsReport,
    getApplicationByInternalId: getApplicationByInternalId,
    EMPTY_PREFIX: EMPTY_PREFIX
  };

  function getChartData(successMetricsReport) {
    const url =
        CLMLocations.getSuccessMetricsChartDataUrl(successMetricsReport.id);

    return $http.get(url).then(function({data}) {
      return {
        mttrData: getMttrData(data.mttrs),
        averagesData: data.averages,
        applicationCountsData: data.applicationCounts,
        lastUpdated: data.lastUpdated,
        monthCount: data.monthCount
      };
    });
  }

  function getMttrData(data) {
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
  }

  function getComponentCountsData(postData) {
    return $http.post(CLMLocations.getSuccessMetricsComponentCountsUrl(), postData).then(function({data}) {
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

  function getSuccessMetricsReportsForCurrentUser() {
    return $http.get(CLMLocations.getSuccessMetricsReportsUrl()).then(getData);
  }

  function createSuccessMetricsReportForCurrentUser(successMetricConfiguration) {
    return $http.post(CLMLocations.getSuccessMetricsReportsUrl(), successMetricConfiguration).then(getData);
  }

  function deleteSuccessMetricsReport(successMetricsReportId) {
    return $http.delete(CLMLocations.getSuccessMetricsReportUrl(successMetricsReportId));
  }

  // ApplicationStore is configured to lookup by public id not internal id
  function getApplicationByInternalId(id) {
    return ApplicationStore.get().then(function(owners) {
      let result = undefined;
      owners.some(function(owner) {
        if (owner.id === id) {
          result = owner;
          return true;
        }
      });

      if (!result) {
        return $q.reject(`Could not find Application with internal id ${id}`);
      }
      return result;
    });
  }
}

successMetricsDataService.$inject = ['$q', '$http', 'CLMLocations', 'ApplicationStore'];
