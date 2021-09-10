/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';
import * as PropTypes from 'prop-types';
import { NxTable, NxTableBody, NxTableCell, NxTableHead, NxTableRow } from '@sonatype/react-shared-components';

import PolicyViolationsTableRow, { violationPropTypes } from './PolicyViolationsTableRow';

export default function PolicyViolationsTable({
  violations,
  error,
  loading,
  retryHandler,
  toggleShowViolationsDetailPopover,
  toggleAddWaiverPopover,
  toggleRequestWaiverPopover,
  hasPermissionToAddWaivers,
  setSelectedPolicyViolationId,
}) {
  return (
    <NxTable className="iq-policy-violations-table">
      <NxTableHead>
        <NxTableRow>
          <NxTableCell>Threat</NxTableCell>
          <NxTableCell>Policy/Action</NxTableCell>
          <NxTableCell>Constraint Name</NxTableCell>
          <NxTableCell>Condition</NxTableCell>
          <NxTableCell />
          <NxTableCell />
        </NxTableRow>
      </NxTableHead>
      <NxTableBody emptyMessage="No policy violations" error={error} isLoading={loading} retryHandler={retryHandler}>
        {violations.map((violation) => (
          <PolicyViolationsTableRow
            key={violation.policyViolationId}
            violation={violation}
            toggleShowViolationsDetailPopover={toggleShowViolationsDetailPopover}
            toggleAddWaiverPopover={toggleAddWaiverPopover}
            toggleRequestWaiverPopover={toggleRequestWaiverPopover}
            hasPermissionToAddWaivers={hasPermissionToAddWaivers}
            setSelectedPolicyViolationId={setSelectedPolicyViolationId}
          />
        ))}
      </NxTableBody>
    </NxTable>
  );
}

PolicyViolationsTable.propTypes = {
  violations: PropTypes.arrayOf(PropTypes.shape(violationPropTypes)),
  error: PropTypes.string,
  loading: PropTypes.bool,
  retryHandler: PropTypes.func,
  toggleShowViolationsDetailPopover: PropTypes.func.isRequired,
  toggleAddWaiverPopover: PropTypes.func.isRequired,
  toggleRequestWaiverPopover: PropTypes.func.isRequired,
  hasPermissionToAddWaivers: PropTypes.bool.isRequired,
  setSelectedPolicyViolationId: PropTypes.func.isRequired,
};
