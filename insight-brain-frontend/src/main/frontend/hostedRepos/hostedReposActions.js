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

export function goToComponentReport(
  applicationPublicId,
  scanId,
  repositoryManagerId,
  repositoryId,
  repositoryPublicId
) {
  return stateGo('applicationReport.policy', {
    publicId: applicationPublicId,
    scanId,
    origin: 'hostedRepoComponents',
    repositoryManagerId,
    repositoryId,
    repositoryPublicId,
  });
}

export function goToComponentPriorities(applicationPublicId, scanId) {
  return stateGo('prioritiesPageFromReports', {
    publicAppId: applicationPublicId,
    scanId,
  });
}
