/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

import { NxCard, NxH2, NxP } from '@sonatype/react-shared-components';
import React from 'react';
import IntegrationsCard from 'MainRoot/development/developmentDashboard/IntegrationsCard';
import AtlassianLogo from 'MainRoot/img/third-party-logos/atlassian_small.png';

export default function IssueTracking() {
  return (
    <div id="iq-integrations-issue-tracking-section">
      <NxH2>Issue Management Systems</NxH2>
      <NxP className="iq-integrations__full-width-text">
        Integrate your DevSecOps SDLC pipeline with Lifecycle using Sonatype integrations plug-ins for complete security
        orchestration, automation and response. You can view the security risks and vulnerabilities for your
        applications and determine steps for remediation.
      </NxP>

      <NxCard.Container className="iq-integrations-card-container">
        <IntegrationsCard
          title="Jira Cloud"
          imgUrl={AtlassianLogo}
          description="Sonatype for Jira Cloud automatically creates issues in response to new violations as they occur during your policy evaluation."
          linkText="Click here for installation help."
          linkUrl="https://help.sonatype.com/en/sonatype-for-jira-cloud.html"
          dataAnalyticsId="sonatype-developer-issue-tracking-jira-cloud"
        />
        <IntegrationsCard
          title="Jira Data Center"
          imgUrl={AtlassianLogo}
          description="Sonatype for Jira Data Center is an Atlassian Jira plugin that automates the creation of Jira issues for new policy violations."
          linkText="Click here for installation help."
          linkUrl="https://help.sonatype.com/en/sonatype-for-jira-data-center.html"
          dataAnalyticsId="sonatype-developer-issue-tracking-jira-data-center"
        />
      </NxCard.Container>
    </div>
  );
}
