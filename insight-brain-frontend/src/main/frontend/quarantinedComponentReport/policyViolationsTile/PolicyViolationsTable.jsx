/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';
import { NxTable } from '@sonatype/react-shared-components';
import PolicyViolationsTableRow from './PolicyViolationsTableRow';

export default function PolicyViolationsTable({ violations }) {
  return (
    <NxTable>
      <NxTable.Head>
        <NxTable.Row>
          <NxTable.Cell>THREAT</NxTable.Cell>
          <NxTable.Cell>POLICY STATUS</NxTable.Cell>
          <NxTable.Cell>CONSTRAINT NAME</NxTable.Cell>
          <NxTable.Cell>CONDITION</NxTable.Cell>
          <NxTable.Cell chevron />
        </NxTable.Row>
      </NxTable.Head>
      <NxTable.Body emptyMessage="No policy violations">
        {violations.activePolicyViolations.map((violation, index) => (
          <PolicyViolationsTableRow key={index} violation={violation} />
        ))}
      </NxTable.Body>
    </NxTable>
  );
}
