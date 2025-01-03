/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

import { setToArray } from '../../util/jsUtil';

export function createDashboardDataRequestPayload(filter, pageSize, sortFields, page) {
  var params = {};
  if (sortFields && sortFields.length) {
    params.orderBy = sortFields.join();
  }
  if (pageSize !== null && pageSize !== undefined) {
    params.pageSize = pageSize;
  }
  if (page !== null && page !== undefined) {
    params.page = page;
  }
  if (filter) {
    params.organizationIds = setToArray(filter.organizations);
    params.applicationIds = setToArray(filter.applications);
    params.repositoryIds = setToArray(filter.repositories);
    params.stageIds = setToArray(filter.stages);
    params.tagIds = setToArray(filter.categories);
    params.policyViolationStates = setToArray(filter.policyViolationStates);
    params.maxDaysOld = filter.maxDaysOld;
    params.policyThreatLevelRange = filter.policyThreatLevels && filter.policyThreatLevels.join(',');
    params.expirationDate = filter.expirationDate;
    params.policyWaiverReasonIds = setToArray(filter.policyWaiverReasonIds);

    if (filter.policyTypes && filter.policyTypes.size > 0) {
      params.policyThreatCategories = setToArray(filter.policyTypes).join(',');
    }
  }
  return params;
}
