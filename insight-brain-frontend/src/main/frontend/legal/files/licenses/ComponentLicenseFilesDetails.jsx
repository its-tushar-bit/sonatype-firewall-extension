/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';
import './_componentLicenseFilesDetails.scss';
import { UIView } from '@uirouter/react';
import LicenseFilesDetailsHeaderContainer from './LicenseFilesDetailsHeaderContainer';
import LicenseFilesDetailsListContainer from './LicenseFilesDetailsListContainer';

export default function ComponentLicenseFilesDetails() {
  return (
    <main className="nx-page-main nx-viewport-sized">
      <LicenseFilesDetailsHeaderContainer />
      <div id="component-license-details-content" className="legal-details-content nx-viewport-sized__container">
        <LicenseFilesDetailsListContainer />
        <UIView />
      </div>
    </main>
  );
}
