/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

import React, { useEffect } from 'react';
import { useDispatch, useSelector } from 'react-redux';
import { NxLoadingSpinner, NxLoadWrapper, NxPageMain, NxPageTitle, NxButton } from '@sonatype/react-shared-components';
import LicenseLockScreen from 'MainRoot/development/developmentDashboard/LicenseLockScreen';
import PrioritiesPageHeader from 'MainRoot/development/prioritiesPage/PrioritiesPageHeader';
import PrioritiesPageTable from 'MainRoot/development/prioritiesPage/PrioritiesPageTable';
import {
  selectLoadingFeatures,
  selectIsDeveloperDashboardEnabled,
} from 'MainRoot/productFeatures/productFeaturesSelectors';
import { selectRouterCurrentParams } from 'MainRoot/reduxUiRouter/routerSelectors';
import { actions } from 'MainRoot/development/prioritiesPage/slices/prioritiesPageSlice';
import { selectPrioritiesPageSlice } from 'MainRoot/development/prioritiesPage/selectors/prioritiesPageSelectors';
import { stateGo } from 'MainRoot/reduxUiRouter/routerActions';
import MenuBarBackButton from 'MainRoot/mainHeader/MenuBar/MenuBarBackButton';
import { selectCurrentRouteName } from 'MainRoot/reduxUiRouter/routerSelectors';
import { useRouterState } from 'MainRoot/react/RouterStateContext';

export default function PrioritiesPage() {
  const currentRouteName = useSelector(selectCurrentRouteName);
  const uiRouterState = useRouterState();
  const { publicAppId, scanId } = useSelector(selectRouterCurrentParams);

  const getHref = () => {
    if (currentRouteName === 'prioritiesPageFromDashboard') {
      return {
        href: uiRouterState.href('integrations'),
        text: 'Back to Developer Dashboard',
      };
    } else if (currentRouteName === 'prioritiesPageFromReports') {
      return {
        href: uiRouterState.href('violations'),
        text: 'Back to Reports',
      };
    } else if (currentRouteName === 'prioritiesPageFromAppReport') {
      return {
        href: uiRouterState.href('applicationReport.policy', {
          publicId: publicAppId,
          scanId,
        }),
        text: 'Back to Application Report',
      };
    }
    return {
      href: uiRouterState.href('violations'),
      text: 'Back to Reports',
    };
  };

  const { href, text } = getHref();
  return (
    <NxPageMain className="iq-priorities-page">
      <MenuBarBackButton href={href} text={text} />
      <PageContents />
    </NxPageMain>
  );
}

function PageContents() {
  const isDeveloperDashboardEnabled = useSelector(selectIsDeveloperDashboardEnabled);
  const productFeaturesLoading = useSelector(selectLoadingFeatures);

  if (productFeaturesLoading) {
    return <NxLoadingSpinner />;
  } else if (isDeveloperDashboardEnabled) {
    return <PrioritiesPageContents />;
  } else {
    return <LicenseLockScreen />;
  }
}

function PrioritiesPageContents() {
  const dispatch = useDispatch();
  const { publicAppId, scanId } = useSelector(selectRouterCurrentParams);
  const { loadingMetadata, loadErrorMetadata, metadata } = useSelector(selectPrioritiesPageSlice);
  const currentRouteName = useSelector(selectCurrentRouteName);

  const getPrioritiesPageStateName = () => {
    if (currentRouteName === 'prioritiesPageFromReports') {
      return 'appReportPageWithinPrioritiesPageContainerFromReports.policy';
    } else if (currentRouteName === 'prioritiesPageFromDashboard') {
      return 'appReportPageWithinPrioritiesPageContainerFromDashboard.policy';
    } else if (currentRouteName === 'prioritiesPageFromAppReport') {
      return 'appReportPageWithinPrioritiesPageContainerFromAppReport.policy';
    }
  };

  const goToFullReport = () => dispatch(stateGo(getPrioritiesPageStateName(), { scanId, publicId: publicAppId }));

  const doLoad = () => {
    dispatch(actions.loadMetadata());
  };

  useEffect(() => {
    doLoad();

    return () => dispatch(actions.resetState());
  }, []);

  return (
    <NxLoadWrapper loading={loadingMetadata} error={loadErrorMetadata} retryHandler={doLoad}>
      {metadata && (
        <>
          <NxPageTitle>
            <PrioritiesPageHeader />
          </NxPageTitle>
          <PrioritiesPageTable />
          <div className="nx-btn-bar">
            <NxButton variant="primary" onClick={goToFullReport}>
              View Full Report
            </NxButton>
          </div>
        </>
      )}
    </NxLoadWrapper>
  );
}
