/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

import { NxTab, NxTabList, NxTabs } from '@sonatype/react-shared-components';
import * as PropTypes from 'prop-types';
import { capitalizeFirstLetter } from 'MainRoot/util/jsUtil';
import React from 'react';
import {
  APPLICATIONS_RESULTS_TYPE,
  COMPONENTS_RESULTS_TYPE,
  VIOLATIONS_RESULTS_TYPE,
  WAIVERS_RESULTS_TYPE,
  WAIVER_REQUESTS_RESULTS_TYPE,
} from 'MainRoot/dashboard/results/dashboardResultsTypes';

export default function DashboardTabs({ currentTab, stateGo, isDashboardEnabled, isWaiversTabEnabled }) {
  // Determine which tabs to show in the dashboard
  // based on which dashboard features are enabled
  const dashboardTabs = () => {
    const tabsToUse = [];
    if (isDashboardEnabled) {
      tabsToUse.push(...[VIOLATIONS_RESULTS_TYPE, COMPONENTS_RESULTS_TYPE, APPLICATIONS_RESULTS_TYPE]);
    }
    if (isWaiversTabEnabled) {
      tabsToUse.push(WAIVERS_RESULTS_TYPE);
    }
    return tabsToUse;
  };

  // Handle the case where the current tab is either of the Waiver sub-tabs
  const getActiveTab = (currentTab) => {
    if (currentTab === WAIVERS_RESULTS_TYPE || currentTab === WAIVER_REQUESTS_RESULTS_TYPE) {
      return dashboardTabs().indexOf(WAIVERS_RESULTS_TYPE);
    } else {
      return dashboardTabs().indexOf(currentTab);
    }
  };

  const handleTabClick = (index) => {
    stateGo(`dashboard.overview.${dashboardTabs()[index]}`);
  };

  return (
    <NxTabs activeTab={getActiveTab(currentTab)} onTabSelect={handleTabClick}>
      <NxTabList>
        {dashboardTabs().map((tab) => (
          <NxTab key={tab}>{capitalizeFirstLetter(tab)}</NxTab>
        ))}
      </NxTabList>
    </NxTabs>
  );
}

export const dashboardTabsPropTypes = {
  currentTab: PropTypes.string.isRequired,
  isDashboardEnabled: PropTypes.bool.isRequired,
  isWaiversTabEnabled: PropTypes.bool.isRequired,
};

DashboardTabs.propTypes = {
  ...dashboardTabsPropTypes,
  stateGo: PropTypes.func.isRequired,
};
