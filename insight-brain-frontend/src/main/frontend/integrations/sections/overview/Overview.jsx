/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

import React from 'react';
import CiCard from 'MainRoot/integrations/sections/overview/CiCard';
import { NxH2, NxCard, NxP } from '@sonatype/react-shared-components';
import IdeIntegrationsCard from './ideIntegrationsCard/IdeIntegrationsCard';

export default function Overview() {
  return (
    <div id="iq-integrations-overview-section">
      <NxP className="iq-integrations__full-width-text">
        Integrate Sonatype Developer in your development pipeline to automate open-source risk management, with
        real-time feedback, early in your development process. Sonatype integrations help you take immediate action to
        avoid surprise compliance issues when changes are pushed to production.
      </NxP>

      <NxH2>Understanding Your Code Risks</NxH2>

      <NxCard.Container>
        <CiCard />
      </NxCard.Container>

      <NxCard.Container>
        <IdeIntegrationsCard />
      </NxCard.Container>
    </div>
  );
}
