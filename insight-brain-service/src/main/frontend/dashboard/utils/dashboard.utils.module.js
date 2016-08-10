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
      params.applicationIds = filter.applicationIds;
      params.policyThreatCategories = (filter.policyThreatTypes &&
      filter.policyThreatTypes.length > 0) ?
          filter.policyThreatTypes.join(',') : undefined;
      params.stageIds = filter.stageTypeIds;
      params.tagIds = filter.applicationTagIds;

      var threatLvls = filter.policyThreatLevel;
      if (threatLvls) {
        params.policyThreatLevelRange = threatLvls.join();
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
