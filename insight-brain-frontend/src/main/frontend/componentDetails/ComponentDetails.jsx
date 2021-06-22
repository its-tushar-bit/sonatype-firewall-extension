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
import AuditLogContainer from './auditLog/AuditLogContainer';
import LoadError from '../react/LoadError';
import {
  ComponentDetailsHeader,
  ComponentDetailsReportInfo,
  ComponentDetailsTags,
  componentDetailsTagsPropTypes,
  Title,
} from './ComponentDetailsHeader';
import { ComponentDetailsFooter, propTypes as footerPropTypes } from './ComponentDetailsFooter';
import { PolicyViolationsContainer } from './violations';

const tabIdPerIndex = ['remediation', 'info', 'violations', 'security', 'legal', 'audit'];

export default function ComponentDetails({
  componentDetails,
  activeTabId,
  onTabChange,
  pagination,
  loadComponentDetails,
  applicationReportLoadError,
}) {
  const uiRouterState = useRouterState();

  useEffect(() => {
    if (!componentDetails && !applicationReportLoadError) {
      loadComponentDetails();
    }
  }, [componentDetails, applicationReportLoadError]);

  // bail out early if no component details (still show back button)
  if (!componentDetails) {
    return (
      <main className="nx-page-main nx-viewport-sized" id="component-details-page">
        <BackButton stateName="applicationReport.policy" $state={uiRouterState} />
        <LoadError
          error={applicationReportLoadError || 'Error getting component details.'}
          retryHandler={loadComponentDetails}
        />
      </main>
    );
  }

  const handleTabChange = (tabIndex) => {
    const tabIdToMoveTo = tabIdPerIndex[tabIndex];
    if (tabIdToMoveTo === activeTabId) {
      return;
    }
    onTabChange(tabIdToMoveTo);
  };

  const { name, metadata, format, dependencyType, isInnerSource, labels } = componentDetails;

  return (
    <main className="nx-page-main nx-viewport-sized" id="component-details-page">
      <section className="nx-viewport-sized__container">
        <div className="nx-scrollable nx-table-container nx-viewport-sized__scrollable">
          <div className="iq-component-details__content--lateral-padding">
            <BackButton stateName="applicationReport.policy" $state={uiRouterState} />
          </div>
          <ComponentDetailsHeader className="iq-component-details-page__header">
            <Title id="component-details-title">{name}</Title>
            <ComponentDetailsReportInfo {...metadata} />
            <ComponentDetailsTags
              format={format}
              dependencyType={dependencyType}
              isInnerSource={isInnerSource}
              labels={labels}
            />
          </ComponentDetailsHeader>

          <div className="iq-component-details__content--lateral-padding">
            <NxStatefulTabs defaultActiveTab={tabIdPerIndex.indexOf(activeTabId)} onTabSelect={handleTabChange}>
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
                <PolicyViolationsContainer />
              </NxTabPanel>
              <NxTabPanel>
                <PlaceholderTabContent tabIndex={3}>Security</PlaceholderTabContent>
              </NxTabPanel>
              <NxTabPanel>
                <PlaceholderTabContent tabIndex={4}>Legal</PlaceholderTabContent>
              </NxTabPanel>
              <NxTabPanel id="audit-log-tab-content">
                <AuditLogContainer />
              </NxTabPanel>
            </NxStatefulTabs>
          </div>

          <div className="nx-table-container__footer">{pagination && <ComponentDetailsFooter {...pagination} />}</div>
        </div>
      </section>
    </main>
  );
}

ComponentDetails.propTypes = {
  componentDetails: PropTypes.shape({
    name: PropTypes.string.isRequired,
    hash: PropTypes.string.isRequired,
    format: componentDetailsTagsPropTypes.form,
    dependencyType: componentDetailsTagsPropTypes.dependencyType,
    isInnerSource: componentDetailsTagsPropTypes.isInnerSource,
    labels: componentDetailsTagsPropTypes.label,
    metadata: PropTypes.shape({
      applicationName: PropTypes.string,
      organizationName: PropTypes.string,
      reportTime: PropTypes.number,
      reportTitle: PropTypes.string,
    }),
  }),
  loadComponentDetails: PropTypes.func.isRequired,

  // activeTabId should be required but marking it as such causes proptype errors when navigating away
  activeTabId: PropTypes.string,
  onTabChange: PropTypes.func.isRequired,
  applicationReportLoadError: PropTypes.string,
  pagination: PropTypes.shape(footerPropTypes),
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
