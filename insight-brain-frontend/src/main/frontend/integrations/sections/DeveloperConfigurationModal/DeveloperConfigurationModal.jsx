/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import {
  NxButton,
  NxFooter,
  NxH1,
  NxModal,
  NxTab,
  NxTabList,
  NxTabPanel,
  NxTabs,
} from '@sonatype/react-shared-components';
import PropTypes from 'prop-types';
import React, { useState } from 'react';
import './DeveloperConfigurationModal.scss';

export default function DeveloperConfigurationModal({ title, tabs, showModal, onClose }) {
  const [activeTabId, setActiveTabId] = useState(0);

  return (
    <>
      <div>
        {showModal && (
          <NxModal id="iq-integrations-developer-configuration-modal" onCancel={onClose}>
            <NxH1 className="iq-integrations-developer-configuration-modal-title">{title}</NxH1>
            <NxModal.Content className="nx-modal-content--tabs">
              <NxTabs activeTab={activeTabId} onTabSelect={setActiveTabId}>
                <NxTabList>
                  {tabs.map((tab, index) => (
                    <NxTab key={index} className="iq-integrations-developer-configuration-tab">
                      {tab.name}
                    </NxTab>
                  ))}
                </NxTabList>
                {tabs.map((tab) => (
                  <NxTabPanel key={tab.name} className="iq-integrations-developer-configuration-tab-panel">
                    {tab.component}
                  </NxTabPanel>
                ))}
              </NxTabs>
            </NxModal.Content>
            <NxFooter>
              <div className="nx-btn-bar">
                <NxButton onClick={onClose}>Close</NxButton>
              </div>
            </NxFooter>
          </NxModal>
        )}
      </div>
    </>
  );
}

//util function for creating tab configuration objects
export const createTabConfiguration = (name, component) => ({
  name,
  component,
});

DeveloperConfigurationModal.propTypes = {
  title: PropTypes.string.isRequired,
  tabs: PropTypes.arrayOf(
    PropTypes.shape({
      name: PropTypes.string.isRequired,
      component: PropTypes.element,
    })
  ).isRequired,
  showModal: PropTypes.bool.isRequired,
  onClose: PropTypes.func.isRequired,
};
