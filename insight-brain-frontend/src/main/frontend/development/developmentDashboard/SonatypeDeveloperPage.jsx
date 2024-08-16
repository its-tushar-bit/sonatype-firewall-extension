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
import CiCd from './sections/CiCd';
import Scm from './sections/Scm';
import IssueTracking from './sections/IssueTracking';
import Ide from './sections/Ide';
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
        {isDeveloperDashboardEnabled ? <SonatypeDeveloperPageContents /> : <LicenseLockScreen />}
      </NxLoadWrapper>
    </NxPageMain>
  );
}

function SonatypeDeveloperPageContents() {
  const dispatch = useDispatch();

  const currentRouteName = useSelector(selectCurrentRouteName);

  const activeTabId = tabStates.find(({ state }) => state === currentRouteName)?.ndx || 0;

  const setTab = (index) => dispatch(stateGo(tabStates.find(({ ndx }) => ndx === index)?.state));

  return (
    <>
      <NxPageTitle>
        <NxH1>Sonatype Developer</NxH1>
      </NxPageTitle>
      <div className="iq-integrations-content">
        <NxP className="iq-integrations__full-width-text">
          Sonatype Developer seamlessly automates open-source risk management within your development pipelines. Use
          Sonatype Developer to receive real-time feedback on risk and remediation suggestions.
        </NxP>
        <NxTabs activeTab={activeTabId} onTabSelect={(index) => setTab(index)}>
          <NxTabList>
            {tabStates.map(({ dataAnalyticsId, tabName }) => (
              <NxTab key={tabName} data-analytics-id={dataAnalyticsId}>
                {tabName}
              </NxTab>
            ))}
          </NxTabList>
          <NxTabPanel>
            <Overview />
          </NxTabPanel>
          <NxTabPanel>
            <CiCd />
          </NxTabPanel>
          <NxTabPanel>
            <Scm />
          </NxTabPanel>
          <NxTabPanel>
            <IssueTracking />
          </NxTabPanel>
          <NxTabPanel>
            <Ide />
          </NxTabPanel>
        </NxTabs>
      </div>
    </>
  );
}
