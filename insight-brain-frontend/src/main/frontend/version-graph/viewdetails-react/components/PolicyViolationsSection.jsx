/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';
import PropTypes from 'prop-types';
import { NxTable, NxThreatIndicator } from '@sonatype/react-shared-components';

/**
 * Component for displaying policy violations
 */
export default function PolicyViolationsSection({ policyAlerts }) {
  return (
    <NxTable caption="Policy Violations">
      <NxTable.Head>
        <NxTable.Row>
          <NxTable.Cell>Policy</NxTable.Cell>
          <NxTable.Cell>Constraint</NxTable.Cell>
          <NxTable.Cell>Summary</NxTable.Cell>
        </NxTable.Row>
      </NxTable.Head>
      <NxTable.Body emptyMessage="None">
        {policyAlerts.map((issue, index) => {
          return (
            <NxTable.Row key={index}>
              <NxTable.Cell>
                <NxThreatIndicator policyThreatLevel={issue.threatLevel} />
                <span>{issue.policyName}</span>
              </NxTable.Cell>
              <NxTable.Cell>{issue.constraintName}</NxTable.Cell>
              <NxTable.Cell>{issue.reason}</NxTable.Cell>
            </NxTable.Row>
          );
        })}
      </NxTable.Body>
    </NxTable>
  );
}

PolicyViolationsSection.propTypes = {
  policyAlerts: PropTypes.arrayOf(
    PropTypes.shape({
      threatLevel: PropTypes.number,
      policyName: PropTypes.string,
      constraintName: PropTypes.string,
      reason: PropTypes.string,
    })
  ),
};
