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
    <NxCard
      className="iq-integrations-card-cicd nx-card--equal"
      aria-label="What are Sonatype CI Integrations used for?"
    >
      <NxCard.Header>
        <NxH3>What are Sonatype CI Integrations used for?</NxH3>
      </NxCard.Header>
      <NxCard.Content>
        <NxCard.Text>
          Lifecycle's CI integrations perform{' '}
          <NxTextLink
            external
            href="https://links.sonatype.com/products/nxiq/doc/integrations/overrides/cicd/ABFAdvancedBinaryFingerprinting"
            data-analytics-id="sonatype-developer-overview-binary-scanning"
          >
            binary scanning
          </NxTextLink>{' '}
          at multiple stages of your deployment. You can reduce disruption by warning of risk during the CI build while
          blocking critical issues from automatically deploying to production.
        </NxCard.Text>

        <NxCard.Text>
          Analyzing manifests alone could miss changes made during a build and end up in production. Sonatype's binary
          fingerprint scanning, run during your CI builds, provides a more accurate assessment of open-source risk.
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
