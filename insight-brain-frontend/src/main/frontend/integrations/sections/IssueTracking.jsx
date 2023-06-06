/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

import { NxCard, NxH2, NxP } from '@sonatype/react-shared-components';
import React from 'react';
import IntegrationsCard from 'MainRoot/integrations/IntegrationsCard';
import AtlassianLogo from '../../img/third-party-logos/atlassian.png';

export default function IssueTracking() {
  return (
    <div id="iq-integrations-issue-tracking-section">
      <NxP className="iq-integrations__full-width-text">
        Integrate your DevSecOps SDLC pipeline with Lifecycle using Sonatype integrations plug-ins for complete security
        orchestration, automation and response. You can view the security risks and vulnerabilities for your
        applications and determine steps for remediation.
      </NxP>

      <NxH2>Issue Management Systems</NxH2>

      <NxCard.Container className="iq-integrations-card-container">
        <IntegrationsCard
          title="Nexus IQ for Jira"
          imgUrl={AtlassianLogo}
          description="Atlassian Jira Server and Datacenter plug-in creating issues in Jira for policy violations."
          linkText="Click here for installation help."
          linkUrl="https://links.sonatype.com/products/nxiq/doc/integrations/scm/issue-tracking/jira"
        />
      </NxCard.Container>
    </div>
  );
}
