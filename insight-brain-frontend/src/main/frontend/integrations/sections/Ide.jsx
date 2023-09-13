/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

import { NxH2, NxCard, NxP } from '@sonatype/react-shared-components';
import React from 'react';
import IntegrationsCard from 'MainRoot/integrations/IntegrationsCard';
import IdeaLogo from '../../img/third-party-logos/idea.png';
import EclipseLogo from '../../img/third-party-logos/eclipse.png';
import VisualStudioLogo from '../../img/third-party-logos/VisualStudio.png';

export default function Ide() {
  return (
    <div id="iq-integrations-ide-section">
      <NxP className="iq-integrations__full-width-text">
        Integrate your DevSecOps SDLC pipeline with Lifecycle using Sonatype integrations plug-ins for complete security
        orchestration, automation and response. You can view the security risks and vulnerabilities for your
        applications and determine steps for remediation.
      </NxP>

      <NxH2>IDEs</NxH2>

      <NxCard.Container className="iq-integrations-card-container">
        <IntegrationsCard
          title="IQ for IDEA"
          imgUrl={IdeaLogo}
          description="Provides Nexus IQ evaluation of project dependencies right inside IntelliJ IDEA."
          linkText="Click here for installation help."
          linkUrl="https://links.sonatype.com/products/nxiq/doc/integrations/scm/ides/idea"
          dataAnalyticsId="sonatype-developer-ide-idea"
        />

        <IntegrationsCard
          title="IQ for Eclipse"
          imgUrl={EclipseLogo}
          description="Provides Nexus IQ evaluation of project dependencies right inside the Eclipse IDE."
          linkText="Click here for installation help."
          linkUrl="https://links.sonatype.com/products/nxiq/doc/integrations/scm/ides/eclipse"
          dataAnalyticsId="sonatype-developer-ide-eclipse"
        />

        <IntegrationsCard
          title="IQ for Visual Studio"
          imgUrl={VisualStudioLogo}
          description="Visual Studio is a full-featured IDE. IQ for Visual Studio provides component analysis for both the Community, Professional, and Enterprise versions of Visual Studio."
          linkText="Click here for installation help."
          linkUrl="https://links.sonatype.com/products/nxiq/doc/integrations/scm/ides/visual-studio"
          dataAnalyticsId="sonatype-developer-ide-visual-studio"
        />
      </NxCard.Container>
    </div>
  );
}
