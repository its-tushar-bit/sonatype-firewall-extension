/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

import { MAX_SEARCH_DEPTH } from './constants';

const MAX_PREVIEW_ITEMS = 3;

export const getChildCount = (value) => {
  if (Array.isArray(value)) return value.length;
  if (typeof value === 'object' && value !== null) return Object.keys(value).length;
  return 0;
};

/**
 * Recursively counts all descendants in a JSON structure
 * This counts all properties, array items, and nested objects/arrays
 * Used to determine if auto-expand is safe (won't cause performance issues)
 */
const countJsonDescendants = (value, depth = 0) => {
  if (depth >= MAX_SEARCH_DEPTH || value === null || value === undefined) {
    return 0;
  }

  // Primitives (strings, numbers, booleans) don't have descendants
  if (typeof value !== 'object') {
    return 0;
  }

  let count = 0;

  if (Array.isArray(value)) {
    // Count each array item
    count += value.length;
    // Recursively count descendants of each array item
    for (let i = 0; i < value.length; i++) {
      if (typeof value[i] === 'object' && value[i] !== null) {
        count += countJsonDescendants(value[i], depth + 1);
      }
    }
  } else {
    // Count each object property
    const keys = Object.keys(value);
    count += keys.length;
    // Recursively count descendants of each property
    for (const key of keys) {
      if (typeof value[key] === 'object' && value[key] !== null) {
        count += countJsonDescendants(value[key], depth + 1);
      }
    }
  }

  return count;
};

/**
 * Gets total descendant count for a JSON node
 * Returns 0 if node has no rawData
 */
export const getJsonTotalDescendants = (node) => {
  if (!node || !node.rawData) {
    return 0;
  }
  return countJsonDescendants(node.rawData, 0);
};

/**
 * Extracts an intelligent display name from an object by looking for identifying fields.
 * Priority order: name@version → purl → name → bom-ref → SPDXID → spdxId → id → type → ref
 * Fallback: serializes first 50 characters of the object with ellipsis
 */
const getObjectDisplayName = (obj, fallbackName) => {
  if (!obj || typeof obj !== 'object') {
    return fallbackName;
  }

  // Check for name@version combination first (CycloneDX format)
  // SPDX uses versionInfo instead of version
  if (obj.name && (obj.version || obj.versionInfo)) {
    const name = String(obj.name).trim();
    const version = String(obj.version || obj.versionInfo).trim();
    if (name && version) {
      return `${name}@${version}`;
    }
  }

  // Priority order for identifying fields
  const identifyingFields = ['purl', 'name', 'bom-ref', 'SPDXID', 'spdxId', 'id', 'type', 'ref'];

  for (const field of identifyingFields) {
    if (obj[field] !== undefined && obj[field] !== null) {
      const fieldValue = String(obj[field]);
      if (fieldValue.trim()) {
        return fieldValue;
      }
    }
  }

  // Fallback to the provided fallback name (typically numeric index for array items)
  return fallbackName;
};

export const createJsonNode = (key, value, path) => {
  const isObject = typeof value === 'object' && value !== null;

  // For objects/arrays, try to get a more intelligent display name
  let displayName = String(key);
  if (isObject && typeof key === 'number') {
    // This is an array item with numeric index - try to find better name
    displayName = getObjectDisplayName(value, String(key));
  }

  const preview = isObject ? generateJsonPreview(value) : null;

  return {
    id: path,
    name: displayName,
    value: !isObject ? String(value) : null,
    rawData: isObject ? value : null,
    preview,
  };
};

export const expandJsonChildren = (rawData, parentPath) => {
  if (Array.isArray(rawData)) {
    return rawData.map((item, index) => {
      const itemPath = `${parentPath}[${index}]`;
      return createJsonNode(index, item, itemPath);
    });
  }

  if (typeof rawData === 'object' && rawData !== null) {
    const children = Object.entries(rawData).map(([key, value]) => {
      const itemPath = parentPath ? `${parentPath}.${key}` : key;
      return createJsonNode(key, value, itemPath);
    });

    // Sort children by priority order
    const priorityOrder = ['purl', 'name', 'bom-ref', 'SPDXID', 'spdxId', 'id', 'type', 'ref'];
    children.sort((a, b) => {
      const aIndex = priorityOrder.indexOf(a.name);
      const bIndex = priorityOrder.indexOf(b.name);

      // If both are in priority list, sort by priority
      if (aIndex !== -1 && bIndex !== -1) {
        return aIndex - bIndex;
      }
      // If only a is in priority list, it comes first
      if (aIndex !== -1) return -1;
      // If only b is in priority list, it comes first
      if (bIndex !== -1) return 1;
      // Neither in priority list, maintain original order
      return 0;
    });

    return children;
  }

  return [];
};

export const generateJsonPreview = (rawData) => {
  if (!rawData || typeof rawData !== 'object') return '';

  try {
    const previewParts = [];
    let count = 0;
    let hasMore = false;

    // Use for-in loop to avoid creating full array of keys
    for (const key in rawData) {
      if (rawData.hasOwnProperty(key)) {
        if (count < MAX_PREVIEW_ITEMS) {
          const val = rawData[key];

          let valStr;
          if (val && typeof val === 'object') {
            valStr = Array.isArray(val) ? '[…]' : '{…}';
          } else {
            valStr = typeof val === 'string' ? `"${val}"` : String(val);
          }

          previewParts.push(Array.isArray(rawData) ? valStr : `${key}: ${valStr}`);
          count++;
        } else {
          // Found more than MAX_PREVIEW_ITEMS
          hasMore = true;
          break;
        }
      }
    }

    return previewParts.join(', ') + (hasMore ? ', …' : '');
  } catch (e) {
    console.error('Error generating JSON preview:', e);
    return '{…}';
  }
};

// Find and extract a specific component by PURL from SBOM JSON
// Supports both CycloneDX (components array) and SPDX (packages array)
export const findComponentInJson = (jsonData, componentPurl) => {
  console.log('[findComponentInJson] Searching for purl:', componentPurl);
  if (!componentPurl || !jsonData) return null;

  try {
    const data = typeof jsonData === 'string' ? JSON.parse(jsonData) : jsonData;

    if (data.components?.length) {
      console.log('[findComponentInJson] Checking components array, length:', data.components.length);
      const component = data.components.find((comp) => comp.purl === componentPurl);
      if (component) {
        console.log('[findComponentInJson] Found component:', component);
        const node = createJsonNode('component', component, 'component');
        console.log('[findComponentInJson] Created node:', node);
        return node;
      }
    }

    if (data.packages?.length) {
      console.log('[findComponentInJson] Checking packages array, length:', data.packages.length);
      const pkg = data.packages.find((p) =>
        p.externalRefs?.some((ref) => ref.referenceType === 'purl' && ref.referenceLocator === componentPurl)
      );
      if (pkg) {
        console.log('[findComponentInJson] Found package:', pkg);
        const node = createJsonNode('package', pkg, 'package');
        console.log('[findComponentInJson] Created node:', node);
        return node;
      }
    }

    console.log('[findComponentInJson] Component not found');
    return null;
  } catch (e) {
    console.error('[findComponentInJson] Error:', e);
    return null;
  }
};
