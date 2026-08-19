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
import {
  buildHrcReportPolicyHref,
  BACK_TO_HRC_REPORT_TEXT,
} from 'MainRoot/hostedRepositoryComponentReport/hrcBackLinkHelper';

export default function BackButton() {
  const uiRouterState = useRouterState();
  const isPrioritiesPageContainer = useSelector(selectIsPrioritiesPageContainer);
  const currentParams = useSelector(selectRouterCurrentParams);
  const { publicId, scanId, hrcId } = currentParams;
  const prioritiesPageContainerName = useSelector(selectPrioritiesPageContainerName);
  const prioritiesPageName = useSelector(selectPrioritiesPageName);
  const isDependencyTreePageFromPrioritiesPage = useSelector(selectIsDependencyTreePageFromPrioritiesPage);
  const prevParams = useSelector(selectRouterPrevParams);

  // HRC report sub-pages (raw, vulnerabilities, etc.) — go back to HRC report policy tab.
  if (hrcId) {
    return (
      <MenuBarBackButton href={buildHrcReportPolicyHref(uiRouterState, hrcId, scanId)} text={BACK_TO_HRC_REPORT_TEXT} />
    );
  }

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

  return <MenuBarBackButton stateName="applicationReport.policy" />;
}
