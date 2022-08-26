/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';
import * as PropTypes from 'prop-types';
import { sort } from 'ramda';
import { NxTable, NxTableBody, NxTableCell, NxTableHead, NxTableRow } from '@sonatype/react-shared-components';
import FirewallPolicyViolationsTableRow from './FirewallPolicyViolationsTableRow';

// exporting function for testing
export const sortPolicyByThreat = sort((threatA, threatB) => {
  return threatB.policyThreatLevel - threatA.policyThreatLevel || threatA.policyName.localeCompare(threatB.policyName);
});

export default function FirewallPolicyViolationsTable({ violations, showProxyState = false }) {
  const orderedViolations = violations ? sortPolicyByThreat(violations) : [];

  return (
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
          <FirewallPolicyViolationsTableRow key={index} violation={violation} showProxyState={showProxyState} />
        ))}
      </NxTableBody>
    </NxTable>
  );
}

FirewallPolicyViolationsTable.propTypes = {
  violations: PropTypes.arrayOf(FirewallPolicyViolationsTableRow.propTypes.violation),
  showProxyState: FirewallPolicyViolationsTableRow.propTypes.showProxyState,
};
