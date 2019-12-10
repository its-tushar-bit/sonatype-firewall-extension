import { always, groupBy, map, pipe, prop, without } from 'ramda';

import { serializeComponentIdentifier } from '../util/componentIdentifierUtils';
import { isNilOrEmpty } from '../util/jsUtil';

const toComponentIdentifierKey = pipe(prop('componentIdentifier'), serializeComponentIdentifier);

const emptyDependencyInfoGenerator = {
  getDependencyInfo: always(null)
};

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

  const allDepIds = map(toComponentIdentifierKey, other),
      directDepIds = new Set(map(toComponentIdentifierKey, root[0].children)),
      transitiveDepIds = new Set(without(directDepIds, allDepIds));

  return {
    getDependencyInfo: ({ componentIdentifier }) => {
      if (!componentIdentifier) {
        return null;
      }

      const key = serializeComponentIdentifier(componentIdentifier);

      return directDepIds.has(key) ? { isDirectDependency: true } :
        transitiveDepIds.has(key) ? { isDirectDependency: false } :
          null;
    }
  };
}
