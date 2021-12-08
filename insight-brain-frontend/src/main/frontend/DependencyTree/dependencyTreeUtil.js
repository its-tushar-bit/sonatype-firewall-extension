/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { mapIndexed, isNilOrEmpty } from 'MainRoot/util/jsUtil';
import { serializeComponentIdentifier } from '../util/componentIdentifierUtils';

export const extendDependencyTreeData = (dependencyTreeData, indexedEntries, treePath = []) => {
  const formatedData = mapIndexed((child, index) => {
    if (!child.componentIdentifier) return null;

    const matcher = indexedEntries[serializeComponentIdentifier(child.componentIdentifier)];

    const newTreePath = [...treePath, index];
    const childData = {
      children: child.children ? extendDependencyTreeData(child, indexedEntries, [...newTreePath, 'children']) : null,
      isOpen: true,
      treePath: newTreePath,
      hash: matcher.hash,
      policyThreatLevel: matcher.policyThreatLevel,
      displayName: matcher.derivedComponentName,
    };

    return childData;
  }, dependencyTreeData.children);

  return formatedData;
};

export const filterDependencyTree = (dependencies, entries) => {
  if (!dependencies?.children) return null;

  const filtered = dependencies.children.reduce(
    (acc, node) => {
      if (isNilOrEmpty(node.componentIdentifier) || !entries[serializeComponentIdentifier(node.componentIdentifier)]) {
        return acc;
      }
      if (node.children) {
        node = { ...node, ...filterDependencyTree(node, entries) };
      }

      return [...acc, node];
    },
    [],
    dependencies.children
  );

  return { children: filtered };
};
