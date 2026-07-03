/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';
import MenuBarBackButton from 'MainRoot/mainHeader/MenuBar/MenuBarBackButton';
import { useRouterState } from 'MainRoot/react/RouterStateContext';
import {
  selectRouterCurrentParams,
  selectIsPrioritiesPageContainer,
  selectPrioritiesPageContainerName,
  selectIsDependencyTreePageFromPrioritiesPage,
  selectPrioritiesPageName,
  selectRouterPrevParams,
} from 'MainRoot/reduxUiRouter/routerSelectors';
import { useSelector } from 'react-redux';

export default function BackButton() {
  const uiRouterState = useRouterState();
  const isPrioritiesPageContainer = useSelector(selectIsPrioritiesPageContainer);
  const currentParams = useSelector(selectRouterCurrentParams);
  const {
    publicId,
    scanId,
    origin,
    repositoryManagerId,
    repositoryId,
    repositoryPublicId,
    componentDisplayName,
  } = currentParams;
  const prioritiesPageContainerName = useSelector(selectPrioritiesPageContainerName);
  const prioritiesPageName = useSelector(selectPrioritiesPageName);
  const isDependencyTreePageFromPrioritiesPage = useSelector(selectIsDependencyTreePageFromPrioritiesPage);
  const prevParams = useSelector(selectRouterPrevParams);

  if (isDependencyTreePageFromPrioritiesPage) {
    const prioritiesPageHref = uiRouterState.href(prioritiesPageName, { scanId, publicAppId: publicId, ...prevParams });
    return <MenuBarBackButton href={prioritiesPageHref} text="Back to Priorities Page" />;
  } else if (isPrioritiesPageContainer) {
    const appReportWithBackToPrioritiesHref = uiRouterState.href(`${prioritiesPageContainerName}.policy`, {
      scanId,
      publicId,
    });
    return <MenuBarBackButton href={appReportWithBackToPrioritiesHref} text="Back to Application Report" />;
  }

  // Hosted-repo flow: the URL carries origin + repo IDs, so refresh keeps the back link consistent.
  // componentDisplayName forwards the friendly component name back to the report (CLM-42090).
  if (origin === 'hostedRepoComponents') {
    const hostedRepoReportHref = uiRouterState.href('applicationReport.policy', {
      publicId,
      scanId,
      origin,
      repositoryManagerId,
      repositoryId,
      repositoryPublicId,
      componentDisplayName,
    });
    return <MenuBarBackButton href={hostedRepoReportHref} text="Back to Repository Component Report" />;
  }

  return <MenuBarBackButton stateName="applicationReport.policy" />;
}
