/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

import React from 'react';
import { NxCard, NxH2, NxTile } from '@sonatype/react-shared-components';
import CiCard from 'MainRoot/integrations/sections/overview/CiCard';
import IdeIntegrationsCard from './ideIntegrationsCard/IdeIntegrationsCard';
import AppIntegrationsAndRiskTable from '../AppIntegrationsAndRiskTable/AppIntegrationsAndRiskTable';
import GraphsContainer from '../Graphs/GraphsContainer';

export default function Overview() {
  return (
    <div id="iq-integrations-overview-section">
      <NxTile>
        <NxTile.Content>
          <GraphsContainer />
        </NxTile.Content>
      </NxTile>

      <NxH2>Applications Configuration Summary</NxH2>

      <AppIntegrationsAndRiskTable />

      <NxCard.Container>
        <CiCard />
        <IdeIntegrationsCard />
      </NxCard.Container>
    </div>
  );
}
