/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import axios from 'axios';

import { getDashboardDeleteFiltersUrl } from '../../util/CLMLocation';
import { Messages } from '../../util/CommonServices';
import { setToArray } from '../../util/jsUtil';

export function filterToJson(filter) {
  return {
    organizationFilters: setToArray(filter.organizations),
    applicationFilters: setToArray(filter.applications),
    policyThreatCategoryFilters: setToArray(filter.policyTypes),
    stageTypeFilters: setToArray(filter.stages),
    tagFilters: setToArray(filter.categories),
    policyViolationStates: setToArray(filter.policyViolationStates),
    maxDaysOld: filter.maxDaysOld,
    minPolicyThreatLevel: filter.policyThreatLevels[0],
    maxPolicyThreatLevel: filter.policyThreatLevels[1]
  };
}

export function deleteSavedFilter(filterName) {
  return axios.post(getDashboardDeleteFiltersUrl(), [filterName])
      .catch(error => {
        error = Messages.getHttpErrorMessage(error);
        if (Array.isArray(error)) {
          return Promise.reject(error[0].errorMessage);
        }
        else {
          return Promise.reject(error);
        }
      });
}
