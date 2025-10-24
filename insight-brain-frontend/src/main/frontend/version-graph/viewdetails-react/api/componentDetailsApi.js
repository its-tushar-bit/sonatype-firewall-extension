/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { defaultTo, map, pipe, prop } from 'ramda';
import axios from 'axios';

import { getComponentDetailsUrl, getApplicationNamesUrl } from 'MainRoot/util/CLMLocation';

import { transformPolicyAlerts } from '../utils/policyUtils';
import { processSecurityVulnerabilities } from '../utils/securityUtils';

/**
 * Extracts the license names from license objects
 */
const toLicenseNames = pipe(defaultTo([]), map(prop('licenseName')));

/**
 * Fetches component details from the server
 *
 * @param {Object} params Query parameters
 * @returns {Promise} Promise that resolves to the component details
 * @throws {Object} Standardized error object with status, data, and headers
 */
export async function fetchComponentDetails(params) {
  const {
    appId,
    hash,
    matchState,
    proprietary,
    componentIdentifier,
    groupId,
    artifactId,
    version,
    classifier,
    extension,
  } = params;

  // Extract identifier based on what was provided in the query params
  const identifier = componentIdentifier
    ? JSON.parse(componentIdentifier)
    : groupId
    ? {
        format: 'maven',
        coordinates: {
          groupId,
          artifactId,
          version,
          classifier,
          extension,
        },
      }
    : {};

  const componentUrl = getComponentDetailsUrl({
    clientType: 'rm',
    ownerType: 'application',
    ownerId: appId,
    componentIdentifier: JSON.stringify(identifier),
    hash,
    matchState,
    proprietary,
  });

  try {
    const requests = [axios.get(componentUrl), axios.get(getApplicationNamesUrl())];
    const [componentResponse, appListResponse] = await Promise.all(requests);
    return processResponses(appId, componentResponse, appListResponse);
  } catch (error) {
    if (error.response) {
      throw {
        status: error.response.status,
        data: error.response.data,
        headers: error.response.headers,
      };
    } else if (error.request) {
      throw {
        status: 0,
        data: 'Network error: No response received',
        headers: {},
      };
    } else {
      throw {
        status: 0,
        data: error.message,
        headers: {},
      };
    }
  }
}

function processResponses(appId, componentResponse, appListResponse) {
  const componentData = componentResponse.data;

  // Transform the data for display
  return {
    ...componentData,
    observedLicenses: toLicenseNames(componentData.observedLicenses),
    declaredLicenses: toLicenseNames(componentData.declaredLicenses),
    overriddenLicenses: toLicenseNames(componentData.overriddenLicenses),
    policyAlerts: transformPolicyAlerts(componentData.policyAlerts),
    securityVulnerabilities: processSecurityVulnerabilities(componentData.securityVulnerabilities),
    appName: appListResponse?.data?.[appId],
  };
}
