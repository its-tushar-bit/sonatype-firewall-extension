/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { always, flatten, indexBy, isNil, groupBy, map, pipe, prop, reduce, reject } from 'ramda';

import { serializeComponentIdentifier } from '../util/componentIdentifierUtils';
import { isNilOrEmpty, lookup, setToArray } from '../util/jsUtil';

const toComponentIdentifierKey = pipe(prop('componentIdentifier'), serializeComponentIdentifier);

const emptyDependencyInfoGenerator = {
  getDependencyInfo: always(null)
};

// given componentId, get all its dependencies from indexedDependencyNodes
const getAllDependenciesFromNodes = indexedDependencyNodes => componentId => {
  const getNextDependenciesLayer = pipe(
      map(pipe(serializeComponentIdentifier, lookup(indexedDependencyNodes), prop('children'))),
      flatten,
      reject(isNil),
      map(prop('componentIdentifier'))
  );
  let dependencies = [],
      dependenciesLayer = [componentId];
  while (!isNilOrEmpty(dependenciesLayer)) {
    dependenciesLayer = getNextDependenciesLayer(dependenciesLayer);
    dependencies = [...dependencies, ...dependenciesLayer];
  }
  return dependencies;
};

// creates reducer of children into rootAncestorsByChild map for given rootAncestorId
const getRootAncestorsByChildReducer = rootAncestorId => (acc, child) => {
  const childKey = serializeComponentIdentifier(child),
      rootAncestors = acc[childKey];
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

  const directDeps = root[0].children,
      directDepIds = new Set(map(toComponentIdentifierKey, directDeps)),
      indexedDependencyNodes = indexBy(toComponentIdentifierKey, other),
      getAllDependencies = getAllDependenciesFromNodes(indexedDependencyNodes),
      // map rootAncestor componentIds to [componentId, children] pairs
      pairWithDependencies = map(
          pipe(prop('componentIdentifier'), componentId => [componentId, getAllDependencies(componentId)])
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
