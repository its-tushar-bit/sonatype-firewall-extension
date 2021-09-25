/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React, { Fragment } from 'react';
import { ViolationsTableTileContainer } from '../ViolationsTableTile/ViolationsTableTileContainer';
import { policyTypes } from '../../dashboard/filter/staticFilterEntries';
import { VulnerabilitiesTableTileContainer } from '../VulnerabilitiesTableTile/VulnerabilitiesTableTileContainer';

export default function ComponentDetailsSecurityTab() {
  const SECURITY = policyTypes[0].id;
  return (
    <Fragment>
      <ViolationsTableTileContainer title="Security Violations" violationType={SECURITY} />
      <VulnerabilitiesTableTileContainer />
    </Fragment>
  );
}
