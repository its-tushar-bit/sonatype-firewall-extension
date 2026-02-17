/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

const MAX_PREVIEW_ITEMS = 3;

export const getChildCount = (value) => {
  if (Array.isArray(value)) return value.length;
  if (typeof value === 'object' && value !== null) return Object.keys(value).length;
  return 0;
};

export const createJsonNode = (key, value, path) => {
  const isObject = typeof value === 'object' && value !== null;

  return {
    id: path,
    name: String(key),
    value: !isObject ? String(value) : null,
    rawData: isObject ? value : null,
    preview: isObject ? generateJsonPreview(value) : null,
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
    return Object.entries(rawData).map(([key, value]) => {
      const itemPath = parentPath ? `${parentPath}.${key}` : key;
      return createJsonNode(key, value, itemPath);
    });
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
  if (!componentPurl || !jsonData) return null;

  try {
    const data = typeof jsonData === 'string' ? JSON.parse(jsonData) : jsonData;

    if (data.components?.length) {
      const component = data.components.find((comp) => comp.purl === componentPurl);
      if (component) {
        return createJsonNode('component', component, 'component');
      }
    }

    if (data.packages?.length) {
      const pkg = data.packages.find((p) =>
        p.externalRefs?.some((ref) => ref.referenceType === 'purl' && ref.referenceLocator === componentPurl)
      );
      if (pkg) {
        return createJsonNode('package', pkg, 'package');
      }
    }

    return null;
  } catch (e) {
    return null;
  }
};
