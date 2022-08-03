/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';
import PropTypes from 'prop-types';
import { NxTable } from '@sonatype/react-shared-components';
import PolicyViolationsTableRow from './PolicyViolationsTableRow';
import PolicyViolationPropType from '../QuarantinedComponentReport';

export default function PolicyViolationsTable({ violations }) {
  return (
    <NxTable className="iq-policy-violations-table">
      <NxTable.Head>
        <NxTable.Row>
          <NxTable.Cell>THREAT</NxTable.Cell>
          <NxTable.Cell>POLICY</NxTable.Cell>
          <NxTable.Cell>CONSTRAINT NAME</NxTable.Cell>
          <NxTable.Cell>CONDITION</NxTable.Cell>
          <NxTable.Cell chevron />
        </NxTable.Row>
      </NxTable.Head>
      <NxTable.Body emptyMessage="No policy violations">
        {violations.map((violation, index) => (
          <PolicyViolationsTableRow key={index} violation={violation} />
        ))}
      </NxTable.Body>
    </NxTable>
  );
}

PolicyViolationsTable.propTypes = {
  violations: PropTypes.arrayOf(PropTypes.shape({ ...PolicyViolationPropType })),
};
