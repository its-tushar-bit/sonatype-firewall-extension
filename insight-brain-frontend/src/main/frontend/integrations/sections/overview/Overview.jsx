/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

import React from 'react';
import CiCard from 'MainRoot/integrations/sections/overview/CiCard';
import { NxH2, NxCard, NxP } from '@sonatype/react-shared-components';
import IdeIntegrationsCard from './ideIntegrationsCard/IdeIntegrationsCard';
import AppsWithoutScmIntegrations from '../AppsWithoutScmIntegrations/AppsWithoutScmIntegrations';
import IntegrationsAlert from '../../IntegrationsAlert';

export default function Overview() {
  return (
    <div id="iq-integrations-overview-section">
      <IntegrationsAlert />
      <NxP className="iq-integrations__full-width-text">
        <strong>Integrate Sonatype</strong> in your development pipeline to automate open-source risk management, with
        real-time feedback, early in your development process.
      </NxP>
      <NxP className="iq-integrations__full-width-text">
        Sonatype integrations help you take immediate action to avoid surprise compliance issues when changes are pushed
        to production.
      </NxP>

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
