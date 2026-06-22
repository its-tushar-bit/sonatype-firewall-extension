/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';
import * as PropTypes from 'prop-types';
import { useDispatch, useSelector } from 'react-redux';
import { NxTabs, NxTab, NxTabList, NxTabPanel } from '@sonatype/react-shared-components';
import { actions } from './usageSlice';
import { selectActiveTab } from './usageSelectors';

const TABS = ['overview', 'trends'];

export default function UsageTabs({ overview, trends }) {
  const dispatch = useDispatch();
  const activeTab = useSelector(selectActiveTab);
  const activeIdx = Math.max(0, TABS.indexOf(activeTab));

  return (
    <NxTabs activeTab={activeIdx} onTabSelect={(idx) => dispatch(actions.setActiveTab(TABS[idx]))}>
      <NxTabList>
        <NxTab>Overview</NxTab>
        <NxTab>Trends</NxTab>
      </NxTabList>
      <NxTabPanel>{overview}</NxTabPanel>
      <NxTabPanel>{trends}</NxTabPanel>
    </NxTabs>
  );
}

UsageTabs.propTypes = {
  overview: PropTypes.node.isRequired,
  trends: PropTypes.node.isRequired,
};
