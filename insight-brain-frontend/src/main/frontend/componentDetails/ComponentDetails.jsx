/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React, { useEffect } from 'react';
import * as PropTypes from 'prop-types';
import { NxStatefulTabs, NxTab, NxTabList, NxTabPanel } from '@sonatype/react-shared-components';

import BackButton from '../react/BackButton';
import { useRouterState } from '../react/RouterStateContext';

const tabIdPerIndex = ['remediation', 'info', 'violations', 'security', 'legal', 'audit'];
import {
  ComponentDetailsHeader,
  Title,
  ComponentDetailsReportInfo,
  ComponentDetailsTags,
  propTypes as componentDetailsTagsPropTypes,
} from './ComponentDetailsHeader';

export default function ComponentDetails({
  componentDetails,
  publicId,
  scanId,
  unknownjs,
  tabId,
  hash,
  loadReportAndSelectComponentByHash,
  stateGo,
}) {
  const uiRouterState = useRouterState();

  useEffect(() => {
    if (!componentDetails) {
      loadReportAndSelectComponentByHash(publicId, scanId, hash, unknownjs);
    }
  }, [componentDetails, publicId, scanId, hash, unknownjs]);

  // bail out early if no component details (still show back button)
  if (!componentDetails) {
    return (
      <main className="nx-page-main nx-viewport-sized" id="component-details-page">
        <BackButton stateName="applicationReport.policy" $state={uiRouterState} />
      </main>
    );
  }

  const goToTab = (tabIndex) => {
    const tabIdToMoveTo = tabIdPerIndex[tabIndex];
    if (tabIdToMoveTo === tabId) {
      return;
    }
    stateGo(`applicationReport.componentDetails.${tabIdToMoveTo}`, { hash });
  };

  const {
    name,
    applicationName,
    organizationName,
    reportTime,
    reportTitle,
    format,
    dependencyType,
    isInnerSource,
    labels,
  } = componentDetails;

  return (
    <main className="nx-page-main nx-viewport-sized" id="component-details-page">
      <BackButton stateName="applicationReport.policy" $state={uiRouterState} />
      <div className="nx-viewport-sized__container">
        <ComponentDetailsHeader>
          <Title id="component-details-title">{name}</Title>
          <ComponentDetailsReportInfo
            applicationName={applicationName}
            organizationName={organizationName}
            reportTime={reportTime}
            reportTitle={reportTitle}
          />
          <ComponentDetailsTags
            format={format}
            dependencyType={dependencyType}
            isInnerSource={isInnerSource}
            labels={labels}
          />
        </ComponentDetailsHeader>

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
    </main>
  );
}

ComponentDetails.propTypes = {
  loadReportAndSelectComponentByHash: PropTypes.func.isRequired,
  stateGo: PropTypes.func.isRequired,
  unknownjs: PropTypes.bool,
  tabId: PropTypes.string,
  // the following 3 should be required but marking them as such causes proptype errors when navigating away
  hash: PropTypes.string,
  publicId: PropTypes.string,
  scanId: PropTypes.string,

  componentDetails: PropTypes.shape({
    name: PropTypes.string,
    applicationName: PropTypes.string,
    organizationName: PropTypes.string,
    reportTime: PropTypes.number,
    reportTitle: PropTypes.string,
    format: componentDetailsTagsPropTypes.form,
    dependencyType: componentDetailsTagsPropTypes.dependencyType,
    isInnerSource: componentDetailsTagsPropTypes.isInnerSource,
    labels: componentDetailsTagsPropTypes.label,
  }),
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
