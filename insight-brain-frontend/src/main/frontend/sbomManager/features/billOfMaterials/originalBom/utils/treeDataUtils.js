/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { expandJsonChildren, getJsonTotalDescendants } from './jsonTreeUtils';
import { isXmlContent, parseXmlToTree, expandXmlChildren, getXmlTotalDescendants } from './xmlTreeUtils';
import { AUTO_EXPAND_THRESHOLD } from './constants';

/**
 * Recursively expands all JSON nodes in a tree
 * Used for auto-expanding small JSON SBOMs
 */
const expandAllJsonNodes = (nodes) => {
  if (!nodes || nodes.length === 0) {
    return nodes;
  }

  return nodes.map((node) => {
    if (!node.rawData) {
      return node;
    }

    const expandedNode = { ...node, isOpen: true };

    // Expand JSON children
    expandedNode.children = expandJsonChildren(node.rawData, node.id);
    if (!expandedNode.children || expandedNode.children.length === 0) {
      return node;
    }

    // Recursively expand grandchildren
    expandedNode.children = expandAllJsonNodes(expandedNode.children);

    return expandedNode;
  });
};

/**
 * Recursively expands all XML nodes in a tree
 * Used for auto-expanding small XML SBOMs
 */
const expandAllXmlNodes = (nodes) => {
  if (!nodes || nodes.length === 0) {
    return nodes;
  }

  return nodes.map((node) => {
    if (!node.xmlNode) {
      return node;
    }

    const expandedNode = { ...node, isOpen: true };

    // Expand XML children
    expandedNode.children = expandXmlChildren(node.xmlNode, node.id);
    if (!expandedNode.children || expandedNode.children.length === 0) {
      return node;
    }

    // Recursively expand grandchildren
    expandedNode.children = expandAllXmlNodes(expandedNode.children);

    return expandedNode;
  });
};

export const processRawDataToTree = (data) => {
  const isXml = typeof data === 'string' && isXmlContent(data);
  const tree = isXml ? parseXmlToTree(data) : expandJsonChildren(data, '');

  let treeData = tree;
  let isAutoExpanded = false;

  if (tree.length > 0) {
    if (isXml && tree[0].xmlNode) {
      // Check if tree is small enough to auto-expand
      // tree.length accounts for root node, getXmlTotalDescendants counts all descendants
      const totalNodes = tree.length + getXmlTotalDescendants(tree[0]);
      const shouldAutoExpand = totalNodes <= AUTO_EXPAND_THRESHOLD;

      if (shouldAutoExpand) {
        // Auto-expand all nodes
        treeData = expandAllXmlNodes(tree);
        isAutoExpanded = true;
      } else {
        // Tree too large - just expand the root node
        tree[0].isOpen = true;
        tree[0].children = expandXmlChildren(tree[0].xmlNode, tree[0].id);
      }
    } else if (!isXml) {
      // Check if tree is small enough to auto-expand
      // Count all nodes in the tree
      const totalNodes = tree.reduce((sum, node) => sum + 1 + getJsonTotalDescendants(node), 0);
      const shouldAutoExpand = totalNodes <= AUTO_EXPAND_THRESHOLD;

      if (shouldAutoExpand) {
        // Auto-expand all nodes
        treeData = expandAllJsonNodes(tree);
        isAutoExpanded = true;
      } else {
        // Tree too large - expand children for all top-level nodes that have rawData
        tree.forEach((node) => {
          if (node.rawData) {
            node.isOpen = true;
            node.children = expandJsonChildren(node.rawData, node.id);
          }
        });
      }
    }
  }

  return { treeData, isXml, rawData: data, isAutoExpanded };
};
