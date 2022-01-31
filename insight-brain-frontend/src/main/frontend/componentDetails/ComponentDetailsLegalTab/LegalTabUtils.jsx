/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';
import { NxList, NxThreatIndicator } from '@sonatype/react-shared-components';
import { contains } from 'ramda';

const claimedComponentAlert = (isEffective, len) => {
  if (isEffective) {
    return !len && <span> (Claimed Component)</span>;
  }

  return <span> (Claimed Component)</span>;
};

const renderOneLicense = (licenseDetails) => {
  return (
    <div key={licenseDetails.licenseId} className="license-list-item__license">
      <NxThreatIndicator policyThreatLevel={licenseDetails.threatGroup?.threatLevel} />
      <span>{licenseDetails.licenseName}</span>
    </div>
  );
};

export const renderLicensesList = (list, licenseLegalMetadata, claimed, isEffective = false) =>
  list?.map((licenseKey) => {
    const licenseDetails = (licenseLegalMetadata || []).find((license) => license.licenseId === licenseKey);

    const multiDisplay = (licenseDetails) => {
      let multiDetails = licenseDetails.singleLicenseIds.map((licenseKey) => {
        return (licenseLegalMetadata || []).find((license) => license.licenseId === licenseKey);
      });
      return multiDetails
        .map((licenseDetails) => renderOneLicense(licenseDetails))
        .reduce((prev, curr) => [prev, ' or ', curr]);
    };

    return (
      licenseDetails && (
        <NxList.Item key={licenseKey}>
          <NxList.Text className="license-list-item">
            {licenseDetails.isMulti ? multiDisplay(licenseDetails) : renderOneLicense(licenseDetails)}
            {claimed && claimedComponentAlert(isEffective, list.length)}
          </NxList.Text>
        </NxList.Item>
      )
    );
  });

export const isOverriddenOrSelected = (status) => contains(status, ['SELECTED', 'OVERRIDDEN']);
