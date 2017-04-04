/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
(function() {
  'use strict';

  var dashboardUtilsModule = angular.module('dashboard.utils',
      ['ui.router', 'Stores', 'AngularCommon', 'ComponentModule', 'ComponentDisplay']);

  dashboardUtilsModule.value('filterToParams', filterToParams);
  dashboardUtilsModule.value('extractColumn', extractColumn);

  function filterToParams(filter, maxResults) {
    var params = {};
    if (maxResults) {
      params.maxResults = maxResults + 1;
    }
    if (filter) {
      params.applicationIds = filter.applicationFilters;
      params.stageIds = filter.stageTypeFilters;
      params.tagIds = filter.tagFilters;
      params.policyViolationStates = filter.policyViolationStates;
      params.maxDaysOld = filter.maxDaysOld;

      if (filter.policyThreatCategoryFilters && filter.policyThreatCategoryFilters.length > 0) {
        params.policyThreatCategories = filter.policyThreatCategoryFilters.join(',');
      }

      if (filter.minPolicyThreatLevel !== undefined && filter.maxPolicyThreatLevel !== undefined) {
        params.policyThreatLevelRange = [filter.minPolicyThreatLevel, filter.maxPolicyThreatLevel].join(',');
      }
    }
    return params;
  }

  function extractColumn(orderedColumn) {
    if (orderedColumn.indexOf('-') === 0) {
      return orderedColumn.substring(1);
    }
    else {
      return orderedColumn;
    }
  }

}());
