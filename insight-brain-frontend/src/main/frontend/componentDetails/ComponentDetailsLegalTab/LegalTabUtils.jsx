/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React, { Fragment } from 'react';
import { NxThreatIndicator } from '@sonatype/react-shared-components';
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
      return multiLicenses.map((licenseItem, index) => (
        <Fragment key={index}>
          {index !== 0 && <span> or </span>}
          <span className="license-list-item__license">
            <NxThreatIndicator policyThreatLevel={licenseItem.threatLevel} />
            <span>{licenseItem.license.licenseName}</span>
          </span>
        </Fragment>
      ));
    };

    return (
      <li className="iq-legal-item" key={licenseKey}>
        <span className="license-list-item">
          {licenses.length > 1 ? multiDisplay(licenses) : renderOneLicense(licenses[0])}
          {claimed && claimedComponentAlert(isEffective, list.length)}
        </span>
      </li>
    );
  });

export const isOverriddenOrSelected = (status) => contains(status, ['SELECTED', 'OVERRIDDEN']);
