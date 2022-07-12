/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';
import { NxTile, NxH2 } from '@sonatype/react-shared-components';
import FirewallPolicyViolationsTable from './FirewallPolicyViolationsTable';

export default function FirewallPolicyViolationsTile({ title, violations }) {
  return (
    <NxTile>
      <NxTile.Header>
        <NxTile.HeaderTitle>
          <NxH2>{title}</NxH2>
        </NxTile.HeaderTitle>
      </NxTile.Header>
      <NxTile.Content>
        <FirewallPolicyViolationsTable {...{ violations }} />
      </NxTile.Content>
    </NxTile>
  );
}
