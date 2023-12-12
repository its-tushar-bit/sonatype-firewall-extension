/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';
import { NxH1, NxPageTitle, NxTableCell, NxTableHead, NxTableRow, NxTile } from '@sonatype/react-shared-components';

export default function SastScanFindingsHeader() {
  return (
    <NxTableHead>
      <NxTableRow>
        <NxTableCell>Findings</NxTableCell>
        <NxTableCell chevron />
      </NxTableRow>
    </NxTableHead>
  );
}
