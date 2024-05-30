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
  selectCurrentRouteName,
} from '../reduxUiRouter/routerSelectors';

export default function ComponentDetailsBackButton(props) {
  const { scanId, publicId } = props;
  const dependencyTreePropsPresent = scanId && publicId;

  const isPrioritiesPageContainer = useSelector(selectIsPrioritiesPageContainer);
  const currentParams = useSelector(selectRouterCurrentParams);
  const currentRouteName = useSelector(selectCurrentRouteName);
  const uiRouterState = useRouterState();

  if (!dependencyTreePropsPresent) {
    if (isPrioritiesPageContainer) {
      if (currentRouteName.includes('componentDetailsFromReport')) {
        const prioritiesPageHref = uiRouterState.href('prioritiesPageContainer.policy', {
          scanId: currentParams.scanId,
          publicId: currentParams.publicId,
        });
        return <MenuBarBackButton href={prioritiesPageHref} text="Back to Application Report" />;
      }
      const prioritiesPageHref = uiRouterState.href('prioritiesPage', {
        scanId: currentParams.scanId,
        publicAppId: currentParams.publicId,
      });
      return <MenuBarBackButton href={prioritiesPageHref} text="Back to Priorities" />;
    }
    return <MenuBarBackButton stateName="applicationReport.policy" />;
  }

  const text = 'Back To Dependency Tree';
  const href = useRouterState().href('applicationReport.dependencyTree', { scanId, publicId });

  return <MenuBarBackButton text={text} href={href} />;
}

ComponentDetailsBackButton.propTypes = {
  scanId: PropTypes.string,
  publicId: PropTypes.string,
};
