/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

import React from 'react';
import { NxCard, NxH2, NxP } from '@sonatype/react-shared-components';
import GithubLogo from 'MainRoot/img/third-party-logos/github_small.png';
import GitlabLogo from 'MainRoot/img/third-party-logos/gitlab_small.png';
import BitbucketLogo from 'MainRoot/img/third-party-logos/bitbucket_small.png';
import AzureDevOpsLogo from 'MainRoot/img/third-party-logos/azure_small.png';
import IntegrationsCard from '../IntegrationsCard';

const scmIntegrations = [
  {
    title: 'GitHub',
    imgUrl: GithubLogo,
    description:
      'Lifecycle pushes component intelligence into GitHub where developers can view and respond to policy violations directly in pull requests.',
    linkText: 'Click here for installation help.',
    linkUrl: 'https://help.sonatype.com/en/github-configuration.html',
    dataAnalyticsId: 'sonatype-developer-scm-github',
  },
  {
    title: 'GitLab',
    imgUrl: GitlabLogo,
    description:
      'Lifecycle pushes component intelligence into GitLab where developers can view and respond to policy violations without breaking a build.',
    linkText: 'Click here for installation help.',
    linkUrl: 'https://help.sonatype.com/en/gitlab-configuration.html',
    dataAnalyticsId: 'sonatype-developer-scm-gitlab',
  },
  {
    title: 'Bitbucket',
    imgUrl: BitbucketLogo,
    description:
      'Lifecycle pushes component intelligence into Bitbucket where developers can view and remediate policy violations with detailed Code Insights.',
    linkText: 'Click here for installation help.',
    linkUrl: 'https://help.sonatype.com/en/bitbucket-configuration.html',
    dataAnalyticsId: 'sonatype-developer-scm-bitbucket',
  },
  {
    title: 'Bitbucket Cloud',
    imgUrl: BitbucketLogo,
    description:
      'Lifecycle pushes component intelligence into Bitbucket Cloud where developers can view and remediate policy violations with detailed Code Insights.',
    linkText: 'Click here for installation help.',
    linkUrl: 'https://help.sonatype.com/en/bitbucket-cloud-configuration.html',
    dataAnalyticsId: 'sonatype-developer-scm-bitbucket-cloud',
  },
  {
    title: 'Azure DevOps',
    imgUrl: AzureDevOpsLogo,
    description:
      'Lifecycle pushes component intelligence into Azure DevOps where developers can view and remediate policy violations with detailed Code Insights.',
    linkText: 'Click here for installation help.',
    linkUrl: 'https://help.sonatype.com/en/azure-devops-configuration.html ',
    dataAnalyticsId: 'sonatype-developer-scm-azure-devops',
  },
];

export default function Scm() {
  return (
    <div id="iq-integrations-scm-section">
      <NxH2>SCM Integrations</NxH2>
      <NxP className="iq-integrations__full-width-text">
        To use Nexus IQ for SCM, your IQ Server must be configured to allow access to your company&apos;s SCM platform.
        To begin, you&apos;ll need to connect Nexus IQ to your SCM system repositories, which is best done by following
        the steps in Easy SCM Onboarding. Following that, you will need to set up your source control configuration.
      </NxP>
      <NxCard.Container className="iq-integrations-card-container">
        {scmIntegrations.map(({ title, imgUrl, description, linkText, linkUrl, dataAnalyticsId }) => (
          <IntegrationsCard
            key={title}
            title={title}
            imgUrl={imgUrl}
            description={description}
            linkText={linkText}
            linkUrl={linkUrl}
            dataAnalyticsId={dataAnalyticsId}
          />
        ))}
      </NxCard.Container>
    </div>
  );
}
