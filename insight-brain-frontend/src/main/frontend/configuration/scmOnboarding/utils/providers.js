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
  return compositeDto === null ? null : compositeDto.value !== null ? compositeDto.value : compositeDto.parentValue;
}

function tokenForOrg(org) {
  if (!org) {
    return null;
  }
  // if the selected org has a custom provider, then any tokens set at earlier levels should be disregarded.
  // They were created by a different provider and so can't be shared. We must use the org's token,
  // even if it is null.
  return org.sourceControl.provider.value ? org.sourceControl.token.value : valueFromHierarchy(org.sourceControl.token);
}

export { displayName, valueFromHierarchy, tokenForOrg };
