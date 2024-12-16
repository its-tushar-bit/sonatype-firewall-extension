/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
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

export default function ComponentDetailsBackButton(props) {
  const { scanId, publicId } = props;
  const dependencyTreePropsPresent = scanId && publicId;

  const isPrioritiesPageContainer = useSelector(selectIsPrioritiesPageContainer);
  const prioritiesPageName = useSelector(selectPrioritiesPageName);
  const prioritiesPageContainerName = useSelector(selectPrioritiesPageContainerName);
  const currentParams = useSelector(selectRouterCurrentParams);
  const prevParams = useSelector(selectRouterPrevParams);
  const uiRouterState = useRouterState();

  if (dependencyTreePropsPresent) {
    const text = 'Back To Dependency Tree';
    const stateName = isPrioritiesPageContainer
      ? `${prioritiesPageContainerName}.dependencyTree`
      : 'applicationReport.dependencyTree';
    const href = uiRouterState.href(stateName, { scanId, publicId });

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
