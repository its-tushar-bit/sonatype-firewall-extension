/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React, { Fragment, useEffect } from 'react';
import * as PropTypes from 'prop-types';
import {
  NxLoadError,
  NxLoadingSpinner,
  NxTabs,
  NxTab,
  NxTabList,
  NxTabPanel,
  NxWarningAlert,
  NxButton,
} from '@sonatype/react-shared-components';

import AuditLogContainer from './auditLog/AuditLogContainer';
import {
  ComponentDetailsHeader,
  ComponentDetailsReportInfo,
  ComponentDetailsTags,
  componentDetailsTagsPropTypes,
  Title,
} from './ComponentDetailsHeader';
import { ComponentDetailsFooter, ComponentDetailsFooterPropTypes as footerPropTypes } from './ComponentDetailsFooter';
import { OverviewContainer } from './overview';
import MenuBarBackButton from '../mainHeader/MenuBar/MenuBarBackButton';
import PolicyViolations from './PolicyViolations/PolicyViolations';
import ComponentDetailsSecurityTab from './ComponentDetailsSecurityTab/ComponentDetailsSecurityTab';
import ComponentDetailsLegalTab from './ComponentDetailsLegalTab/ComponentDetailsLegalTab';
import ManageComponentLabelsContainer from './ManageComponentLabels/ManageComponentLabelsContainer';

export default function ComponentDetails({
  componentDetails,
  activeTabId,
  onTabChange,
  backToOffspringOnClick,
  pagination,
  loadComponentDetails,
  loadError,
  loading,
}) {
  const customError = loadError || 'Error getting component details.';

  useEffect(() => {
    loadComponentDetails();
  }, []);

  // bail out early if no component details (still show back button)
  if (loadError || loading || !componentDetails) {
    return (
      <main className="nx-page-main nx-viewport-sized iq-component-details-page iq-component-details-page--loading">
        <MenuBarBackButton stateName="applicationReport.policy" />
        <div className="iq-component-details-page__error">
          {loading ? <NxLoadingSpinner /> : <NxLoadError error={customError} retryHandler={loadComponentDetails} />}
        </div>
      </main>
    );
  }

  const {
    name,
    metadata,
    format,
    dependencyType,
    isInnerSource,
    labels,
    matchState,
    identificationSource,
  } = componentDetails;
  const isUnknown = !matchState || matchState === 'unknown';
  const isClaimed = identificationSource === 'Manual';

  const getTabIdPerIndex = () => {
    if (isUnknown) {
      return ['overview', 'violations', 'claim'];
    }

    return isClaimed
      ? ['overview', 'violations', 'security', 'legal', 'labels', 'claim', 'audit']
      : ['overview', 'violations', 'security', 'legal', 'labels', 'audit'];
  };

  const tabIdPerIndex = getTabIdPerIndex();

  const handleTabChange = (tabIndex) => {
    const tabIdToMoveTo = tabIdPerIndex[tabIndex];
    if (tabIdToMoveTo === activeTabId) {
      return;
    }
    onTabChange(tabIdToMoveTo);
  };

  const unknownComponentAlert = (
    <NxWarningAlert className="iq-component-details-unknown-component-alert">
      The component is unknown.
      <NxButton
        id="iq-component-details-unknown-component-claim"
        onClick={() => {
          handleTabChange(tabIdPerIndex.indexOf('claim'));
        }}
        variant="secondary"
        title="Claim Component"
      >
        Claim Component
      </NxButton>
      <NxButton onClick={() => {}} variant="primary" title="Add Proprietary Component Matchers">
        Add Proprietary Component Matchers
      </NxButton>
    </NxWarningAlert>
  );

  const generateContent = (isUnknown, isClaimed) => {
    return (
      <div>
        {isUnknown && unknownComponentAlert}
        <NxTabs activeTab={tabIdPerIndex.indexOf(activeTabId)} onTabSelect={handleTabChange}>
          <NxTabList aria-label="Component detail tabs">
            <NxTab>Overview</NxTab>
            <NxTab>Policy Violations</NxTab>
            {!isUnknown && <NxTab>Security</NxTab>}
            {!isUnknown && <NxTab>Legal</NxTab>}
            {!isUnknown && <NxTab>Labels</NxTab>}
            {(isClaimed || isUnknown) && <NxTab>Claim</NxTab>}
            {!isUnknown && <NxTab>Audit Log</NxTab>}
          </NxTabList>
          <NxTabPanel id="component-details-overview-tab-content">
            <OverviewContainer />
          </NxTabPanel>
          <NxTabPanel id="component-details-policy-violations">
            <PolicyViolations />
          </NxTabPanel>
          {!isUnknown && (
            <NxTabPanel id="component-details-security-tab-content">
              <ComponentDetailsSecurityTab />
            </NxTabPanel>
          )}
          {!isUnknown && (
            <NxTabPanel id="component-details-legal-tab-content">
              <ComponentDetailsLegalTab />
            </NxTabPanel>
          )}
          {!isUnknown && (
            <NxTabPanel id="manage-component-labels">
              <ManageComponentLabelsContainer />
            </NxTabPanel>
          )}
          {(isClaimed || isUnknown) && (
            <NxTabPanel id="component-details-claim-unknown-component">
              <h2>TODO</h2>
            </NxTabPanel>
          )}
          {!isUnknown && (
            <NxTabPanel id="audit-log-tab-content">
              <AuditLogContainer />
            </NxTabPanel>
          )}
        </NxTabs>
      </div>
    );
  };

  return (
    <main className="nx-page-main nx-viewport-sized iq-component-details-page">
      <div className="nx-viewport-sized__scrollable nx-scrollable iq-component-details-page__content">
        <MenuBarBackButton stateName="applicationReport.policy" />
        <ComponentDetailsHeader>
          <Title id="component-details-title">{name}</Title>
          <ComponentDetailsReportInfo {...metadata} />
          <ComponentDetailsTags
            format={format}
            dependencyType={dependencyType}
            isInnerSource={isInnerSource}
            labels={labels}
          />
        </ComponentDetailsHeader>
        {generateContent(isUnknown, isClaimed)}
      </div>
      {pagination && <ComponentDetailsFooter {...pagination} backToOffspringOnClick={backToOffspringOnClick} />}
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
    matchState: PropTypes.string,
    identificationSource: PropTypes.string,
  }),
  loadComponentDetails: PropTypes.func.isRequired,

  // activeTabId should be required but marking it as such causes proptype errors when navigating away
  activeTabId: PropTypes.string,
  onTabChange: PropTypes.func.isRequired,
  backToOffspringOnClick: PropTypes.func.isRequired,
  loading: PropTypes.bool.isRequired,
  loadError: PropTypes.string,
  pagination: PropTypes.shape(footerPropTypes),
};
