/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
export const getProviderTypesMap = () => {
  let providerTypesMap = {};
  providerTypes.forEach(function (providerType) {
    providerTypesMap[providerType.value] = providerType.name;
  });

  return providerTypesMap;
};

const providerTypes = [
  { name: 'Azure DevOps', value: 'azure' },
  { name: 'Bitbucket', value: 'bitbucket' },
  { name: 'GitHub', value: 'github' },
  { name: 'GitLab', value: 'gitlab' },
];
