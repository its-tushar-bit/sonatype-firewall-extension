/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';
import * as PropTypes from 'prop-types';
import { NxTab, NxTabList, NxTabPanel, NxTabs } from '@sonatype/react-shared-components';

export default function ComponentDetailsTabs({ activeTabId, onTabChange, tabsConfiguration }) {
  const handleTabChange = (tabIndex) => {
    return onTabChange(tabsConfiguration?.[tabIndex]?.tabId);
  };

  const findTabIndexByTabId = (tabIdToFind) => tabsConfiguration.findIndex((tc) => tc.tabId === tabIdToFind);

  return (
    <NxTabs activeTab={findTabIndexByTabId(activeTabId)} onTabSelect={handleTabChange}>
      <NxTabList aria-label="Component detail tabs">
        {tabsConfiguration.map((tabConfiguration) => (
          <NxTab key={tabConfiguration.tabId}>{tabConfiguration.title}</NxTab>
        ))}
      </NxTabList>
      {tabsConfiguration.map((tabConfiguration) => (
        <NxTabPanel key={tabConfiguration.tabId} id={`component-details-${tabConfiguration.tabId}-tab-content`}>
          {tabConfiguration.component}
        </NxTabPanel>
      ))}
    </NxTabs>
  );
}

ComponentDetailsTabs.propTypes = {
  // activeTabId should be required but marking it as such causes proptype errors when navigating away
  activeTabId: PropTypes.string,
  onTabChange: PropTypes.func.isRequired,
  tabsConfiguration: PropTypes.arrayOf(
    PropTypes.shape({
      tabId: PropTypes.string.isRequired,
      title: PropTypes.string.isRequired,
      component: PropTypes.element,
    })
  ).isRequired,
};
