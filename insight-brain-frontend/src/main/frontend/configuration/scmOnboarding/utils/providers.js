/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

const GIT_HOST_NAMES = {
  github: 'GitHub',
  bitbucket: 'Bitbucket',
  gitlab: 'GitLab',
  azure: 'Azure DevOps',
};

const displayName = (provider) => {
  if (provider in GIT_HOST_NAMES) {
    return GIT_HOST_NAMES[provider];
  }
  return provider;
};

function valueFromHierarchy(compositeDto) {
  return compositeDto == null ? null : compositeDto.value !== null ? compositeDto.value : compositeDto.parentValue;
}

function gitHubAppFromEntries(githubApps, localOnly) {
  if (!githubApps) {
    return null;
  }

  const entries = Array.isArray(githubApps) ? githubApps : [githubApps];
  const app = entries
    .map((entry) => (localOnly ? entry?.value : entry?.value ?? entry?.parentValue))
    .find((githubApp) => githubApp?.installationId);

  return app ?? null;
}

function gitHubAppFromSourceControl(sourceControl, localOnly) {
  const pluralGitHubApp = gitHubAppFromEntries(sourceControl.githubApps, localOnly);
  if (pluralGitHubApp) {
    return pluralGitHubApp;
  }

  return localOnly ? sourceControl.githubApp?.value : valueFromHierarchy(sourceControl.githubApp);
}

function tokenForOrg(org) {
  const authMethod = getAuthMethodForOrg(org);
  return authMethod === 'PAT' ? 'token' : authMethod;
}

function getAuthMethodForOrg(org) {
  if (!org || !org.sourceControl) {
    return null;
  }

  // Determine effective authentication type (value or inherited)
  const authType = org.sourceControl.provider.value
    ? org.sourceControl.authenticationType?.value
    : valueFromHierarchy(org.sourceControl.authenticationType);

  // Check GitHub App authentication
  if (authType === 'GITHUB_APP') {
    const githubApp = gitHubAppFromSourceControl(org.sourceControl, !!org.sourceControl.provider.value);

    // Valid if GitHub App has installationId
    return githubApp?.installationId ? 'GITHUB_APP' : null;
  }

  // Check PAT authentication
  const token = org.sourceControl.provider.value
    ? org.sourceControl.token?.value
    : valueFromHierarchy(org.sourceControl.token);

  return token ? 'PAT' : null;
}

function hasAuth(org) {
  return !!getAuthMethodForOrg(org);
}

export { displayName, valueFromHierarchy, tokenForOrg, getAuthMethodForOrg, hasAuth };
