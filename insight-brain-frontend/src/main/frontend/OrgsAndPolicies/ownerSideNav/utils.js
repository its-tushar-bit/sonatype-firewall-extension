/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import Fuse from 'fuse.js';
import { ascend, prop, sortWith, toUpper, isEmpty, map, pipe } from 'ramda';

export const sortOwnerByName = (listOrMapper) => {
  const byName = pipe(prop('name'), toUpper);
  if (Array.isArray(listOrMapper)) return sortWith([ascend(byName)])(listOrMapper);

  const byNameFromMappedItem = pipe(listOrMapper, byName);
  if (typeof listOrMapper === 'function') return sortWith([ascend(byNameFromMappedItem)]);
  return [];
};

// original FuseFilter is in MainRoot/utility/filters/fuzzy.filter.js
// this implementation has no angular dependencies
export const fuzzyFilter = (input, term, searchField, resultField) => {
  if (!input || isEmpty(input) || !term || !searchField) {
    return input;
  }

  const fuse = new Fuse(input, {
    keys: [searchField],
    threshold: 0.1,
    ignoreLocation: true,
  });

  // If provided, we need to extract `resultField` from the filtered items
  // fuse.js used to do this in older versions via its `id` config option but not anymore.
  const selectorFn = ({ item }) => (resultField ? prop(resultField, item) : item);
  return map(selectorFn, fuse.search(term));
};

/**
 * Flat recursive organizations list
 * @param {[Object]} ownersMap - owners map
 * @param {Object} res - object to be returned { applications: [], organizations: [] }
 * @returns separate flat lists of organizations and applications
 */
export const flatEntries = (ownersMap, res) => {
  res = res || { organizations: [], applications: [], repositories: [], repositoryManagers: [] };

  const isApplication = (owner) => owner.type === 'application';
  const isOrganization = (owner) => owner.type === 'organization';
  const isRepository = (owner) => owner.type === 'repository';
  const isRepositoryManager = (owner) => owner.type === 'repository_manager';

  for (const ownerId in ownersMap) {
    const owner = ownersMap[ownerId];
    if (isApplication(owner)) res.applications.push(owner);
    if (isRepositoryManager(owner)) res.repositoryManagers.push(owner);
    if (isOrganization(owner)) res.organizations.push(owner);
    if (isRepository(owner)) res.repositories.push(owner);
  }

  return res;
};

export const getOwnerInfo = (owner) => {
  const { id } = owner || {};

  if (owner.type === 'repository') return ['parentId', { repositoryId: id }];
  if (owner.type === 'repository_manager') return ['parentId', { repositoryManagerId: id }];
  if (owner.type === 'repository_container') return ['parentId', { repositoryContainerId: id }];
  return ['parentOrganizationId', { organizationId: id }];
};
