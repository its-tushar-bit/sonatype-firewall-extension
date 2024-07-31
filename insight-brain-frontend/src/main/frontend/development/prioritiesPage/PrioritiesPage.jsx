/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

import React, { useEffect } from 'react';
import { useDispatch, useSelector } from 'react-redux';
import {
  NxLoadingSpinner,
  NxLoadWrapper,
  NxPageMain,
  NxPageTitle,
  NxTextLink,
} from '@sonatype/react-shared-components';
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
import MenuBarBackButton from 'MainRoot/mainHeader/MenuBar/MenuBarBackButton';
import { selectCurrentRouteName } from 'MainRoot/reduxUiRouter/routerSelectors';
import { useRouterState } from 'MainRoot/react/RouterStateContext';

export default function PrioritiesPage() {
  const currentRouteName = useSelector(selectCurrentRouteName);
  const uiRouterState = useRouterState();
  const getHref = () => {
    if (currentRouteName === 'prioritiesPageFromDashboard') {
      return {
        href: uiRouterState.href('developer.dashboard'),
        text: 'Back to Developer Dashboard',
      };
    } else if (currentRouteName === 'prioritiesPageFromReports' || currentRouteName === 'prioritiesPageFromAppReport') {
      return {
        href: uiRouterState.href('developer.reports'),
        text: 'Back to Reports',
      };
    }
    return {
      href: uiRouterState.href('developer.dashboard'),
      text: 'Back to Developer Dashboard',
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
  const uiRouterState = useRouterState();

  const doLoad = () => {
    dispatch(actions.loadMetadata());
  };

  useEffect(() => {
    doLoad();

    return () => dispatch(actions.resetState());
  }, []);

  const getApplicationReportHref = () => {
    return uiRouterState.href('applicationReport.policy', {
      publicId: publicAppId,
      scanId: scanId,
    });
  };

  return (
    <NxLoadWrapper loading={loadingMetadata} error={loadErrorMetadata} retryHandler={doLoad}>
      {metadata && (
        <>
          <NxPageTitle>
            <PrioritiesPageHeader />
          </NxPageTitle>
          <PrioritiesPageTable />
          <div className="nx-btn-bar">
            <NxTextLink
              className="nx-btn nx-btn--primary iq-priorities-page-view-full-report-btn"
              href={getApplicationReportHref()}
              newTab
            >
              View Full Report
            </NxTextLink>
          </div>
        </>
      )}
    </NxLoadWrapper>
  );
}
