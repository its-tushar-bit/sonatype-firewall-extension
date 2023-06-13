/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

import React from 'react';
import { NxH2, NxCard } from '@sonatype/react-shared-components';
import IdeIntegrationsCard from './ideIntegrationsCard/IdeIntegrationsCard';

export default function Overview() {
  return (
    <div id="iq-integrations-overview-section">
      <NxH2>Overview</NxH2>

      <NxCard.Container>
        <IdeIntegrationsCard />
      </NxCard.Container>
    </div>
  );
}
