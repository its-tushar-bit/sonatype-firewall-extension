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
        <NxH3>SCM Feedback</NxH3>
      </NxCard.Header>
      <NxCard.Content>
        <NxCard.Text>
          Inform Developers of unnecessary risk as they introduce it. Enable{' '}
          <NxTextLink href="https://links.sonatype.com/products/nxiq/doc/integrations/scm/automatic-source-control-feedback">
            Automated Feedback
          </NxTextLink>{' '}
          by turning on &quot;Pull Request Commenting&quot; and &quot;Automated Commit Feedback&quot;.
        </NxCard.Text>

        <NxCard.Text>
          We recommend this combination of settings to help developers get the most out of Lifecycle.
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
