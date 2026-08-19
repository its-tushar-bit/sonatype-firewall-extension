/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { setToArray } from '../../util/jsUtil';

export function filterToJson(filter) {
  return {
    organizationFilters: setToArray(filter.organizations),
    applicationFilters: setToArray(filter.applications),
    repositoryFilters: setToArray(filter.repositories),
    policyThreatCategoryFilters: setToArray(filter.policyTypes),
    stageTypeFilters: setToArray(filter.stages),
    tagFilters: setToArray(filter.categories),
    policyViolationStates: setToArray(filter.policyViolationStates),
    maxDaysOld: filter.maxDaysOld,
    minPolicyThreatLevel: filter.policyThreatLevels[0],
    maxPolicyThreatLevel: filter.policyThreatLevels[1],
    expirationDate: filter.expirationDate,
    policyWaiverReasonIds: setToArray(filter.policyWaiverReasonIds),
  };
}
