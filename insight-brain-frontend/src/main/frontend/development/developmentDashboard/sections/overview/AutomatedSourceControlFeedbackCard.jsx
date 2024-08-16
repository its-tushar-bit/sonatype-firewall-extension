/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

import React from 'react';
import { NxCard, NxH3, NxTextLink } from '@sonatype/react-shared-components';
import { SECTIONS } from 'MainRoot/development/developmentDashboard/sections';
import useGetIntegrationsLink from 'MainRoot/development/developmentDashboard/useGetIntegrationsLink';

export default function AutomatedSourceControlFeedbackCard() {
  const scmHref = useGetIntegrationsLink(SECTIONS.SCM);
  return (
    <NxCard className="iq-integrations-card-ascf nx-card--equal" aria-label="SCM Feedback">
      <NxCard.Header>
        <NxH3>Sync with SCM</NxH3>
      </NxCard.Header>
      <NxCard.Content>
        <NxCard.Text>
          Integrate with your SCM environment to monitor your codebase and detect policy violations. Get automatic
          feedback during the development process by enabling Automated Source Control Feedback features.
        </NxCard.Text>
      </NxCard.Content>
      <NxCard.Footer>
        <NxTextLink
          data-analytics-id="sonatype-development-dashboard-overview-scm-feedback-tile-learn-more-link"
          href={scmHref}
        >
          Learn more about our SCM integrations
        </NxTextLink>
      </NxCard.Footer>
    </NxCard>
  );
}
