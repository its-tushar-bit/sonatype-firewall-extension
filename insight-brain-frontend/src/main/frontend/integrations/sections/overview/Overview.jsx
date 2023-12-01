/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

import React from 'react';
import { NxCard, NxH2 } from '@sonatype/react-shared-components';
import CiCard from 'MainRoot/integrations/sections/overview/CiCard';
import IdeIntegrationsCard from './ideIntegrationsCard/IdeIntegrationsCard';
import AppIntegrationsAndRiskTable from '../AppIntegrationsAndRiskTable/AppIntegrationsAndRiskTable';
import AdoptionGraph from '../AdoptionGraph/AdoptionGraph';

export default function Overview() {
  return (
    <div id="iq-integrations-overview-section">
      <NxH2>Understanding Your Code Risks</NxH2>

      <AdoptionGraph />

      <AppIntegrationsAndRiskTable />

      <NxCard.Container>
        <CiCard />
        <IdeIntegrationsCard />
      </NxCard.Container>
    </div>
  );
}
