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
