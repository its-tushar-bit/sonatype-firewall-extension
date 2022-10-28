/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

import { NxTab, NxTabList, NxTabs } from '@sonatype/react-shared-components';
import * as PropTypes from 'prop-types';
import { isNil, path, toUpper, replace } from 'ramda';
import React from 'react';
import {
  APPLICATIONS_RESULTS_TYPE,
  COMPONENTS_RESULTS_TYPE,
  VIOLATIONS_RESULTS_TYPE,
  WAIVERS_RESULTS_TYPE,
} from 'MainRoot/dashboard/results/dashboardResultsTypes';

const capitalizeFirstLetter = replace(/^./, toUpper);

const tabs = [VIOLATIONS_RESULTS_TYPE, COMPONENTS_RESULTS_TYPE, APPLICATIONS_RESULTS_TYPE, WAIVERS_RESULTS_TYPE];

export default function DashboardTabs(props) {
  const { currentTab, stateGo } = props;

  const handleTabClick = (index) => {
    stateGo(`dashboard.overview.${tabs[index]}`);
  };

  return (
    <NxTabs activeTab={tabs.indexOf(currentTab)} onTabSelect={handleTabClick}>
      <NxTabList>
        {tabs.map((tab) => (
          <NxTab key={tab}>
            {capitalizeFirstLetter(tab)}
            {!isNil(path([tab, 'numResults'], props)) && (
              <span className={`nx-counter ${currentTab === tab && 'nx-counter--active'}`}>
                {path([tab, 'numResults'], props)}
              </span>
            )}
          </NxTab>
        ))}
      </NxTabList>
    </NxTabs>
  );
}

export const dashboardTabsPropTypes = {
  currentTab: PropTypes.string.isRequired,
  violations: PropTypes.shape({ numResults: PropTypes.number }).isRequired,
  components: PropTypes.shape({ numResults: PropTypes.number }).isRequired,
  applications: PropTypes.shape({ numResults: PropTypes.number }).isRequired,
  waivers: PropTypes.shape({ numResults: PropTypes.number }).isRequired,
};

DashboardTabs.propTypes = {
  ...dashboardTabsPropTypes,
  stateGo: PropTypes.func.isRequired,
};
