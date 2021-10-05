/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React, { Fragment } from 'react';
import { ViolationsTableTileContainer } from '../ViolationsTableTile/ViolationsTableTileContainer';
import { policyTypes } from '../../dashboard/filter/staticFilterEntries';
import { LicenseDetectionsTileContainer } from '../LicenseDetectionsTile';
import EditLicensesPopoverContainer from './EditLicensesPopover/EditLicensesPopoverContainer';

export default function ComponentDetailsLegalTab() {
  const LEGAL = policyTypes[1].id;
  return (
    <Fragment>
      <LicenseDetectionsTileContainer />
      <ViolationsTableTileContainer title="Legal Policy Violations" violationType={LEGAL} />
      <EditLicensesPopoverContainer />
    </Fragment>
  );
}
