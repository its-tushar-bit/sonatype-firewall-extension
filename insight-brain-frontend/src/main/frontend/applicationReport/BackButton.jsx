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
} from 'MainRoot/reduxUiRouter/routerSelectors';
import { useSelector } from 'react-redux';

export default function BackButton() {
  const uiRouterState = useRouterState();
  const isPrioritiesPageContainer = useSelector(selectIsPrioritiesPageContainer);
  const { publicId, scanId } = useSelector(selectRouterCurrentParams);
  const prioritiesPageContainerName = useSelector(selectPrioritiesPageContainerName);

  if (isPrioritiesPageContainer) {
    const appReportWithBackToPrioritiesHref = uiRouterState.href(`${prioritiesPageContainerName}.policy`, {
      scanId,
      publicId,
    });
    return <MenuBarBackButton href={appReportWithBackToPrioritiesHref} text="Back to Application Report" />;
  }
  return <MenuBarBackButton stateName="applicationReport.policy" />;
}
