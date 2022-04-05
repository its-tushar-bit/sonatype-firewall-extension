/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { always, map, reduce } from 'ramda';

import { serializeComponentIdentifier } from '../util/componentIdentifierUtils';
import { getKey, setToArray } from 'MainRoot/util/jsUtil';
import { flattenModuleDirectDependencies } from 'MainRoot/DependencyTree/dependencyTreeUtil';

const emptyDependencyInfoGenerator = {
  getDependencyInfo: always(null),
};

export const populateDependencyNodeKeys = (node) => ({
  ...node,
  key: node.componentIdentifier && serializeComponentIdentifier(node.componentIdentifier),
  children: node.children && map(populateDependencyNodeKeys, node.children),
});

// creates reducer of children into rootAncestorsByChild map for given rootAncestorId
const getRootAncestorsByChildReducer = (rootAncestorId) => (acc, childKey) => {
  const rootAncestors = acc[childKey];

  if (rootAncestors) {
    rootAncestors.add(rootAncestorId);
  } else {
    acc[childKey] = new Set([rootAncestorId]);
  }

  return acc;
};

const getAllChildrenKeys = (acc, node) => {
  if (!node.children) {
    return acc;
  }

  return reduce(
    (acc, child) => {
      return getAllChildrenKeys([...acc, child.key], child);
    },
    acc,
    node.children
  );
};

export default function DependencyInfoGenerator(dependencies) {
  const dependencyTree = dependencies?.dependencyTree?.children;
  if (!dependencyTree) {
    return emptyDependencyInfoGenerator;
  }

  const flattenDependencyTree = flattenModuleDirectDependencies(dependencies.dependencyTree);

  const dependencyTreeWithKeys = map(populateDependencyNodeKeys, flattenDependencyTree.children);

  const directDepIds = new Set(map(getKey, dependencyTreeWithKeys));

  // generate rootAncestorsByChild map
  // where key is child key, and value is a Set of its unique rootAncestor componentIds
  const mapRootAncestorsToChildren = reduce((acc, directDependency) => {
    const childrenKeys = getAllChildrenKeys([], directDependency);
    return reduce(getRootAncestorsByChildReducer(directDependency.componentIdentifier), acc, childrenKeys);
  }, {});

  const rootAncestorsSetByChild = mapRootAncestorsToChildren(dependencyTreeWithKeys);
  const rootAncestorsByChild = map(setToArray, rootAncestorsSetByChild);

  return {
    getDependencyInfo: ({ componentIdentifier }) => {
      if (!componentIdentifier) {
        return null;
      }

      const key = serializeComponentIdentifier(componentIdentifier);
      const rootAncestors = rootAncestorsByChild[key];

      return directDepIds.has(key)
        ? { isDirectDependency: true }
        : rootAncestors
        ? { isDirectDependency: false, rootAncestors }
        : null;
    },
  };
}
