/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { complement, concat, drop, dropWhile, equals, map, objOf, takeWhile } from 'ramda';

const EMPTY_PREFIX = '~empty~';

const getData = ({ data }) => data;

const MONTHS = ['Jan', 'Feb', 'Mar', 'Apr', 'May', 'Jun', 'Jul', 'Aug', 'Sep', 'Oct', 'Nov', 'Dec'];

export default function successMetricsDataService($q, $http, CLMLocations, ApplicationStore) {
  return {
    getChartData: getChartData,
    getComponentCountsData: getComponentCountsData,
    getSuccessMetricsReportsForCurrentUser: getSuccessMetricsReportsForCurrentUser,
    createSuccessMetricsReportForCurrentUser: createSuccessMetricsReportForCurrentUser,
    deleteSuccessMetricsReport: deleteSuccessMetricsReport,
    getApplicationByInternalId: getApplicationByInternalId,
    EMPTY_PREFIX: EMPTY_PREFIX,
  };

  function getChartData(successMetricsReport) {
    const url = CLMLocations.getSuccessMetricsChartDataUrl(successMetricsReport.id);

    return $http.get(url).then(function ({ data }) {
      return {
        mttrData: getMttrData(data.mttrs),
        averagesData: data.averages,
        applicationCountsData: data.applicationCounts,
        violationsByCategoryData: data.violationsByCategoryWeeks,
        lastUpdated: data.lastUpdated,
        monthCount: data.monthCount,
        violationCounts: data.violationCounts,
      };
    });
  }

  function getMttrData(data) {
    const monthsOfMttr = data.length;

    if (monthsOfMttr === 0) {
      return data;
    } else {
      if (monthsOfMttr < 12) {
        const firstMonthName = data[0].timePeriodName,
          // function which matches months which are not the first month from the data
          isNotFirstMonthOfData = complement(equals(firstMonthName)),
          // previous months that come before firstMonthName in the year
          previousMonthsInYear = takeWhile(isNotFirstMonthOfData, MONTHS),
          // additional months from the previous year for a total of a full year
          additionalMonthsInPreviousYear = dropWhile(isNotFirstMonthOfData, MONTHS),
          yearOfMonthsBeforeData = concat(additionalMonthsInPreviousYear, previousMonthsInYear),
          // months that we need to add to the data to get a year overall
          missingMonths = drop(monthsOfMttr, yearOfMonthsBeforeData),
          missingRecords = map(objOf('timePeriodName'), missingMonths);

        return concat(missingRecords, data);
      }
      return data;
    }
  }

  function getComponentCountsData(successMetricsReport) {
    return $http
      .get(CLMLocations.getSuccessMetricsComponentCountsUrl(successMetricsReport.id))
      .then(function ({ data }) {
        var componentCountMostApplications = data.componentsInTheMostApplications.length;
        if (componentCountMostApplications > 0 && componentCountMostApplications < 5) {
          data.componentsInTheMostApplications = data.componentsInTheMostApplications.concat(
            padMissingComponents(componentCountMostApplications)
          );
        }
        var componentCountViolations = data.componentsWithTheMostViolations.length;
        if (componentCountViolations > 0 && componentCountViolations < 5) {
          data.componentsWithTheMostViolations = data.componentsWithTheMostViolations.concat(
            padMissingComponents(componentCountViolations)
          );
        }

        return data;
      });

    function padMissingComponents(componentCount) {
      var paddedComponents = [],
        missingComponentCount = 5 - componentCount;

      for (var i = 0; i < missingComponentCount; i++) {
        // a unique component name is needed for the chart to display properly
        paddedComponents.push({
          componentDisplayName: EMPTY_PREFIX + i,
          count: 0,
        });
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
    return ApplicationStore.get().then(function (owners) {
      let result = undefined;
      owners.some(function (owner) {
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
