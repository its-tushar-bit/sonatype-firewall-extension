/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

import { NxTab, NxTabList, NxTabs } from '@sonatype/react-shared-components';
import * as PropTypes from 'prop-types';
import { toUpper, replace } from 'ramda';
import React from 'react';
import {
  APPLICATIONS_RESULTS_TYPE,
  COMPONENTS_RESULTS_TYPE,
  VIOLATIONS_RESULTS_TYPE,
  WAIVERS_RESULTS_TYPE,
} from 'MainRoot/dashboard/results/dashboardResultsTypes';

const capitalizeFirstLetter = replace(/^./, toUpper);

const dashboardTabs = [VIOLATIONS_RESULTS_TYPE, COMPONENTS_RESULTS_TYPE, APPLICATIONS_RESULTS_TYPE];

export default function DashboardTabs({ currentTab, stateGo, isDashboardEnabled, isWaiversTabEnabled }) {
  const handleTabClick = (index) => {
    stateGo(`dashboard.overview.${getTabsToUse()[index]}`);
  };

  const getTabsToUse = () => {
    const tabsToUse = [];
    if (isDashboardEnabled) {
      tabsToUse.push(...dashboardTabs);
    }
    if (isWaiversTabEnabled) {
      tabsToUse.push(WAIVERS_RESULTS_TYPE);
    }
    return tabsToUse;
  };

  return (
    <NxTabs activeTab={getTabsToUse().indexOf(currentTab)} onTabSelect={handleTabClick}>
      <NxTabList>
        {getTabsToUse().map((tab) => (
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
