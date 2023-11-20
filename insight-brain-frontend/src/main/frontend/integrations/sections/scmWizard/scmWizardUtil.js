/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

export const getSCMProviderTokenUrl = (scmProvider) => {
  switch (scmProvider.toLowerCase()) {
    case 'github':
      return 'https://github.com/settings/tokens';
    case 'gitlab':
      return 'https://gitlab.com/-/profile/personal_access_tokens';
    case 'azure devops':
      return 'https://dev.azure.com/{organization}/_usersSettings/tokens';
    case 'bitbucket':
      return 'https://bitbucket.org/{workspace_name}/{repository_name}/admin/access-tokens';
    default:
      return '';
  }
};

export const getSCMProviderTokenDocUrl = (scmProvider) => {
  switch (scmProvider.toLowerCase()) {
    case 'github':
      return 'https://docs.github.com/en/authentication/keeping-your-account-and-data-secure/managing-your-personal-access-tokens';
    case 'gitlab':
      return 'https://docs.gitlab.com/ee/user/profile/personal_access_tokens.html';
    case 'azure devops':
      return 'https://docs.microsoft.com/en-us/azure/devops/organizations/accounts/use-personal-access-tokens-to-authenticate?view=azure-devops';
    case 'bitbucket':
      return 'https://support.atlassian.com/bitbucket-cloud/docs/app-passwords/';
    default:
      return '';
  }
};

export const formatSCMProvider = (scmProvider) => {
  switch (scmProvider.toLowerCase()) {
    case 'github':
      return 'GitHub';
    case 'gitlab':
      return 'GitLab';
    case 'azure devops':
      return 'Azure DevOps';
    case 'bitbucket':
      return 'Bitbucket';
    default:
      return scmProvider;
  }
};
