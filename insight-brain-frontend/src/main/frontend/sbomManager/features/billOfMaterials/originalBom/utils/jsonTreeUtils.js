/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

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

// Find and extract a specific component by PURL from SBOM JSON
// Supports both CycloneDX (components array) and SPDX (packages array)
export const findComponentInJson = (jsonData, componentPurl) => {
  if (!componentPurl || !jsonData) return null;

  try {
    const data = typeof jsonData === 'string' ? JSON.parse(jsonData) : jsonData;

    // Search in components array (CycloneDX format) - direct purl match
    if (data.components?.length) {
      const component = data.components.find((comp) => comp.purl === componentPurl);
      if (component) {
        return createJsonNode('component', component, 'component');
      }
    }

    // Search in packages array (SPDX format) - check externalRefs for purl
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
    console.error('Error finding component in JSON:', e);
    return null;
  }
};
