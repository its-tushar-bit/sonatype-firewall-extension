/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */
import { MAX_SEARCH_DEPTH } from './constants';
import { expandJsonChildren } from './jsonTreeUtils';
import { expandXmlChildren } from './xmlTreeUtils';

/**
 * Returns highlight data for matching text within a string.
 * Returns either the original text (string) if no match, or an object with parts to highlight.
 *
 * @param {string|number|null|undefined} text - The text to search within
 * @param {string} searchTerm - The term to highlight (case-insensitive)
 * @returns {string|Object} The original text or { before, match, after } for highlighting
 */
export const getHighlightParts = (text, searchTerm) => {
  if (text === null || text === undefined) return '';
  if (!searchTerm) return text;

  const textStr = String(text);
  if (textStr === '') return '';

  const term = searchTerm.toLowerCase();
  const index = textStr.toLowerCase().indexOf(term);

  if (index === -1) return textStr;

  return {
    before: textStr.substring(0, index),
    match: textStr.substring(index, index + term.length),
    after: textStr.substring(index + term.length),
  };
};

/**
 * Checks if a node matches the search term in its name or value.
 *
 * @param {Object} node - The tree node to check
 * @param {string} term - The search term (case-insensitive, already lowercased)
 * @returns {boolean} True if the node name or value contains the search term
 */
export const nodeMatchesSearch = (node, term) => {
  if (!term) return true;
  if (node.name && String(node.name).toLowerCase().includes(term)) return true;
  if (node.value && String(node.value).toLowerCase().includes(term)) return true;
  return false;
};

/**
 * Recursively filters a single tree node and its descendants based on search term.
 * Expands collapsed nodes temporarily to search within them, but limits depth
 * to prevent UI freezes on large SBOMs (50K+ components).
 *
 * @param {Object} node - The tree node to filter
 * @param {string} term - The search term (lowercased)
 * @param {number} depth - Current recursion depth (used to limit expansion)
 * @returns {Object|null} Filtered node with matching descendants, or null if no matches
 */
const filterSingleNode = (node, term, depth = 0) => {
  if (!node) return null;

  if (depth > MAX_SEARCH_DEPTH * 2) return null;

  const currentMatches = nodeMatchesSearch(node, term);

  let filteredChildren = null;
  let hasMatchingDescendants = false;

  let childrenToSearch = node.children;

  if (!childrenToSearch && (node.rawData || node.xmlNode) && depth < MAX_SEARCH_DEPTH) {
    try {
      if (node.xmlNode) {
        childrenToSearch = expandXmlChildren(node.xmlNode, node.id);
      } else if (node.rawData) {
        childrenToSearch = expandJsonChildren(node.rawData, node.id);
      }
    } catch (error) {
      console.warn('Failed to expand node during search:', error);
      childrenToSearch = null;
    }
  }

  if (childrenToSearch && childrenToSearch.length > 0) {
    filteredChildren = childrenToSearch.map((child) => filterSingleNode(child, term, depth + 1)).filter(Boolean);
    hasMatchingDescendants = filteredChildren.length > 0;
    if (!hasMatchingDescendants) {
      filteredChildren = null;
    }
  }

  if (currentMatches || hasMatchingDescendants) {
    return {
      ...node,
      matchesSearch: currentMatches,
      hasMatchingDescendants,
      children: filteredChildren,
    };
  }

  return null;
};

/**
 * Counts total matching nodes recursively in the tree.
 *
 * @param {Array} nodes - Array of tree nodes
 * @returns {number} Total count of nodes with matchesSearch=true
 */
export const countMatchingNodes = (nodes) => {
  if (!nodes || nodes.length === 0) return 0;

  let count = 0;
  for (const node of nodes) {
    if (node.matchesSearch) {
      count++;
    }
    if (node.children && node.children.length > 0) {
      count += countMatchingNodes(node.children);
    }
  }
  return count;
};

/**
 * Filters an array of tree nodes based on a search term.
 * Returns only nodes that match or have matching descendants.
 * Expands collapsed nodes temporarily during search to find matches within them.
 *
 * @param {Array} nodes - Array of tree nodes to filter
 * @param {string} searchTerm - The search term (case-insensitive)
 * @returns {Array} Filtered array of matching nodes
 */
export const filterTreeNodes = (nodes, searchTerm) => {
  if (!nodes || nodes.length === 0 || !searchTerm) {
    return nodes ? [...nodes] : nodes;
  }

  const term = searchTerm.toLowerCase();
  return nodes.map((node) => filterSingleNode(node, term)).filter(Boolean);
};
