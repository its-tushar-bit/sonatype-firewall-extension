/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React, { useState } from 'react';
import * as PropTypes from 'prop-types';
import { sort } from 'ramda';
import { NxTable, NxTableBody, NxTableCell, NxTableHead, NxTableRow } from '@sonatype/react-shared-components';
import FirewallPolicyViolationsTableRow from './FirewallPolicyViolationsTableRow';
import PolicyViolationDetailsPopover from './FirewallPolicyViolationDetailsPopover';

// exporting function for testing
export const sortPolicyByThreat = sort((threatA, threatB) => {
  return threatB.policyThreatLevel - threatA.policyThreatLevel || threatA.policyName.localeCompare(threatB.policyName);
});

export default function FirewallPolicyViolationsTable({ violations, showProxyState = false }) {
  const orderedViolations = violations ? sortPolicyByThreat(violations) : [];
  const [showViolationsDetailPopover, showPopover] = useState(false);
  const [selectPolicyId, savePolicyId] = useState('');

  return (
    <>
      <NxTable className="iq-policy-violations-table firewall-policy-violation-table">
        <NxTableHead>
          <NxTableRow>
            <NxTableCell>Threat</NxTableCell>
            <NxTableCell>Policy/Action</NxTableCell>
            <NxTableCell>Constraint Name</NxTableCell>
            <NxTableCell>Condition</NxTableCell>
            <NxTableCell />
            <NxTableCell chevron />
          </NxTableRow>
        </NxTableHead>
        <NxTableBody emptyMessage="No policy violations">
          {orderedViolations.map((violation, index) => (
            <FirewallPolicyViolationsTableRow
              key={index}
              showPopover={showPopover}
              orderedViolations={orderedViolations}
              violation={violation}
              selectPolicyId={selectPolicyId}
              savePolicyId={savePolicyId}
              showProxyState={showProxyState}
            />
          ))}
        </NxTableBody>
      </NxTable>
      {showViolationsDetailPopover && (
        <PolicyViolationDetailsPopover
          showViolationsDetailPopover={showViolationsDetailPopover}
          selectPolicyId={selectPolicyId}
          savePolicyId={savePolicyId}
          showPopover={showPopover}
        />
      )}
    </>
  );
}

FirewallPolicyViolationsTable.propTypes = {
  violations: PropTypes.arrayOf(FirewallPolicyViolationsTableRow.propTypes.violation),
  showProxyState: FirewallPolicyViolationsTableRow.propTypes.showProxyState,
};
