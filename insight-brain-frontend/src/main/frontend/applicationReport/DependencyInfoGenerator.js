/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { always, indexBy, groupBy, map, pipe, prop, reduce } from 'ramda';

import { serializeComponentIdentifier } from '../util/componentIdentifierUtils';
import { isNilOrEmpty, setToArray } from '../util/jsUtil';

const getKey = prop('key');

const emptyDependencyInfoGenerator = {
  getDependencyInfo: always(null)
};

// given serialized component id of a component, get all its dependencies from indexedDependencyNodes
function getAllDependenciesFromNodes(indexedDependencyNodes) {
  // imperative for performance CLM-15122
  function getNextDependenciesLayer(currentLayer) {
    const retval = new Set();

    for (const key of currentLayer) {
      const childDependencies = indexedDependencyNodes[key].children || [];

      for (const dependency of childDependencies) {
        if (dependency) {
          retval.add(getKey(dependency));
        }
      }
    }

    return retval;
  }

  return function(parentKey) {
    let dependencies = new Set(),
        dependenciesLayer = new Set([parentKey]);
    while (dependenciesLayer && dependenciesLayer.size) {
      dependenciesLayer = getNextDependenciesLayer(dependenciesLayer);
      for (const layer of dependenciesLayer) {
        dependencies.add(layer);
      }
    }
    return dependencies;
  };
}

// creates reducer of children into rootAncestorsByChild map for given rootAncestorId
const getRootAncestorsByChildReducer = rootAncestorId => (acc, childKey) => {
  const rootAncestors = acc[childKey];

  if (rootAncestors) {
    rootAncestors.add(rootAncestorId);
  }
  else {
    acc[childKey] = new Set([rootAncestorId]);
  }

  return acc;
};

// given list of [rootAncestorId, children] pairs, generate rootAncestorsByChild map
// where key is child iD, and value is a Set of its unique rootAncestorIds
const mapRootAncestorsToChildren = reduce((acc, [rootAncestorId, children]) => {
  return reduce(getRootAncestorsByChildReducer(rootAncestorId), acc, children);
}, {});

const populateDependencyNodeKeys = node => ({
  ...node,
  key: node.componentIdentifier && serializeComponentIdentifier(node.componentIdentifier),
  children: node.children && map(populateDependencyNodeKeys, node.children)
});

export default function DependencyInfoGenerator(dependencies) {
  if (!dependencies || isNilOrEmpty(dependencies.dependencyGraph)) {
    return emptyDependencyInfoGenerator;
  }

  // an object containing two lists: a list containing only the root dependency node (under the 'root' prop)
  // and a list containing all other top-level dependency nodes (under the 'other') prop
  const {root, other} = groupBy(({ componentIdentifier }) => componentIdentifier ? 'other' : 'root',
      dependencies.dependencyGraph);

  if (isNilOrEmpty(root) || isNilOrEmpty(other)) {
    return emptyDependencyInfoGenerator;
  }

  const dependencyNodesWithKeys = map(populateDependencyNodeKeys, other),
      directDeps = populateDependencyNodeKeys(root[0]).children,
      directDepIds = new Set(map(getKey, directDeps)),
      indexedDependencyNodes = indexBy(getKey, dependencyNodesWithKeys),
      getAllDependencies = getAllDependenciesFromNodes(indexedDependencyNodes),
      // map rootAncestors to [componentId, childrenKeyArray] pairs
      pairWithDependencies = map(
          ({ componentIdentifier, key }) => [componentIdentifier, getAllDependencies(key)]
      ),
      rootAncestorsToArray = map(rootAncestorsSet => setToArray(rootAncestorsSet)),
      rootAncestorsByChild = pipe(pairWithDependencies, mapRootAncestorsToChildren, rootAncestorsToArray)(directDeps);

  return {
    getDependencyInfo: ({ componentIdentifier }) => {
      if (!componentIdentifier) {
        return null;
      }

      const key = serializeComponentIdentifier(componentIdentifier);
      const rootAncestors = rootAncestorsByChild[key];

      return directDepIds.has(key) ? { isDirectDependency: true } :
        rootAncestors ? { isDirectDependency: false, rootAncestors } :
          null;
    }
  };
}
