/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

import axios from 'axios';
import moment from 'moment';
import { actions } from './firewallBulkWaiverSlice';
import {
  getPolicyWaiverReasonsUrl,
  getOwnerContextHierarchyUrl,
  getRepositoryComponentsUrl,
  getFirewallBulkWaiverUrl,
} from 'MainRoot/util/CLMLocation';
import { normalizeFirewallOwnerType } from './firewallWaiverUtils';

function processOwnerHierarchy(context) {
  const processedChildren =
    context.children && context.children.length > 0 ? processOwnerHierarchy(context.children[0]) : [];
  const { type, id, name } = context;
  const ownerType = type;
  const ownerId = id;
  const ownerName = name;

  return processedChildren.concat({ ownerType, ownerId, ownerName, id, type, name });
}

export const loadFirewallWaiverReasons = () => {
  return (dispatch) => {
    dispatch(actions.setLoadingWaiverReasons(true));

    return axios
      .get(getPolicyWaiverReasonsUrl())
      .then((response) => {
        dispatch(actions.setWaiverReasons(response.data));
      })
      .catch((error) => {
        dispatch(actions.setWaiverReasonsError(error.message || 'Failed to load waiver reasons'));
      });
  };
};

export const loadFirewallWaiverScopes = (repositoryId, contextId) => {
  return (dispatch) => {
    dispatch(actions.setLoadingWaiverScopes(true));

    const url = getOwnerContextHierarchyUrl('repository', repositoryId, contextId);

    return axios
      .get(url)
      .then((response) => {
        const processedScopes = processOwnerHierarchy(response.data);
        dispatch(actions.setAvailableWaiverScopes(processedScopes));
        if (processedScopes && processedScopes.length > 0) {
          dispatch(actions.setSelectedWaiverScope(processedScopes[0]));
        }
      })
      .catch((error) => {
        dispatch(actions.setWaiverScopesError(error.message || 'Failed to load waiver scopes'));
      });
  };
};

export const loadAllFilteredViolations = (repositoryId, componentsRequestBody = {}) => {
  return async (dispatch) => {
    dispatch(actions.setLoadingAllViolations(true));

    try {
      let allViolations = [];
      let page = 1;
      let hasMorePages = true;
      const pageSize = 100;

      while (hasMorePages) {
        const requestBody = {
          ...componentsRequestBody,
          page,
          pageSize,
          searchFilters: componentsRequestBody.searchFilters || [],
          sortFields: componentsRequestBody.sortFields || [
            {
              sortableField: 'POLICY_THREAT_LEVEL',
              asc: false,
              sortPriority: 1,
            },
          ],
          aggregate: false,
          matchStateFilters: componentsRequestBody.matchStateFilters || [],
          violationStateFilters: componentsRequestBody.violationStateFilters || [],
          threatLevelFilters: componentsRequestBody.threatLevelFilters || [0, 10],
          isBulkWaiverPage: true,
        };

        const response = await axios.post(getRepositoryComponentsUrl('repository', repositoryId), requestBody);
        const { repositoryResultsDetails, hasNextPage } = response.data;

        allViolations = allViolations.concat(repositoryResultsDetails);
        hasMorePages = hasNextPage;
        page++;
      }

      const filteredViolations = allViolations
        .map((v) => ({ ...v, threatLevel: v.threatLevel ?? v.policyThreatLevel }))
        .filter((violation) => violation.threatLevel > 0);

      dispatch(actions.setAllFilteredViolations(filteredViolations));
    } catch (error) {
      dispatch(actions.setAllViolationsError(error.message || 'Failed to load violations'));
    }
  };
};

export const submitFirewallBulkWaiver = (params) => {
  return async (dispatch) => {
    const { repositoryId, selectedViolations, waiverConfiguration } = params;

    dispatch(actions.setSubmitting(true));

    try {
      const violationIds = selectedViolations.map((v) => v.policyViolationId);

      const apiWaiverOptionsDTO = {
        comment: waiverConfiguration.comments || '',
        matcherStrategy: waiverConfiguration.componentMatcherStrategy,
      };

      if (waiverConfiguration.waiverReasonId) {
        apiWaiverOptionsDTO.waiverReasonId = waiverConfiguration.waiverReasonId;
      }

      if (waiverConfiguration.expiryTime) {
        const { expiryTime, customExpiryTime } = waiverConfiguration;

        if (expiryTime === 'custom' && customExpiryTime?.value) {
          const customDate = moment(customExpiryTime.value, 'YYYY-MM-DD').endOf('day');
          apiWaiverOptionsDTO.expiryTime = customDate.toISOString();
        } else if (expiryTime === 'never') {
          apiWaiverOptionsDTO.expiryTime = null;
        } else if (expiryTime && expiryTime !== 'custom') {
          const daysToAdd = parseInt(expiryTime, 10);
          const expiryDate = moment().add(daysToAdd, 'days').endOf('day');
          apiWaiverOptionsDTO.expiryTime = expiryDate.toISOString();
        }
      }

      const requestBody = {
        violationIds,
        apiWaiverOptionsDTO,
      };

      const rawOwnerType = waiverConfiguration.selectedWaiverScope?.type;
      const ownerType = normalizeFirewallOwnerType(rawOwnerType);
      const ownerId = waiverConfiguration.selectedWaiverScope?.id;

      const url = getFirewallBulkWaiverUrl(ownerType, ownerId);
      const response = await axios.post(url, requestBody);

      dispatch(actions.setSubmitSuccess(true));
      return response.data;
    } catch (error) {
      let errorMessage = error.response?.data || error.message || 'Failed to submit bulk waiver';
      if(errorMessage.includes('already exists')) {
          errorMessage = 'Waiver already exists for few of the selected violations. Please re-evaluate the report and try again.';
      }
    dispatch(actions.setSubmitError(errorMessage));
      throw error;
    }
  };
};
