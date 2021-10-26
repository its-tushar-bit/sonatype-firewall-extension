/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';
import * as PropTypes from 'prop-types';
import { NxTab, NxTabList, NxTabPanel, NxTabs } from '@sonatype/react-shared-components';

import AuditLogContainer from './auditLog/AuditLogContainer';
import { OverviewContainer } from './overview';
import PolicyViolations from './PolicyViolations/PolicyViolations';
import ComponentDetailsSecurityTab from './ComponentDetailsSecurityTab/ComponentDetailsSecurityTab';
import ComponentDetailsLegalTab from './ComponentDetailsLegalTab/ComponentDetailsLegalTab';
import ManageComponentLabelsContainer from './ManageComponentLabels/ManageComponentLabelsContainer';
import { ClaimContainer } from './claim/ClaimContainer';

export default function ComponentDetailsTabs({ activeTab, onTabChange, isUnknown, isClaimed }) {
  return (
    <NxTabs activeTab={activeTab} onTabSelect={onTabChange}>
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
          <ClaimContainer />
        </NxTabPanel>
      )}
      {!isUnknown && (
        <NxTabPanel id="audit-log-tab-content">
          <AuditLogContainer />
        </NxTabPanel>
      )}
    </NxTabs>
  );
}

ComponentDetailsTabs.propTypes = {
  // activeTab should be required but marking it as such causes proptype errors when navigating away
  activeTab: PropTypes.number,
  onTabChange: PropTypes.func.isRequired,
  isUnknown: PropTypes.bool.isRequired,
  isClaimed: PropTypes.bool.isRequired,
};
