/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

import React from 'react';
import { NxCard, NxH2, NxP, NxTextLink } from '@sonatype/react-shared-components';
import JenkinsLogo from 'MainRoot/img/third-party-logos/Jenkins.png';
import AzureDevOpsLogo from 'MainRoot/img/third-party-logos/AzureDevOps.png';
import IntegrationsCard from 'MainRoot/development/developmentDashboard/IntegrationsCard';

export default function CiCd() {
  return (
    <div id="iq-integrations-cicd-section">
      <NxH2>CI/CD Integrations</NxH2>
      <NxP className="iq-integrations__full-width-text">
        Integrate your DevSecOps SDLC pipeline with Lifecycle using Sonatype integrations plug-ins for complete security
        orchestration, automation and response. You can view the security risks and vulnerabilities for your
        applications and determine steps for remediation.
      </NxP>

      <NxCard.Container className="iq-integrations-card-container">
        <IntegrationsCard
          title="Azure DevOps"
          imgUrl={AzureDevOpsLogo}
          description="Nexus IQ for Azure DevOps evaluates pipeline builds for all supported component types and presents policy results and widgets within Azure DevOps."
          linkText="Click here for installation help."
          linkUrl="https://links.sonatype.com/products/nxiq/doc/nexus-iq-for-azure-devops"
          dataAnalyticsId="sonatype-developer-cicd-azure-devops"
        />

        <IntegrationsCard
          title="Plugin for Jenkins 2.x"
          imgUrl={JenkinsLogo}
          description="Nexus IQ for Jenkins 2.x plugin provides full component intelligence and the ability to run policy against your application."
          linkText="Click here for installation help."
          linkUrl="https://links.sonatype.com/products/nxiq/doc/nexus-platform-plugin-for-jenkins"
          dataAnalyticsId="sonatype-developer-cicd-jenkins"
        />
      </NxCard.Container>

      <NxP className="iq-integrations__full-width-text">
        If you’re looking to create build automation on a CI system that is not listed above consider using our{' '}
        <NxTextLink
          external
          href="https://help.sonatype.com/iqserver/integrations/nexus-iq-cli"
          data-analytics-id="sonatype-developer-iq-cli"
        >
          IQ CLI
        </NxTextLink>{' '}
        → with different flavors tailored to how your team builds software.
      </NxP>
    </div>
  );
}
