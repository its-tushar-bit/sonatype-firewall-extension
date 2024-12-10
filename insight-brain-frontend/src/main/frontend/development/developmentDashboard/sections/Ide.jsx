/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

import { NxH2, NxCard, NxP } from '@sonatype/react-shared-components';
import React from 'react';
import IntegrationsCard from 'MainRoot/development/developmentDashboard/IntegrationsCard';
import IdeaLogo from 'MainRoot/img/third-party-logos/idea.png';
import EclipseLogo from 'MainRoot/img/third-party-logos/eclipse_small.png';
import VisualStudioLogo from 'MainRoot/img/third-party-logos/visual_studio_small.png';
import VSCode from 'MainRoot/img/third-party-logos/vs_code_small.png';

export default function Ide() {
  return (
    <div id="iq-integrations-ide-section">
      <NxH2>IDEs</NxH2>
      <NxP className="iq-integrations__full-width-text">
        Integrate your DevSecOps SDLC pipeline with Lifecycle using Sonatype integrations plug-ins for complete security
        orchestration, automation and response. You can view the security risks and vulnerabilities for your
        applications and determine steps for remediation.
      </NxP>
      <NxCard.Container className="iq-integrations-card-container">
        <IntegrationsCard
          title="Eclipse"
          imgUrl={EclipseLogo}
          description="Sonatype Nexus IQ scans open source dependencies for policy violations and security vulnerabilities, providing actionable insights and remediation advice."
          linkText="Click here for installation help."
          linkUrl="https://help.sonatype.com/en/iq-for-eclipse.html"
          dataAnalyticsId="sonatype-developer-ide-eclipse"
        />

        <IntegrationsCard
          title="IDEA"
          imgUrl={IdeaLogo}
          description="Sonatype Nexus IQ evaluates project dependencies right inside IntelliJ IDEA, providing actionable insights and remediation advice."
          linkText="Click here for installation help."
          linkUrl="https://help.sonatype.com/en/iq-for-idea.html"
          dataAnalyticsId="sonatype-developer-ide-idea"
        />

        <IntegrationsCard
          title="Visual Studio 2022"
          imgUrl={VisualStudioLogo}
          description="Sonatype for Visual Studio 2022 provides component analysis for both the Community, Professional, and Enterprise versions of Visual Studio."
          linkText="Click here for installation help."
          linkUrl="https://help.sonatype.com/en/sonatype-for-visual-studio-2022.html"
          dataAnalyticsId="sonatype-developer-ide-visual-studio"
        />

        <IntegrationsCard
          title="VS Code"
          imgUrl={VSCode}
          description="Sonatype for VS Code allows you to surface and remediate issues in your workspace dependencies, a true Shift Left in application security for development teams."
          linkText="Click here for installation help."
          linkUrl="https://help.sonatype.com/en/sonatype-for-vs-code.html"
          dataAnalyticsId="sonatype-developer-ide-vs-code"
        />
      </NxCard.Container>
    </div>
  );
}
