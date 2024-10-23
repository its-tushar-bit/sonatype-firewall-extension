/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

import React from 'react';
import { NxH1, NxP, NxPageMain, NxPageTitle } from '@sonatype/react-shared-components';

import SbomApplicationsTable from './sbomApplicationsTable/SbomApplicationsTable';

export default function SbomApplicationsPage() {
  return (
    <NxPageMain id="sbom-manager-applications-page" className="sbom-manager-applications-page">
      <NxPageTitle>
        <NxPageTitle.Headings>
          <NxH1>Applications</NxH1>
        </NxPageTitle.Headings>
        <NxPageTitle.Description>
          <NxP>
            List of all applications along with their latest SBOM versions. Easily track and manage your applications
            and SBOMs from one location.
          </NxP>
        </NxPageTitle.Description>
      </NxPageTitle>
      <div className="sbom-manager-applications-page__content">
        <SbomApplicationsTable />
      </div>
    </NxPageMain>
  );
}
