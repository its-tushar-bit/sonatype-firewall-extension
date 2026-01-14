/*
 * Copyright (c) 2011-present Sonatype, Inc. All rights reserved.
 * Includes the third-party code listed at http://links.sonatype.com/products/clm/attributions.
 * "Sonatype" is a trademark of Sonatype, Inc.
 */

export const isXmlContent = (content) => {
  if (typeof content !== 'string') return false;
  const trimmed = content.trim();
  return trimmed.startsWith('<');
};

const createXmlElementNode = (node, path, elementIndex = null) => {
  const nodePath =
    elementIndex !== null
      ? `${path}.${node.nodeName}[${elementIndex}]`
      : path
      ? `${path}.${node.nodeName}`
      : node.nodeName;

  // Check if this is a simple text node (no attributes, single text child)
  const hasAttributes = node.attributes && node.attributes.length > 0;
  const isSingleTextNode =
    !hasAttributes && node.childNodes.length === 1 && node.childNodes[0].nodeType === Node.TEXT_NODE;

  if (isSingleTextNode) {
    return {
      id: nodePath,
      name: node.nodeName,
      value: node.childNodes[0].textContent?.trim(),
      xmlNode: null,
    };
  }

  return {
    id: nodePath,
    name: node.nodeName,
    value: null,
    xmlNode: node,
  };
};

const createXmlTextNode = (path, text, index) => ({
  id: `${path}_text_${index}`,
  name: text,
  value: null,
  xmlNode: null,
});

export const parseXmlToTree = (xmlString) => {
  try {
    const parser = new DOMParser();
    const xmlDoc = parser.parseFromString(xmlString, 'text/xml');
    const root = createXmlElementNode(xmlDoc.documentElement, '');
    return root ? [root] : [];
  } catch (e) {
    console.error('Error parsing XML:', e);
    return [];
  }
};

const createAttributeNode = (attr, parentPath) => ({
  id: `${parentPath}@${attr.name}`,
  name: `@${attr.name}`,
  value: attr.value,
  xmlNode: null,
});

export const expandXmlChildren = (xmlNode, parentPath) => {
  const children = [];
  const elementNameCounts = {};
  let textIndex = 0;

  if (xmlNode.attributes) {
    for (let i = 0; i < xmlNode.attributes.length; i++) {
      children.push(createAttributeNode(xmlNode.attributes[i], parentPath));
    }
  }

  for (let i = 0; i < xmlNode.childNodes.length; i++) {
    const child = xmlNode.childNodes[i];

    if (child.nodeType === Node.TEXT_NODE) {
      const text = child.textContent?.trim();
      if (text) {
        children.push(createXmlTextNode(parentPath, text, textIndex++));
      }
    } else if (child.nodeType === Node.ELEMENT_NODE) {
      const elementName = child.nodeName;
      elementNameCounts[elementName] = (elementNameCounts[elementName] || 0) + 1;
      const elementIndex = elementNameCounts[elementName] - 1;
      children.push(createXmlElementNode(child, parentPath, elementIndex));
    }
  }

  return children;
};
