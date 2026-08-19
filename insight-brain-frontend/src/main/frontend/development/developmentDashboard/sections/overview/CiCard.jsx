/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

import React from 'react';
import { NxCard, NxH3, NxTextLink } from '@sonatype/react-shared-components';
import { SECTIONS } from 'MainRoot/development/developmentDashboard/sections';
import useGetIntegrationsLink from 'MainRoot/development/developmentDashboard/useGetIntegrationsLink';

export default function CiCard() {
  const ciUrl = useGetIntegrationsLink(SECTIONS.CICD);
  return (
    <NxCard className="iq-integrations-card-cicd nx-card--equal" aria-label="Sync CI With Sonatype Developer">
      <NxCard.Header>
        <NxH3>Sync CI With Sonatype Developer</NxH3>
      </NxCard.Header>
      <NxCard.Content>
        <NxCard.Text>
          Integrate with your CI/CD pipelines to perform binary scanning at multiple stages. The scan reports generated
          at each stage give an accurate assessment of your open source risk and prevent deployment delays.
        </NxCard.Text>
      </NxCard.Content>
      <NxCard.Footer>
        <NxTextLink href={ciUrl} data-analytics-id="sonatype-developer-overview-ci-integrations">
          Learn more about our CI systems integrations
        </NxTextLink>
      </NxCard.Footer>
    </NxCard>
  );
}
