/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

import ClassyBrew from './classybrew.factory';
import windowEventsFactory from './windowEventsFactory';
import emptyToEnd from './filters/emptyToEnd.filter';
import stageFilter from './filters/stageFilter.filter';
import stageTypeSort from './filters/stageTypeSort.filter';
import removeDashes from './filters/removeDashes.filter';
import wrapWith from './filters/wrap.with.filter';
import angularCommonModule from '../../util/AngularCommon';
import storesModule from '../../util/Stores';
import { setToArray } from '../../util/jsUtil';
import ComponentModule from '../ComponentController';
import ComponentDisplayModule from '../../ComponentDisplay/module';

export default angular
  .module('dashboard.utils', [
    'ui.router',
    storesModule.name,
    angularCommonModule.name,
    ComponentModule.name,
    ComponentDisplayModule.name,
  ])
  .value('createDashboardDataRequestPayload', createDashboardDataRequestPayload)
  .value('extractColumn', extractColumn)
  .factory('ClassyBrew', ClassyBrew)
  .factory('windowEventsFactory', windowEventsFactory)
  .filter('emptyToEnd', emptyToEnd)
  .filter('stageFilter', stageFilter)
  .filter('stageTypeSort', stageTypeSort)
  .filter('removeDashes', removeDashes)
  .filter('wrapWith', wrapWith);

export function createDashboardDataRequestPayload(
  filter,
  maxResults,
  sortFields
) {
  var params = {};
  if (sortFields && sortFields.length) {
    params.orderBy = sortFields.join();
  }
  if (maxResults) {
    params.maxResults = maxResults;
  }
  if (filter) {
    params.organizationIds = setToArray(filter.organizations);
    params.applicationIds = setToArray(filter.applications);
    params.stageIds = setToArray(filter.stages);
    params.tagIds = setToArray(filter.categories);
    params.policyViolationStates = setToArray(filter.policyViolationStates);
    params.maxDaysOld = filter.maxDaysOld;
    params.policyThreatLevelRange =
      filter.policyThreatLevels && filter.policyThreatLevels.join(',');

    if (filter.policyTypes && filter.policyTypes.size > 0) {
      params.policyThreatCategories = setToArray(filter.policyTypes).join(',');
    }
  }
  return params;
}

export function extractColumn(orderedColumn) {
  if (orderedColumn.indexOf('-') === 0) {
    return orderedColumn.substring(1);
  } else {
    return orderedColumn;
  }
}
