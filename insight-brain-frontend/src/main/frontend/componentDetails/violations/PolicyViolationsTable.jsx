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
  goToWaivers,
  setSelectedViolationId,
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
            goToWaivers={goToWaivers}
            setSelectedViolationId={setSelectedViolationId}
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
  goToWaivers: PropTypes.func,
  setSelectedViolationId: PropTypes.func,
};
