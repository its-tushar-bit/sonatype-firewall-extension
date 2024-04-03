/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

import React from 'react';
import {
  NxH1,
  NxInfoAlert,
  NxP,
  NxPageMain,
  NxPageTitle,
  NxTab,
  NxTabList,
  NxTabPanel,
  NxTabs,
  NxTextLink,
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
import { selectIsDeveloperDashboardEnabled } from 'MainRoot/productFeatures/productFeaturesSelectors';
import LicenseLockScreen from 'MainRoot/development/developmentDashboard/LicenseLockScreen';

export default function SonatypeDeveloperPage() {
  const tabStates = [
    {
      state: `integrations.${SECTIONS.OVERVIEW}`,
      ndx: 0,
      tabName: 'Overview',
      dataAnalyticsId: 'sonatype-developer-nav-tab-overview',
    },
    {
      state: `integrations.${SECTIONS.CICD}`,
      ndx: 1,
      tabName: 'CI/CD Integrations',
      dataAnalyticsId: 'sonatype-developer-nav-tab-cicd',
    },
    {
      state: `integrations.${SECTIONS.SCM}`,
      ndx: 2,
      tabName: 'SCM Integrations',
      dataAnalyticsId: 'sonatype-developer-nav-tab-scm',
    },
    {
      state: `integrations.${SECTIONS.ISSUE_TRACKING}`,
      tabName: 'Issue Tracking Integrations',
      ndx: 3,
      dataAnalyticsId: 'sonatype-developer-nav-tab-issue-tracking',
    },
    {
      state: `integrations.${SECTIONS.IDE}`,
      ndx: 4,
      tabName: 'IDE Integrations',
      dataAnalyticsId: 'sonatype-developer-nav-tab-ide',
    },
  ];

  const currentRouteName = useSelector(selectCurrentRouteName);
  const isDeveloperDashboardEnabled = useSelector(selectIsDeveloperDashboardEnabled);

  const activeTabId = tabStates.find(({ state }) => state === currentRouteName)?.ndx || 0;
  const dispatch = useDispatch();

  const setTab = (index) => dispatch(stateGo(tabStates.find(({ ndx }) => ndx === index)?.state));

  if (!isDeveloperDashboardEnabled) {
    return <LicenseLockScreen />;
  }

  return (
    <NxPageMain>
      <NxInfoAlert className="iq-integrations-page-top-level-alert">
        Sonatype Development is available for free in the <strong>Product Preview Program (PPP)</strong>. Innovate with
        us by submitting your feedback to{' '}
        <NxTextLink
          external
          href="mailto:sonatype-developer@sonatype.com"
          data-analytics-id="sonatype-developer-feedback-mailto"
        >
          sonatype-developer@sonatype.com
        </NxTextLink>
        .
      </NxInfoAlert>
      <NxPageTitle>
        <NxH1>Sonatype Development</NxH1>
      </NxPageTitle>
      <div className="iq-integrations-content">
        <NxP className="iq-integrations__full-width-text">
          <strong>Integrate Sonatype Development</strong> in your development pipeline to automate open-source risk
          management, with real-time feedback, early in your development process. Sonatype integrations help you take
          immediate action to avoid surprise compliance issues when changes are pushed to production.
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
    </NxPageMain>
  );
}
