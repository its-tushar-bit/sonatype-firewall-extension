/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';
import { sort } from 'ramda';
import { NxTable, NxTableBody, NxTableCell, NxTableHead, NxTableRow } from '@sonatype/react-shared-components';
import FirewallPolicyViolationsTableRow from './FirewallPolicyViolationsTableRow';

// exporting function for testing
export const sortPolicyByThreat = sort((threatA, threatB) => {
  return threatB.threatLevel - threatA.threatLevel || threatA.policyName.localeCompare(threatB.policyName);
});

export default function FirewallPolicyViolationsTable({ violations }) {
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
          <FirewallPolicyViolationsTableRow key={index} violation={violation} />
        ))}
      </NxTableBody>
    </NxTable>
  );
}
