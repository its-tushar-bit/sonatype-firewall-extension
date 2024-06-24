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
  selectPrioritiesPageContainerName,
  selectPrioritiesPageName,
  selectCurrentRouteName,
} from '../reduxUiRouter/routerSelectors';

export default function ComponentDetailsBackButton(props) {
  const { scanId, publicId } = props;
  const dependencyTreePropsPresent = scanId && publicId;

  const isPrioritiesPageContainer = useSelector(selectIsPrioritiesPageContainer);
  const prioritiesPageContainerName = useSelector(selectPrioritiesPageContainerName);
  const prioritiesPageName = useSelector(selectPrioritiesPageName);
  const currentParams = useSelector(selectRouterCurrentParams);
  const currentRouteName = useSelector(selectCurrentRouteName);
  const uiRouterState = useRouterState();

  if (dependencyTreePropsPresent) {
    const text = 'Back To Dependency Tree';
    const href = useRouterState().href('applicationReport.dependencyTree', { scanId, publicId });

    return <MenuBarBackButton text={text} href={href} />;
  }

  if (isPrioritiesPageContainer) {
    if (currentRouteName.includes('componentDetailsFromReport')) {
      const href = uiRouterState.href(`${prioritiesPageContainerName}.policy`, {
        scanId: currentParams.scanId,
        publicId: currentParams.publicId,
      });
      return <MenuBarBackButton href={href} text="Back to Application Report" />;
    }

    const href = uiRouterState.href(prioritiesPageName, {
      scanId: currentParams.scanId,
      publicAppId: currentParams.publicId,
    });
    return <MenuBarBackButton href={href} text="Back to Priorities" />;
  }

  return <MenuBarBackButton stateName="applicationReport.policy" />;
}

ComponentDetailsBackButton.propTypes = {
  scanId: PropTypes.string,
  publicId: PropTypes.string,
};
