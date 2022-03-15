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

const renderOneLicense = (licenseItem) => {
  return (
    <div key={licenseItem.license.licenseId} className="license-list-item__license">
      <NxThreatIndicator policyThreatLevel={licenseItem.threatLevel} />
      <span>{licenseItem.license.licenseName}</span>
    </div>
  );
};

export const renderLicensesList = (list, claimed, isEffective = false) =>
  list?.map((item) => {
    const { licenses } = item;
    const licenseKey = licenses.map((licenseItem) => licenseItem.license.licenseId).join();

    const multiDisplay = (multiLicenses) => {
      return multiLicenses
        .map((licenseItem) => renderOneLicense(licenseItem))
        .reduce((prev, curr) => [prev, ' or ', curr]);
    };

    return (
      <NxList.Item key={licenseKey}>
        <NxList.Text className="license-list-item">
          {licenses.length > 1 ? multiDisplay(licenses) : renderOneLicense(licenses[0])}
          {claimed && claimedComponentAlert(isEffective, list.length)}
        </NxList.Text>
      </NxList.Item>
    );
  });

export const isOverriddenOrSelected = (status) => contains(status, ['SELECTED', 'OVERRIDDEN']);
