/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { stateGo } from 'MainRoot/reduxUiRouter/routerActions';
import { selectIsStandaloneFirewall } from 'MainRoot/reduxUiRouter/routerSelectors';

export function goToRepositoryComponentDetails(
  repositoryId,
  componentIdentifier,
  componentHash,
  matchState,
  pathname,
  componentDisplayName
) {
  return (dispatch, getState) => {
    const prefix = selectIsStandaloneFirewall(getState()) ? 'firewall' : 'repository';
    dispatch(
      stateGo(`${prefix}.componentDetailsPage`, {
        repositoryId,
        componentIdentifier: JSON.stringify(componentIdentifier),
        componentHash,
        matchState,
        pathname,
        componentDisplayName,
      })
    );
  };
}

// Navigate to the native HRC lifecycle report (CLM-44275). Uses the hostedRepositoryComponent id
// returned by /api/v2/repositories/{rm}/{repo}/components (ApiHostedRepositoryComponentDTO.id) —
// which is the HRC's public id — as the route param.
export function goToHrcReport(hrcId, scanId, componentDisplayName) {
  return stateGo('hostedRepositoryComponentReport.policy', {
    hrcId,
    scanId,
    componentDisplayName,
  });
}

export function goToComponentPriorities(applicationPublicId, scanId) {
  return stateGo('prioritiesPageFromReports', {
    publicAppId: applicationPublicId,
    scanId,
  });
}
