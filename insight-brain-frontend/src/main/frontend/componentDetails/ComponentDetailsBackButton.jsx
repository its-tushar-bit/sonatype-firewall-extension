/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
/* eslint-disable react/prop-types */
import React from 'react';
import * as PropTypes from 'prop-types';

import MenuBarBackButton from '../mainHeader/MenuBar/MenuBarBackButton';
import { useRouterState } from '../react/RouterStateContext';
import { useSelector } from 'react-redux';
import {
  selectRouterCurrentParams,
  selectIsPrioritiesPageContainer,
  selectPrioritiesPageName,
  selectPrioritiesPageContainerName,
  selectRouterPrevParams,
} from '../reduxUiRouter/routerSelectors';
import { selectIsContainerImagesEvaluationEnabledAndProxyStage } from 'MainRoot/applicationReport/applicationReportSelectors';
import { FIREWALL_CONTAINER_REPOSITORY_RESULTS } from 'MainRoot/constants/states/firewall';
import {
  buildHrcReportPolicyHref,
  BACK_TO_HRC_REPORT_TEXT,
} from 'MainRoot/hostedRepositoryComponentReport/hrcBackLinkHelper';

export default function ComponentDetailsBackButton(props) {
  const { scanId, publicId, fromDependencyTree } = props;

  const isPrioritiesPageContainer = useSelector(selectIsPrioritiesPageContainer);
  const prioritiesPageName = useSelector(selectPrioritiesPageName);
  const prioritiesPageContainerName = useSelector(selectPrioritiesPageContainerName);
  const currentParams = useSelector(selectRouterCurrentParams);
  const prevParams = useSelector(selectRouterPrevParams);
  const uiRouterState = useRouterState();

  const isContainerImagesEvaluationEnabled = useSelector(selectIsContainerImagesEvaluationEnabledAndProxyStage);

  // HRC route → back to HRC report policy tab (URL carries hrcId, so it survives refresh).
  const hrcId = currentParams?.hrcId || prevParams?.hrcId;
  if (hrcId) {
    const href = buildHrcReportPolicyHref(uiRouterState, hrcId, currentParams?.scanId || scanId);
    return <MenuBarBackButton href={href} text={BACK_TO_HRC_REPORT_TEXT} />;
  }

  if (fromDependencyTree) {
    const text = 'Back To Dependency Tree';
    const stateName = isPrioritiesPageContainer
      ? `${prioritiesPageContainerName}.dependencyTree`
      : 'applicationReport.dependencyTree';
    const href = uiRouterState.href(stateName, { scanId, publicId });

    return <MenuBarBackButton text={text} href={href} />;
  }

  if (isContainerImagesEvaluationEnabled) {
    const text = 'Back To Container Report';
    const stateName = 'firewall.containerReport';
    const href = uiRouterState.href(stateName, {
      scanId,
      publicId,
      origin: currentParams?.origin || FIREWALL_CONTAINER_REPOSITORY_RESULTS,
    });

    return <MenuBarBackButton text={text} href={href} />;
  }

  if (isPrioritiesPageContainer) {
    const href = uiRouterState.href(prioritiesPageName, {
      scanId: currentParams.scanId,
      publicAppId: currentParams.publicId,
      ...prevParams,
    });
    return <MenuBarBackButton href={href} text="Back to Priorities" />;
  }

  return <MenuBarBackButton stateName="applicationReport.policy" />;
}

ComponentDetailsBackButton.propTypes = {
  scanId: PropTypes.string,
  publicId: PropTypes.string,
};
