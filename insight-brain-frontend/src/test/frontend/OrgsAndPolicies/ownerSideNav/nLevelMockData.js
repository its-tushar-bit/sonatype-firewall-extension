/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

const generateNLevelOrganizations = (
  ownersMap,
  depth = 1,
  currentParentOrganizationId = 'ROOT_ORGANIZATION_ID',
  currentDepth = 0
) => {
  if (currentDepth === depth) {
    return ownersMap;
  }

  const depthText = currentDepth + 1;
  const newOrganization = {
    id: `organization id ${depthText}`,
    name: `organization name ${depthText}`,
    type: 'organization',
    parentOrganizationId: currentParentOrganizationId,
    organizationIds: [],
    subOrgs: 3,
    totalApps: 2,
    synthetic: false,
  };

  ownersMap[newOrganization.id] = newOrganization;
  ownersMap[currentParentOrganizationId].organizationIds.push(newOrganization.id);

  const applications = new Array(depth).fill().map((_, index) => {
    const applicationDepthText = index + 1;
    return {
      id: `application id ${applicationDepthText} at organization ${depthText}`,
      publicId: `application publicId ${applicationDepthText} at organization ${depthText}`,
      type: 'application',
      organizationId: newOrganization.id,
      name: `application name ${applicationDepthText} at organization ${depthText}`,
    };
  });
  newOrganization.applicationIds = applications.map((app) => app.publicId);
  applications.forEach((app) => {
    ownersMap[app.publicId] = app;
  });

  return generateNLevelOrganizations(ownersMap, depth, newOrganization.id, currentDepth + 1);
};

/**
 * @param {number} depth - the number of organization levels and applications per organization
 * @returns {rootOrganization}
 */
export const getOwnersMap = (depth = 1, includeRepositoriesHierarchy = true) => {
  const ownersMap = {
    ROOT_ORGANIZATION_ID: {
      id: 'ROOT_ORGANIZATION_ID',
      name: 'ROOT_ORGANIZATION_NAME',
      type: 'organization',
      organizationIds: [],
      repositoryContainerId: 'REPOSITORY_CONTAINER_ID',
      // Root Organization is not allow to have applications
    },
  };

  if (includeRepositoriesHierarchy) {
    ownersMap.REPOSITORY_CONTAINER_ID = {
      id: 'REPOSITORY_CONTAINER_ID',
      parentId: 'ROOT_ORGANIZATION_ID',
      name: 'Repository Managers',
      type: 'repository_container',
      repositoryManagers: ['repository manager 1', 'repository manager 2'],
    };
    ownersMap['repository manager 1'] = {
      id: 'repository manager 1',
      parentId: 'REPOSITORY_CONTAINER_ID',
      name: 'repository manager 1',
      type: 'repository_manager',
      repositoryIds: ['repository 1'],
    };
    ownersMap['repository manager 2'] = {
      id: 'repository manager 2',
      parentId: 'REPOSITORY_CONTAINER_ID',
      name: 'repository manager 2',
      type: 'repository_manager',
      repositoryIds: [],
    };
    ownersMap['repository 1'] = {
      id: 'repository 1',
      parentId: 'repository manager 1',
      name: 'repository 1',
      type: 'repository',
      repositoryIds: [],
    };
  }

  return generateNLevelOrganizations(ownersMap, depth);
};
