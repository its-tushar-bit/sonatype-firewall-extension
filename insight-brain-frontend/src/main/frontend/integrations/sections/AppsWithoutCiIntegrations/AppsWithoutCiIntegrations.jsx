/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

import { NxH2, NxTextLink, NxTile } from '@sonatype/react-shared-components';
import AppsWithoutCiIntegrationsTable from './AppsWithoutCiIntegrationsTable';
import React from 'react';
import { useRouterState } from 'MainRoot/react/RouterStateContext';
import { SECTIONS } from 'MainRoot/integrations/integrations.module';

export default function AppsWithoutCiIntegrations() {
  const uiRouterState = useRouterState();
  const pathToCICD = uiRouterState.href(`integrations.${SECTIONS.CICD}`);

  return (
    <div id="iq-integrations-apps-without-ci-integrations-section">
      <NxTile>
        <NxH2>Apps without CI System Integrations</NxH2>
        <NxTile.Content>
          Learn more about our <NxTextLink href={pathToCICD}>CI System Integration Plugins</NxTextLink> in details.
        </NxTile.Content>
        <AppsWithoutCiIntegrationsTable />
      </NxTile>
    </div>
  );
}
