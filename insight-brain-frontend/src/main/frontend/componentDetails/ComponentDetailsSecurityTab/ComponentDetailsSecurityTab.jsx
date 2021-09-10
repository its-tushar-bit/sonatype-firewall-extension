/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';
import { ViolationsTableTileContainer } from '../ViolationsTableTile/ViolationsTableTileContainer';
import { policyTypes } from '../../dashboard/filter/staticFilterEntries';

export default function Security() {
  const SECURITY = policyTypes[0].id;
  return <ViolationsTableTileContainer title="Security Violations" violationType={SECURITY} />;
}
