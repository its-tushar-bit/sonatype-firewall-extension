/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';
import { NxThreatIndicator } from '@sonatype/react-shared-components';
import { contains } from 'ramda';

const claimedComponentAlert = (isEffective, len) => {
  if (isEffective) {
    return !len && <span> (Claimed Component)</span>;
  }

  return <span> (Claimed Component)</span>;
};

export const renderLicensesList = (list, claimed, isEffective = false) =>
  list?.map((license) => (
    <div key={license.license?.licenseId} className="license-list-item">
      <NxThreatIndicator policyThreatLevel={license.threatLevel} />
      <span>{license.license?.licenseName}</span>
      {claimed && claimedComponentAlert(isEffective, list.length)}
    </div>
  ));

export const isOverriddenOrSelected = (status) => contains(status, ['SELECTED', 'OVERRIDDEN']);
