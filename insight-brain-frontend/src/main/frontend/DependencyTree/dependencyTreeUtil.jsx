/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import React from 'react';
import { propEq, sort, prop, pipe, descend, curry, contains, toLower, split, match } from 'ramda';

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
      originalTreePath: newTreePath,
      hash: matcher.hash,
      policyThreatLevel: matcher.policyThreatLevel,
      displayName: matcher.derivedComponentName,
      isInnerSource: matcher.innerSource && matcher.directDependency,
    };

    return childData;
  }, sortByPolicyThreatLevel(dependencyTreeData.children));

  return formatedData;
};

export const deepReduce = curry((callback, initialValue, tree) =>
  tree.reduce((acc, node) => {
    if (isNilOrEmpty(node.children)) return callback(acc, node);
    return deepReduce(callback, callback(acc, node), node.children);
  }, initialValue)
);

const filterDependencyTreeBy = curry((predicate, tree) =>
  tree.reduce((dependencies, node) => {
    const isCurrentNodeSatisfyingPredicate = predicate(node);
    if (isCurrentNodeSatisfyingPredicate) return [...dependencies, node];

    const filteredChildren = node.children && filterDependencyTreeBy(predicate, node.children);
    const hasWantedNodesWithinChildren = !isNilOrEmpty(filteredChildren);
    if (hasWantedNodesWithinChildren) return [...dependencies, { ...node, children: filteredChildren }];

    return dependencies;
  }, [])
);

export const flattenModuleDirectDependencies = (dependencyTree) => {
  const newChildren = [];
  dependencyTree.children?.forEach((child) => {
    if (child.module) {
      if (child.children) {
        child.children.forEach((grandchild) => {
          newChildren.push(grandchild);
        });
      }
    } else {
      newChildren.push(child);
    }
  });
  return { ...dependencyTree, children: newChildren };
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
      const isDirect = node.treePath.length < 3;
      if (hasMatchingHash(node)) {
        return [...acc, { ...node, isOpen: isDirect }];
      }

      const filteredChildren = node.children && reduceByHash(node.children);
      const hasMatchInChildren = !isNilOrEmpty(filteredChildren);

      if (!hasMatchInChildren) {
        return acc;
      }

      return [...acc, { ...node, children: filteredChildren, isOpen: true }];
    }, []);

  const subset = reduceByHash(dependencyTree);

  return updateTreePath(subset);
};

export const filterDependencyTreeBySearchTerm = (dependencyTree, searchTerm) => {
  if (isNilOrEmpty(dependencyTree) || !searchTerm) return [];

  const componentNameContainsSearchTerm = pipe(prop('displayName'), toLower, contains(toLower(searchTerm)));
  const filteredTree = filterDependencyTreeBy(componentNameContainsSearchTerm)(dependencyTree);

  return updateTreePath(filteredTree);
};

export const isFlatDependencyTree = (dependencyTree) => {
  if (isNilOrEmpty(dependencyTree)) return true;

  const hasNoChildren = pipe(prop('children'), isNilOrEmpty);

  return dependencyTree.every(hasNoChildren);
};

/**
 * used to highlight text when searching in a filter, returns the displayName styled as a react component, it is case insensitive
 * @param {string} displayName - The original text to display
 * @param {string} searchTerm - The text to highlight
 * @param {string} className - Optional classname to style
 * @returns object
 */
export const renderDisplayName = (displayName, searchTerm, className = '') => {
  if (!searchTerm) return displayName;
  const searchRegex = new RegExp(searchTerm, 'gi');
  const textArr = split(searchRegex, displayName);
  const matchArr = match(searchRegex, displayName);
  matchArr.forEach((matchedText, index) => {
    textArr.splice(
      index * 2 + 1,
      0,
      <mark key={`renderDisplayName${matchedText}${index}`} className={className}>
        {matchedText}
      </mark>
    );
  });
  return textArr;
};
