/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

import React from 'react';
import ScmWizard from 'MainRoot/development/developmentDashboard/sections/scmWizard/ScmWizard';
import AzureWizard from 'MainRoot/development/developmentDashboard/sections/CiCdWizards/Azure/AzureWizard';
import BambooWizard from 'MainRoot/development/developmentDashboard/sections/CiCdWizards/Bamboo/BambooWizard';
import JenkinsWizard from 'MainRoot/development/developmentDashboard/sections/CiCdWizards/Jenkins/JenkinsWizard';
import GitlabWizard from 'MainRoot/development/developmentDashboard/sections/CiCdWizards/GitLab/GitlabWizard';

export const createTabConfiguration = (name, analyticsId, component) => ({
  name,
  analyticsId,
  component,
});

const scmProviders = ['GitHub', 'Bitbucket', 'Azure DevOps', 'GitLab'];

export const createScmTabs = (applicationPublicId) => {
  return scmProviders.map((provider) => {
    return createTabConfiguration(
      provider,
      `sonatype-developer-scm-configuration-wizard-tab-${provider}`,
      <ScmWizard applicationPublicId={applicationPublicId} scmProvider={provider} />
    );
  });
};

export const createCiCdTabs = (applicationPublicId, organizationId) => {
  return [
    createTabConfiguration(
      'Jenkins',
      'sonatype-developer-cicd-configuration-wizard-tab-jenkins',
      <JenkinsWizard iqOrganization={organizationId} iqApplication={applicationPublicId} />
    ),
    createTabConfiguration(
      'Azure',
      'sonatype-developer-cicd-configuration-wizard-tab-azure',
      <AzureWizard iqOrganization={organizationId} iqApplication={applicationPublicId} />
    ),
    createTabConfiguration(
      'Bamboo',
      'sonatype-developer-cicd-configuration-wizard-tab-bamboo',
      <BambooWizard iqOrganization={organizationId} iqApplication={applicationPublicId} />
    ),
    createTabConfiguration(
      'GitLab CLI',
      'sonatype-developer-cicd-configuration-wizard-tab-gitlab-cli',
      <GitlabWizard iqApplication={applicationPublicId} />
    ),
  ];
};
