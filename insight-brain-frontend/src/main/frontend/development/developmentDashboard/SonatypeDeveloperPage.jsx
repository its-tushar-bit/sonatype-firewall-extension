/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

import React from 'react';
import {
  NxH1,
  NxLoadWrapper,
  NxP,
  NxPageMain,
  NxPageTitle,
  NxTab,
  NxTabList,
  NxTabPanel,
  NxTabs,
} from '@sonatype/react-shared-components';
import { useDispatch, useSelector } from 'react-redux';
import { selectCurrentRouteName } from 'MainRoot/reduxUiRouter/routerSelectors';
import Overview from './sections/overview/Overview';
import { stateGo } from 'MainRoot/reduxUiRouter/routerActions';
import { SECTIONS } from 'MainRoot/development/developmentDashboard/sections';
import {
  selectIsDeveloperDashboardEnabled,
  selectProductFeaturesSlice,
} from 'MainRoot/productFeatures/productFeaturesSelectors';
import LicenseLockScreen from 'MainRoot/development/developmentDashboard/LicenseLockScreen';
import { actions } from 'MainRoot/productFeatures/productFeaturesSlice';

const tabStates = [
  {
    state: `developer.dashboard.${SECTIONS.OVERVIEW}`,
    ndx: 0,
    tabName: 'Overview',
    dataAnalyticsId: 'sonatype-developer-nav-tab-overview',
  },
  {
    state: `developer.dashboard.${SECTIONS.CICD}`,
    ndx: 1,
    tabName: 'CI/CD Integrations',
    dataAnalyticsId: 'sonatype-developer-nav-tab-cicd',
  },
  {
    state: `developer.dashboard.${SECTIONS.SCM}`,
    ndx: 2,
    tabName: 'SCM Integrations',
    dataAnalyticsId: 'sonatype-developer-nav-tab-scm',
  },
  {
    state: `developer.dashboard.${SECTIONS.ISSUE_TRACKING}`,
    tabName: 'Issue Tracking Integrations',
    ndx: 3,
    dataAnalyticsId: 'sonatype-developer-nav-tab-issue-tracking',
  },
  {
    state: `developer.dashboard.${SECTIONS.IDE}`,
    ndx: 4,
    tabName: 'IDE Integrations',
    dataAnalyticsId: 'sonatype-developer-nav-tab-ide',
  },
];

export default function SonatypeDeveloperPage() {
  const dispatch = useDispatch();

  const { loading, loadError } = useSelector(selectProductFeaturesSlice);
  const isDeveloperDashboardEnabled = useSelector(selectIsDeveloperDashboardEnabled);

  const doLoad = () => dispatch(actions.fetchProductFeaturesIfNeeded());

  return (
    <NxPageMain>
      <NxLoadWrapper loading={loading} error={loadError} retryHandler={doLoad}>
        <NxH1>Dashboard</NxH1>
        {isDeveloperDashboardEnabled ? <Overview /> : <LicenseLockScreen />}
      </NxLoadWrapper>
    </NxPageMain>
  );
}
