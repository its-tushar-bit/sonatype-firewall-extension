/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

import { propEq, sort, prop, pipe, descend } from 'ramda';

import { mapIndexed, isNilOrEmpty } from 'MainRoot/util/jsUtil';
import { serializeComponentIdentifier } from '../util/componentIdentifierUtils';

export const extendDependencyTreeData = (dependencyTreeData, indexedEntries, treePath = []) => {
  const getComponentMatch = (node) => indexedEntries[serializeComponentIdentifier(node?.componentIdentifier)];
  const sortByPolicyThreatLevel = sort(descend(pipe(getComponentMatch, prop('policyThreatLevel'))));

  const formatedData = mapIndexed((child, index) => {
    if (!child.componentIdentifier) return null;

    const matcher = getComponentMatch(child);

    const newTreePath = [...treePath, index];
    const childData = {
      children: child.children ? extendDependencyTreeData(child, indexedEntries, [...newTreePath, 'children']) : null,
      isOpen: true,
      treePath: newTreePath,
      hash: matcher.hash,
      policyThreatLevel: matcher.policyThreatLevel,
      displayName: matcher.derivedComponentName,
      isInnerSource: matcher.innerSource && matcher.directDependency,
    };

    return childData;
  }, sortByPolicyThreatLevel(dependencyTreeData.children));

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

/**
 * Updates each dependency tree item and its children's 'treePath' property
 * to reflect their current position in the tree's hierarchy
 * @param dependencyTree object where each tree item's children may contain a subtree of the same shape.
 * @param treePath an array of values that alternate between a number and the string "children". This property will used for updating the appropriate dependency tree item.
 * Example Value: [0, "children", 3]
 */
const updateTreePath = (dependencyTree, treePath = []) =>
  dependencyTree.map((treeItem, index) => {
    const newTreePath = [...treePath, index];
    return {
      ...treeItem,
      children: treeItem.children ? updateTreePath(treeItem.children, [...newTreePath, 'children']) : null,
      treePath: newTreePath,
    };
  });

export const getDependencyTreeSubset = (dependencyTree, hash) => {
  if (isNilOrEmpty(dependencyTree) || !hash) {
    return [];
  }

  const hasMatchingHash = propEq('hash', hash);
  const reduceByHash = (tree) =>
    tree.reduce((acc, node) => {
      if (hasMatchingHash(node)) {
        return [...acc, node];
      }

      const filteredChildren = node.children && reduceByHash(node.children);
      const hasMatchInChildren = !isNilOrEmpty(filteredChildren);

      if (!hasMatchInChildren) {
        return acc;
      }

      return [...acc, { ...node, children: filteredChildren }];
    }, []);

  const subset = reduceByHash(dependencyTree);

  return updateTreePath(subset);
};
