/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

import ClassyBrew from './classybrew.factory';
import windowEventsFactory from './windowEventsFactory';
import getDashboardResultsDirective from './directives/dashboard.results.directives';
import dashboardCommonResults from './components/dashboardCommonResults';
import dashboardTabs from './directives/dashboardTabs.component';
import sparkline from './directives/sparkline.directive';
import valueBars from './directives/valueBars.directive';
import emptyToEnd from './filters/emptyToEnd.filter';
import stageFilter from './filters/stageFilter.filter';
import stageTypeSort from './filters/stageTypeSort.filter';
import removeDashes from './filters/removeDashes.filter';
import wrapWith from './filters/wrap.with.filter';
import angularCommonModule from '../../util/AngularCommon';
import storesModule from '../../util/Stores';

export default angular.module('dashboard.utils',
    ['ui.router', storesModule.name, angularCommonModule.name, 'ComponentModule', 'ComponentDisplay'])
    .value('filterToParams', filterToParams)
    .value('extractColumn', extractColumn)
    .factory('ClassyBrew', ClassyBrew)
    .factory('windowEventsFactory', windowEventsFactory)
    .directive('violationsResults', getDashboardResultsDirective('getNewestRisks'))
    .directive('applicationsResults', getDashboardResultsDirective('getApplicationRisks'))
    .directive('componentsResults', getDashboardResultsDirective('getComponentRisks'))
    .directive('sparkline', sparkline)
    .directive('valueBars', valueBars)
    .filter('emptyToEnd', emptyToEnd)
    .filter('stageFilter', stageFilter)
    .filter('stageTypeSort', stageTypeSort)
    .filter('removeDashes', removeDashes)
    .filter('wrapWith', wrapWith)
    .component('dashboardCommonResults', dashboardCommonResults)
    .component('dashboardTabs', dashboardTabs)
;

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
