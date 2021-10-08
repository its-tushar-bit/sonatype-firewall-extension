/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';
import { NxThreatIndicator } from '@sonatype/react-shared-components';

const licenseListItem = (license) => (
  <div key={license.license?.licenseId} className="license-list-item">
    <NxThreatIndicator policyThreatLevel={license.threatLevel} />
    <span>{license.license?.licenseName}</span>
  </div>
);

export const renderLicensesList = (list) => list?.map((license) => licenseListItem(license));
