/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

import React from 'react';
import { NxCard, NxH2, NxP, NxTextLink } from '@sonatype/react-shared-components';
import JenkinsLogo from 'MainRoot/img/third-party-logos/jenkins_small.png';
import AzureDevOpsLogo from 'MainRoot/img/third-party-logos/azure_small.png';
import BambooLogo from 'MainRoot/img/third-party-logos/bamboo_small.png';
import GitHubLogo from 'MainRoot/img/third-party-logos/github_small.png';
import GitLabLogo from 'MainRoot/img/third-party-logos/gitlab_small.png';
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
          description="Sonatype for Azure DevOps evaluates pipeline builds for all supported component types and presents policy results and widgets within Azure DevOps."
          linkText="Click here for installation help."
          linkUrl="https://help.sonatype.com/en/sonatype-for-azure-devops.html"
          dataAnalyticsId="sonatype-developer-cicd-azure-devops"
        />
        <IntegrationsCard
          title="Bamboo Data Center"
          imgUrl={BambooLogo}
          description="Sonatype for Bamboo Data Center integrates with Atlassian Bamboo to run policy evaluations and display scan results within the build workspace."
          linkText="Click here for installation help."
          linkUrl="https://help.sonatype.com/en/sonatype-for-bamboo-data-center.html"
          dataAnalyticsId="sonatype-developer-cicd-bamboo"
        />
        <IntegrationsCard
          title="GitHub Actions"
          imgUrl={GitHubLogo}
          description="Sonatype GitHub Actions provides a set of actions for interacting with different Sonatype products directly within your GitHub workflows."
          linkText="Click here for installation help."
          linkUrl="https://help.sonatype.com/en/sonatype-github-actions.html"
          dataAnalyticsId="sonatype-developer-cicd-github"
        />
        <IntegrationsCard
          title="GitLab CI"
          imgUrl={GitLabLogo}
          description="Sonatype for GitLab CI is packaged as a Docker image that allows you to perform policy evaluations against one or more build artifacts during a GitLab CI/CD pipeline run."
          linkText="Click here for installation help."
          linkUrl="https://help.sonatype.com/en/sonatype-for-gitlab-ci.html"
          dataAnalyticsId="sonatype-developer-cicd-gitlab"
        />
        <IntegrationsCard
          title="Jenkins 2.x"
          imgUrl={JenkinsLogo}
          description="Sonatype for Jenkins provides full component intelligence and the ability to run policy against your application build."
          linkText="Click here for installation help."
          linkUrl="https://help.sonatype.com/en/sonatype-platform-plugin-for-jenkins.html"
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
