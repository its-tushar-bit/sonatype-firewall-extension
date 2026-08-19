/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';
import { NxTable, NxThreatIndicator } from '@sonatype/react-shared-components';
import * as PropTypes from 'prop-types';

export default function PolicyViolationsTableRow({ violation }) {
  const { policyThreatLevel, policyName, constraints } = violation;
  const [constraint] = constraints;
  const reasons = constraint.conditions.map((condition) => condition.conditionReason);

  return (
    <NxTable.Row className="iq-quarantine-report-component-violations">
      <NxTable.Cell>
        <NxThreatIndicator policyThreatLevel={policyThreatLevel} />
        <span className="nx-threat-number">{policyThreatLevel}</span>
      </NxTable.Cell>
      <NxTable.Cell className="iq-quarantine-report-component-violations__policy-name">{policyName}</NxTable.Cell>
      <NxTable.Cell>{constraint.constraintName}</NxTable.Cell>
      <NxTable.Cell>
        {reasons?.map((reason, index) => {
          return (
            <p className="nx-p" key={index}>
              {reason}
            </p>
          );
        })}
      </NxTable.Cell>
    </NxTable.Row>
  );
}

export const violationPropTypes = {
  policyThreatLevel: PropTypes.number.isRequired,
  policyName: PropTypes.string.isRequired,
  constraints: PropTypes.arrayOf(
    PropTypes.shape({
      constraintName: PropTypes.string,
      conditions: PropTypes.arrayOf(
        PropTypes.shape({
          conditionReason: PropTypes.string,
        })
      ),
    })
  ),
};

PolicyViolationsTableRow.propTypes = {
  violation: PropTypes.shape(violationPropTypes),
};
