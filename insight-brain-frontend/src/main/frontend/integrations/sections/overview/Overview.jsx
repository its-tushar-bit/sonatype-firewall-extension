/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

import React from 'react';
import CiCard from 'MainRoot/integrations/sections/overview/CiCard';
import { NxH2, NxCard } from '@sonatype/react-shared-components';
import IdeIntegrationsCard from './ideIntegrationsCard/IdeIntegrationsCard';
import AppsWithoutScmIntegrations from '../AppsWithoutScmIntegrations/AppsWithoutScmIntegrations';

export default function Overview() {
  return (
    <div id="iq-integrations-overview-section">
      <NxH2>Understanding Your Code Risks</NxH2>

      <CiCard />

      <NxH2>Remediating Your Code Risks</NxH2>
      <AppsWithoutScmIntegrations />

      <NxCard.Container>
        <IdeIntegrationsCard />
      </NxCard.Container>
    </div>
  );
}
