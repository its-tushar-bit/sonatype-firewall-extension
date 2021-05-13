/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React, { useEffect } from 'react';
import * as PropTypes from 'prop-types';
import BackButton from '../react/BackButton';
import { useRouterState } from '../react/RouterStateContext';
import { NxStatefulTabs, NxTab, NxTabList, NxTabPanel } from '@sonatype/react-shared-components';

const tabIdPerIndex = ['remediation', 'info', 'violations', 'security', 'legal', 'audit'];

export default function ComponentDetails({
  selectedComponent,
  publicId,
  scanId,
  unknownjs,
  tabId,
  hash,
  loadReportAndSelectComponentByHash,
  stateGo,
}) {
  const uiRouterState = useRouterState();
  const goToTab = (tabIndex) => {
    const tabIdToMoveTo = tabIdPerIndex[tabIndex];
    if (tabIdToMoveTo === tabId) {
      return;
    }
    stateGo(`applicationReport.componentDetails.${tabIdToMoveTo}`, { hash });
  };

  useEffect(() => {
    if (!selectedComponent) {
      loadReportAndSelectComponentByHash(publicId, scanId, hash, unknownjs);
    }
  }, [selectedComponent, publicId, scanId, hash, unknownjs]);

  return (
    <main className="nx-page-main nx-viewport-sized" id="component-details-page">
      <BackButton stateName="applicationReport.policy" $state={uiRouterState} />
      {selectedComponent && (
        <div className="nx-viewport-sized__container">
          <h1 className="title">{selectedComponent.derivedComponentName}</h1>
          <NxStatefulTabs defaultActiveTab={tabIdPerIndex.indexOf(tabId)} onTabSelect={goToTab}>
            <NxTabList aria-label="Component detail tabs">
              <NxTab>Remediation</NxTab>
              <NxTab>Component Info</NxTab>
              <NxTab>Policy Violations</NxTab>
              <NxTab>Security</NxTab>
              <NxTab>Legal</NxTab>
              <NxTab>Audit Log</NxTab>
            </NxTabList>
            <NxTabPanel>
              <PlaceholderTabContent tabIndex={0}>Remediation</PlaceholderTabContent>
            </NxTabPanel>
            <NxTabPanel>
              <PlaceholderTabContent tabIndex={1}>Component Info</PlaceholderTabContent>
            </NxTabPanel>
            <NxTabPanel>
              <PlaceholderTabContent tabIndex={2}>Policy Violations</PlaceholderTabContent>
            </NxTabPanel>
            <NxTabPanel>
              <PlaceholderTabContent tabIndex={3}>Security</PlaceholderTabContent>
            </NxTabPanel>
            <NxTabPanel>
              <PlaceholderTabContent tabIndex={4}>Legal</PlaceholderTabContent>
            </NxTabPanel>
            <NxTabPanel>
              <PlaceholderTabContent tabIndex={5}>Audit Log</PlaceholderTabContent>
            </NxTabPanel>
          </NxStatefulTabs>
        </div>
      )}
    </main>
  );
}

ComponentDetails.propTypes = {
  loadReportAndSelectComponentByHash: PropTypes.func.isRequired,
  stateGo: PropTypes.func.isRequired,
  selectedComponent: PropTypes.object,
  unknownjs: PropTypes.bool,
  tabId: PropTypes.string,
  // the following 3 should be required but marking them as such causes proptype errors when navigating away
  hash: PropTypes.string,
  publicId: PropTypes.string,
  scanId: PropTypes.string,
};

/*
 * Placeholder component for the tab content, should be deleted after all tabs are implemented.
 */
function PlaceholderTabContent({ tabIndex, children }) {
  return (
    <div>
      <h2>{`Tab #${tabIndex + 1} (index ${tabIndex})`}</h2>
      <span>{children}</span>
    </div>
  );
}

PlaceholderTabContent.propTypes = {
  tabIndex: PropTypes.number.isRequired,
  children: PropTypes.node.isRequired,
};
/* End of placeholder component */
