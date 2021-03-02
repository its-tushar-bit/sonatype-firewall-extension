/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

const GIT_HOST_NAMES = {
  'github': 'GitHub',
  'bitbucket': 'Bitbucket',
  'gitlab': 'GitLab'
};

const displayName = (provider) => {
  if (provider in GIT_HOST_NAMES) {
    return GIT_HOST_NAMES[provider];
  }
  return provider;
};

export {
  displayName
};
